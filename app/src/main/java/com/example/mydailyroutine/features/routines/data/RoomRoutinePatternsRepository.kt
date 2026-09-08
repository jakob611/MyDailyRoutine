package com.example.mydailyroutine.features.routines.data

import androidx.room.withTransaction
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.core.database.entities.TimeBlockEntity
import com.example.mydailyroutine.core.database.entities.EventOverrideEntity
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.repository.*
import com.example.mydailyroutine.domain.routines.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Atomic multi-day creation and managed sleep patterns; the canonical timeline remains the only agenda. */
class RoomRoutinePatternsRepository(private val db: RoutineDatabase, private val timeline: TimelineRepository,
    private val onChanged: () -> Unit, private val clock: Clock = Clock.systemDefaultZone()) : RoutinePatternsRepository {
    override val sleep = db.invalidationTracker.createFlow("routine_blocks",emitInitialState=true).map {
        withContext(Dispatchers.IO) { db.withTransaction { decodeSleep(db.routines().currentSleep()) } }
    }.distinctUntilChanged()
    private suspend fun decodeSleep(rows: List<TimeBlockEntity>): SleepSchedule {
        if (rows.isEmpty()) return SleepSchedule()
        val first = rows.first()
        val start = LocalTime.ofSecondOfDay(first.startMinutes * 60L)
        val morning = db.routines().companion(first.id)?.durationMinutes ?: 0
        return SleepSchedule(first.isEnabled,start,start.plusMinutes(first.durationMinutes.toLong()),Weekdays.mask(rows.map { it.dayOfWeek }.toSet()),morning)
    }
    private suspend fun <T> write(action: suspend () -> T): T = withContext(Dispatchers.IO) {
        db.withTransaction { action().also { onChanged() } }
    }
    override suspend fun create(request: RoutinePatternRequest): RoutinePatternResult = write {
        val roots = RoutinePatternExpander.expand(request,UUID.randomUUID().toString())
        val ids = mutableListOf<Long>(); val companions = mutableListOf<Long>()
        roots.forEach { root ->
            val id = timeline.saveRoutine(root); ids += id
            if (request.afterLessonBreakMinutes > 0) companions += insertCompanion(root.copy(id=id),request.afterLessonBreakMinutes,
                request.breakTitle,RoutineOrigin.LESSON_BREAK,request.breakNotifications)
        }
        RoutinePatternResult(ids,companions)
    }
    private suspend fun insertCompanion(parent: RoutineBlueprint, duration: Int, title: String, origin: RoutineOrigin, notify: Boolean): Long {
        val start = parent.endTime
        val child = RoutineBlueprint(subjectId=null,title=title,category=RoutineCategory.REST_BUFFER,dayOfWeek=parent.dayOfWeek,
            startTime=start,endTime=start.plusMinutes(duration.toLong()),isNotificationEnabled=notify,
            validFrom=parent.validFrom,validUntil=parent.validUntil,minDurationMinutes=duration,elasticity=0.0,priorityWeight=parent.priorityWeight,
            isFixedCommitment=true,seriesKey=parent.seriesKey,parentRoutineId=parent.id,origin=origin,isEnabled=parent.isEnabled)
        ScheduleValidation.routine(child)
        return db.routines().insert(child.entity())
    }
    override suspend fun editSeries(seriesKey: String, title: String, start: LocalTime, end: LocalTime): Unit = write {
        val roots = db.routines().seriesRoots(seriesKey)
        require(roots.isNotEmpty() && roots.all { it.origin==RoutineOrigin.USER })
        val active = db.execution().get()
        if (roots.any { it.id==active?.routineBlockId }) throw ScheduleConflict(ScheduleConflictReason.ACTIVE_EXECUTION)
        roots.forEach { root ->
            val date = (root.validFrom ?: LocalDate.now(clock)).with(java.time.temporal.TemporalAdjusters.nextOrSame(root.dayOfWeek))
            timeline.editBlock(root.id,date,title,start,end,true)
        }
    }
    override suspend fun deleteSeries(seriesKey: String): Unit = write {
        val roots = db.routines().seriesRoots(seriesKey)
        require(roots.all { it.origin==RoutineOrigin.USER })
        val active = db.execution().get()
        if (roots.any { it.id==active?.routineBlockId }) throw ScheduleConflict(ScheduleConflictReason.ACTIVE_EXECUTION)
        roots.forEach { timeline.deleteRoutine(it.id) }
    }
    override suspend fun saveSleep(schedule: SleepSchedule, sleepTitle: String, morningTitle: String): Unit = write {
        ScheduleValidation.title(sleepTitle); ScheduleValidation.title(morningTitle)
        val previous = db.routines().currentSleep()
        if (previous.isNotEmpty() && decodeSleep(previous)==schedule) return@write
        val now = LocalDateTime.now(clock)
        // Preserve an already-started night. New hours apply to the next bedtime, not past history.
        val effective = if (previous.any { it.isEnabled && it.dayOfWeek==now.dayOfWeek && it.startMinutes <= now.toLocalTime().toSecondOfDay()/60 }) now.toLocalDate().plusDays(1) else now.toLocalDate()
        val exceptions = mutableListOf<Triple<java.time.DayOfWeek,RoutineOrigin,EventOverrideEntity>>()
        previous.forEach { root ->
            for (row in listOfNotNull(root,db.routines().companion(root.id))) {
                db.overrides().forRoutine(row.id).filter { it.overrideDate >= effective }.forEach { exception ->
                    exceptions += Triple(row.dayOfWeek,row.origin,exception)
                    db.overrides().delete(row.id,exception.overrideDate)
                }
            }
        }
        previous.forEach { row ->
            val begins = row.validFrom
            if (begins != null && begins >= effective) db.routines().delete(row.id)
            else {
                db.routines().update(row.copy(validUntil=effective.minusDays(1)))
                db.routines().companion(row.id)?.let { db.routines().update(it.copy(validUntil=effective.minusDays(1))) }
            }
        }
        val first = if (previous.isEmpty() && schedule.enabled && schedule.bedtime > schedule.wakeTime && now.toLocalTime() < schedule.wakeTime)
            effective.minusDays(1) else effective
        val key = UUID.randomUUID().toString()
        val newIds = mutableMapOf<Pair<java.time.DayOfWeek,RoutineOrigin>,Long>()
        Weekdays.fromMask(schedule.weekdaysMask).sortedBy { it.value }.forEach { day ->
            val parent = RoutineBlueprint(subjectId=null,title=sleepTitle,category=RoutineCategory.ADMIN,dayOfWeek=day,
                startTime=schedule.bedtime,endTime=schedule.wakeTime,isNotificationEnabled=false,validFrom=first,
                minDurationMinutes=schedule.durationMinutes,elasticity=0.0,priorityWeight=10.0,isFixedCommitment=true,
                seriesKey=key,origin=RoutineOrigin.SLEEP,isEnabled=schedule.enabled)
            ScheduleValidation.routine(parent)
            val id = db.routines().insert(parent.entity())
            newIds[day to RoutineOrigin.SLEEP] = id
            if (schedule.morningBufferMinutes > 0) newIds[day to RoutineOrigin.MORNING_BUFFER] = insertCompanion(parent.copy(id=id),schedule.morningBufferMinutes,morningTitle,RoutineOrigin.MORNING_BUFFER,false)
        }
        exceptions.forEach { (day,origin,exception) -> newIds[day to origin]?.let { id ->
            db.overrides().insert(exception.copy(id=0,routineBlockId=id,customStartTime=null,customEndTime=null,dayShift=0))
        } }
    }
}
