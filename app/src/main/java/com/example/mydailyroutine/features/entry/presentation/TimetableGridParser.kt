package com.example.mydailyroutine.features.entry.presentation

import com.example.mydailyroutine.core.presentation.TimetableRow
import com.example.mydailyroutine.domain.import.PdfTextExtractor
import java.time.DayOfWeek

/**
 * Rebuilds the week a school's PDF *draws*: a grid of day columns and period rows.
 *
 * This is the second half of the PDF import, and it exists because a school timetable is not a list
 * of lines — it is a table. Reading it as text loses the one thing that matters, which column a
 * period belongs to: a cell that is empty on Tuesday shifts every following title by one day, and the
 * reader ends up with a week that looks plausible and is wrong.
 *
 * The layout it understands is the one ManageBac exports (and the one a printed week in general has):
 * a header of day names once per page, one time range per cell, and the subject drawn in the same
 * column as that time — under it in ManageBac's own export, which is why the title is looked up
 * *below* the time and not above it. Pages are handled separately, because the header repeats on
 * every one of them, and a week that continues on a second page must not be read into the first.
 *
 * Two limits are deliberate: the grid only counts when at least two day columns and two periods are
 * found (otherwise this is not a table and the caller should fall back to reading text), and a column
 * is only accepted when a run is close enough to it, so the period-number column on the left never
 * swallows a period.
 */
object TimetableGridParser {

    /** The week, plus whether the file numbered its columns instead of naming weekdays. */
    data class Grid(val rows: List<TimetableRow>, val numberedDays: Boolean)

    private val Time = Regex(
        "(\\d{1,2})[:.](\\d{2})\\s*([AaPp]\\.?[Mm]\\.?)?\\s*[-–—]\\s*(\\d{1,2})[:.](\\d{2})\\s*([AaPp]\\.?[Mm]\\.?)?",
    )
    private val NumberedDay = Regex("(?:day|dan)\\s*(\\d+)", RegexOption.IGNORE_CASE)

    /** Weekday tokens in both languages the app speaks, so a school's file reads either way. */
    private val DayNames: Map<String, DayOfWeek> = listOf(
        DayOfWeek.MONDAY to listOf("mon", "monday", "pon", "ponedeljek"),
        DayOfWeek.TUESDAY to listOf("tue", "tues", "tuesday", "tor", "torek"),
        DayOfWeek.WEDNESDAY to listOf("wed", "wednesday", "sre", "sreda"),
        DayOfWeek.THURSDAY to listOf("thu", "thur", "thurs", "thursday", "cet", "cetrtek", "čet", "četrtek"),
        DayOfWeek.FRIDAY to listOf("fri", "friday", "pet", "petek"),
        DayOfWeek.SATURDAY to listOf("sat", "saturday", "sob", "sobota"),
        DayOfWeek.SUNDAY to listOf("sun", "sunday", "ned", "nedelja"),
    ).flatMap { (day, names) -> names.map { name -> name to day } }.toMap()

    /** How far above a time its title may sit, in PDF points. One cell is about 30 pt tall. */
    private const val CellHeight = 34f

    /** Two runs belong to the same drawn line when their baselines are this close. */
    private const val LineTolerance = 2f

    fun parse(document: PdfTextExtractor.Document, firstColumnDay: DayOfWeek = DayOfWeek.MONDAY): Grid {
        val rows = mutableListOf<TimetableRow>()
        var numbered = false
        document.pages.forEach { page ->
            val parsed = parsePage(page.runs, firstColumnDay)
            numbered = numbered || parsed.numberedDays
            rows += parsed.rows
        }
        val week = rows.distinct().sortedWith(compareBy({ it.day }, { it.startMinute }, { it.title }))
        // One column and one period is not a table; callers fall back to reading text instead.
        val weekdays = week.map { it.day }.distinct().size
        val starts = week.map { it.startMinute }.distinct().size
        return Grid(if (weekdays >= 2 && starts >= 2) week else emptyList(), numbered)
    }

    private fun parsePage(runs: List<PdfTextExtractor.Run>, firstColumnDay: DayOfWeek): Grid {
        val columns = columns(runs)
        if (columns.size < 2) return Grid(emptyList(), columns.any { it.numbered })
        val spacing = (columns.last().x - columns.first().x) / (columns.size - 1)
        if (spacing <= 0f) return Grid(emptyList(), false)

        fun columnOf(x: Float): Int? {
            var best = -1
            var bestDistance = Float.MAX_VALUE
            columns.forEachIndexed { index, column ->
                val distance = kotlin.math.abs(x - column.x)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = index
                }
            }
            return if (bestDistance <= spacing) best else null
        }

