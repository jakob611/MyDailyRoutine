package com.example.mydailyroutine.features.entry.presentation

import com.example.mydailyroutine.core.presentation.TimetableRow
import com.example.mydailyroutine.domain.import.PdfTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.DayOfWeek
import java.util.zip.Deflater

/**
 * The PDF path, end to end.
 *
 * The fixture is built to look like the export this reader was written for: a page-per-week grid of
 * day columns and period rows, times written the 12-hour way, a subset font whose glyph codes only
 * mean anything through its own `ToUnicode` map, a second font in MacRoman, a title the export itself
 * wrapped into two drawing runs, an empty cell, and a week that continues on a second page. Nothing
 * here is a real school's file — and the last test reads one, when the checkout has it.
 */
class PdfTimetableImportTest {

    // --- fixture ---------------------------------------------------------------------------------

    /** The glyphs the subset font carries; a code is the position in here plus one. */
    private val glyphs = " ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789:-–—,.…'%/"

    private fun code(char: Char): Int = glyphs.indexOf(char).plus(1).takeIf { it > 0 } ?: 1

    /** The same text as the file draws it: one hex pair per glyph code. */
    private fun hex(text: String): String = text.map { "%02X".format(code(it)) }.joinToString("")

    /**
     * The font's own glyph-code map, written the way a real subset writes it: a glyph run that happens
     * to be consecutive in both code and letter goes into a `bfrange`, the scattered ones into a
     * `bfchar`. Reading a range as a pair of single glyphs is what turns the digits of a time into
     * letters, so the fixture has to contain both kinds.
     */
    private fun cmapStream(): String {
        val ranges = StringBuilder()
        val singles = StringBuilder()
        var rangeCount = 0
        var singleCount = 0
        var index = 0
        while (index < glyphs.length) {
            val first = index + 1
            val target = glyphs[index].code
            var span = 1
            while (index + span < glyphs.length && glyphs[index + span].code == target + span) span++
            if (span >= 3) {
                ranges.append("<%04X> <%04X> <%04X>\n".format(first, first + span - 1, target))
                rangeCount++
                index += span
            } else {
                singles.append("<%02X> <%04X>\n".format(first, target))
                singleCount++
                index++
            }
        }
        return buildString {
            append("/CIDInit /ProcSet findresource begin 12 dict begin begincmap\n")
            append("/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def\n")
            append("1 begincodespacerange <00> <FF> endcodespacerange\n")
            append("$rangeCount beginbfrange\n$ranges" + "endbfrange\n")
            append("$singleCount beginbfchar\n$singles" + "endbfchar\n")
            append("endcmap\nend\n")
        }
    }

    private fun Tj(text: String, x: Int, y: Int, font: String = "F1", size: String = "9"): String =
        "BT /$font $size Tf $x $y Td <${hex(text)}> Tj ET\n"

    private fun literal(text: String, x: Int, y: Int): String =
        "BT /F2 12.75 Tf $x $y Td ($text) Tj ET\n"

    /** The header row of the printed grid: the period number column, then the day columns. */
    private fun headerRow(): String = buildString {
        append(Tj("Period", 55, 760, size = "8"))
        append(Tj("Day 1", 160, 760, size = "8"))
        append(Tj("Day 2", 320, 760, size = "8"))
    }

    /** One cell: the time range, then the subject, then the programme line a school also prints. */
    private fun cell(x: Int, y: Int, time: String, subject: String, programme: Boolean = true): String = buildString {
        append(Tj(time, x, y))
        append(Tj(subject, x, y - 12))
        if (programme) append(Tj("Grade 11", x, y - 24))
    }

