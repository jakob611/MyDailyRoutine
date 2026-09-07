package com.example.mydailyroutine.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.database.*
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
            estimatedEffortHours=100,isTerminalExam=true))
        planning.planMilestone(id,date,PlanningConfig(),context.getString(R.string.preparation_title_pattern),context.getString(R.string.reserve_title))
        val entry=planning.backlog.first().first()
        val result=planning.scheduleBacklog(entry.id,date.plusDays(3),PlanningConfig())
        assertFalse(result.placed);assertEquals(PlacementFailure.DEADLINE,result.failure)
        assertTrue(planning.backlog.first().any { it.id==entry.id })
    }
    private class MutableClock(var current: Instant, private val timeZone: ZoneId=ZoneId.systemDefault()): Clock() {
        override fun instant(): Instant=current
        override fun getZone(): ZoneId=timeZone
        override fun withZone(zone: ZoneId): Clock=MutableClock(current,zone)
    }
}
