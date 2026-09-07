package com.example.mydailyroutine.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mydailyroutine.R
import com.example.mydailyroutine.data.local.*
import com.example.mydailyroutine.data.repository.*
import com.example.mydailyroutine.domain.learning.StudyTopic
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.planning.PlanningConfig
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResearchIntegrationTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: RoutineDatabase
    private lateinit var timeline: RoomTimelineRepository
    private lateinit var planning: RoomPlanningRepository
    private val date=LocalDate.now().plusDays(1)
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(context,RoutineDatabase::class.java).addCallback(SeedAndIntegrityCallback(context.resources)).build()
        timeline=RoomTimelineRepository(db,{})
        planning=RoomPlanningRepository(db,timeline,{})
    }
    @After fun cleanup() { db.close() }
    private suspend fun add(title: String, start: Int, duration: Int, minimum: Int=25, weight: Double=3.0, fixed: Boolean=false,
        category: RoutineCategory=RoutineCategory.FOCUS_ANALYTICAL): Long = timeline.saveRoutine(RoutineBlueprint(
        subjectId=null,title=title,category=category,dayOfWeek=date.dayOfWeek,startTime=LocalTime.ofSecondOfDay(start*60L),
        endTime=LocalTime.ofSecondOfDay(start*60L).plusMinutes(duration.toLong()),isNotificationEnabled=false,
        minDurationMinutes=minimum,elasticity=if(fixed)0.0 else 1.0,priorityWeight=weight,isFixedCommitment=fixed))

    @Test fun healingPersistsBacklogAndPreservesWeeklyBlueprint(): Unit = runBlocking {
        add("Pomembno",840,60,30,10.0);add("Pozneje",900,60,30,1.0)
        val fixed=add("Obveznost",960,60,60,10.0,true,RoutineCategory.ADMIN)
        val result=planning.autoHeal(date,915,75,PlanningConfig())
        assertEquals(1,result.deferredCount)
        assertEquals(1,planning.backlog.first().size)
        val today=timeline.getTimelineForDate(date).first().filterIsInstance<ResolvedTimelineItem.Block>()
        assertEquals(960,today.single { it.routineBlockId==fixed }.startMinute)
        assertTrue(today.filterNot { it.isFixedCommitment }.all { it.endMinute<=960 })
        val next=timeline.getTimelineForDate(date.plusWeeks(1)).first().filterIsInstance<ResolvedTimelineItem.Block>()
        assertEquals(3,next.size)
        assertEquals(120,next.filter { it.category.isDeepWork }.sumOf { it.durationMinutes })
        val entry=planning.backlog.first().single()
        assertTrue(planning.scheduleBacklog(entry.id,date.plusDays(2),PlanningConfig()).placed)
        assertTrue(planning.backlog.first().isEmpty())
    }
    @Test fun extremeDelayLeavesNoLostOrDuplicateWork(): Unit = runBlocking {
        add("A",840,60);add("B",900,60)
        add("Obveznost",960,60,60,10.0,true,RoutineCategory.ADMIN)
        planning.autoHeal(date,900,Int.MAX_VALUE,PlanningConfig())
        assertEquals(2,planning.backlog.first().size)
        assertEquals(120,planning.backlog.first().sumOf { it.durationMinutes })
        planning.autoHeal(date,900,Int.MAX_VALUE,PlanningConfig())
        assertEquals(2,planning.backlog.first().size)
    }
    @Test fun actualCompletionUpdatesOneHistoricalRatioAndUndoRemovesIt(): Unit = runBlocking {
        val subject=timeline.saveSubject(Subject(name="Matematika",colorHex=0xFF3B82F6,defaultDurationMinutes=60))
        val id=timeline.saveRoutine(RoutineBlueprint(subjectId=subject,title="Analiza",category=RoutineCategory.FOCUS_ANALYTICAL,
            dayOfWeek=date.dayOfWeek,startTime=LocalTime.of(9,0),endTime=LocalTime.of(10,30),isNotificationEnabled=false,
            validFrom=date,validUntil=date,rawDurationMinutes=60))
        timeline.setCompleted(id,date,true,90)
        assertEquals(90,planning.getCalibratedDuration(60,subject.toString()))
        assertEquals(90,db.routines().get(id)!!.completedActualMinutes)
        timeline.setCompleted(id,date,true,120)
        assertEquals(1,planning.history.first().size)
        assertEquals(120,planning.getCalibratedDuration(60,subject.toString()))
        timeline.setCompleted(id,date,false)
        assertTrue(planning.history.first().isEmpty())
        assertEquals(60,planning.getCalibratedDuration(60,subject.toString()))
    }
    @Test fun reviewsAreRealTimelineBlocksAndCascadeWithTheirTopic(): Unit = runBlocking {
        val result=planning.createTopic(StudyTopic(title="Optika",subjectId=null,initialDate=date,finalDate=date.plusDays(30)),
            PlanningConfig(),context.getString(R.string.review_title_pattern))
        assertEquals(4,result.reviews)
        val reviews=db.learning().reviews(date.toEpochDay(),date.plusDays(30).toEpochDay())
        assertEquals(4,reviews.size);assertTrue(reviews.all { it.timeBlockId!=null })
        for(review in reviews) {
            val day=timeline.getTimelineForDate(LocalDate.ofEpochDay(review.scheduledEpochDay)).first().filterIsInstance<ResolvedTimelineItem.Block>()
            assertTrue(day.any { it.reviewId==review.id })
            assertTrue(day.filter { it.reviewId!=null }.sumOf { it.durationMinutes }<=54)
        }
        planning.deleteTopic(planning.topics.first().single().id)
        assertTrue(db.learning().reviews(date.toEpochDay(),date.plusDays(30).toEpochDay()).isEmpty())
    }
    @Test fun linkedReviewsDoNotPreventReverseMilestonePreparation(): Unit = runBlocking {
        val deadline=date.plusDays(30)
        val id=timeline.saveMilestone(Milestone(subjectId=null,title="Zaključni test",dueDate=deadline,dueTime=LocalTime.of(9,0),isExam=true,
            estimatedEffortHours=2.0,isTerminalExam=true))
        planning.createTopic(StudyTopic(title="Ponavljanje",subjectId=null,initialDate=date,finalDate=deadline.minusDays(1),milestoneId=id),
            PlanningConfig(),context.getString(R.string.review_title_pattern))
        val result=planning.planMilestone(id,date,PlanningConfig(),context.getString(R.string.preparation_title_pattern),context.getString(R.string.reserve_title))
        assertEquals(120,result.studyMinutes+result.deferredMinutes)
        assertTrue(result.reserveMinutes>0)
        val repeat=planning.planMilestone(id,date,PlanningConfig(),context.getString(R.string.preparation_title_pattern),context.getString(R.string.reserve_title))
        assertEquals(0,repeat.studyMinutes)
    }
}
