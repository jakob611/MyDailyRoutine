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
import java.time.Month

/** Best-effort task draft parsed from a shared/plain text snippet (e.g. a ManageBac deadline). */
data class SharedTaskDraft(val title: String, val dueEpochDay: Long?)

/**
 * Tolerant one-shot parser for deadline texts students share from ManageBac or chat:
 * recognizes ISO (2026-10-15), European dotted (15. 10. 2026) and English month-date forms in SL+EN,
 * strips the noise around them and keeps the rest as the task title. No schema, no network -
 * the app stays fully usable when the school feed is unavailable.
 */
object ShareTextParser {

    private val DateShapes = listOf(
        Regex("""(\d{4})-(\d{1,2})-(\d{1,2})"""),
        Regex("""(\d{1,2})[.]\s?(\d{1,2})[.]\s?(\d{4})"""),
        Regex("""(?i)(\d{1,2})(?:st|nd|rd|th)?\s+([a-z]{3,9})\.?,?\s+(\d{4})"""),
        Regex("""(?i)([a-z]{3,9})\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4})"""),
    )
    private val NoiseWord = Regex("""(?i)\s*\b(due date|due|deadline|rok)\b\s*[:=-]?\s*""")
    private val UrlToken = Regex("""https?://\S+""")

    private val MonthNames: Map<String, Month> = listOf(
        "jan" to Month.JANUARY, "feb" to Month.FEBRUARY, "mar" to Month.MARCH, "apr" to Month.APRIL,
        "maj" to Month.MAY, "may" to Month.MAY, "jun" to Month.JUNE, "jul" to Month.JULY,
        "avg" to Month.AUGUST, "aug" to Month.AUGUST, "sep" to Month.SEPTEMBER, "okt" to Month.OCTOBER,
        "oct" to Month.OCTOBER, "nov" to Month.NOVEMBER, "dec" to Month.DECEMBER,
    ).toMap()

    fun parse(text: String): SharedTaskDraft {
        val trimmed = text.trim()
        var due: LocalDate? = null
        var matched = ""
        for (shape in DateShapes) {
            val match = shape.find(trimmed) ?: continue
            val parsed = buildDate(listOf(match.groupValues[1], match.groupValues[2], match.groupValues[3]))
            if (parsed != null) {
                due = parsed
                matched = match.value
                break
            }
        }
        var title = trimmed.lineSequence().firstOrNull { it.isNotBlank() && !UrlToken.matches(it.trim()) }.orEmpty()
        if (matched.isNotEmpty()) title = title.replace(matched, " ")
        title = title.replace(NoiseWord, " ")
            .trim().trim('-', '–', '—', ':', ';', ',').trim()
            .replace(Regex("\\s{2,}"), " ")
        if (title.isBlank()) title = trimmed.replace(UrlToken, " ").trim().ifBlank { "Naloga" }
        return SharedTaskDraft(title.take(120), due?.toEpochDay())
    }

    private fun buildDate(parts: List<String>): LocalDate? {
        return try {
            val a = parts[0].trim()
            val b = parts[1].trim()
            val c = parts[2].trim()
            fun year(v: String): Int? {
                val n = v.toIntOrNull() ?: return null
                return if (v.length == 2) 2000 + n else n
            }
            val day = a.toIntOrNull()
            val monthKey = b.take(3).lowercase()
            val date = when {
                a.length == 4 -> LocalDate.of(a.toInt(), b.toInt(), c.toInt())
                day != null && MonthNames.containsKey(monthKey) ->
                    LocalDate.of(year(c) ?: return null, MonthNames.getValue(monthKey), day)
                MonthNames.containsKey(a.take(3).lowercase()) ->
                    LocalDate.of(year(c) ?: return null, MonthNames.getValue(a.take(3).lowercase()), b.toInt())
                else -> {
                    val m = b.toIntOrNull() ?: return null
                    val y = year(c) ?: return null
                    if (day != null && m in 1..12 && day in 1..31) LocalDate.of(y, m, day) else null
                }
            }
            date?.takeIf { it.year in 2020..2100 }
        } catch (error: Exception) {
            null
        }
    }
}
