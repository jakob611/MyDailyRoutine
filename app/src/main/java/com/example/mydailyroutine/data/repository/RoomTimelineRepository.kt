package com.example.mydailyroutine.data.repository

import androidx.room.withTransaction
import com.example.mydailyroutine.data.local.*
import com.example.mydailyroutine.domain.model.*
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
            "subjects", "routine_blocks", "event_overrides", "school_calendar", "milestones", "routine_completions",
            emitInitialState = true,
        ).map { snapshot(from, through) }.distinctUntilChanged()

    override suspend fun snapshot(from: LocalDate, through: LocalDate): ScheduleSnapshot = withContext(io) {
        require(through >= from && ChronoUnit.DAYS.between(from, through) <= 731) { "Choose at most two years." }
        val firstOrigin = from.minusDays(1)
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
                completions = db.completions().inRange(firstOrigin, through).map { OccurrenceCompletion(it.routineBlockId, it.date) },
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
        saveRoutineInTransaction(routine.copy(title = routine.title.trim()))
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
        saveOverrideInTransaction(existing.copy(isCancelled = true))
    }

    override suspend fun restoreOccurrence(routineId: Long, date: LocalDate) = write {
        db.overrides().get(routineId, date)?.let { saveOverrideInTransaction(it.domain().copy(isCancelled = false)) }
        Unit
    }

    override suspend fun resetOverride(routineId: Long, date: LocalDate) = write { db.overrides().delete(routineId, date) }

    override suspend fun editBlock(
        routineId: Long, date: LocalDate, title: String, start: LocalTime, end: LocalTime, wholeTemplate: Boolean,
    ) = write {
        if (wholeTemplate) {
            saveRoutineInTransaction(routine(routineId).copy(title = title.trim(), startTime = start, endTime = end))
        } else {
            val previous = db.overrides().get(routineId, date)?.domain()
                ?: EventOverride(routineBlockId = routineId, overrideDate = date)
            saveOverrideInTransaction(previous.copy(customTitle = title.trim(), customStartTime = start, customEndTime = end))
        }
        Unit
    }

    override suspend fun setCompleted(routineId: Long, date: LocalDate, completed: Boolean) = write {
        require(routine(routineId).occursOn(date)) { "This routine does not occur on that date." }
        if (completed) db.completions().insert(RoutineCompletionEntity(routineId, date))
        else db.completions().delete(routineId, date)
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
}
