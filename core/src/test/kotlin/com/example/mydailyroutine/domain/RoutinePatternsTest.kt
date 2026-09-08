package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.health.ScheduleMetrics
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.repository.TimelineResolver
import com.example.mydailyroutine.domain.routines.*
import com.example.mydailyroutine.domain.scheduling.AlarmPlanner
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class RoutinePatternsTest {
    private val date = LocalDate.of(2026,9,7)
    private val lesson = RoutineBlueprint(1,null,"Pouk",RoutineCategory.SCHOOL,DayOfWeek.MONDAY,
        LocalTime.of(8,0),LocalTime.of(8,45),true,seriesKey="group")
    private val pause = RoutineBlueprint(2,null,"Odmor",RoutineCategory.REST_BUFFER,DayOfWeek.MONDAY,
        LocalTime.of(8,45),LocalTime.of(8,50),true,minDurationMinutes=5,elasticity=0.0,isFixedCommitment=true,
        seriesKey="group",parentRoutineId=1,origin=RoutineOrigin.LESSON_BREAK)
    private fun resolved(rows: List<RoutineBlueprint> = listOf(lesson,pause), overrides: List<EventOverride> = emptyList(), calendar: List<CalendarEntry> = emptyList(), day: LocalDate=date) =
        TimelineResolver().resolve(day,ScheduleSnapshot(date,date.plusDays(2),routines=rows,overrides=overrides,calendar=calendar)).filterIsInstance<ResolvedTimelineItem.Block>()

    @Test fun startChangeKeepsFortyFiveMinutes() {
        val state=TimeEntryState.at(LocalTime.of(8,0),45).withStart("09:15")
        assertEquals("10:00",state.endText); assertEquals(45,state.durationMinutes)
    }
    @Test fun manualEndBecomesTheNewPreservedDuration() {
        val state=TimeEntryState.at(LocalTime.of(8,0),45).withEnd("08:50").withStart("09:00")
        assertEquals("09:50",state.endText);assertEquals(50,state.durationMinutes)
    }
    @Test fun partialInputDoesNotDestroyTheLastEnd() {
        val state=TimeEntryState.at(LocalTime.of(8,0),45).withStart("09:")
        assertEquals("08:45",state.endText)
        assertEquals("10:15",state.withStart("09:30").endText)
    }
    @Test fun midnightEndAndNextLessonAreCorrect() {
        assertEquals("00:30",TimeEntryState.at(LocalTime.of(23,45),45).endText)
        assertEquals(date.plusDays(1).atTime(0,35),RoutinePatternExpander.followingStart(date,LocalTime.of(23,45),45,5))
        assertEquals(setOf(DayOfWeek.TUESDAY),Weekdays.fromMask(Weekdays.shifted(Weekdays.mask(setOf(DayOfWeek.MONDAY)),1)))
    }
    @Test fun multiDayExpansionIsStableAndBoundedFromTheSelectedDate() {
        val rows=RoutinePatternExpander.expand(RoutinePatternRequest(lesson.copy(id=0,validFrom=date),setOf(DayOfWeek.FRIDAY,DayOfWeek.MONDAY,DayOfWeek.WEDNESDAY),true),"series")
        assertEquals(listOf(DayOfWeek.MONDAY,DayOfWeek.WEDNESDAY,DayOfWeek.FRIDAY),rows.map { it.dayOfWeek })
        assertTrue(rows.all { it.seriesKey=="series" && it.validFrom==date && it.validUntil==null })
        assertEquals(date.plusDays(2),RoutinePatternExpander.firstOccurrence(date.plusDays(1),rows.map { it.dayOfWeek }.toSet()))
    }
    @Test fun oneOffDoesNotCreateOtherSelectedWeekdays() {
        val rows=RoutinePatternExpander.expand(RoutinePatternRequest(lesson.copy(id=0,validFrom=date),setOf(DayOfWeek.TUESDAY,DayOfWeek.FRIDAY),false),"ignored")
        assertEquals(1,rows.size);assertEquals(DayOfWeek.MONDAY,rows.single().dayOfWeek);assertEquals(date,rows.single().validUntil)
    }
    @Test fun companionFollowsDateOverrideAndHoliday() {
        val moved=resolved(overrides=listOf(EventOverride(routineBlockId=1,overrideDate=date,customStartTime=LocalTime.of(9,0),customEndTime=LocalTime.of(9,45))))
        assertEquals(LocalTime.of(9,45),moved.single { it.parentRoutineId==1L }.startTime)
        assertTrue(resolved(calendar=listOf(CalendarEntry(date=date,title="Prosto",isWorkFreeDay=true))).all { it.isSuppressed })
    }
    @Test fun skippingParentAlsoSkipsItsCompanion() {
        assertTrue(resolved(overrides=listOf(EventOverride(routineBlockId=1,overrideDate=date,isCancelled=true))).isEmpty())
    }
    @Test fun linkedBreakNeverMovesTheNextLesson() {
        val next=lesson.copy(id=3,title="Naslednja ura",startTime=LocalTime.of(8,45),endTime=LocalTime.of(9,30),seriesKey=null)
        val items=resolved(listOf(lesson,pause,next))
        val child=items.single { it.parentRoutineId==1L }
        assertTrue(child.companionConflict);assertTrue(child.isSuppressed)
        assertEquals(525,items.single { it.routineBlockId==3L }.startMinute)
        assertNull(AlarmPlanner().forOccurrence(child,ZoneOffset.UTC))
        assertEquals(0,ScheduleMetrics.forDay(items).recoveryMinutes)
    }
    @Test fun existingRestIsNotDoubleCounted() {
        val manual=pause.copy(id=4,parentRoutineId=null,origin=RoutineOrigin.USER,seriesKey=null)
        val items=resolved(listOf(lesson,pause,manual))
        assertTrue(items.single { it.routineBlockId==2L }.companionConflict)
        assertEquals(5,ScheduleMetrics.forDay(items).recoveryMinutes)
    }
    @Test fun midnightCompanionBelongsToItsOriginalSchoolDay() {
        val late=lesson.copy(startTime=LocalTime.of(23,30),endTime=LocalTime.of(0,15))
        val next=resolved(listOf(late,pause),day=date.plusDays(1))
        val child=next.single { it.parentRoutineId==1L }
        assertEquals(date,child.occurrenceDate)
        assertEquals(date.plusDays(1).atTime(0,15),child.startsAt)
        assertEquals(5,child.durationMinutes)
    }
    @Test fun disabledSleepAndItsWakeBufferDoNotAppear() {
        val sleep=lesson.copy(category=RoutineCategory.ADMIN,startTime=LocalTime.of(23,0),endTime=LocalTime.of(7,0),
            origin=RoutineOrigin.SLEEP,isNotificationEnabled=false,isEnabled=false)
        val morning=pause.copy(origin=RoutineOrigin.MORNING_BUFFER,isNotificationEnabled=false)
        assertTrue(resolved(listOf(sleep,morning),day=date.plusDays(1)).isEmpty())
    }
    @Test fun sleepIsPlannedContextNotAnUncompletableTask() {
        val sleep=lesson.copy(category=RoutineCategory.ADMIN,startTime=LocalTime.of(23,0),endTime=LocalTime.of(7,0),
            origin=RoutineOrigin.SLEEP,isNotificationEnabled=false)
        val items=resolved(listOf(sleep),day=date.plusDays(1))
        assertEquals(420,items.single().durationMinutes)
        assertEquals(0,ScheduleMetrics.forDay(items).blockCount)
        assertEquals(0,ScheduleMetrics.forDay(items).occupiedMinutes)
    }
}
