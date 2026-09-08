package com.example.mydailyroutine.features.timeline.data

import androidx.room.withTransaction
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.core.database.daos.*
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.routines.RoutineOrigin
import com.example.mydailyroutine.domain.health.*
import com.example.mydailyroutine.domain.planning.BacklogEntry
import com.example.mydailyroutine.domain.planning.BacklogReason
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
                    it.actualStartEpochMinute?.let { minute -> java.time.LocalDateTime.ofEpochSecond(minute * 60, 0, java.time.ZoneOffset.UTC) },
                    if (it.actualStartedAtEpochMillis != null && it.actualEndedAtEpochMillis != null && it.actualZoneId != null)
                        ActualTiming(java.time.Instant.ofEpochMilli(it.actualStartedAtEpochMillis), java.time.Instant.ofEpochMilli(it.actualEndedAtEpochMillis), it.actualZoneId) else null) },
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
        if (routine.id != 0L) {
            requireNotRunning(routine.id)
            require(this.routine(routine.id).origin == RoutineOrigin.USER) { "Managed patterns are changed through their settings" }
        }
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

    private suspend fun requireNotRunning(id: Long, date: LocalDate? = null) {
        val active = db.execution().get()
        if (active?.routineBlockId == id && (date == null || active.occurrenceDate == date)) throw ScheduleConflict(ScheduleConflictReason.ACTIVE_EXECUTION)
    }
    override suspend fun deleteRoutine(id: Long) = write {
        requireNotRunning(id)
        require(routine(id).origin != RoutineOrigin.SLEEP && routine(id).origin != RoutineOrigin.MORNING_BUFFER) { "Use sleep settings" }
        db.routines().delete(id)
    }

    override suspend fun setNotificationEnabled(routineId: Long, enabled: Boolean) = write {
        require(routine(routineId).origin != RoutineOrigin.SLEEP && routine(routineId).origin != RoutineOrigin.MORNING_BUFFER) { "A sleep plan is not an alarm clock" }
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
        synchronizeReview(base, clean.overrideDate, clean)
    }
    private suspend fun synchronizeReview(base: RoutineBlueprint, origin: LocalDate, exception: EventOverride?) {
        db.learning().reviewForBlock(base.id)?.let { review ->
            val duration = nominalMinutes(exception?.customStartTime ?: base.startTime, exception?.customEndTime ?: base.endTime)
            db.learning().updateReview(review.copy(scheduledEpochDay = origin.plusDays((exception?.dayShift ?: 0).toLong()).toEpochDay(), durationMinutes = duration))
        }
    }

    override suspend fun saveOverride(override: EventOverride) = write { saveOverrideInTransaction(override) }

    override suspend fun cancelOccurrence(routineId: Long, date: LocalDate) = write {
        requireNotRunning(routineId,date)
        val existing = db.overrides().get(routineId, date)?.domain() ?: EventOverride(routineBlockId = routineId, overrideDate = date)
        saveOverrideInTransaction(existing.copy(isCancelled = true, cancellationReason = CancellationReason.MANUAL))
    }

    override suspend fun restoreOccurrence(routineId: Long, date: LocalDate) = write {
        db.overrides().get(routineId, date)?.let { saveOverrideInTransaction(it.domain().copy(isCancelled = false)) }
        db.backlog().deleteOccurrence(routineId, date)
        Unit
    }

    override suspend fun resetOverride(routineId: Long, date: LocalDate) = write {
        requireNotRunning(routineId,date)
        db.overrides().delete(routineId,date)
        db.backlog().deleteOccurrence(routineId,date)
        synchronizeReview(routine(routineId),date,null)
    }

    override suspend fun editBlock(
        routineId: Long, date: LocalDate, title: String, start: LocalTime, end: LocalTime, wholeTemplate: Boolean,
    ) = write {
        requireNotRunning(routineId,date)
        require(routine(routineId).origin == RoutineOrigin.USER) { "A linked block follows its parent" }
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

    override suspend fun setCompleted(routineId: Long, date: LocalDate, completed: Boolean, actualMinutes: Int?, actualStartedAt: java.time.LocalDateTime?, actualTiming: ActualTiming?) = write {
        val base = routine(routineId)
        require(base.origin == RoutineOrigin.USER) { "Managed sleep and breaks are plans, not measured work" }
        require(base.occursOn(date)) { "This routine does not occur on that date." }
        require(actualMinutes == null || actualMinutes in 1..10080)
        if (completed) {
            val row = RoutineCompletionEntity(routineId, date, actualMinutes, actualStartedAt?.toEpochSecond(java.time.ZoneOffset.UTC)?.div(60),
                actualTiming?.startedAt?.toEpochMilli(), actualTiming?.endedAt?.toEpochMilli(), actualTiming?.zoneId)
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
            val previous = checkNotNull(db.milestones().get(clean.id)).domain()
            check(db.milestones().update(clean.entity()) == 1) { "This milestone was deleted." }
            if (previous.dueDate != clean.dueDate || previous.dueTime != clean.dueTime || previous.isExam != clean.isExam || previous.isTerminalExam != clean.isTerminalExam)
                reconcileChangedDeadline(clean)
            clean.id
        }
    }

    private suspend fun reconcileChangedDeadline(goal: Milestone) {
        val cutoff = goal.dueTime?.let { goal.dueDate.atTime(it) } ?: goal.dueDate.plusDays(1).atStartOfDay()
        for (entity in db.routines().linkedToMilestone(goal.id)) {
            val origin = entity.validFrom ?: continue
            if (entity.validUntil != origin || entity.isFixedCommitment || entity.category == RoutineCategory.SCHOOL || entity.category == RoutineCategory.REST_BUFFER) continue
            if (db.completions().get(entity.id,origin) != null) continue // Observed history is not rewritten.
            val exception = db.overrides().get(entity.id,origin)?.domain()
                ?: EventOverride(routineBlockId=entity.id,overrideDate=origin)
            if (exception.isCancelled) continue
            val base = entity.domain()
            val start = origin.plusDays(exception.dayShift.toLong()).atTime(exception.customStartTime ?: base.startTime)
            val duration = nominalMinutes(exception.customStartTime ?: base.startTime,exception.customEndTime ?: base.endTime)
            val review = db.learning().reviewForBlock(entity.id)
            val reviewTooLate = review != null && (goal.isExam || goal.isTerminalExam) && start.toLocalDate() >= goal.dueDate
            if (start.plusMinutes(duration.toLong()) <= cutoff && !reviewTooLate) continue
            requireNotRunning(entity.id,origin)
            val reserve = entity.category == RoutineCategory.EMERGENCY_RESERVE
            saveOverrideInTransaction(exception.copy(isCancelled=true,cancellationReason=if(reserve) CancellationReason.BUFFER_CONSUMED else CancellationReason.BACKLOG))
            if (!reserve) db.backlog().insert(BacklogEntry(title=exception.customTitle ?: entity.title,category=entity.category,
                durationMinutes=duration,minDurationMinutes=minOf(entity.minDurationMinutes,duration),elasticity=entity.elasticity,
                priorityWeight=entity.priorityWeight,subjectId=entity.subjectId,sourceRoutineId=entity.id,occurrenceDate=origin,
                milestoneId=goal.id,topicId=entity.topicId,reviewId=review?.id,reason=BacklogReason.CAPACITY,
                rawDurationMinutes=entity.rawDurationMinutes,stageOrder=entity.stageOrder).entity())
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
        today.filterIsInstance<ResolvedTimelineItem.Block>().firstOrNull { it.key == anchorKey }?.let { requireNotRunning(it.routineBlockId,it.occurrenceDate) }
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
