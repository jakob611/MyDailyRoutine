package com.example.mydailyroutine.domain.import

import java.util.zip.Inflater

/**
 * Reads the text of a PDF on the device, without a PDF library and without a network call.
 *
 * A timetable exported from a school system is a boring PDF: pages of text runs drawn at absolute
 * positions on a canvas. That is all this reader needs to handle — it walks the file's content
 * streams, replays the text operators (`Tj`, `TJ`, `'`, `"`, `Td`, `TD`, `Tm`, `T*`, `TL`), keeps the
 * position of every run, and lays the runs back out as lines, left to right, top to bottom. The
 * result is the same shape a student gets from selecting all and copying: one period per line.
 *
 * What it deliberately does not do: render, follow cross-reference tables, handle encrypted files, or
 * expand object streams (PDF 1.5+ containers). A timetable from a school web export is a plain
 * content-stream document. When a file cannot be read, the caller says so out loud instead of
 * guessing — a silently half-imported timetable is worse than none.
 */
object PdfTextExtractor {

    /**
     * What came out of a file: the text, how many content streams actually held a timetable, and how
     * many streams could not be read at all (encrypted, or compressed in a way this reader does not
     * know). Callers use [unreadable] to say "this file did not work", never to quietly import part of
     * a week.
     */
    data class Result(val text: String, val pageCount: Int, val unreadable: Int) {
        val isEmpty: Boolean get() = text.isBlank()
    }

    private const val MaxPages = 400

    fun extract(bytes: ByteArray): Result {
        // One byte is one character in the file's own syntax, so the ASCII markers can be found with
        // ordinary string search. Text inside strings is decoded per string, where the encoding is known.
        val source = String(bytes, Charsets.ISO_8859_1)
        val streams = locateStreams(source)
        val runs = mutableListOf<TextRun>()
        var pages = 0
        var unreadable = 0
        for (stream in streams.take(MaxPages)) {
            if (stream.encrypted) {
                unreadable++
                continue
            }
            val content = if (stream.flate) inflate(stream.bytes) else stream.bytes
            if (content == null) {
                // A stream that claims FlateDecode and cannot be inflated is a stream we do not
                // understand; skipping it silently would quietly drop a whole page of the timetable.
                unreadable++
                continue
            }
            val pageRuns = TextOperators().read(String(content, Charsets.ISO_8859_1))
            if (pageRuns.isNotEmpty()) {
                pages++
                runs += pageRuns
            }
        }
        return Result(render(runs), pages, unreadable)
    }

    // --- file level ---------------------------------------------------------

    private class Stream(val bytes: ByteArray, val flate: Boolean, val encrypted: Boolean)

    private fun locateStreams(source: String): List<Stream> {
        val streams = mutableListOf<Stream>()
        var index = 0
        while (index < source.length) {
            val marker = source.indexOf("stream", index)
            if (marker < 0) break
            val before = source.getOrNull(marker - 1)
            // `endstream` ends a stream; only a standalone `stream` keyword starts one.
            val isKeyword = before == null || before == '\n' || before == '\r' || before == '>' || before == ' '
            if (!isKeyword) {
                index = marker + 1
                continue
            }
            val headerStart = source.lastIndexOf("<<", marker)
            val header = if (headerStart >= 0) source.substring(headerStart, marker) else ""
            var dataStart = marker + "stream".length
            if (source.startsWith("\r\n", dataStart)) dataStart += 2
            else if (source.startsWith("\n", dataStart) || source.startsWith("\r", dataStart)) dataStart += 1
            val end = source.indexOf("endstream", dataStart)
            if (end < 0) break
            streams += Stream(
                bytes = source.substring(dataStart, end).toByteArray(Charsets.ISO_8859_1),
                flate = header.contains("FlateDecode"),
                encrypted = header.contains("/Encrypt") || header.contains("/Crypt"),
            )
            index = end + "endstream".length
        }
        return streams
    }