        val rows = mutableListOf<TimetableRow>()
        runs.forEach { run ->
            val match = Time.matchEntire(run.text.trim()) ?: return@forEach
            val column = columnOf(run.x) ?: return@forEach
            val start = minuteOf(match.groupValues[1], match.groupValues[2], match.groupValues[3]) ?: return@forEach
            val end = minuteOf(match.groupValues[4], match.groupValues[5], match.groupValues[6]) ?: return@forEach
            if (end <= start) return@forEach

            // The title is the line directly under the time, inside the same column. A title the
            // school's export wrapped arrives as more than one run on that same line, so the whole
            // line is joined back together.
            val below = runs.filter { candidate ->
                columnOf(candidate.x) == column && candidate.y < run.y && run.y - candidate.y <= CellHeight &&
                    Time.matchEntire(candidate.text.trim()) == null
            }
            if (below.isEmpty()) return@forEach
            val titleY = below.maxOf { it.y }
            val line = below.filter { kotlin.math.abs(it.y - titleY) <= LineTolerance }.sortedBy { it.x }
            val title = join(line).trim()
            if (title.isEmpty()) return@forEach

            val day = columns[column].day ?: weekdayAfter(firstColumnDay, column)
            rows += TimetableRow(day, start, end, title)
        }
        return Grid(rows, columns.any { it.numbered })
    }

    /** Monday + 0 is Monday; the offset walks the week without ever leaving it. */
    private fun weekdayAfter(first: DayOfWeek, offset: Int): DayOfWeek {
        val index = (first.value - 1 + offset) % 7
        return DayOfWeek.of((if (index < 0) index + 7 else index) + 1)
    }

    private class Column(val x: Float, val day: DayOfWeek?, val numbered: Boolean)

    /** The day columns of one page, left to right, from the header line of the printed grid. */
    private fun columns(runs: List<PdfTextExtractor.Run>): List<Column> {
        val found = mutableListOf<Column>()
        runs.forEach { run ->
            val token = run.text.trim().removeSuffix(":").trim()
            val lowered = token.lowercase()
            val number = NumberedDay.matchEntire(lowered)
            when {
                number != null -> {
                    val value = number.groupValues[1].toIntOrNull()
                    if (value != null && value >= 1) found += Column(run.x, null, numbered = true)
                }
                DayNames.containsKey(lowered) -> found += Column(run.x, DayNames.getValue(lowered), numbered = false)
            }
        }
        // Two header runs on almost the same x are the same column read twice (a title that repeated);
        // one column per x, in the order a reader walks the page.
        val unique = mutableListOf<Column>()
        found.sortedBy { it.x }.forEach { column ->
            if (unique.none { kotlin.math.abs(it.x - column.x) < 4f }) unique += column
        }
        return unique
    }

    /** Runs on one drawn line, joined with a space only where the pen really jumped. */
    private fun join(line: List<PdfTextExtractor.Run>): String {
        val out = StringBuilder()
        var pen = Float.NEGATIVE_INFINITY
        line.forEach { run ->
            if (pen != Float.NEGATIVE_INFINITY && run.x - pen > run.size * 0.18f &&
                out.isNotEmpty() && out.last() != ' ' && run.text.first() != ' '
            ) {
                out.append(' ')
            }
            out.append(run.text)
            pen = run.x + run.advance
        }
        return out.toString()
    }

    /** A clock time as minutes since midnight; 12-hour times only make sense with their meridian. */
    private fun minuteOf(hour: String, minute: String, meridian: String): Int? {
        val h = hour.toIntOrNull() ?: return null
        val m = minute.toIntOrNull() ?: return null
        if (m !in 0..59) return null
        var value = h * 60 + m
        val flag = meridian.replace(".", "").lowercase()
        when {
            flag == "pm" && value < 12 * 60 -> value += 12 * 60
            flag == "am" && value >= 12 * 60 -> value -= 12 * 60
            flag.isEmpty() && h > 23 -> return null
        }
        return if (value in 0 until 24 * 60) value else null
    }
}
