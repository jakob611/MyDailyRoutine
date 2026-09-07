package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.repository.TimelineResolver
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class TimelineResolverTest {
    private val resolver = TimelineResolver()
    private val routine = RoutineBlueprint(1, 1, "Math HL", RoutineCategory.SCHOOL, DayOfWeek.MONDAY,
        LocalTime.of(8, 0), LocalTime.of(9, 0), true)
    private val subject = Subject(1, "Mathematics", 0xFF4499CC, 60)
    private fun snapshot(
        routines: List<RoutineBlueprint> = listOf(routine), overrides: List<EventOverride> = emptyList(),
        calendar: List<CalendarEntry> = emptyList(), milestones: List<Milestone> = emptyList(),
        completions: List<OccurrenceCompletion> = emptyList(),
    ) = ScheduleSnapshot(monday, monday.plusDays(1), listOf(subject), routines, overrides, calendar, milestones, completions)

    @Test fun `weekly day and subject are resolved`() {
        val data = snapshot()
        val item = resolver.resolve(monday, data).single() as ResolvedTimelineItem.Block
        assertEquals("Mathematics", item.subject?.name)
        assertEquals(480, item.startMinute)
        assertTrue(resolver.resolve(monday.plusDays(1), data).isEmpty())
    }

    @Test fun `overrides are date scoped and partial values inherit`() {
        val override = EventOverride(routineBlockId = 1, overrideDate = monday, customStartTime = LocalTime.of(8, 30), customTitle = "Revision")
        val data = snapshot(overrides = listOf(override))
        val today = resolver.resolve(monday, data).single() as ResolvedTimelineItem.Block
        assertEquals("Revision", today.title)
        assertEquals(510, today.startMinute)
        assertEquals(540, today.endMinute)
        assertEquals("Math HL", resolver.resolve(monday.plusWeeks(1), data).single().title)
    }

    @Test fun `cancellation excludes only one occurrence`() {
        val data = snapshot(overrides = listOf(EventOverride(routineBlockId = 1, overrideDate = monday, isCancelled = true)))
        assertTrue(resolver.resolve(monday, data).isEmpty())
        assertEquals(1, resolver.resolve(monday.plusWeeks(1), data).size)
    }

    @Test fun `holidays tag school without suppressing personal study`() {
        val data = snapshot(routines = listOf(routine, routine.copy(id = 2, category = RoutineCategory.FOCUS_ANALYTICAL)),
            calendar = listOf(CalendarEntry(date = monday, title = "Vacation", isWorkFreeDay = true)))
        val items = resolver.resolve(monday, data).filterIsInstance<ResolvedTimelineItem.Block>()
        assertTrue(items.single { it.routineBlockId == 1L }.isSuppressed)
        assertFalse(items.single { it.routineBlockId == 2L }.isSuppressed)
    }

    @Test fun `non work free observance is not a school holiday`() {
        val data = snapshot(calendar = listOf(CalendarEntry(date = monday, title = "Observance", isWorkFreeDay = false)))
        assertFalse((resolver.resolve(monday, data).single() as ResolvedTimelineItem.Block).isSuppressed)
    }

    @Test fun `milestones including all day are sorted chronologically with stable ties`() {
        val data = snapshot(milestones = listOf(
            Milestone(2, null, "Later", monday, LocalTime.NOON, false),
            Milestone(1, 1, "All day", monday, null, true),
            Milestone(3, 1, "Timed", monday, LocalTime.of(8, 0), true),
        ))
        assertEquals(listOf("All day", "Timed", "Math HL", "Later"), resolver.resolve(monday, data).map { it.title })
    }

    @Test fun `overnight block carries into next day and keeps completion ownership`() {
        val data = snapshot(routines = listOf(routine.copy(startTime = LocalTime.of(23, 0), endTime = LocalTime.of(7, 0), category = RoutineCategory.ADMIN)),
            completions = listOf(OccurrenceCompletion(1, monday)))
        val start = resolver.resolve(monday, data).single() as ResolvedTimelineItem.Block
        val next = resolver.resolve(monday.plusDays(1), data).single() as ResolvedTimelineItem.Block
        assertEquals(60, start.durationMinutes)
        assertEquals(420, next.durationMinutes)
        assertEquals(0, next.startMinute)
        assertTrue(next.isCarryIn)
        assertTrue(next.isCompleted)
        assertEquals(start.occurrenceKey, next.occurrenceKey)
        assertNotEquals(start.key, next.key)
    }

    @Test fun `cancelled overnight occurrence has no next day carry in`() {
        val data = snapshot(routines = listOf(routine.copy(startTime = LocalTime.of(23, 0), endTime = LocalTime.of(2, 0))),
            overrides = listOf(EventOverride(routineBlockId = 1, overrideDate = monday, isCancelled = true)))
        assertTrue(resolver.resolve(monday.plusDays(1), data).isEmpty())
    }

    @Test fun `one off and future weekly validity bounds are honored`() {
        val data = snapshot(routines = listOf(routine.copy(validFrom = monday, validUntil = monday)))
        assertEquals(1, resolver.resolve(monday, data).size)
        assertTrue(resolver.resolve(monday.minusWeeks(1), data).isEmpty())
        assertTrue(resolver.resolve(monday.plusWeeks(1), data).isEmpty())
    }

    @Test fun `skipped overnight carry in is restorable from the following day`() {
        val data = snapshot(routines = listOf(routine.copy(startTime = LocalTime.of(23, 0), endTime = LocalTime.of(7, 0))),
            overrides = listOf(EventOverride(routineBlockId = 1, overrideDate = monday, isCancelled = true)))
        val skipped = resolver.prepare(data).cancelledForDate(monday.plusDays(1)).single()
        assertEquals(monday, skipped.occurrenceDate)
        assertEquals(1L, skipped.routineBlockId)
    }

    @Test fun `block ending exactly at midnight has no restorable carry in`() {
        val data = snapshot(routines = listOf(routine.copy(startTime = LocalTime.of(23, 0), endTime = LocalTime.MIDNIGHT)),
            overrides = listOf(EventOverride(routineBlockId = 1, overrideDate = monday, isCancelled = true)))
        assertTrue(resolver.prepare(data).cancelledForDate(monday.plusDays(1)).isEmpty())
    }

    @Test fun `completion does not complete next weeks blueprint`() {
        val data = snapshot(completions = listOf(OccurrenceCompletion(1, monday)))
        assertTrue(resolver.resolve(monday, data).single().isCompleted)
        assertFalse(resolver.resolve(monday.plusWeeks(1), data).single().isCompleted)
    }
}
