package com.example.mydailyroutine.features.goals.presentation

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Half an hour is half an hour.
 *
 * Progress is stored as decimal hours and the rows used to print it with `toInt()`, which turned a
 * 30-minute entry into "0 h" and made every 45-minute one invisible. These are the cases the reader
 * sees on the CAS/EE screen, pinned so a rounding change cannot quietly bring the zero back.
 */
class GoalsProgressFormatTest {
    private val slovenian = Locale.forLanguageTag("sl-SI")
    private val english = Locale.ENGLISH

    @Test fun aHalfHourIsNotZero() {
        assertEquals("0,5", formatHoursRounded(0.5, slovenian))
        assertEquals("0.5", formatHoursRounded(0.5, english))
    }

    @Test fun wholeHoursStayWhole() {
        assertEquals("1", formatHoursRounded(1.0, slovenian))
        assertEquals("12", formatHoursRounded(12.0, english))
    }

    @Test fun anHourAndAHalfSurvivesBothLanguages() {
        assertEquals("1,5", formatHoursRounded(1.5, slovenian))
        assertEquals("1.5", formatHoursRounded(1.5, english))
    }

    @Test fun threeTapsOfHalfAnHourDoNotReadAsZero() {
        assertEquals("1,5", formatHoursRounded(0.5 * 3, slovenian))
        assertEquals("37,5", formatHoursRounded(0.5 * 75, slovenian))
    }

    @Test fun aFortiethOfAnHourStillRoundsToSomething() {
        assertEquals("0,1", formatHoursRounded(0.05, slovenian))
    }
}
