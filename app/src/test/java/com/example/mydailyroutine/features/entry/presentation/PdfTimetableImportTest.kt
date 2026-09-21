package com.example.mydailyroutine.features.entry.presentation

import com.example.mydailyroutine.domain.import.PdfTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.util.zip.Deflater

/**
 * The PDF path, end to end and without a real school file: build the smallest PDF that behaves like a
 * timetable export (a content stream that draws one line per period), read it back, and feed the text
 * to the same paste parser the reader uses. Two things are being pinned here — that the reader finds
 * the text at all, and that the week it produces is the week that was drawn.
 */
class PdfTimetableImportTest {

    /** One page, one uncompressed content stream, exactly as a "print to PDF" export looks. */
    private fun pdf(contentStream: String, flate: Boolean = false): ByteArray {
        val streamBytes = if (flate) deflate(contentStream) else contentStream.toByteArray(Charsets.ISO_8859_1)
        val dictionary = if (flate) "<</Length ${streamBytes.size} /Filter /FlateDecode>>" else "<</Length ${streamBytes.size}>>"
        val header = "%PDF-1.4\n"
        val body = buildString {
            append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
            append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R >>\nendobj\n")
            append("4 0 obj\n$dictionary\nstream\n")
            append(String(streamBytes, Charsets.ISO_8859_1))
            append("\nendstream\nendobj\n")
        }
        return header.toByteArray(Charsets.ISO_8859_1) + body.toByteArray(Charsets.ISO_8859_1)
    }

    private fun deflate(text: String): ByteArray {
        val deflater = Deflater()
        val out = ByteArrayOutputStream()
        deflater.setInput(text.toByteArray(Charsets.ISO_8859_1))
        deflater.finish()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) {
            val read = deflater.deflate(buffer)
            out.write(buffer, 0, read)
        }
        deflater.end()
        return out.toByteArray()
    }

    /**
     * A timetable page: five periods, drawn from the top of the page downwards.
     *
     * The titles deliberately avoid Slovenian diacritics: a PDF string is a byte string, and these
     * fixtures write plain Latin-1, which carries `s` and `c` but not `š` and `č`. The real reader has
     * the same limit, which is why the sheet tells the reader to paste the text when a file does not
     * come back readable. Day tokens are the same in both alphabets.
     */
    private val weekDays = listOf(
        "Pon 08:00-08:50 Matematika HL",
        "Pon 09:00-09:50 Fizika HL",
        "Pon 10:00-10:50 Anglescina A",
        "Tor 08:00-08:50 Nemscina B",
        "Sre 11:00-11:50 Zgodovina SL",
    )

    private fun page(rows: List<String>): String {
        val content = StringBuilder("BT /F1 11 Tf\n")
        rows.forEachIndexed { index, row ->
            // Each line is drawn at its own position, the way a table is drawn.
            content.append("1 0 0 1 60 ${760 - index * 20} Tm (").append(row).append(") Tj\n")
        }
        content.append("ET\n")
        return content.toString()
    }

    @Test fun textIsFoundInAnUncompressedExport() {
        val result = PdfTextExtractor.extract(pdf(page(weekDays)))
        assertTrue("nobeno besedilo ni bilo prebrano", result.text.isNotBlank())
        weekDays.forEach { row -> assertTrue("manjka vrstica: $row\nprebrano: ${result.text}", result.text.contains(row)) }
    }

    @Test fun textIsFoundInACompressedExport() {
        val result = PdfTextExtractor.extract(pdf(page(weekDays), flate = true))
        assertTrue(result.text.contains("Matematika HL"))
        assertTrue(result.text.contains("Zgodovina SL"))
    }

    @Test fun theRowsComeOutInReadingOrder() {
        val result = PdfTextExtractor.extract(pdf(page(weekDays)))
        val lines = result.text.lines().filter { it.isNotBlank() }
        // The page is drawn top to bottom; the text must be read top to bottom as well, or a timetable
        // turns into a shuffled pile of periods.
        assertEquals("Pon 08:00-08:50 Matematika HL", lines.first().trim())
        assertEquals("Sre 11:00-11:50 Zgodovina SL", lines.last().trim())
    }

    @Test fun theReaderFeedsTheNormalImportParser() {
        val result = PdfTextExtractor.extract(pdf(page(weekDays)))
        val rows = TimetablePasteParser.parse(result.text)
        assertEquals(5, rows.size)
        val monday = rows.filter { it.day == DayOfWeek.MONDAY }
        assertEquals(listOf(8 * 60, 9 * 60, 10 * 60), monday.map { it.startMinute })
        assertTrue(rows.any { it.day == DayOfWeek.TUESDAY && it.title.contains("Nemscina") })
        assertTrue(rows.any { it.day == DayOfWeek.WEDNESDAY && it.title.contains("Zgodovina") })
    }

    @Test fun severalPeriodsOnOneLineBecomeSeveralBlocks() {
        // Select-all from a grid export often lands as one long line per day; the parser splits it.
        val result = PdfTextExtractor.extract(pdf(page(listOf("Pon 08:00-08:50 Matematika  09:00-09:50 Fizika"))))
        val rows = TimetablePasteParser.parse(result.text)
        assertEquals(2, rows.size)
        assertEquals(8 * 60, rows[0].startMinute)
        assertEquals(9 * 60, rows[1].startMinute)
    }

    @Test fun anEmptyFileSaysSoInsteadOfInventingATimetable() {
        val result = PdfTextExtractor.extract(byteArrayOf())
        assertTrue(result.isEmpty)
        assertEquals(0, result.pageCount)
        assertEquals(0, result.unreadable)
        assertTrue(TimetablePasteParser.parse(result.text).isEmpty())
    }

    @Test fun aFileThatIsNotAPdfIsNotMistakenForOne() {
        val result = PdfTextExtractor.extract("to je navadno besedilo, ne PDF".toByteArray())
        assertTrue(result.isEmpty)
    }
}
