package com.example.mydailyroutine.features.entry.presentation

import com.example.mydailyroutine.core.presentation.TimetableRow
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek

class TimetablePasteParserTest {
    @Test fun slovenianLine() {
        val rows = TimetablePasteParser.parse("Pon 08:00-08:50 Matematika")
        assertEquals(listOf(TimetableRow(DayOfWeek.MONDAY, 480, 530, "Matematika")), rows)
    }

    @Test fun englishLineWithFullDayAndDash() {
        val rows = TimetablePasteParser.parse("Monday 8:00–8:50 Physics")
        assertEquals(1, rows.size)
        assertEquals(DayOfWeek.MONDAY, rows[0].day)
        assertEquals("Physics", rows[0].title)
    }

    @Test fun multipleRangesOnOneGridLine() {
        val rows = TimetablePasteParser.parse("Tor 08:00-08:50 Kemi 09:00-09:50 Bio")
        assertEquals(2, rows.size)
        assertEquals(DayOfWeek.TUESDAY, rows[0].day)
        assertEquals("Kemi", rows[0].title)
        assertEquals("Bio", rows[1].title)
        assertEquals(540, rows[1].startMinute)
    }

    @Test fun missingTitleMakesRowInvalid() {
        assertTrue(TimetablePasteParser.parse("Sre 10:00-11:00").isEmpty())
        assertEquals("angleščina", TimetablePasteParser.parse("Ned angleščina 09:00-10:00").single().title)
    }

    @Test fun malformedLinesAreSkipped() {
        assertTrue(TimetablePasteParser.parse("brez urice in dneva").isEmpty())
        assertTrue(TimetablePasteParser.parse("Pon 09:00-08:00 Zgodnji konec").isEmpty())
        assertTrue(TimetablePasteParser.parse("Neznan dan 08:00-09:00 Predmet").isEmpty())
    }

    @Test fun endsAtMidnightClamps() {
        val rows = TimetablePasteParser.parse("Pet 22:00-24:00 Skupinsko delo")
        assertEquals(1440, rows.single().endMinute)
    }

    @Test fun sortedByDayThenTime() {
        val rows = TimetablePasteParser.parse("Pet 08:00-08:50 E\nPon 10:00-10:50 B\nPon 08:00-08:50 A")
        assertEquals(listOf("A", "B", "E"), rows.map { it.title })
    }

    @Test fun diacriticsNormalize() {
        assertEquals(listOf(TimetableRow(DayOfWeek.THURSDAY, 600, 650, "Fizika")), TimetablePasteParser.parse("Črt 10:00-10:50 Fizika"))
    }
}
