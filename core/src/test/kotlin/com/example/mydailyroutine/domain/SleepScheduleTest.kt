package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.routines.SleepSchedule
import java.time.DayOfWeek
import com.example.mydailyroutine.domain.routines.Weekdays
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The sleep rhythm the settings sheet edits and the patterns repository encodes as managed blocks.
 * Sleep crosses midnight more often than not, so the overnight length is the case that matters.
 */
class SleepScheduleTest {
    @Test fun defaultsAreARealisticSchoolNight() {
        val sleep = SleepSchedule()
        assertEquals(LocalTime.of(23, 0), sleep.bedtime)
        assertEquals(LocalTime.of(7, 0), sleep.wakeTime)
        assertEquals(480, sleep.durationMinutes)
        assertEquals(30, sleep.morningBufferMinutes)
    }

    @Test fun overnightDurationWrapsPastMidnight() {
        val sleep = SleepSchedule(bedtime = LocalTime.of(22, 30), wakeTime = LocalTime.of(6, 45))
        assertEquals(495, sleep.durationMinutes)
    }

    @Test fun weekendVariantHasItsOwnLength() {
        val sleep = SleepSchedule(
            weekendEnabled = true,
            weekendBedtime = LocalTime.of(0, 30),
            weekendWakeTime = LocalTime.of(9, 30),
        )
        assertEquals(540, sleep.weekendDurationMinutes)
    }

    @Test fun bedAndWakeMustDiffer() {
        assertFailsWith<IllegalArgumentException> {
            SleepSchedule(bedtime = LocalTime.of(23, 0), wakeTime = LocalTime.of(23, 0))
        }
    }

    @Test fun anInvalidWeekendVariantFailsOnlyWhenEnabled() {
        assertFailsWith<IllegalArgumentException> {
            SleepSchedule(weekendEnabled = true, weekendBedtime = LocalTime.of(9, 0), weekendWakeTime = LocalTime.of(9, 0))
        }
        SleepSchedule(weekendEnabled = false, weekendBedtime = LocalTime.of(9, 0), weekendWakeTime = LocalTime.of(9, 0))
    }

    @Test fun weekdayMaskAndBufferStayInsideBounds() {
        assertFailsWith<IllegalArgumentException> { SleepSchedule(weekdaysMask = 0) }
        assertFailsWith<IllegalArgumentException> { SleepSchedule(morningBufferMinutes = 121) }
        assertEquals(Weekdays.ALL, SleepSchedule(weekdaysMask = Weekdays.ALL).weekdaysMask)
    }

    @Test fun workdayMaskShiftsToNightBeforeSchoolDays() {
        // "Before workdays" in the sleep card is the workday mask moved one day earlier, so the
        // rhythm covers Sunday night to Thursday night — the nights that precede Mon-Fri classes.
        assertEquals(0b1001111, Weekdays.shifted(Weekdays.WORKDAYS, -1))
        assertEquals(Weekdays.WORKDAYS, Weekdays.shifted(Weekdays.WORKDAYS, 0))
        assertEquals(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
            Weekdays.fromMask(Weekdays.shifted(Weekdays.WORKDAYS, -1)))
        assertEquals(Weekdays.ALL, Weekdays.mask(Weekdays.fromMask(Weekdays.ALL)))
    }
}
