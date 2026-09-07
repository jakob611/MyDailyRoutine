package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.learning.*
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.planning.*
import com.example.mydailyroutine.domain.repository.TimelineResolver
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** Regression cases tied to the complete PDF, not only the previously truncated prompt. */
class FullPaperAuditTest {
    @Test fun `page 17 applies max one velocity and the 60 to 81 example`() {
        val samples=listOf(HistoricalVelocity("fast",100,50,1),HistoricalVelocity("chemistry",100,135,1))
        val model=VelocityCalibrator(samples)
        assertEquals(60,model.getCalibratedDuration(60,"fast"))
        assertEquals(81,model.getCalibratedDuration(60,"chemistry"))
    }
    @Test fun `page 13 compresses retrieval before putting it in backlog`() {
        val result=SpacedRepetitionPlanner().plan(ReviewRequest(1,0,30,3,45),
            (1L..30L).map { ReviewDayCapacity(it,180,0,0,180) })
        assertTrue(result.all { it.scheduledEpochDay!=null && it.durationMinutes==36 && it.isCompact })
    }
    @Test fun `zero available capacity remains explicit backlog rather than breaking cap`() {
        val result=SpacedRepetitionPlanner().plan(ReviewRequest(1,0,30,3,45),
            (1L..30L).map { ReviewDayCapacity(it,0,0,0,0) })
        assertTrue(result.all { it.scheduledEpochDay==null })
    }
    @Test fun `page 18 recovery is not an emergency reserve`() {
        val study=TimeBlock("study","Study",RoutineCategory.FOCUS_ANALYTICAL,840,60,25,false,1.0,1.0)
        val rest=TimeBlock("rest","Rest",RoutineCategory.REST_BUFFER,900,45,5,false,10.0,1.0)
        val result=DeterministicReschedulingEngine().recover(listOf(study,rest),840,45)
        assertEquals(rest,result.blocks.single { it.id=="rest" })
        assertEquals(0,result.report.bufferUsedMinutes)
    }
    @Test fun `page 8 historical unmarked work is not replayed`() {
        val past=TimeBlock("past","Past",RoutineCategory.FOCUS_ANALYTICAL,480,60,25,false,1.0,1.0)
        val next=TimeBlock("next","Next",RoutineCategory.FOCUS_ANALYTICAL,840,60,25,false,1.0,1.0)
        val result=DeterministicReschedulingEngine().recover(listOf(past,next),840,30)
        assertEquals(past,result.blocks.single { it.id=="past" })
        assertTrue(result.deferred.none { it.id=="past" })
    }
    @Test fun `real capacity not nominal setting determines review cap`() {
        val config=PlanningConfig(studyStartMinutes=540,studyEndMinutes=600)
        val occupied=block(540,555,RoutineCategory.ADMIN)
        val day=DailyCapacityPlanner(monday,listOf(occupied),config)
        assertEquals(45,day.effectiveStudyCapacity)
        assertEquals(9,day.effectiveReviewCapacity)
        assertNull(day.candidate(10,RoutineCategory.FOCUS_ANALYTICAL,1.0,review=true))
    }
    @Test fun `page 3 task windows differ despite the same Gaussian`() {
        val day=DailyCapacityPlanner(monday,emptyList(),PlanningConfig())
        val analytical=day.candidate(75,RoutineCategory.FOCUS_ANALYTICAL,1.0)!!
        val synthesis=day.candidate(75,RoutineCategory.FOCUS_SYNTHESIZING,1.0)!!
        assertTrue(analytical in 540..675)
        assertTrue(synthesis in 990..1095)
    }
    @Test fun `page 16 completed geometry uses actual duration`() {
        val blueprint=RoutineBlueprint(id=1,subjectId=null,title="Study",category=RoutineCategory.FOCUS_ANALYTICAL,
            dayOfWeek=monday.dayOfWeek,startTime=java.time.LocalTime.of(9,0),endTime=java.time.LocalTime.of(10,0),isNotificationEnabled=false)
        val data=ScheduleSnapshot(monday,monday,routines=listOf(blueprint),completions=listOf(OccurrenceCompletion(1,monday,80,monday.atTime(9,15))))
        val resolved=TimelineResolver().resolve(monday,data).filterIsInstance<ResolvedTimelineItem.Block>().single()
        assertEquals(80,resolved.durationMinutes)
        assertEquals(monday.atTime(10,35),resolved.endsAt)
    }
    @Test fun `primary planning defaults follow 270 and 45 minute bounds`() {
        assertEquals(270,PlanningConfig().dailyStudyCapacityMinutes)
        assertEquals(45,PlanningConfig().postSchoolRecoveryMinutes)
        assertEquals(270,HealthConfig().dailyFocusLimitMinutes)
        assertEquals(45,HealthConfig().transitionMinutes)
    }
    @Test fun `page 12 stage boundaries run backwards with buffers`() {
        val from=LocalDate.of(2027,1,1);val until=from.plusDays(20);val config=PlanningConfig()
        val stages=listOf(PreparationStage("Research",30),PreparationStage("Draft",50),PreparationStage("Check",20))
        val plan=MilestoneBackPlanner().plan(from,until,10.0,true,RoutineCategory.FOCUS_SYNTHESIZING,1.0,1.5,
            (0L..20L).map { DailyCapacityPlanner(from.plusDays(it),emptyList(),config) },config,stages)
        assertEquals(600,plan.blocks.filterNot { it.reserve }.sumOf { it.durationMinutes }+plan.unplacedChunks.sumOf { it.durationMinutes })
        for(i in 0..1) {
            val earlier=plan.blocks.filter { it.stageOrder==i }.maxOf { it.date.toEpochDay()*1440+it.startMinutes+it.durationMinutes }
            val later=plan.blocks.filter { it.stageOrder==i+1 }.minOf { it.date.toEpochDay()*1440+it.startMinutes }
            assertTrue(earlier<=later)
        }
    }
}