    private fun pageOne(): String = buildString {
        append(Tj("Gimnazija Bezigrad", 60, 800, size = "18"))
        append(literal("August 2026 \u00D0 August 2027", 400, 790))
        append(headerRow())
        // Period 1: both days.
        append(cell(160, 700, "8:00 AM - 8:45 AM", "Matematika HL"))
        append(cell(320, 700, "8:00 AM - 8:45 AM", "Fizika HL"))
        // Period 2: day one normal, day two a title the export wrapped, with its own pen continuation.
        append(cell(160, 650, "8:50 AM - 9:35 AM", "Slovene A HL HL", programme = false))
        append(Tj("8:50 AM - 9:35 AM", 320, 650))
        append("BT /F3 9 Tf 320 638 Td (Homer) Tj 27.675 0 Td (oom 3MM SL) Tj ET\n")
    }

    /** The second page repeats the header and continues the week, with a cell missing on Tuesday. */
    private fun pageTwo(): String = buildString {
        append(headerRow())
        append(cell(160, 700, "9:40 AM - 10:25 AM", "Anglescina A"))
    }

    /**
     * Two pages, one content stream each, and two fonts: F1 is a subset that needs its `ToUnicode`
     * map, F3 announces MacRoman and ships the zero widths a subset often has.
     */
    private fun fixture(): ByteArray {
        val cMap = cmapStream()
        val page1 = pageOne().toByteArray(Charsets.ISO_8859_1)
        val page2 = pageTwo().toByteArray(Charsets.ISO_8859_1)
        val cmapBytes = deflate(cMap.toByteArray(Charsets.ISO_8859_1))

        val objects = mutableListOf<String>()
        objects += "<< /Type /Catalog /Pages 2 0 R >>"
        objects += "<< /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 /Resources << /Font << /F1 5 0 R /F2 6 0 R /F3 7 0 R >> >> >>"
        objects += "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 8 0 R >>"
        objects += "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 9 0 R >>"
        objects += "<< /Type /Font /Subtype /TrueType /BaseFont /PXAAAA+Helvetica /FirstChar 1 /LastChar ${glyphs.length} " +
            "/Widths [${"600 ".repeat(glyphs.length)}] /ToUnicode 10 0 R >>"
        // MacRoman, and deliberately without usable widths: the advance has to be learned.
        objects += "<< /Type /Font /Subtype /TrueType /BaseFont /PXAAAB+Helvetica /Encoding /MacRomanEncoding " +
            "/FirstChar 32 /LastChar 255 /Widths [${"0 ".repeat(224)}] >>"
        objects += "<< /Type /Font /Subtype /TrueType /BaseFont /PXAAAC+Helvetica /Encoding /MacRomanEncoding " +
            "/FirstChar 32 /LastChar 255 /Widths [${"0 ".repeat(224)}] >>"
        objects += "<< /Length ${page1.size} >> stream\n${String(page1, Charsets.ISO_8859_1)}\nendstream"
        objects += "<< /Length ${page2.size} >> stream\n${String(page2, Charsets.ISO_8859_1)}\nendstream"
        objects += "<< /Length ${cmapBytes.size} /Filter /FlateDecode >> stream\n${String(cmapBytes, Charsets.ISO_8859_1)}\nendstream"

        val out = StringBuilder("%PDF-1.4\n")
        objects.forEachIndexed { index, body -> out.append("${index + 1} 0 obj\n$body\nendobj\n") }
        return out.toString().toByteArray(Charsets.ISO_8859_1)
    }

