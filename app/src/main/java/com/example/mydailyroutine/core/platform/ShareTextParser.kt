package com.example.mydailyroutine.core.platform

import java.time.LocalDate

/** A task draft extracted from shared text (e.g. a deadline shared straight from the ManageBac app). */
data class SharedTaskDraft(val title: String, val dueEpochDay: Long?)

/**
 * Pulls a due date out of arbitrary shared text and cleans the remaining title.
 * Deliberately forgiving: an unreadable date simply means "no date", the sheet always lets the user fix things.
 */
object ShareTextParser {
    private val MonthNames = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "maj" to 5, "may" to 5, "jun" to 6, "jul" to 7,
        "avg" to 8, "aug" to 8, "sep" to 9, "okt" to 10, "oct" to 10, "nov" to 11, "dec" to 12,
    )

    private val Candidates = listOf(
        Regex("""(\d{4})-(\d{1,2})-(\d{1,2})"""),
        Regex("""(\d{1,2})[.]\s?(\d{1,2})[.]\s?(\d{4})"""),
        Regex("""(?i)(\d{1,2})(?:st|nd|rd|th)?\s+([a-z]{3,9})\.?,?\s+(\d{4})"""),
        Regex("""(?i)([a-z]{3,9})\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4})"""),
    )

    fun parse(raw: String?): SharedTaskDraft? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return null
        var due: LocalDate? = null
        var matched = ""
        for (regex in Candidates) {
            val match = regex.find(text) ?: continue
            val parsed = buildDate(match.groupValues.drop(1))
            if (parsed != null) { due = parsed; matched = match.value; break }
        }
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("http") }.toList()
        var title = (lines.firstOrNull { matched.isEmpty() || it.contains(matched) } ?: lines.firstOrNull()).orEmpty()
        if (matched.isNotEmpty()) title = title.replace(matched, " ")
        title = title.replace(Regex("""(?i)\s*\b(due date|due|deadline|rok)\b\s*[:=-]?\s*""), " ")
            .trim().trim('-', '–', '—', ':', ';', ',').trim().replace(Regex("""\s{2,}""), " ")
        if (title.isBlank()) title = text.replace(Regex("""https?://\S+"""), " ").trim().ifBlank { "Naloga" }
        return SharedTaskDraft(title.take(120), due?.toEpochDay())
    }

    private fun buildDate(parts: List<String>): LocalDate? = try {
        val a = parts[0].trim(); val b = parts[1].trim(); val c = parts[2].trim()
        fun year(v: String): Int? = if (v.length == 2) 2000 + (v.toIntOrNull() ?: return null) else v.toIntOrNull()
        val day = a.toIntOrNull()
        val date = when {
            a.length == 4 -> LocalDate.of(a.toInt(), b.toInt(), c.toInt())
            MonthNames.containsKey(b.take(3).lowercase()) && day != null -> LocalDate.of(year(c) ?: return null, MonthNames.getValue(b.take(3).lowercase()), day)
            MonthNames.containsKey(a.take(3).lowercase()) -> LocalDate.of(year(c) ?: return null, MonthNames.getValue(a.take(3).lowercase()), b.toInt())
            else -> {
                val m = b.toIntOrNull() ?: return null
                val y = year(c) ?: return null
                if (day != null && m in 1..12 && day in 1..31) LocalDate.of(y, m, day) else null
            }
        }
        date?.takeIf { it.year in 2020..2100 }
    } catch (error: Exception) { null }
}
