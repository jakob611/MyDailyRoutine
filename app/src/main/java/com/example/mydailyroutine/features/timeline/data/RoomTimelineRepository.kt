package com.example.mydailyroutine.features.timeline.data

import androidx.room.withTransaction
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.core.database.daos.*
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.health.*
import com.example.mydailyroutine.domain.repository.TimelineRepository
import com.example.mydailyroutine.domain.repository.TimelineResolver
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomTimelineRepository(
    private val db: RoutineDatabase,
    private val onChanged: () -> Unit,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : TimelineRepository {
    private val resolver = TimelineResolver()

    override fun getTimelineForDate(date: LocalDate): Flow<List<ResolvedTimelineItem>> =
        observeSnapshot(date, date).map { snapshot ->
            withContext(Dispatchers.Default) { resolver.resolve(date, snapshot) }
        }.distinctUntilChanged()

    override fun observeSnapshot(from: LocalDate, through: LocalDate): Flow<ScheduleSnapshot> =
        db.invalidationTracker.createFlow(
            "subjects", "routine_blocks", "event_overrides", "school_calendar", "milestones", "routine_completions", "spaced_reviews",
            emitInitialState = true,
        ).map { snapshot(from, through) }.distinctUntilChanged()

    override suspend fun snapshot(from: LocalDate, through: LocalDate): ScheduleSnapshot = withContext(io) {
        require(through >= from && ChronoUnit.DAYS.between(from, through) <= 731) { "Choose at most two years." }
        val firstOrigin = from.minusDays((MAX_OCCURRENCE_SHIFT_DAYS + 8).toLong())
        val weekdays = (0L..minOf(6L, ChronoUnit.DAYS.between(firstOrigin, through)))
            .map { firstOrigin.plusDays(it).dayOfWeek }
        db.withTransaction {
            ScheduleSnapshot(
                from = from,
                through = through,
                subjects = db.subjects().getAll().map { it.domain() },
                routines = db.routines().candidates(weekdays, firstOrigin, through).map { it.domain() },
                overrides = db.overrides().inRange(firstOrigin, through).map { it.domain() },
                calendar = db.calendar().inRange(firstOrigin, through).map { it.domain() },
                milestones = db.milestones().inRange(from, through).map { it.domain() },
                completions = db.completions().inRange(firstOrigin, through).map { OccurrenceCompletion(it.routineBlockId, it.date, it.actualMinutes,
                    it.actualStartEpochMinute?.let { minute -> java.time.LocalDateTime.ofEpochSecond(minute * 60, 0, java.time.ZoneOffset.UTC) }) },
                reviews = db.learning().reviews(firstOrigin.toEpochDay(), through.toEpochDay()).map { it.domain() },
            )
        }
    }

    private suspend fun <T> write(block: suspend () -> T): T = withContext(io) {
        db.withTransaction {
            // Queue refresh before returning to a potentially cancelled UI coroutine. The refresh's
            // own transaction waits for this commit, so it never observes a half-written mutation.
            block().also { onChanged() }
        }
    }

    override suspend fun saveSubject(subject: Subject): Long = write {
        val clean = subject.copy(name = subject.name.trim())
        ScheduleValidation.subject(clean)
        if (clean.id == 0L) db.subjects().insert(clean.entity()) else {
            check(db.subjects().update(clean.entity()) == 1) { "This subject was deleted." }
            clean.id
        }
    }

    override suspend fun deleteSubject(id: Long) = write { db.subjects().delete(id) }

    override suspend fun saveRoutine(routine: RoutineBlueprint): Long = write {
        val duration = nominalMinutes(routine.startTime, routine.endTime)
        val clean = if (routine.isFixedCommitment || routine.category == RoutineCategory.SCHOOL) routine.copy(
            minDurationMinutes = duration, elasticity = 0.0, isFixedCommitment = true) else routine
        saveRoutineInTransaction(clean.copy(title = clean.title.trim()))
    }

    private suspend fun saveRoutineInTransaction(routine: RoutineBlueprint): Long {
        ScheduleValidation.routine(routine)
        // Reject a blueprint edit that would invalidate a previously saved date-specific exception.
        db.overrides().forRoutine(routine.id).forEach { ScheduleValidation.exception(it.domain(), routine) }
        return if (routine.id == 0L) db.routines().insert(routine.entity()) else {
            check(db.routines().update(routine.entity()) == 1) { "This routine was deleted." }
            routine.id
        }
    }

    override suspend fun deleteRoutine(id: Long) = write { db.routines().delete(id) }

    override suspend fun setNotificationEnabled(routineId: Long, enabled: Boolean) = write {
        check(db.routines().setNotificationEnabled(routineId, enabled) == 1) { "This routine was deleted." }
    }

    private suspend fun routine(id: Long): RoutineBlueprint =
        checkNotNull(db.routines().get(id)) { "This routine was deleted." }.domain()

    private suspend fun saveOverrideInTransaction(override: EventOverride) {
        val base = routine(override.routineBlockId)
        val clean = override.copy(customTitle = override.customTitle?.trim())
        ScheduleValidation.exception(clean, base)
        val existing = db.overrides().get(clean.routineBlockId, clean.overrideDate)
        // Never use REPLACE: it deletes the old row and can cascade into related tables.
        if (existing == null) db.overrides().insert(clean.copy(id = 0).entity())
        else check(db.overrides().update(clean.copy(id = existing.id).entity()) == 1)
    }

    override suspend fun saveOverride(override: EventOverride) = write { saveOverrideInTransaction(override) }

    override suspend fun cancelOccurrence(routineId: Long, date: LocalDate) = write {
        val existing = db.overrides().get(routineId, date)?.domain() ?: EventOverride(routineBlockId = routineId, overrideDate = date)
        saveOverrideInTransaction(existing.copy(isCancelled = true, cancellationReason = CancellationReason.MANUAL))
    }

    override suspend fun restoreOccurrence(routineId: Long, date: LocalDate) = write {
        db.overrides().get(routineId, date)?.let { saveOverrideInTransaction(it.domain().copy(isCancelled = false)) }
        db.backlog().deleteOccurrence(routineId, date)
        Unit
    }

    override suspend fun resetOverride(routineId: Long, date: LocalDate) = write { db.overrides().delete(routineId, date) }

    override suspend fun editBlock(
        routineId: Long, date: LocalDate, title: String, start: LocalTime, end: LocalTime, wholeTemplate: Boolean,
    ) = write {
        if (wholeTemplate) {
            val base = routine(routineId)
            val duration = nominalMinutes(start, end)
            saveRoutineInTransaction(base.copy(title = title.trim(), startTime = start, endTime = end,
                minDurationMinutes = if (base.isFixedCommitment) duration else minOf(base.minDurationMinutes, duration), rawDurationMinutes = duration))
        } else {
            val previous = db.overrides().get(routineId, date)?.domain()
                ?: EventOverride(routineBlockId = routineId, overrideDate = date)
            saveOverrideInTransaction(previous.copy(customTitle = title.trim(), customStartTime = start, customEndTime = end))
        }
        Unit
    }

    override suspend fun setCompleted(routineId: Long, date: LocalDate, completed: Boolean, actualMinutes: Int?, actualStartedAt: java.time.LocalDateTime?) = write {
        val base = routine(routineId)
        require(base.occursOn(date)) { "This routine does not occur on that date." }
        require(actualMinutes == null || actualMinutes in 1..10080)
        if (completed) {
            val row = RoutineCompletionEntity(routineId, date, actualMinutes, actualStartedAt?.toEpochSecond(java.time.ZoneOffset.UTC)?.div(60))
            if (db.completions().get(routineId, date) == null) db.completions().insert(row) else db.completions().update(row)
            if (actualMinutes != null && base.subjectId != null && base.category.isDeepWork) {
                val previous = db.learning().sample(routineId, date)
                val exception = db.overrides().get(routineId, date)
                val explicitDuration = exception?.let { nominalMinutes(it.customStartTime ?: base.startTime, it.customEndTime ?: base.endTime) }
                val rawEstimate = if (exception?.cancellationReason == CancellationReason.MANUAL && explicitDuration != null &&
                    explicitDuration != nominalMinutes(base.startTime, base.endTime)) explicitDuration else base.rawDurationMinutes
                val sample = HistoricalVelocityEntity(previous?.id ?: 0, base.subjectId, rawEstimate,
                    actualMinutes, java.time.Instant.now().toEpochMilli(), routineId, date)
                if (previous == null) db.learning().insertSample(sample) else db.learning().updateSample(sample)
            }
        } else {
            db.completions().delete(routineId, date)
            db.learning().deleteSample(routineId, date)
        }
        if (base.validFrom == base.validUntil && base.validFrom != null) {
            db.routines().update(base.copy(completedActualMinutes = if (completed) actualMinutes else null).entity())
        }
        db.learning().setReviewCompleted(routineId, completed)
        Unit
    }

    override suspend fun saveMilestone(milestone: Milestone): Long = write {
        val clean = milestone.copy(title = milestone.title.trim())
        ScheduleValidation.milestone(clean)
        if (clean.id == 0L) db.milestones().insert(clean.entity()) else {
            check(db.milestones().update(clean.entity()) == 1) { "This milestone was deleted." }
            clean.id
        }
    }

    override suspend fun setMilestoneCompleted(id: Long, completed: Boolean) = write {
        check(db.milestones().setCompleted(id, completed) == 1) { "This milestone was deleted." }
    }
    override suspend fun deleteMilestone(id: Long) = write { db.milestones().delete(id) }

    override suspend fun insertRecovery(
        date: LocalDate, type: WarningType, anchorKey: String, config: HealthConfig,
        recoveryTitle: String, continuationSuffix: String,
    ): RecoveryResult = write {
        val data = snapshot(date.minusDays(1), date.plusDays(1))
        val resolved = resolver.prepare(data)
        val today = resolved.forDate(date)
        val warning = ScheduleHealthEngine().evaluate(today, config).firstOrNull {
            it.type == type && anchorKey in it.relatedItemKeys
        } ?: return@write RecoveryResult(RecoveryStatus.ALREADY_HANDLED)
        val items = resolved.forDate(date.minusDays(1)) + today + resolved.forDate(date.plusDays(1))
        when (val decision = RecoveryPlanner().plan(date, items, warning, config)) {
            RecoveryDecision.AlreadyHandled -> RecoveryResult(RecoveryStatus.ALREADY_HANDLED)
            RecoveryDecision.NoSpace -> RecoveryResult(RecoveryStatus.NO_SPACE)
            is RecoveryDecision.Insert -> {
                val plan = decision.plan
                plan.change?.let { change ->
                    val previous = db.overrides().get(change.original.routineBlockId, change.original.occurrenceDate)?.domain()
                        ?: EventOverride(routineBlockId = change.original.routineBlockId, overrideDate = change.original.occurrenceDate)
                    saveOverrideInTransaction(previous.copy(customStartTime = change.start.toLocalTime(), customEndTime = change.end.toLocalTime()))
                }
                plan.continuation?.let { part ->
                    saveRoutineInTransaction(RoutineBlueprint(
                        subjectId = part.original.subject?.id,
                        title = "${part.original.title.take((119 - continuationSuffix.length).coerceAtLeast(1))} $continuationSuffix",
                        category = part.original.category, dayOfWeek = part.start.dayOfWeek,
                        startTime = part.start.toLocalTime(), endTime = part.end.toLocalTime(),
                        isNotificationEnabled = part.original.isNotificationEnabled,
                        validFrom = part.start.toLocalDate(), validUntil = part.start.toLocalDate(),
                    ))
                }
                saveRoutineInTransaction(RoutineBlueprint(
                    subjectId = null, title = recoveryTitle, category = RoutineCategory.REST_BUFFER,
                    dayOfWeek = plan.start.dayOfWeek, startTime = plan.start.toLocalTime(), endTime = plan.end.toLocalTime(),
                    isNotificationEnabled = true, validFrom = plan.start.toLocalDate(), validUntil = plan.start.toLocalDate(),
                ))
                RecoveryResult(when {
                    plan.continuation != null -> RecoveryStatus.FOCUS_SPLIT
                    plan.change != null -> RecoveryStatus.FOCUS_MOVED
                    else -> RecoveryStatus.INSERTED
                }, plan.minutes, plan.start.toLocalDate())
            }
        }
    }

}