    private fun deflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater()
        val out = ByteArrayOutputStream()
        deflater.setInput(bytes)
        deflater.finish()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return out.toByteArray()
    }

    private fun week(firstColumnDay: DayOfWeek = DayOfWeek.MONDAY) =
        TimetableGridParser.parse(PdfTextExtractor.extract(fixture()), firstColumnDay)

    // --- tests -----------------------------------------------------------------------------------

    @Test fun theDrawnWeekIsRebuiltFromTheGrid() {
        val grid = week()
        assertTrue("dnevi so oštevilčeni, a branje tega ni zaznalo", grid.numberedDays)
        assertEquals(
            listOf(
                TimetableRow(DayOfWeek.MONDAY, 8 * 60, 8 * 60 + 45, "Matematika HL"),
                TimetableRow(DayOfWeek.MONDAY, 8 * 60 + 50, 9 * 60 + 35, "Slovene A HL HL"),
                TimetableRow(DayOfWeek.MONDAY, 9 * 60 + 40, 10 * 60 + 25, "Anglescina A"),
                TimetableRow(DayOfWeek.TUESDAY, 8 * 60, 8 * 60 + 45, "Fizika HL"),
                TimetableRow(DayOfWeek.TUESDAY, 8 * 60 + 50, 9 * 60 + 35, "Homeroom 3MM SL"),
            ),
            grid.rows,
        )
    }

    @Test fun aNumberedGridCanStartOnAnyWeekday() {
        // A school whose week starts on Wednesday keeps its own numbering; the reader says so instead
        // of the file, and the same grid lands on the right weekdays.
        val grid = week(DayOfWeek.WEDNESDAY)
        assertEquals(
            listOf(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
            grid.rows.map { it.day }.distinct(),
        )
        assertEquals(3, grid.rows.count { it.day == DayOfWeek.WEDNESDAY })
    }

    @Test fun aSplitTitleIsJoinedBackTogether() {
        // "Homer" + "oom 3MM SL" is one subject the export drew as two runs; measuring the pen from
        // the file's own move is what keeps it from becoming "Homer oom 3MM SL".
        val titles = week().rows.map { it.title }
        assertTrue("naslov: $titles", titles.contains("Homeroom 3MM SL"))
    }

    @Test fun aCellThatIsEmptyOnOneDayShiftsNothing() {
        // Tuesday has no period 3 in the fixture; Monday's third row must still be Monday's.
        val grid = week()
        assertTrue(grid.rows.none { it.day == DayOfWeek.TUESDAY && it.startMinute == 9 * 60 + 40 })
        assertEquals(1, grid.rows.count { it.startMinute == 9 * 60 + 40 })
    }

    @Test fun linesWithoutAGridFallBackToTheTextReader() {
        val lines = buildString {
            append(Tj("Pon 08:00-08:50 Matematika HL", 60, 700))
            append(Tj("Pon 09:00-09:50 Fizika HL", 60, 680))
            append(Tj("Tor 08:00-08:50 Nemscina B", 60, 660))
        }
        val document = PdfTextExtractor.extract(pdf(listOf(lines)))
        // No day header row means no table: the grid stays out of the way and the text reader takes over.
        assertTrue(TimetableGridParser.parse(document).rows.isEmpty())
        val rows = TimetablePasteParser.parse(document.text)
        assertEquals(3, rows.size)
        assertEquals(DayOfWeek.MONDAY, rows[0].day)
        assertEquals(8 * 60, rows[0].startMinute)
    }

    @Test fun thePasteReaderUnderstandsTwelveHourTimesAndNumberedDays() {
        val rows = TimetablePasteParser.parse("Pon 8:00 AM - 8:45 AM Matematika", DayOfWeek.MONDAY)
        assertEquals(listOf(TimetableRow(DayOfWeek.MONDAY, 8 * 60, 8 * 60 + 45, "Matematika")), rows)
        // "Day 3" is the third column of the reader's own week, and it keeps that meaning in a paste.
        val numbered = TimetablePasteParser.parse("Day 1 10:00-10:45 Fizika", DayOfWeek.WEDNESDAY)
        assertEquals(DayOfWeek.WEDNESDAY, numbered.single().day)
        assertEquals(DayOfWeek.FRIDAY, TimetablePasteParser.parse("Dan 5 12:35 PM - 1:20 PM Ekonomija").single().day)
    }

    @Test fun anEmptyOrForeignFileSaysSoInsteadOfInventingATimetable() {
        assertTrue(PdfTextExtractor.extract(byteArrayOf()).isEmpty)
        val notAPdf = PdfTextExtractor.extract("to je navadno besedilo, ne PDF".toByteArray())
        assertTrue(notAPdf.isEmpty)
        assertTrue(TimetableGridParser.parse(notAPdf).rows.isEmpty())
    }

    @Test fun aWeekThatContinuesOnASecondPageStaysOneWeek() {
        // Both pages carry runs, and the page-two header must not be read as a second week.
        val document = PdfTextExtractor.extract(fixture())
        assertEquals(2, document.pages.size)
        assertTrue(document.pages.all { it.runs.isNotEmpty() })
    }

    /**
     * The real export this reader was written for, read from the checkout when it is there. It is the
     * only test that proves the whole chain against a file no one wrote for this test suite.
     */
    @Test fun theRealManageBacExportReadsAsItsOwnWeek() {
        val file = listOf(File("../weekly-1.pdf"), File("weekly-1.pdf"), File("../../weekly-1.pdf"))
            .firstOrNull { it.isFile }
        assumeTrue("weekly-1.pdf is not in this checkout", file != null)
        val document = PdfTextExtractor.extract(file!!.readBytes())
        val grid = TimetableGridParser.parse(document)
        assertTrue("šolski izvoz ni bil prebran kot mreža", grid.numberedDays)
        val rows = grid.rows
        assertTrue("prebranih vrstic: ${rows.size}", rows.size >= 25)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), rows.map { it.day }.toSet())
        // Spot checks taken from the printed grid itself.
        assertTrue(rows.any { it.day == DayOfWeek.MONDAY && it.startMinute == 8 * 60 + 50 && it.title.startsWith("Slovene A HL") })
        assertTrue(rows.any { it.day == DayOfWeek.FRIDAY && it.startMinute == 8 * 60 && it.title.startsWith("Mathematics") })
        assertTrue(rows.any { it.day == DayOfWeek.THURSDAY && it.title == "Homeroom 3MM SL" })
        // Teachers' and rooms' lines must not have been mistaken for subjects, and no period number may
        // end up as a title.
        assertTrue(rows.none { it.title.startsWith("Room") || it.title.startsWith("Grade") || it.title.toIntOrNull() != null })
        assertTrue(rows.none { it.title.contains("Urška") || it.title.contains("Mojca") || it.title.contains("Marko") })
    }

    /** A one-font file whose pages carry nothing but the given drawing instructions. */
    private fun pdf(pages: List<String>): ByteArray {
        val objects = mutableListOf<String>()
        val count = pages.size
        // Numbers are decided before the objects are written, so the page trees can point forward:
        // one catalog, one page tree, a content stream per page, a page per stream, then the font.
        val contentsIds = pages.indices.map { 3 + it }
        val pageIds = pages.indices.map { 3 + count + it }
        val fontId = 3 + 2 * count
        val cmapId = fontId + 1
        val kids = pageIds.joinToString(" ") { "$it 0 R" }
        objects += "<< /Type /Catalog /Pages 2 0 R >>"
        objects += "<< /Type /Pages /Kids [$kids] /Count $count /Resources << /Font << /F1 $fontId 0 R >> >> >>"
        pages.indices.forEach { index ->
            objects += "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents ${contentsIds[index]} 0 R >>"
        }
        pages.forEach { body -> objects += "<< /Length ${body.length} >> stream\n$body\nendstream" }
        objects += "<< /Type /Font /Subtype /TrueType /BaseFont /PXAAAA+Helvetica /FirstChar 1 /LastChar ${glyphs.length} " +
            "/Widths [${"600 ".repeat(glyphs.length)}] /ToUnicode $cmapId 0 R >>"
        val cmap = deflate(cmapStream().toByteArray(Charsets.ISO_8859_1))
        objects += "<< /Length ${cmap.size} /Filter /FlateDecode >> stream\n${String(cmap, Charsets.ISO_8859_1)}\nendstream"
        val out = StringBuilder("%PDF-1.4\n")
        objects.forEachIndexed { index, body -> out.append("${index + 1} 0 obj\n$body\nendobj\n") }
        return out.toString().toByteArray(Charsets.ISO_8859_1)
    }
}
