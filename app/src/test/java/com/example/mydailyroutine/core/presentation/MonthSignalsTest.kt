package com.example.mydailyroutine.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule this file protects: a month cell says *what kind of day* it is with one flat colour and
 * *how much* is planned with up to three marks, so no day is painted louder than another merely
 * because it is fuller. It also protects the shapes: a test and a deadline must never end up as the
 * same symbol, because that is how the grid used to look to everyone, colour vision or not.
 */
class MonthSignalsTest {
    @Test fun aFullerDayIsNotADifferentKindOfDay() {
        val quiet = MonthSignals.of(focusMinutes = 60, examCount = 0, deadlineCount = 0, isWorkFree = false, hasSchool = true)
        val full = MonthSignals.of(focusMinutes = 240, examCount = 0, deadlineCount = 0, isWorkFree = false, hasSchool = true)
        assertEquals(MonthDayType.School, quiet.dayType)
        assertEquals(MonthDayType.School, full.dayType)
        // The only thing that changes is the number of marks, which stays readable in greyscale.
        assertEquals(1, quiet.bars)
        assertEquals(3, full.bars)
    }

    @Test fun barsCountStartedHoursAndStopAtThree() {
        assertEquals(0, MonthSignals.barsFor(0))
        assertEquals(1, MonthSignals.barsFor(1))
        assertEquals(1, MonthSignals.barsFor(60))
        assertEquals(2, MonthSignals.barsFor(61))
        assertEquals(2, MonthSignals.barsFor(120))
        assertEquals(3, MonthSignals.barsFor(121))
        assertEquals(3, MonthSignals.barsFor(600))
    }

    @Test fun aWorkFreeDayIsFreeEvenWhenSomethingIsPlanned() {
        val signals = MonthSignals.of(
            focusMinutes = 120, examCount = 1, deadlineCount = 0, isWorkFree = true, hasSchool = true,
        )
        assertEquals(MonthDayType.Free, signals.dayType)
    }

    @Test fun aTestAndADeadlineAreDifferentShapes() {
        val test = MonthSignals.of(focusMinutes = 0, examCount = 1, deadlineCount = 0, isWorkFree = false, hasSchool = true)
        val deadline = MonthSignals.of(focusMinutes = 0, examCount = 0, deadlineCount = 1, isWorkFree = false, hasSchool = true)
        assertEquals(listOf(MonthMark.Triangle), test.marks)
        assertEquals(listOf(MonthMark.Diamond), deadline.marks)
        assertNotEquals(test.marks, deadline.marks)
    }

    @Test fun bothMarksFitOnTheSameDay() {
        val signals = MonthSignals.of(focusMinutes = 90, examCount = 1, deadlineCount = 1, isWorkFree = false, hasSchool = true)
        assertEquals(listOf(MonthMark.Triangle, MonthMark.Diamond), signals.marks)
    }

    @Test fun theFocusDotOnlyAppearsWhenNothingElseDoes() {
        val alone = MonthSignals.of(focusMinutes = 45, examCount = 0, deadlineCount = 0, isWorkFree = false, hasSchool = false)
        assertEquals(MonthDayType.Focus, alone.dayType)
        assertEquals(listOf(MonthMark.Dot), alone.marks)

        val crowded = MonthSignals.of(focusMinutes = 45, examCount = 0, deadlineCount = 1, isWorkFree = false, hasSchool = false)
        assertEquals(listOf(MonthMark.Diamond), crowded.marks)
    }

    @Test fun anEmptyDayStaysEmpty() {
        val signals = MonthSignals.of(focusMinutes = 0, examCount = 0, deadlineCount = 0, isWorkFree = false, hasSchool = false)
        assertEquals(MonthDayType.Quiet, signals.dayType)
        assertTrue(signals.marks.isEmpty())
        assertEquals(0, signals.bars)
    }

    @Test fun aDayOffShowsTheRing() {
        val signals = MonthSignals.of(focusMinutes = 0, examCount = 0, deadlineCount = 0, isWorkFree = true, hasSchool = false)
        assertEquals(listOf(MonthMark.Ring), signals.marks)
    }
}
