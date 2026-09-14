package com.example.mydailyroutine.core.platform

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ShareTextParserTest {
    @Test fun isoDeadlineWithSubjectPrefix() {
        val draft = ShareTextParser.parse("Math IA — deadline 2026-10-15 submit final draft")
        assertNotNull(draft)
        assertEquals(LocalDate.of(2026, 10, 15), LocalDate.ofEpochDay(draft!!.dueEpochDay!!))
        assertFalse(draft.title.contains("2026-10-15"))
        assertTrue(draft.title.contains("IA"))
    }

    @Test fun englishMonthNameFormats() {
        val forward = ShareTextParser.parse("Essay due Oct 15, 2026")!!.dueEpochDay
        val back = ShareTextParser.parse("Essay due 15 Oct 2026")!!.dueEpochDay
        assertEquals(LocalDate.of(2026, 10, 15), LocalDate.ofEpochDay(forward!!))
        assertEquals(forward, back)
    }

    @Test fun slovenianDottedDate() {
        val draft = ShareTextParser.parse("Rok oddaje: 15. 10. 2026 — Fizija IA")
        assertEquals(LocalDate.of(2026, 10, 15), LocalDate.ofEpochDay(draft!!.dueEpochDay!!))
        assertTrue(draft.title.contains("Fizija"))
    }

    @Test fun textWithoutDateStillProducesTitle() {
        val draft = ShareTextParser.parse("Read chapter 4 for tomorrow's seminar")
        assertNull(draft!!.dueEpochDay)
        assertEquals("Read chapter 4 for tomorrow's seminar", draft.title)
    }

    @Test fun urlOnlyShareFallsBackToPlaceholder() {
        val draft = ShareTextParser.parse("https://school.managebac.com/tasks/123")
        assertEquals("Naloga", draft!!.title)
    }

    @Test fun blankInputIsRejected() {
        assertNull(ShareTextParser.parse(null))
        assertNull(ShareTextParser.parse("   "))
    }

    @Test fun nonsenseNumbersAreNotDates() {
        val draft = ShareTextParser.parse("Chapter 23:14 vs 99:99 notes")
        assertNull(draft!!.dueEpochDay)
    }

    @Test fun farFutureYearIsIgnored() {
        assertNull(ShareTextParser.parse("Legacy plan from 1988-01-02")!!.dueEpochDay)
    }
}
