/*
 * Copyright 2026 MyDailyRoutine contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mydailyroutine.core.platform

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextParserTest {

    @Test
    fun isoDeadline() {
        val draft = ShareTextParser.parse("Math IA - deadline 2026-10-15 submit final draft")
        assertEquals(LocalDate.of(2026, 10, 15).toEpochDay(), draft.dueEpochDay)
        assertTrue(draft.title.contains("Math IA"))
        assertTrue(!draft.title.contains("2026"))
    }

    @Test
    fun slovenianDottedDate() {
        val draft = ShareTextParser.parse("Fizija: oddaja - rok 15. 10. 2026")
        assertEquals(LocalDate.of(2026, 10, 15).toEpochDay(), draft.dueEpochDay)
        assertTrue(draft.title.contains("Fizija"))
    }

    @Test
    fun englishMonthDate() {
        val draft = ShareTextParser.parse("Chemistry lab report due Oct 15, 2026")
        assertEquals(LocalDate.of(2026, 10, 15).toEpochDay(), draft.dueEpochDay)
        assertTrue(draft.title.contains("Chemistry"))
    }

    @Test
    fun dayMonthYearOrder() {
        val draft = ShareTextParser.parse("Submit EE first draft on 12 Nov 2026")
        assertEquals(LocalDate.of(2026, 11, 12).toEpochDay(), draft.dueEpochDay)
    }

    @Test
    fun textWithoutDateStillProducesTitle() {
        val draft = ShareTextParser.parse("Read chapter 4 for the seminar")
        assertNull(draft.dueEpochDay)
        assertEquals("Read chapter 4 for the seminar", draft.title)
    }

    @Test
    fun urlOnlyShareFallsBackToPlaceholder() {
        val draft = ShareTextParser.parse("https://managebac.com/assignments/42")
        assertNull(draft.dueEpochDay)
        assertEquals("Naloga", draft.title)
    }

    @Test
    fun implausibleYearIgnored() {
        val draft = ShareTextParser.parse("Legacy plan from 1988-01-02")
        assertNull(draft.dueEpochDay)
    }
}
