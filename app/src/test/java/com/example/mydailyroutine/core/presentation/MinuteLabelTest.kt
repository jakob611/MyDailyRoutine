package com.example.mydailyroutine.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The gutter of every card on the timeline, pinned.
 *
 * The label is built by hand instead of by `String.format`, because a day of twenty blocks asks for
 * it forty times per recomposition. Hand-built padding is the kind of thing that is right for the
 * value someone tested and one character short for the value nobody did, and a short label does not
 * just read wrong: the gutter is a fixed-width column, so every card beside it shifts.
 */
class MinuteLabelTest {
    @Test fun theEdgesOfTheDayAreFiveCharacters() {
        assertEquals("00:00", minuteLabel(0))
        assertEquals("23:59", minuteLabel(1439))
    }

    @Test fun singleDigitsArePaddedNotShortened() {
        assertEquals("07:05", minuteLabel(425))
        assertEquals("09:09", minuteLabel(549))
        assertEquals("00:01", minuteLabel(1))
        assertEquals("01:00", minuteLabel(60))
    }

    @Test fun noonAndTheHoursAroundItStayTwoDigits() {
        assertEquals("12:00", minuteLabel(720))
        assertEquals("10:10", minuteLabel(610))
        assertEquals("23:00", minuteLabel(1380))
    }
}
