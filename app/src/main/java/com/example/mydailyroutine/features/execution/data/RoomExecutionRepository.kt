package com.example.mydailyroutine.features.execution.data

import androidx.room.withTransaction
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.core.database.daos.*
import com.example.mydailyroutine.domain.execution.*
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.planning.PlanningConfig
import com.example.mydailyroutine.domain.repository.*
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import java.time.*
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Explicit sessions survive process death. Reconciliation runs on visible UI ticks/resume, never via polling. */
class RoomExecutionRepository(private val db: RoutineDatabase, private val timeline: TimelineRepository,
    private val planning: PlanningRepository, private val onChanged: () -> Unit,
    private val clock: Clock = Clock.systemUTC()) : ExecutionRepository {
    override val active = db.execution().observe().map { it?.domain() }
    private val resolver = TimelineResolver()
    private fun ActiveExecutionEntity.domain() = ActiveExecution(routineBlockId, occurrenceDate, Instant.ofEpochMilli(startedAtEpochMillis),
        Instant.ofEpochMilli(expectedEndEpochMillis), Instant.ofEpochMilli(lastHealedEndEpochMillis), stoppedAtEpochMillis?.let(Instant::ofEpochMilli))
    private suspend fun <T> write(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        db.withTransaction { block().also { onChanged() } }
    }
    private suspend fun occurrence(id: Long, origin: LocalDate, displayDate: LocalDate): ResolvedTimelineItem.Block =
        checkNotNull(resolver.resolve(displayDate, timeline.snapshot(origin, maxOf(origin, displayDate))).filterIsInstance<ResolvedTimelineItem.Block>()
            .firstOrNull { it.routineBlockId == id && it.occurrenceDate == origin })

    override suspend fun start(routineId: Long, date: LocalDate, config: PlanningConfig, automatic: Boolean): Unit = write {
        val now = clock.instant(); val zone = ZoneId.systemDefault(); val today = now.atZone(zone).toLocalDate()
        val existing = db.execution().get()
        if (existing != null) { require(existing.routineBlockId == routineId && existing.occurrenceDate == date); return@write }
        val block = occurrence(routineId, date, today)
        require(!block.isCompleted && !block.isSuppressed && !block.isFixedCommitment && !block.category.isBuffer)
        if (block.milestoneId != null && block.stageOrder != null)
            require(!db.backlog().hasEarlierStage(block.milestoneId, block.stageOrder)) { "An earlier deliverable stage is still unscheduled" }
        val end = OccurrenceTimes.window(block, zone).end
        val peers = resolver.resolve(today, timeline.snapshot(today, today.plusDays(1))).filterIsInstance<ResolvedTimelineItem.Block>()
            .filter { it.occurrenceKey != block.occurrenceKey && !it.isSuppressed && (it.isFixedCommitment || it.category == RoutineCategory.SCHOOL || it.category == RoutineCategory.REST_BUFFER) }
        require(peers.none { val window = OccurrenceTimes.window(it, zone); now >= window.start && now < window.end }) { "A fixed commitment currently occupies this time" }
        val hard = peers.map { OccurrenceTimes.window(it, zone).start }.filter { it >= now }.minOrNull()
        val proposed = minOf(now.plusSeconds(Duration.between(block.startsAt, block.endsAt).seconds), hard ?: now.plusSeconds(24 * 3600))
        val startLocal = now.atZone(zone).toLocalDateTime().withSecond(0).withNano(0)
        val endLocal = proposed.atZone(zone).toLocalDateTime().withSecond(0).withNano(0)
        if (endLocal > startLocal && Duration.between(startLocal, endLocal).toMinutes() < 1440) {
            val previous = db.overrides().get(routineId, date)?.domain() ?: EventOverride(routineBlockId = routineId, overrideDate = date)
            timeline.saveOverride(previous.copy(customStartTime = startLocal.toLocalTime(), customEndTime = endLocal.toLocalTime(),
                dayShift = ChronoUnit.DAYS.between(date, startLocal.toLocalDate()).toInt(), cancellationReason = CancellationReason.AUTO_HEAL))
        }
        db.execution().insert(ActiveExecutionEntity(routineBlockId = routineId, occurrenceDate = date,
            startedAtEpochMillis = now.toEpochMilli(), expectedEndEpochMillis = end.toEpochMilli(), lastHealedEndEpochMillis = proposed.toEpochMilli()))
        if (automatic && proposed > end) repairAfter(end, proposed, config, block.occurrenceKey)
    }
    override suspend fun synchronize(config: PlanningConfig, automatic: Boolean): Boolean = if (!automatic) false else withContext(Dispatchers.IO) {
        db.withTransaction {
            val before = db.execution().get() ?: return@withTransaction false
            val stopped = reconcile(config)
            if (db.execution().get() != before) onChanged()
            stopped
        }
    }
    private suspend fun repairAfter(expectedEnd: Instant, actualEnd: Instant, config: PlanningConfig, excludedKey: String) {
        val zone = ZoneId.systemDefault()
        val expected = expectedEnd.atZone(zone).toLocalDateTime()
        val actual = actualEnd.atZone(zone).toLocalDateTime()
        val date = expected.toLocalDate()
        val minute = Duration.between(date.atStartOfDay(), actual).toMinutes().coerceIn(0,2879).toInt()
        val delay = Duration.between(expected, actual).toMinutes().coerceIn(0,Int.MAX_VALUE.toLong()).toInt()
        if (delay > 0) planning.autoHeal(date, minute, delay, config, excludedKey)
    }
    private suspend fun reconcile(config: PlanningConfig): Boolean {
        val row = db.execution().get() ?: return false
        if (row.stoppedAtEpochMillis != null) return false
        val now = clock.instant(); val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(row.startedAtEpochMillis)
        val base = start.atZone(zone).toLocalDate()
        val schedule = resolver.prepare(timeline.snapshot(base, base.plusDays(1)))
        val items = schedule.forDate(base) + schedule.forDate(base.plusDays(1))
        val key = "block:${row.routineBlockId}:${row.occurrenceDate}"
        val hardStarts = items.filterIsInstance<ResolvedTimelineItem.Block>().filter {
            it.occurrenceKey != key && !it.isSuppressed && (it.isFixedCommitment || it.category == RoutineCategory.SCHOOL || it.category == RoutineCategory.REST_BUFFER)
        }.map { OccurrenceTimes.window(it, zone).start } + items.filterIsInstance<ResolvedTimelineItem.Milestone>().filter { it.isExam && it.dueTime != null }
            .map { it.date.atTime(it.dueTime).atZone(zone).toInstant() }
        val hardEnd = hardStarts.filter { it >= start }.minOrNull() ?: start.plusSeconds(24 * 3600)
        val stopped = now >= hardEnd
        val end = minOf(if (stopped) hardEnd else Instant.ofEpochMilli(((now.toEpochMilli() + 59999) / 60000) * 60000), hardEnd)
        if (end.toEpochMilli() > row.lastHealedEndEpochMillis) {
            val actual = end.atZone(zone).toLocalDateTime()
            val actualStart = start.atZone(zone).toLocalDateTime().withSecond(0).withNano(0)
            if (actual > actualStart && Duration.between(actualStart, actual).toMinutes() < 1440) {
                val previous = db.overrides().get(row.routineBlockId, row.occurrenceDate)?.domain()
                    ?: EventOverride(routineBlockId = row.routineBlockId, overrideDate = row.occurrenceDate)
                timeline.saveOverride(previous.copy(customStartTime = actualStart.toLocalTime(), customEndTime = actual.toLocalTime(),
                    dayShift = ChronoUnit.DAYS.between(row.occurrenceDate, actualStart.toLocalDate()).toInt(), cancellationReason = CancellationReason.AUTO_HEAL))
            }
            repairAfter(Instant.ofEpochMilli(row.expectedEndEpochMillis), end, config, key)
        }
        if (stopped || end.toEpochMilli() > row.lastHealedEndEpochMillis) db.execution().update(row.copy(
            lastHealedEndEpochMillis = maxOf(row.lastHealedEndEpochMillis, end.toEpochMilli()),
            stoppedAtEpochMillis = if (stopped) end.toEpochMilli() else null))
        return stopped
    }
    override suspend fun finish(config: PlanningConfig, automatic: Boolean): Int = write {
        if (automatic) reconcile(config)
        val row = checkNotNull(db.execution().get())
        val value = row.domain(); val now = clock.instant(); val minutes = value.elapsedMinutes(now)
        val actualStart = value.startedAt.atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0)
        timeline.setCompleted(row.routineBlockId, row.occurrenceDate, true, minutes, actualStart)
        if (automatic) repairAfter(value.expectedEnd, actualStart.plusMinutes(minutes.toLong()).atZone(ZoneId.systemDefault()).toInstant(), config, "block:${row.routineBlockId}:${row.occurrenceDate}")
        db.execution().clear()
        minutes
    }
    override suspend fun record(block: ResolvedTimelineItem.Block, actualMinutes: Int, config: PlanningConfig, automatic: Boolean): Unit = write {
        require(actualMinutes in 1..10080)
        val session = db.execution().get()?.takeIf { it.routineBlockId == block.routineBlockId && it.occurrenceDate == block.occurrenceDate }
        val zone = ZoneId.systemDefault()
        val start = session?.let { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(zone).toLocalDateTime().withSecond(0).withNano(0) } ?: block.startsAt
        timeline.setCompleted(block.routineBlockId, block.occurrenceDate, true, actualMinutes, start)
        if (automatic) repairAfter(block.endsAt.atZone(zone).toInstant(), start.plusMinutes(actualMinutes.toLong()).atZone(zone).toInstant(), config, block.occurrenceKey)
        if (session != null) db.execution().clear()
    }
    override suspend fun cancel(): Unit = write { db.execution().clear() }
}