    private fun inflate(bytes: ByteArray): ByteArray? {
        val inflater = Inflater()
        return try {
            inflater.setInput(bytes)
            val out = java.io.ByteArrayOutputStream(bytes.size * 4)
            val buffer = ByteArray(16 * 1024)
            while (!inflater.finished()) {
                val read = inflater.inflate(buffer)
                if (read == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) return null
                } else {
                    out.write(buffer, 0, read)
                }
            }
            out.toByteArray()
        } catch (_: java.util.zip.DataFormatException) {
            null
        } finally {
            inflater.end()
        }
    }

    // --- content stream level ----------------------------------------------

    private data class TextRun(val text: String, val x: Float, val y: Float, val size: Float)

    /**
     * Replays one content stream. Only the operators that place text are honoured; everything that
     * draws lines, boxes or images is skipped, which is exactly what a timetable export needs.
     */
    private class TextOperators {
        private val runs = mutableListOf<TextRun>()
        private val operands = mutableListOf<Any>()
        private var x = 0f
        private var lineStartX = 0f
        private var y = 0f
        private var leading = 0f
        private var fontSize = 12f

        fun read(content: String): List<TextRun> {
            var index = 0
            while (index < content.length) {
                when (val char = content[index]) {
                    '(' -> {
                        val (value, next) = readLiteralString(content, index)
                        operands += value
                        index = next
                    }
                    '<' -> {
                        if (content.getOrNull(index + 1) == '<') {
                            index += 2
                        } else {
                            val close = content.indexOf('>', index)
                            if (close < 0) break
                            operands += readHexString(content.substring(index + 1, close))
                            index = close + 1
                        }
                    }
                    '>' -> index += if (content.getOrNull(index + 1) == '>') 2 else 1
                    '[' -> {
                        operands += '['
                        index++
                    }
                    ']' -> {
                        operands += ']'
                        index++
                    }
                    '%' -> {
                        // A comment: skip to the end of the line.
                        val end = content.indexOf('\n', index)
                        index = if (end < 0) content.length else end + 1
                    }
                    '/' -> {
                        // A name (font, colour space): read past it, keep it only as a marker.
                        var next = index
                        while (next < content.length && !content[next].isWhitespace() && content[next] !in "()[]<>/{}%") next++
                        operands += "/name"
                        index = next
                    }
                    else -> {
                        if (char.isWhitespace() || char == '{' || char == '}') {
                            index++
                        } else {
                            var next = index
                            while (next < content.length && !content[next].isWhitespace() && content[next] !in "()[]<>/{}%") next++
                            val token = content.substring(index, next)
                            val number = token.toFloatOrNull()
                            if (number != null) operands += number else runOperator(token)
                            index = next
                        }
                    }
                }
            }
            return runs
        }

        private fun runOperator(operator: String) {
            when (operator) {
                "Tf" -> (operands.lastOrNull { it is Float } as? Float)?.let { fontSize = it }
                "TL" -> (operands.firstOrNull() as? Float)?.let { leading = it }
                "Td", "TD" -> {
                    val dy = operands.getOrNull(operands.size - 1) as? Float
                    val dx = operands.getOrNull(operands.size - 2) as? Float
                    if (dx != null && dy != null) {
                        x += dx
                        lineStartX = x
                        y += dy
                        if (operator == "TD") leading = -dy
                    }
                }
                "Tm" -> {
                    val values = operands.filterIsInstance<Float>()
                    if (values.size >= 6) {
                        x = values[4]
                        y = values[5]
                        lineStartX = x
                    }
                }
                "T*" -> {
                    y -= leading
                    x = lineStartX
                }
                "Tj", "'", "\"" -> {
                    // `'` and `"` both move to the next line before showing; `"` also sets word and
                    // character spacing, which a timetable export has no use for.
                    if (operator != "Tj") {
                        y -= leading
                        x = lineStartX
                    }
                    show(operands.lastOrNull { it is String } as? String)
                }
                "TJ" -> {
                    // An array of strings and kerning numbers: a big negative number is a wide gap,
                    // which in a timetable is a column break and reads as a space.
                    val array = operands.dropWhile { it != '[' }.drop(1).takeWhile { it != ']' }
                    val text = StringBuilder()
                    array.forEach { element ->
                        when (element) {
                            is String -> text.append(element)
                            is Float -> if (element <= -100f && text.isNotEmpty() && text.last() != ' ') text.append(' ')
                            else -> Unit
                        }
                    }
                    show(text.toString())
                }
            }
            operands.clear()
        }

        private fun show(value: String?) {
            val text = value?.trim() ?: return
            if (text.isEmpty()) return
            runs += TextRun(text, x, y, fontSize)
            // Advance the pen by an estimate: the exact widths live in the embedded font, and for
            // deciding where one column ends and the next begins an average advance is enough.
            x += text.length * fontSize * 0.5f
        }
    }

    // --- string decoding ----------------------------------------------------

    private fun readLiteralString(content: String, start: Int): Pair<String, Int> {
        val out = StringBuilder()
        var index = start + 1
        var depth = 1
        while (index < content.length) {
            when (val char = content[index]) {
                '\\' -> {
                    val escape = content.getOrNull(index + 1)
                    when (escape) {
                        'n' -> { out.append('\n'); index += 2 }
                        'r' -> { out.append('\r'); index += 2 }
                        't' -> { out.append('\t'); index += 2 }
                        'b' -> { out.append('\b'); index += 2 }
                        'f' -> { out.append('\u000C'); index += 2 }
                        '(', ')', '\\' -> { out.append(escape); index += 2 }
                        '\n' -> index += 2
                        '\r' -> index += if (content.getOrNull(index + 2) == '\n') 3 else 2
                        in '0'..'7' -> {
                            var digits = ""
                            var next = index + 1
                            while (digits.length < 3 && content.getOrNull(next) in '0'..'7') {
                                digits += content[next]
                                next++
                            }
                            out.append((digits.toInt(8) and 0xFF).toChar())
                            index = next
                        }
                        else -> index += 2
                    }
                }
                '(' -> { depth++; out.append(char); index++ }
                ')' -> {
                    depth--
                    if (depth == 0) return out.toString() to index + 1
                    out.append(char)
                    index++
                }
                else -> { out.append(char); index++ }
            }
        }
        return out.toString() to content.length
    }

    private fun readHexString(hex: String): String {
        val digits = hex.filter { it.isLetterOrDigit() }
        val out = StringBuilder()
        var index = 0
        while (index + 1 < digits.length) {
            val value = digits.substring(index, index + 2).toIntOrNull(16) ?: break
            out.append(value.toChar())
            index += 2
        }
        return out.toString()
    }

    // --- laying text back out ----------------------------------------------

    /**
     * Groups runs into lines and orders them the way a reader sees them: top to bottom (PDF measures
     * from the bottom of the page), then left to right, inserting a space where the pen jumped.
     */
    private fun render(runs: List<TextRun>): String {
        if (runs.isEmpty()) return ""
        val out = StringBuilder()
        val sorted = runs.sortedWith(compareByDescending<TextRun> { it.y }.thenBy { it.x })
        // Two runs belong to the same line when their baselines are within a fraction of the type size.
        val tolerance = (sorted.minOf { it.size } * 0.4f).coerceAtLeast(1.5f)
        var row = mutableListOf<TextRun>()
        var rowY = Float.NaN
        fun flush() {
            if (row.isEmpty()) return
            val line = StringBuilder()
            var penEnd = Float.NEGATIVE_INFINITY
            row.sortedBy { it.x }.forEach { run ->
                val gap = run.x - penEnd
                if (penEnd != Float.NEGATIVE_INFINITY && gap > run.size * 0.2f && line.isNotEmpty() && line.last() != ' ') {
                    line.append(' ')
                }
                line.append(run.text)
                penEnd = run.x + run.text.length * run.size * 0.5f
            }
            val text = line.toString().trim()
            if (text.isNotEmpty()) {
                if (out.isNotEmpty()) out.append('\n')
                out.append(text)
            }
            row = mutableListOf()
        }
        sorted.forEach { run ->
            if (row.isEmpty() || kotlin.math.abs(run.y - rowY) <= tolerance) {
                if (row.isEmpty()) rowY = run.y
                row += run
            } else {
                flush()
                rowY = run.y
                row += run
            }
        }
        flush()
        return out.toString()
    }
}
