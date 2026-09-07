package com.example.mydailyroutine.data.repository

import androidx.room.withTransaction
import com.example.mydailyroutine.data.local.*
import com.example.mydailyroutine.domain.learning.*
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.planning.*
import com.example.mydailyroutine.domain.repository.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Research operations reuse the canonical CRUD/resolver and commit through the same Room database. */
class RoomPlanningRepository(private val db: RoutineDatabase, private val timeline: TimelineRepository,
    private val onChanged: () -> Unit) : PlanningRepository {
    override val backlog = db.backlog().observeAll().map { it.map { row -> row.domain() } }
    override val history = db.learning().observeHistory().map { it.map { row -> row.domain() } }
    override val topics = db.learning().observeTopics().map { it.map { row -> row.domain() } }
    override val milestones = db.milestones().observeAll().map { it.map { row -> row.domain() } }
    private val resolver = TimelineResolver()

    private suspend fun <T> transaction(operation: suspend () -> T): T = withContext(Dispatchers.IO) {
        db.withTransaction { operation().also { onChanged() } }
    }
    override suspend fun getCalibratedDuration(rawMinutes: Int, subjectId: String): Int = withContext(Dispatchers.IO) {
        val data = subjectId.toLongOrNull()?.let { db.learning().history(it).map { sample -> sample.domain() } }.orEmpty()
        VelocityCalibrator(data).getCalibratedDuration(rawMinutes, subjectId)
    }
    override suspend fun autoHeal(date: LocalDate, actualStartMinutes: Int, delayMinutes: Int, config: PlanningConfig): HealingReport = transaction {
        require(actualStartMinutes in 0..2879 && delayMinutes >= 0)
        val baseDate = if (delayMinutes > actualStartMinutes && delayMinutes <= actualStartMinutes + 1440) date.minusDays(1) else date
        val actualRelative = actualStartMinutes + if (baseDate < date) 1440 else 0
        val snapshot = timeline.snapshot(baseDate, baseDate.plusDays(1))
        val prepared = resolver.prepare(snapshot)
        val items = prepared.forDate(baseDate) + prepared.forDate(baseDate.plusDays(1))
        val blocks = items.filterIsInstance<ResolvedTimelineItem.Block>().filter { !it.isSuppressed && !it.isCompleted }.distinctBy { it.occurrenceKey }
        val byKey = blocks.associateBy { it.occurrenceKey }
        val midnight = baseDate.atStartOfDay()
        val numerical = blocks.map { block ->
            val duration = Duration.between(block.startsAt, block.endsAt).toMinutes().toInt()
            TimeBlock(block.occurrenceKey, block.title, block.category, Duration.between(midnight, block.startsAt).toMinutes().toInt(),
                duration, minOf(block.minDurationMinutes, duration), block.isFixedCommitment, block.elasticity, block.priorityWeight)
        } + items.filterIsInstance<ResolvedTimelineItem.Milestone>().filter { it.dueTime != null && (it.isExam || it.isTerminalExam) && !it.isCompleted }
            .distinctBy { it.key }.map { TimeBlock(it.key, it.title, RoutineCategory.SCHOOL,
                Duration.between(midnight, it.date.atTime(it.dueTime)).toMinutes().toInt(), 0, 0, true, 0.0, 10.0) }
        val anchor = (actualRelative.toLong() - delayMinutes).coerceAtLeast(0).toInt()
        // Preserve the actual delay even when it began before midnight; extreme deficits simply backlog work.
        val actualDelay = if (delayMinutes > actualRelative) delayMinutes else actualRelative - anchor
        val result = withContext(Dispatchers.Default) {
            DeterministicReschedulingEngine(CircadianPenalty(config.dipCenterMinutes, config.dipSigmaMinutes))
                .recover(numerical, anchor, actualDelay, maxOf(1440, numerical.maxOfOrNull { it.endMinutes } ?: 1440).coerceAtMost(2880))
        }
        val deferred = result.deferred.map { it.id }.toMutableSet()
        val remaining = result.blocks.associateBy { it.id }.toMutableMap()
        // Moving a review across midnight must not evade the 20% cap, nor overload tomorrow's study budget.
        for (day in listOf(baseDate, baseDate.plusDays(1))) {
            val scheduled = remaining.values.filter { midnight.plusMinutes(it.startMinutes.toLong()).toLocalDate() == day && it.category.isDeepWork && byKey[it.id] != null }
            var total = scheduled.sumOf { it.durationMinutes }
            var reviews = scheduled.filter { byKey.getValue(it.id).reviewId != null }.sumOf { it.durationMinutes }
            for (task in scheduled.filterNot { it.isFixed }.sortedWith(compareBy<TimeBlock> { it.priorityWeight / it.durationMinutes.coerceAtLeast(1) }.thenBy { it.id })) {
                val review = byKey.getValue(task.id).reviewId != null
                if (total > config.dailyStudyCapacityMinutes || (review && reviews > config.dailyReviewCap)) {
                    deferred += task.id; remaining.remove(task.id); total -= task.durationMinutes
                    if (review) reviews -= task.durationMinutes
                }
            }
        }
        blocks.filterNot { it.isFixedCommitment || it.category == RoutineCategory.SCHOOL }.forEach { original ->
            val previous = db.overrides().get(original.routineBlockId, original.occurrenceDate)?.domain()
                ?: EventOverride(routineBlockId = original.routineBlockId, overrideDate = original.occurrenceDate)
            val revised = remaining[original.occurrenceKey]
            when {
                original.occurrenceKey in deferred -> {
                    timeline.saveOverride(previous.copy(isCancelled = true, cancellationReason = CancellationReason.BACKLOG))
                    db.backlog().insert(BacklogEntry(title = original.title, category = original.category,
                        durationMinutes = Duration.between(original.startsAt, original.endsAt).toMinutes().toInt(),
                        minDurationMinutes = original.minDurationMinutes, elasticity = original.elasticity, priorityWeight = original.priorityWeight,
                        subjectId = original.subject?.id, sourceRoutineId = original.routineBlockId, occurrenceDate = original.occurrenceDate,
                        milestoneId = original.milestoneId, topicId = original.topicId, reviewId = original.reviewId).entity())
                }
                revised == null && original.category.isBuffer -> timeline.saveOverride(previous.copy(isCancelled = true, cancellationReason = CancellationReason.BUFFER_CONSUMED))
                revised != null -> {
                    val start = midnight.plusMinutes(revised.startMinutes.toLong())
                    val end = start.plusMinutes(revised.durationMinutes.toLong())
                    if (start != original.startsAt || end != original.endsAt) {
                        val shift = ChronoUnit.DAYS.between(original.occurrenceDate, start.toLocalDate()).toInt()
                        timeline.saveOverride(previous.copy(customStartTime = start.toLocalTime(), customEndTime = end.toLocalTime(),
                            dayShift = shift, cancellationReason = CancellationReason.AUTO_HEAL))
                        db.learning().reviewForBlock(original.routineBlockId)?.let { db.learning().updateReview(it.copy(
                            scheduledEpochDay = start.toLocalDate().toEpochDay(), durationMinutes = revised.durationMinutes)) }
                    }
                }
            }
        }
        result.report.copy(deferredCount = deferred.size, deferredMinutes = deferred.sumOf {
            byKey[it]?.let { block -> Duration.between(block.startsAt, block.endsAt).toMinutes().toInt() } ?: 0
        })
    }

    override suspend fun ensureReserve(date: LocalDate, config: PlanningConfig, title: String): Int = transaction {
        val items = resolver.resolve(date, timeline.snapshot(date, date))
        val blocks = items.filterIsInstance<ResolvedTimelineItem.Block>().filter { !it.isSuppressed && !it.isCompleted }
        val calibrators = blocks.mapNotNull { it.subject?.id }.distinct().associateWith { id -> VelocityCalibrator(db.learning().history(id).map { it.domain() }) }
        val required = RsemBufferSizer.minutes(blocks.filter { it.category.isDeepWork }.map { block ->
            val nominal = block.durationMinutes
            val multiplier = block.subject?.id?.let { (calibrators.getValue(it).conservativeMultiplier(it.toString()) / calibrators.getValue(it).multiplier(it.toString())) } ?: 1.5
            DurationEstimate(nominal, maxOf(nominal, ceil(nominal * multiplier).toInt()))
        })
        var needed = (required - blocks.filter { it.category == RoutineCategory.EMERGENCY_RESERVE }.sumOf { it.durationMinutes }).coerceAtLeast(0)
        val capacity = capacity(date, items, config)
        var added = 0
        while (needed > 0) {
            val size = minOf(60, needed)
            val start = capacity.candidate(size, RoutineCategory.EMERGENCY_RESERVE, 1.0, preferLate = true) ?: break
            createBlock(date, start, size, title, RoutineCategory.EMERGENCY_RESERVE, null, minDuration = 0)
            capacity.allocate(start, size, RoutineCategory.EMERGENCY_RESERVE); added += size; needed -= size
        }
        added
    }
    override suspend fun scheduleBacklog(id: Long, date: LocalDate, config: PlanningConfig): PlacementResult = transaction {
        val entry = db.backlog().get(id)?.domain() ?: return@transaction PlacementResult(false)
        require(date >= LocalDate.now())
        val items = resolver.resolve(date, timeline.snapshot(date, date))
        val planner = capacity(date, items, config)
        val start = planner.candidate(entry.durationMinutes, entry.category, entry.priorityWeight, review = entry.reviewId != null)
            ?: return@transaction PlacementResult(false)
        val blockId = createBlock(date, start, entry.durationMinutes, entry.title, entry.category, entry.subjectId,
            entry.milestoneId, entry.topicId, entry.minDurationMinutes, entry.elasticity, entry.priorityWeight)
        entry.reviewId?.let { reviewId -> db.learning().getReview(reviewId)?.let { review ->
            db.learning().updateReview(review.copy(timeBlockId = blockId, scheduledEpochDay = date.toEpochDay()))
        } }
        db.backlog().delete(id)
        PlacementResult(true, date)
    }
    override suspend fun deleteBacklog(id: Long) = transaction { db.backlog().delete(id) }
    override suspend fun deleteTopic(id: Long) = transaction { db.learning().deleteTopic(id) }

    override suspend fun createTopic(topic: StudyTopic, config: PlanningConfig, reviewTitlePattern: String): PlanSummary = transaction {
        ScheduleValidation.title(topic.title)
        require(topic.initialDate >= LocalDate.now() && topic.finalDate >= topic.initialDate && ChronoUnit.DAYS.between(topic.initialDate, topic.finalDate) <= 366)
        val topicId = db.learning().insertTopic(topic.entity())
        val snapshot = timeline.snapshot(topic.initialDate, topic.finalDate)
        val schedule = resolver.prepare(snapshot)
        val days = (0L..ChronoUnit.DAYS.between(topic.initialDate, topic.finalDate)).map { offset ->
            val date = topic.initialDate.plusDays(offset); capacity(date, schedule.forDate(date), config)
        }.associateBy { it.date.toEpochDay() }
        val request = ReviewRequest(topicId, topic.initialDate.toEpochDay(), topic.finalDate.toEpochDay(), topic.reviewCount, topic.reviewDurationMinutes, topic.priorityWeight)
        val plan = withContext(Dispatchers.Default) { SpacedRepetitionPlanner().plan(request, days.map { (epoch, day) ->
            ReviewDayCapacity(epoch, config.dailyStudyCapacityMinutes, day.studyMinutes, day.reviewMinutes, day.freeMinutes())
        }) }
        var placed = 0; var deferred = 0
        plan.forEach { review ->
            val title = String.format(Locale.forLanguageTag("sl"), reviewTitlePattern, topic.title.take(95), review.ordinal).take(120)
            val day = review.scheduledEpochDay?.let(days::get)
            val start = day?.candidate(review.durationMinutes, RoutineCategory.FOCUS_ANALYTICAL, topic.priorityWeight, review = true)
            val routineId = if (day != null && start != null) {
                day.allocate(start, review.durationMinutes, RoutineCategory.FOCUS_ANALYTICAL, review = true)
                placed++
                createBlock(day.date, start, review.durationMinutes, title, RoutineCategory.FOCUS_ANALYTICAL, topic.subjectId,
                    topic.milestoneId, topicId, review.durationMinutes, priority = topic.priorityWeight)
            } else null
            val reviewId = db.learning().insertReview(SpacedReviewEntity(topicId = topicId, scheduledEpochDay = day?.date?.toEpochDay() ?: review.idealEpochDay,
                durationMinutes = review.durationMinutes, priorityWeight = topic.priorityWeight, ordinal = review.ordinal, timeBlockId = routineId))
            if (routineId == null) {
                deferred += review.durationMinutes
                db.backlog().insert(BacklogEntry(title = title, category = RoutineCategory.FOCUS_ANALYTICAL, durationMinutes = review.durationMinutes,
                    minDurationMinutes = review.durationMinutes, elasticity = 1.0, priorityWeight = topic.priorityWeight,
                    subjectId = topic.subjectId, milestoneId = topic.milestoneId, topicId = topicId, reviewId = reviewId,
                    reason = BacklogReason.REVIEW_CAPACITY).entity())
            }
        }
        PlanSummary(placed * topic.reviewDurationMinutes, 0, placed, deferred)
    }

    override suspend fun planMilestone(id: Long, initialDate: LocalDate, config: PlanningConfig,
        preparationTitlePattern: String, reserveTitle: String, category: RoutineCategory): PlanSummary = transaction {
        val milestone = checkNotNull(db.milestones().get(id)).domain()
        require(category.isDeepWork)
        require(initialDate >= LocalDate.now() && milestone.dueDate >= initialDate && milestone.estimatedEffortHours > 0)
        if (db.routines().forMilestoneCount(id) + db.backlog().forMilestoneCount(id) > 0) return@transaction PlanSummary(0,0,0,0)
        val snapshot = timeline.snapshot(initialDate, milestone.dueDate)
        val schedule = resolver.prepare(snapshot)
        val days = (0L..ChronoUnit.DAYS.between(initialDate, milestone.dueDate)).map { offset ->
            val date = initialDate.plusDays(offset)
            capacity(date, schedule.forDate(date), config, if (date == milestone.dueDate) milestone.dueTime?.toSecondOfDay()?.div(60) ?: config.studyEndMinutes else config.studyEndMinutes)
        }
        val calibration = VelocityCalibrator(milestone.subjectId?.let { db.learning().history(it).map { row -> row.domain() } }.orEmpty())
        val subject = milestone.subjectId?.toString().orEmpty()
        val plan = withContext(Dispatchers.Default) { MilestoneBackPlanner().plan(initialDate, milestone.dueDate,
            milestone.estimatedEffortHours, milestone.isTerminalExam, category,
            calibration.multiplier(subject), calibration.conservativeMultiplier(subject), days, config) }
        val title = String.format(Locale.forLanguageTag("sl"), preparationTitlePattern, milestone.title.take(98)).take(120)
        plan.blocks.forEach { block -> createBlock(block.date, block.startMinutes, block.durationMinutes,
            if (block.reserve) reserveTitle else title, if (block.reserve) RoutineCategory.EMERGENCY_RESERVE else category,
            milestone.subjectId, milestone.id, minDuration = if (block.reserve) 0 else minOf(25, block.durationMinutes),
            priority = if (milestone.isTerminalExam) 8.0 else 4.0, rawMinutes = block.rawMinutes) }
        plan.unplacedChunks.forEach { minutes -> db.backlog().insert(BacklogEntry(title = title, category = category,
            durationMinutes = minutes, minDurationMinutes = minOf(25, minutes), elasticity = 1.0, priorityWeight = if (milestone.isTerminalExam) 8.0 else 4.0,
            subjectId = milestone.subjectId, milestoneId = milestone.id, reason = BacklogReason.CAPACITY).entity()) }
        PlanSummary(plan.blocks.filterNot { it.reserve }.sumOf { it.durationMinutes }, plan.blocks.filter { it.reserve }.sumOf { it.durationMinutes }, 0, plan.unplacedChunks.sum())
    }

    private fun capacity(date: LocalDate, items: List<ResolvedTimelineItem>, config: PlanningConfig, latest: Int = config.studyEndMinutes): DailyCapacityPlanner =
        DailyCapacityPlanner(date, items, config, if (date == LocalDate.now()) maxOf(config.studyStartMinutes, LocalTime.now().toSecondOfDay() / 60 + 1) else config.studyStartMinutes, latest)

    private suspend fun createBlock(date: LocalDate, start: Int, duration: Int, title: String, category: RoutineCategory,
        subjectId: Long?, milestoneId: Long? = null, topicId: Long? = null, minDuration: Int = minOf(25,duration),
        elasticity: Double = 1.0, priority: Double = 3.0, rawMinutes: Int = duration): Long = timeline.saveRoutine(RoutineBlueprint(
        subjectId = subjectId, title = title, category = category, dayOfWeek = date.dayOfWeek,
        startTime = LocalTime.ofSecondOfDay(start * 60L), endTime = LocalTime.ofSecondOfDay(start * 60L).plusMinutes(duration.toLong()),
        isNotificationEnabled = category != RoutineCategory.EMERGENCY_RESERVE, validFrom = date, validUntil = date,
        minDurationMinutes = minDuration, elasticity = elasticity, priorityWeight = priority, rawDurationMinutes = rawMinutes.coerceIn(1,1439),
        topicId = topicId, milestoneId = milestoneId,
    ))
}
