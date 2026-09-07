package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.learning.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.planning.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ChronobiologyLearningTest {
    @Test fun `Gaussian center sigma and semantic weights match specified model`() {
        val model=CircadianPenalty()
        assertEquals(1.0,model.kernel(855.0),1e-12)
        assertEquals(kotlin.math.exp(-0.5),model.kernel(900.0),1e-12)
        val analytical=model.cost(RoutineCategory.FOCUS_ANALYTICAL,810,90,1.0)
        assertEquals(90.0,analytical,1e-9)
        assertEquals(analytical*0.6,model.cost(RoutineCategory.FOCUS_SYNTHESIZING,810,90),1e-9)
        assertEquals(analytical*0.05,model.cost(RoutineCategory.ADMIN,810,90),1e-9)
        assertEquals(0.0,model.cost(RoutineCategory.REST_BUFFER,810,90),0.0)
        assertTrue(model.cost(RoutineCategory.FOCUS_ANALYTICAL,540,90)<analytical)
    }
    @Test fun `Gaussian is periodic across midnight`() { assertEquals(CircadianPenalty().kernel(10.0),CircadianPenalty().kernel(1450.0),1e-12) }
    @Test fun `velocity uses ratio of sums not mean of ratios`() {
        val model=VelocityCalibrator(listOf(HistoricalVelocity("math",30,60,1),HistoricalVelocity("math",90,90,2)))
        assertEquals(75,model.getCalibratedDuration(60,"math"))
        assertEquals(60,model.getCalibratedDuration(60,"unknown"))
    }
    @Test fun `velocity skips invalid samples clamps outliers and rounds upward`() {
        val model=VelocityCalibrator(listOf(HistoricalVelocity("a",0,100,1),HistoricalVelocity("a",60,61,2),HistoricalVelocity("b",1,10000,3)))
        assertEquals(63,model.getCalibratedDuration(61,"a"))
        assertEquals(150,model.getCalibratedDuration(60,"b"))
        assertEquals(1439,model.getCalibratedDuration(Int.MAX_VALUE,"b"))
    }
    @Test fun `exact integer ratio avoids binary ceiling drift`() {
        val model=VelocityCalibrator(listOf(HistoricalVelocity("a",100,110,0)))
        assertEquals(99,model.getCalibratedDuration(90,"a"))
    }
    @Test fun `history is bounded to most recent 120 per subject`() {
        val samples=List(130) { HistoricalVelocity("a",60,if(it<10) 300 else 60,it.toLong(),it.toLong()) }
        assertEquals(60,VelocityCalibrator(samples).getCalibratedDuration(60,"a"))
        assertEquals(120,VelocityCalibrator(samples).summaries().single().sampleCount)
    }
    @Test fun `RSEM returns pooled root square buffer`() {
        assertEquals(50,RsemBufferSizer.minutes(listOf(DurationEstimate(60,90),DurationEstimate(60,100))))
        assertEquals(0,RsemBufferSizer.minutes(emptyList()))
    }
    @Test fun `geometric spacing expands and ends at final horizon`() {
        val request=ReviewRequest(1,0,100,4,15)
        val capacity=(1L..100L).map { ReviewDayCapacity(it,270,0,0,600) }
        val result=SpacedRepetitionPlanner().plan(request,capacity)
        assertEquals(listOf(8L,29L,60L,100L),result.map { it.idealEpochDay })
        assertEquals(result.map { it.idealEpochDay }, result.map { it.scheduledEpochDay })
    }
    @Test fun `jitter selects less congested day within fifteen percent ISI`() {
        val request=ReviewRequest(1,0,100,2,15)
        val capacity=(1L..100L).map { ReviewDayCapacity(it,270,if(it==29L)200 else 0,0,600) }
        val result=SpacedRepetitionPlanner().plan(request,capacity)
        assertEquals(28L,result.first().scheduledEpochDay)
        assertTrue(result.all { it.scheduledEpochDay==null || kotlin.math.abs(it.scheduledEpochDay-it.idealEpochDay)<=it.jitterRadiusDays })
    }
    @Test fun `reviews never exceed integer fifth of daily capacity`() {
        val result=SpacedRepetitionPlanner().plan(ReviewRequest(1,0,1,1,15),listOf(ReviewDayCapacity(1,100,20,10,600)))
        assertNull(result.single().scheduledEpochDay)
    }
    @Test fun `short horizon defers excess sessions rather than creating same day duplicates`() {
        val result=SpacedRepetitionPlanner().plan(ReviewRequest(1,0,2,6,15),(1L..2L).map { ReviewDayCapacity(it,270,0,0,600) })
        val scheduled=result.mapNotNull { it.scheduledEpochDay }
        assertEquals(scheduled.toSet().size,scheduled.size)
        assertTrue(result.any { it.scheduledEpochDay==null })
    }
    @Test fun `no free minutes yields unscheduled reviews not fake slots`() {
        val result=SpacedRepetitionPlanner().plan(ReviewRequest(1,0,20,3,15),(1L..20L).map { ReviewDayCapacity(it,270,270,54,0) })
        assertTrue(result.all { it.scheduledEpochDay==null })
    }
    @Test fun `analytical slot search prefers outside dip and observes school recovery`() {
        val date=LocalDate.of(2027,1,4)
        val school=block(540,870,RoutineCategory.SCHOOL).copy(date=date,occurrenceDate=date,startsAt=date.atTime(9,0),endsAt=date.atTime(14,30))
        val day=DailyCapacityPlanner(date,listOf(school),PlanningConfig())
        val start=day.candidate(75,RoutineCategory.FOCUS_ANALYTICAL,3.0)!!
        assertTrue(start>=915)
        assertTrue(CircadianPenalty().kernel(start+37.5)<0.5)
    }
    @Test fun `backplanner preserves integer effort and earlier terminal deadline`() {
        val first=LocalDate.of(2027,1,1);val end=first.plusDays(20);val config=PlanningConfig()
        val days=(0L..20L).map { DailyCapacityPlanner(first.plusDays(it),emptyList(),config) }
        val result=MilestoneBackPlanner().plan(first,end,10.0,true,RoutineCategory.FOCUS_ANALYTICAL,1.0,1.5,days,config)
        assertEquals(600,result.blocks.filterNot { it.reserve }.sumOf { it.durationMinutes }+result.unplacedChunks.sum())
        assertEquals(first.plusDays(17),result.virtualDeadline)
        assertTrue(result.blocks.filterNot { it.reserve }.all { it.durationMinutes in 1..75 && it.date<=result.virtualDeadline })
        assertTrue(days.all { it.studyMinutes<=270 })
        assertTrue(result.blocks.any { it.reserve })
    }
    @Test fun `backplanner capacity exhaustion preserves entire remainder`() {
        val date=LocalDate.of(2027,1,1);val config=PlanningConfig(dailyStudyCapacityMinutes=30)
        val result=MilestoneBackPlanner().plan(date,date,100.0,false,RoutineCategory.FOCUS_ANALYTICAL,1.0,1.5,
            listOf(DailyCapacityPlanner(date,emptyList(),config)),config)
        assertEquals(6000,result.blocks.filterNot { it.reserve }.sumOf { it.durationMinutes }+result.unplacedChunks.sum())
        assertTrue(result.unplacedChunks.isNotEmpty())
    }
}
