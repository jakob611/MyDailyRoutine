package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.scheduling.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class AlarmPlannerTest {
    private val planner = AlarmPlanner()
    private val utc = ZoneId.of("UTC")
    private val routine = RoutineBlueprint(1, null, "Study", RoutineCategory.FOCUS_STUDY,
        monday.dayOfWeek, LocalTime.of(8, 0), LocalTime.of(9, 0), true)
    private fun snapshot(routines: List<RoutineBlueprint> = listOf(routine), overrides: List<EventOverride> = emptyList(),
        completions: List<OccurrenceCompletion> = emptyList(), calendar: List<CalendarEntry> = emptyList()) =
        ScheduleSnapshot(monday.minusDays(1), monday.plusDays(370), routines = routines, overrides = overrides, completions = completions, calendar = calendar)

    @Test fun `focus pre alert is five minutes before but recovery is at start`() {
        val focus = planner.forOccurrence(block(480, 540), utc)!!
        val rest = planner.forOccurrence(block(480, 540, RoutineCategory.REST_BREAK), utc)!!
        assertEquals(monday.atTime(7, 55).toInstant(ZoneOffset.UTC), focus.triggerAt)
        assertEquals(monday.atTime(8, 0).toInstant(ZoneOffset.UTC), rest.triggerAt)
        assertEquals(AlarmKind.RECOVERY_START, rest.kind)
    }

    @Test fun `earliest next day pre alert is found across midnight`() {
        val tomorrow = routine.copy(id = 2, dayOfWeek = monday.plusDays(1).dayOfWeek,
            startTime = LocalTime.of(0, 2), endTime = LocalTime.of(1, 0))
        val tonight = routine.copy(category = RoutineCategory.REST_BREAK, startTime = LocalTime.of(23, 59), endTime = LocalTime.of(0, 30))
        val now = monday.atTime(23, 50).toInstant(ZoneOffset.UTC)
        val next = planner.next(snapshot(listOf(tonight, tomorrow)), now, utc)!!
        assertEquals(monday.atTime(23, 57).toInstant(ZoneOffset.UTC), next.triggerAt)
        assertEquals(2L, next.alarms.single().block.routineBlockId)
    }

    @Test fun `simultaneous reminders are batched instead of overwriting one another`() {
        val batch = planner.next(snapshot(listOf(routine, routine.copy(id = 2))), monday.atStartOfDay().toInstant(ZoneOffset.UTC), utc)!!
        assertEquals(2, batch.alarms.size)
    }

    @Test fun `late stale cancelled and completed deliveries are suppressed`() {
        val fire = monday.atTime(7, 55).toInstant(ZoneOffset.UTC)
        assertEquals(1, planner.due(snapshot(), fire, fire.plusSeconds(60), utc).size)
        assertTrue(planner.due(snapshot(), fire, fire.plusSeconds(601), utc).isEmpty())
        assertTrue(planner.due(snapshot(), fire, fire.minusSeconds(1), utc).isEmpty())
        assertTrue(planner.due(snapshot(overrides = listOf(EventOverride(routineBlockId = 1, overrideDate = monday, isCancelled = true))), fire, fire, utc).isEmpty())
        assertTrue(planner.due(snapshot(completions = listOf(OccurrenceCompletion(1, monday))), fire, fire, utc).isEmpty())
        assertTrue(planner.due(snapshot(routines = listOf(routine.copy(startTime = LocalTime.of(8, 30)))), fire, fire, utc).isEmpty())
    }

    @Test fun `school holiday is skipped when scheduling and when delivering`() {
        val data = snapshot(routines = listOf(routine.copy(category = RoutineCategory.SCHOOL)),
            calendar = listOf(CalendarEntry(date = monday, title = "No school", isWorkFreeDay = true)))
        val now = monday.atStartOfDay().toInstant(ZoneOffset.UTC)
        assertEquals(monday.plusWeeks(1), planner.next(data, now, utc)!!.alarms.single().block.occurrenceDate)
        assertTrue(planner.due(data, monday.atTime(7, 55).toInstant(ZoneOffset.UTC), monday.atTime(7, 55).toInstant(ZoneOffset.UTC), utc).isEmpty())
    }

    @Test fun `spring DST gap shifts entire block and subtracts preview on instant`() {
        val date = LocalDate.of(2027, 3, 28)
        val input = block(150, 180).copy(date = date, occurrenceDate = date, startsAt = date.atTime(2, 30), endsAt = date.atTime(3, 0))
        val zone = ZoneId.of("Europe/Ljubljana")
        val window = OccurrenceTimes.window(input, zone)
        assertEquals(LocalTime.of(3, 30), window.start.atZone(zone).toLocalTime())
        assertEquals(LocalTime.of(4, 0), window.end.atZone(zone).toLocalTime())
        assertEquals(Duration.ofMinutes(30), Duration.between(window.start, window.end))
        assertEquals(window.start.minusSeconds(300), planner.forOccurrence(input, zone)!!.triggerAt)
    }

    @Test fun `autumn DST overlap chooses first offset once`() {
        val date = LocalDate.of(2026, 10, 25)
        val input = block(150, 210).copy(date = date, occurrenceDate = date, startsAt = date.atTime(2, 30), endsAt = date.atTime(3, 30))
        val window = OccurrenceTimes.window(input, ZoneId.of("Europe/Ljubljana"))
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), window.start)
        assertEquals(Duration.ofHours(2), Duration.between(window.start, window.end))
    }

    @Test fun `no enabled routines arms no notification`() {
        assertNull(planner.next(snapshot(listOf(routine.copy(isNotificationEnabled = false))), monday.atStartOfDay().toInstant(ZoneOffset.UTC), utc))
        assertNull(planner.forOccurrence(block(480, 540).copy(isCompleted = true), utc))
    }

    @Test fun `next boundary selects end when active and midnight when empty`() {
        val now = monday.atTime(8, 30).toInstant(ZoneOffset.UTC)
        assertEquals(monday.atTime(9, 0).toInstant(ZoneOffset.UTC), planner.nextWidgetBoundary(snapshot(), now, utc))
        assertEquals(monday.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC), planner.nextWidgetBoundary(snapshot(emptyList()), now, utc))
    }
}
