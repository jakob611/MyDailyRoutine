package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.routines.EntryDefaults
import com.example.mydailyroutine.domain.routines.TimeEntryState
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * The time pair behind every add-block and edit-block sheet. Its contract is the one the device
 * test drives through the wheel: moving the start keeps the length, editing the end re-measures it,
 * and a school category states its length once through [TimeEntryState.withDuration].
 */
class TimeEntryStateTest {
    @Test fun atFormatsStartEndAndDuration() {
        val state = TimeEntryState.at(LocalTime.of(8, 0), 45)
        assertEquals("08:00", state.startText)
        assertEquals("08:45", state.endText)
        assertEquals(45, state.durationMinutes)
    }

    @Test fun changingStartKeepsTheDuration() {
        val state = TimeEntryState.at(LocalTime.of(8, 0), 50).withStart("09:00")
        assertEquals("09:00", state.startText)
        assertEquals("09:50", state.endText)
        assertEquals(50, state.durationMinutes)
    }

    @Test fun changingStartAcrossMidnightKeepsTheDuration() {
        val state = TimeEntryState.at(LocalTime.of(23, 30), 90).withStart("23:45")
        assertEquals("01:15", state.endText)
    }

    @Test fun editingTheEndReMeasuresTheDuration() {
        val state = TimeEntryState.at(LocalTime.of(8, 0), 45).withEnd("08:50")
        assertEquals("08:50", state.endText)
        assertEquals(50, state.durationMinutes)
    }

    @Test fun anUnparsableEndKeepsThePreviousDuration() {
        val state = TimeEntryState.at(LocalTime.of(8, 0), 45).withEnd("7:4")
        assertEquals("7:4", state.endText)
        assertEquals(45, state.durationMinutes)
    }

    @Test fun anUnparsableStartMovesNothing() {
        val state = TimeEntryState.at(LocalTime.of(8, 0), 45).withStart("neveljavno")
        assertEquals("08:00", state.startText)
        assertEquals("08:45", state.endText)
    }

    @Test fun withDurationRestatesTheEnd() {
        val state = TimeEntryState.at(LocalTime.of(8, 0), 45).withDuration(60)
        assertEquals("09:00", state.endText)
        assertEquals(60, state.durationMinutes)
    }

    @Test fun zeroAndDayLongDurationsAreRejected() {
        assertFailsWith<IllegalArgumentException> { TimeEntryState("08:00", "08:45", 0) }
        assertFailsWith<IllegalArgumentException> { TimeEntryState("08:00", "08:45", 1440) }
        assertFailsWith<IllegalArgumentException> { TimeEntryState.at(LocalTime.of(8, 0), 45).withDuration(0) }
    }

    @Test fun entryDefaultsStayInsideSchoolReality() {
        assertEquals(45, EntryDefaults().lessonDurationMinutes)
        assertFailsWith<IllegalArgumentException> { EntryDefaults(lessonDurationMinutes = 241) }
        assertFailsWith<IllegalArgumentException> { EntryDefaults(lessonBreakMinutes = 0) }
    }

    @Test fun parseTimeAcceptsOnlyMinutePrecisionClockText() {
        assertEquals(LocalTime.of(7, 5), com.example.mydailyroutine.domain.model.ScheduleValidation.parseTime("07:05"))
        assertNull(com.example.mydailyroutine.domain.model.ScheduleValidation.parseTime("7:5"))
        assertNull(com.example.mydailyroutine.domain.model.ScheduleValidation.parseTime(""))
    }
}
