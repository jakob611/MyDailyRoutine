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
package com.example.mydailyroutine.features.entry.presentation

import com.example.mydailyroutine.core.presentation.TimetableRow
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetablePasteParserTest {

    @Test
    fun slovenianLine() {
        val rows = TimetablePasteParser.parse("Pon 08:00-08:50 Matematika")
        assertEquals(listOf(TimetableRow(DayOfWeek.MONDAY, 480, 530, "Matematika")), rows)
    }

    @Test
    fun endsAtMidnightAllowed() {
        val row = TimetablePasteParser.parse("Pet 22:00-24:00 Skupinsko delo").single()
        assertEquals(1440, row.endMinute)
    }

    @Test
    fun severalRangesOnOneGridRow() {
        val rows = TimetablePasteParser.parse("Tor 08:00-08:50 Kemi 09:00-09:50 Bio")
        assertEquals(2, rows.size)
        assertEquals(listOf("Kemi", "Bio"), rows.map { it.title })
    }

    @Test
    fun titleBeforeTimeIsFound() {
        val row = TimetablePasteParser.parse("Sre angleščina 09:00-10:00").single()
        assertEquals(DayOfWeek.WEDNESDAY, row.day)
        assertEquals("angleščina", row.title)
    }

    @Test
    fun malformedLinesAreSkipped() {
        val text = "Pon 09:00-08:00 Zamenjani uri\nNeznan dan 08:00-09:00 Predmet\nbrez uric in dneva"
        assertTrue(TimetablePasteParser.parse(text).isEmpty())
    }

    @Test
    fun sortedByDayThenTime() {
        val text = "Pet 16:00-16:50 E\nPon 10:00-10:50 B\nPon 08:00-08:50 A"
        val rows = TimetablePasteParser.parse(text)
        assertEquals(listOf("A", "B", "E"), rows.map { it.title })
    }

    @Test
    fun diacriticsAndAbbreviations() {
        val row = TimetablePasteParser.parse("Črt 10:00-10:50 Fizika").single()
        assertEquals(DayOfWeek.THURSDAY, row.day)
    }

    @Test
    fun duplicatesCollapsed() {
        val rows = TimetablePasteParser.parse("Pon 08:00-08:50 Mat\nPon 08:00-08:50 Mat")
        assertEquals(1, rows.size)
    }
}
