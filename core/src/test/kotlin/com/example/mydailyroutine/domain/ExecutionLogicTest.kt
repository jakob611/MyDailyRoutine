package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.execution.ExecutionProtection
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.planning.*
import com.example.mydailyroutine.domain.repository.TimelineResolver
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ExecutionLogicTest {
    @Test fun `school recovery exists even without a saved rest block`() {
        val school=block(480,840,RoutineCategory.SCHOOL)
        val windows=ExecutionProtection.windows(listOf(school),PlanningConfig(),ZoneOffset.UTC,"other")
        assertTrue(windows.any { it.contains(monday.atTime(14,20).toInstant(ZoneOffset.UTC)) })
        assertFalse(windows.any { it.contains(monday.atTime(14,45).toInstant(ZoneOffset.UTC)) })
    }
    @Test fun `a newly inserted overlapping commitment stops now not before actual work began`() {
        val start=monday.atTime(9,0).toInstant(ZoneOffset.UTC)
        val now=monday.atTime(10,0).toInstant(ZoneOffset.UTC)
        val window=com.example.mydailyroutine.domain.execution.ProtectedWindow(start.minusSeconds(1800),now.plusSeconds(3600))
        assertEquals(now,ExecutionProtection.stopBoundary(listOf(window),start,now))
    }
    @Test fun `measured interval survives a fall DST fold without inventing an hour`() {
        val zone=ZoneId.of("Europe/Ljubljana")
        val date=LocalDate.of(2026,10,25)
        val start=date.atTime(2,45).atZone(zone).withEarlierOffsetAtOverlap().toInstant()
        val end=start.plusSeconds(30*60)
        val timing=ActualTiming(start,end,zone.id)
        val item=block(165,195).copy(date=date,occurrenceDate=date,startsAt=date.atTime(2,45),endsAt=date.atTime(3,15),isCompleted=true,actualTiming=timing)
        val window=OccurrenceTimes.window(item,zone)
        assertEquals(30L,Duration.between(window.start,window.end).toMinutes())
        assertEquals(LocalTime.of(2,15),window.end.atZone(zone).toLocalTime())
    }
    @Test fun `completed timing metadata is retained by recurrence resolution`() {
        val date=monday
        val timing=ActualTiming(date.atTime(9,0).toInstant(ZoneOffset.UTC),date.atTime(9,20).toInstant(ZoneOffset.UTC),"UTC")
        val routine=RoutineBlueprint(id=1,subjectId=null,title="Delo",category=RoutineCategory.FOCUS_ANALYTICAL,dayOfWeek=date.dayOfWeek,
            startTime=LocalTime.of(9,0),endTime=LocalTime.of(10,0),isNotificationEnabled=false)
        val input=ScheduleSnapshot(date,date,routines=listOf(routine),completions=listOf(OccurrenceCompletion(1,date,20,date.atTime(9,0),timing)))
        val resolved=TimelineResolver().resolve(date,input).single() as ResolvedTimelineItem.Block
        assertEquals(timing,resolved.actualTiming)
        assertEquals(20,resolved.durationMinutes)
    }
    @Test fun `daily-cap deferral also closes dependent stages`() {
        val first=TimeBlock("first","Research",RoutineCategory.FOCUS_ANALYTICAL,600,30,25,false,1.0,1.0,precedenceGroup=1,stageOrder=0)
        val next=first.copy(id="next",name="Draft",startMinutes=630,stageOrder=1)
        val other=next.copy(id="other",precedenceGroup=2)
        assertEquals(listOf(next),StageDependencies.dependentsToDefer(listOf(next,other),listOf(first)))
        assertTrue(StageDependencies.dependentsToDefer(listOf(next),listOf(first),setOf("other-window")).isEmpty())
    }
}
