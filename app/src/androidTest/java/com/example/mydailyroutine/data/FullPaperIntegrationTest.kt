package com.example.mydailyroutine.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.features.execution.data.RoomExecutionRepository
import com.example.mydailyroutine.features.planning.data.RoomPlanningRepository
import com.example.mydailyroutine.features.timeline.data.RoomTimelineRepository
import com.example.mydailyroutine.domain.learning.StudyTopic
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.planning.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class FullPaperIntegrationTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: RoutineDatabase
    private lateinit var timeline: RoomTimelineRepository
    private lateinit var planning: RoomPlanningRepository
    private lateinit var execution: RoomExecutionRepository
    private val date=LocalDate.now().plusDays(2)
    private val clock=MutableClock(date.atTime(9,0).atZone(ZoneId.systemDefault()).toInstant())
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(context,RoutineDatabase::class.java).addCallback(SeedAndIntegrityCallback(context.resources)).build()
        timeline=RoomTimelineRepository(db,{})
        planning=RoomPlanningRepository(db,timeline,{})
        execution=RoomExecutionRepository(db,timeline,planning,{},clock)
    }
    @After fun cleanup() { db.close() }
    private suspend fun add(title: String,start: LocalTime,end: LocalTime,fixed: Boolean=false): Long = timeline.saveRoutine(
        RoutineBlueprint(subjectId=null,title=title,category=if(fixed)RoutineCategory.ADMIN else RoutineCategory.FOCUS_ANALYTICAL,
            dayOfWeek=date.dayOfWeek,startTime=start,endTime=end,isNotificationEnabled=false,validFrom=date,validUntil=date,isFixedCommitment=fixed))
    @Test fun explicitTimerHealsDownstreamWithoutReplayingActiveWork(): Unit = runBlocking {
        val active=add("Delo",LocalTime.of(9,0),LocalTime.of(10,0))
        val next=add("Nadaljevanje",LocalTime.of(10,0),LocalTime.of(11,0))
        val fixed=add("Obveznost",LocalTime.NOON,LocalTime.of(13,0),true)
        execution.start(active,date,PlanningConfig(),true)
        clock.current=date.atTime(10,15).atZone(clock.zone).toInstant()
        execution.synchronize(PlanningConfig(),true)
        val first=timeline.getTimelineForDate(date).first().filterIsInstance<ResolvedTimelineItem.Block>()
        assertEquals(75,first.single { it.routineBlockId==active }.durationMinutes)
        assertEquals(615,first.single { it.routineBlockId==next }.startMinute)
        assertEquals(720,first.single { it.routineBlockId==fixed }.startMinute)
        execution.synchronize(PlanningConfig(),true)
        assertEquals(first,timeline.getTimelineForDate(date).first())
        assertEquals(75,execution.finish(PlanningConfig(),true))
        assertNull(execution.active.first())
        assertEquals(75,timeline.getTimelineForDate(date).first().filterIsInstance<ResolvedTimelineItem.Block>().single { it.routineBlockId==active }.durationMinutes)
    }
    @Test fun timerStopsAtFixedBoundaryWithoutMovingIt(): Unit = runBlocking {
        val work=add("Delo",LocalTime.of(9,0),LocalTime.of(10,0))
        val fixed=add("Obveznost",LocalTime.of(10,30),LocalTime.of(11,30),true)
        execution.start(work,date,PlanningConfig(),true)
        clock.current=date.atTime(10,45).atZone(clock.zone).toInstant()
        assertTrue(execution.synchronize(PlanningConfig(),true))
        assertEquals(date.atTime(10,30).atZone(clock.zone).toInstant(),execution.active.first()!!.stoppedAt)
        assertEquals(90,execution.finish(PlanningConfig(),true))
        assertEquals(630,timeline.getTimelineForDate(date).first().filterIsInstance<ResolvedTimelineItem.Block>().single { it.routineBlockId==fixed }.startMinute)
    }
    @Test fun persistedExecutionCanBeReconstructedAfterProcessDeath(): Unit = runBlocking {
        val id=add("Delo",LocalTime.of(9,0),LocalTime.of(10,0))
        execution.start(id,date,PlanningConfig(),true)
        val restarted=RoomExecutionRepository(db,timeline,planning,{},clock)
        clock.current=date.atTime(9,20).atZone(clock.zone).toInstant()
        assertEquals(20,restarted.active.first()!!.elapsedMinutes(clock.current))
        assertEquals(20,restarted.finish(PlanningConfig(),true))
    }
    @Test fun reviewCompressionIsPersistedAndUsesActualDailyCapacity(): Unit = runBlocking {
        val config=PlanningConfig(dailyStudyCapacityMinutes=180)
        val summary=planning.createTopic(StudyTopic(title="Tema",subjectId=null,initialDate=date,finalDate=date.plusDays(30),reviewCount=3,reviewDurationMinutes=45),
            config,context.getString(R.string.review_title_pattern))
        assertEquals(3,summary.reviews)
        assertEquals(108,summary.studyMinutes)
        assertTrue(db.learning().reviews(date.toEpochDay(),date.plusDays(30).toEpochDay()).all { it.durationMinutes==36 })
    }
    @Test fun backlogCannotBeAutomaticallyPlacedAfterItsDeadline(): Unit = runBlocking {
        val id=timeline.saveMilestone(Milestone(subjectId=null,title="Rok",dueDate=date.plusDays(2),dueTime=LocalTime.of(9,0),isExam=true,
            estimatedEffortHours=100.0,isTerminalExam=true))
        planning.planMilestone(id,date,PlanningConfig(),context.getString(R.string.preparation_title_pattern),context.getString(R.string.reserve_title))
        val entry=planning.backlog.first().first()
        val result=planning.scheduleBacklog(entry.id,date.plusDays(3),PlanningConfig())
        assertFalse(result.placed);assertEquals(PlacementFailure.DEADLINE,result.failure)
        assertTrue(planning.backlog.first().any { it.id==entry.id })
    }
    @Test fun manualModeStillProtectsFixedTimeWithoutMovingOtherWork(): Unit = runBlocking {
        val work=add("Delo",LocalTime.of(9,0),LocalTime.of(10,0))
        val fixed=add("Obveznost",LocalTime.of(10,30),LocalTime.of(11,30),true)
        val next=add("Pozneje",LocalTime.of(11,30),LocalTime.of(12,0))
        execution.start(work,date,PlanningConfig(),false)
        clock.current=date.atTime(10,45).atZone(clock.zone).toInstant()
        assertTrue(execution.synchronize(PlanningConfig(),false))
        assertEquals(90,execution.finish(PlanningConfig(),false))
        val items=timeline.getTimelineForDate(date).first().filterIsInstance<ResolvedTimelineItem.Block>()
        assertEquals(630,items.single { it.routineBlockId==fixed }.startMinute)
        assertEquals(690,items.single { it.routineBlockId==next }.startMinute)
    }
    @Test fun newlyAddedEarlierBoundaryShrinksTheActiveProjection(): Unit = runBlocking {
        val work=add("Delo",LocalTime.of(9,0),LocalTime.of(11,0))
        execution.start(work,date,PlanningConfig(),true)
        val fixed=add("Nova obveznost",LocalTime.of(10,30),LocalTime.of(11,30),true)
        clock.current=date.atTime(10,45).atZone(clock.zone).toInstant()
        execution.synchronize(PlanningConfig(),true)
        val items=timeline.getTimelineForDate(date).first().filterIsInstance<ResolvedTimelineItem.Block>()
        assertEquals(630,items.single { it.routineBlockId==work }.endMinute)
        assertEquals(630,items.single { it.routineBlockId==fixed }.startMinute)
    }
    @Test fun changingRunningWorkRequiresAnExplicitFinishOrCancel(): Unit = runBlocking {
        val work=add("Delo",LocalTime.of(9,0),LocalTime.of(10,0))
        execution.start(work,date,PlanningConfig(),true)
        try { timeline.cancelOccurrence(work,date); fail("Must preserve the active session") }
        catch(error: ScheduleConflict) { assertEquals(ScheduleConflictReason.ACTIVE_EXECUTION,error.reason) }
        try { timeline.deleteRoutine(work); fail("Must preserve the active session") }
        catch(error: ScheduleConflict) { assertEquals(ScheduleConflictReason.ACTIVE_EXECUTION,error.reason) }
        assertNotNull(execution.active.first())
    }
    @Test fun resetReviewOverrideRestoresItsCanonicalScheduledDate(): Unit = runBlocking {
        planning.createTopic(StudyTopic(title="Optika",subjectId=null,initialDate=date,finalDate=date.plusDays(10),reviewCount=1,reviewDurationMinutes=15),
            PlanningConfig(),context.getString(R.string.review_title_pattern))
        val review=db.learning().reviews(date.toEpochDay(),date.plusDays(10).toEpochDay()).single()
        val blockId=review.timeBlockId!!
        val origin=LocalDate.ofEpochDay(review.scheduledEpochDay)
        timeline.saveOverride(EventOverride(routineBlockId=blockId,overrideDate=origin,dayShift=1,cancellationReason=CancellationReason.AUTO_HEAL))
        assertEquals(origin.plusDays(1).toEpochDay(),db.learning().getReview(review.id)!!.scheduledEpochDay)
        timeline.resetOverride(blockId,origin)
        assertEquals(origin.toEpochDay(),db.learning().getReview(review.id)!!.scheduledEpochDay)
        assertTrue(timeline.getTimelineForDate(origin).first().filterIsInstance<ResolvedTimelineItem.Block>().any { it.reviewId==review.id })
    }
    @Test fun stagePlacementAndExecutionCannotSkipPredecessors(): Unit = runBlocking {
        val goal=timeline.saveMilestone(Milestone(subjectId=null,title="Projekt",dueDate=date.plusDays(10),dueTime=null,isExam=false))
        suspend fun stage(day: LocalDate, order: Int): Long = timeline.saveRoutine(RoutineBlueprint(subjectId=null,title="Faza $order",category=RoutineCategory.FOCUS_ANALYTICAL,
            dayOfWeek=day.dayOfWeek,startTime=LocalTime.of(9,0),endTime=LocalTime.of(10,0),isNotificationEnabled=false,validFrom=day,validUntil=day,milestoneId=goal,stageOrder=order))
        val first=stage(date.plusDays(3),0)
        stage(date.plusDays(5),2)
        val backlog=db.backlog().insert(BacklogEntryEntity(title="Osnutek",category=RoutineCategory.FOCUS_ANALYTICAL,durationMinutes=60,minDurationMinutes=25,
            elasticity=1.0,priorityWeight=3.0,subjectId=null,sourceRoutineId=null,occurrenceDate=null,milestoneId=goal,topicId=null,reviewId=null,reason="CAPACITY",stageOrder=1))
        assertEquals(PlacementFailure.DEPENDENCY,planning.scheduleBacklog(backlog,date.plusDays(2),PlanningConfig()).failure)
        assertEquals(PlacementFailure.DEPENDENCY,planning.scheduleBacklog(backlog,date.plusDays(6),PlanningConfig()).failure)
        assertTrue(planning.scheduleBacklog(backlog,date.plusDays(4),PlanningConfig()).placed)
        val middle=timeline.getTimelineForDate(date.plusDays(4)).first().filterIsInstance<ResolvedTimelineItem.Block>().single()
        clock.current=date.plusDays(4).atTime(9,0).atZone(clock.zone).toInstant()
        try { execution.start(middle.routineBlockId,middle.occurrenceDate,PlanningConfig(),true); fail("Previous stage is unfinished") }
        catch(error: ScheduleConflict) { assertEquals(ScheduleConflictReason.PREVIOUS_STAGE,error.reason) }
        timeline.setCompleted(first,date.plusDays(3),true,60)
        execution.start(middle.routineBlockId,middle.occurrenceDate,PlanningConfig(),true)
        assertEquals(middle.routineBlockId,execution.active.first()!!.routineBlockId)
    }
    @Test fun measuredCompletionRetainsRealInstantsAcrossAutumnClockChange(): Unit = runBlocking {
        val zone=ZoneId.of("Europe/Ljubljana")
        val day=LocalDate.of(2026,10,25)
        val started=day.atTime(2,45).atZone(zone).withEarlierOffsetAtOverlap().toInstant()
        val localClock=MutableClock(started,zone)
        val timed=RoomExecutionRepository(db,timeline,planning,{},localClock) { zone }
        val work=timeline.saveRoutine(RoutineBlueprint(subjectId=null,title="Nočno delo",category=RoutineCategory.FOCUS_ANALYTICAL,
            dayOfWeek=day.dayOfWeek,startTime=LocalTime.of(2,45),endTime=LocalTime.of(3,15),isNotificationEnabled=false,validFrom=day,validUntil=day))
        timed.start(work,day,PlanningConfig(),false)
        localClock.current=started.plusSeconds(1800)
        assertEquals(30,timed.finish(PlanningConfig(),false))
        val block=timeline.getTimelineForDate(day).first().filterIsInstance<ResolvedTimelineItem.Block>().single()
        assertEquals(started,block.actualTiming!!.startedAt)
        assertEquals(localClock.current,block.actualTiming!!.endedAt)
        assertEquals(LocalTime.of(2,15),block.actualTiming!!.endedAt.atZone(zone).toLocalTime())
    }
    @Test fun movingADeadlineEarlierRequeuesInvalidFuturePreparationWithoutLosingWork(): Unit = runBlocking {
        val goal=Milestone(subjectId=null,title="Rok",dueDate=date.plusDays(10),dueTime=LocalTime.of(12,0),isExam=true,isTerminalExam=true)
        val goalId=timeline.saveMilestone(goal)
        val taskDay=date.plusDays(8)
        val task=timeline.saveRoutine(RoutineBlueprint(subjectId=null,title="Priprava",category=RoutineCategory.FOCUS_ANALYTICAL,
            dayOfWeek=taskDay.dayOfWeek,startTime=LocalTime.of(9,0),endTime=LocalTime.of(10,0),isNotificationEnabled=false,
            validFrom=taskDay,validUntil=taskDay,milestoneId=goalId,stageOrder=1))
        timeline.saveMilestone(goal.copy(id=goalId,dueDate=date.plusDays(5)))
        assertFalse(timeline.getTimelineForDate(taskDay).first().filterIsInstance<ResolvedTimelineItem.Block>().any { it.routineBlockId==task })
        val entry=planning.backlog.first().single()
        assertEquals(60,entry.durationMinutes)
        assertEquals(goalId,entry.milestoneId)
        assertEquals(PlacementFailure.DEADLINE,planning.scheduleBacklog(entry.id,taskDay,PlanningConfig()).failure)
        assertEquals(1,planning.backlog.first().size)
    }
    private class MutableClock(var current: Instant, private val timeZone: ZoneId=ZoneId.systemDefault()): Clock() {
        override fun instant(): Instant=current
        override fun getZone(): ZoneId=timeZone
        override fun withZone(zone: ZoneId): Clock=MutableClock(current,zone)
    }
}
