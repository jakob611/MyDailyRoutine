package com.example.mydailyroutine.domain.import

import java.util.zip.Inflater

/**
 * Reads the text of a PDF **on the device**, without a PDF library and without a network call.
 *
 * A timetable exported from a school system is a boring PDF: pages of glyph runs drawn at absolute
 * positions on a canvas. This reader handles exactly that — it walks the file's content streams, uses
 * each font's own `ToUnicode` map (or its declared 8-bit encoding) to turn those runs into text,
 * tracks the pen so it knows where every run ended, and can hand the runs back with their positions
 * so a caller can rebuild the printed grid ([Run] and [Document.runs]).
 *
 * What it deliberately does not do: render, follow cross-reference tables, decrypt, or expand object
 * streams (PDF 1.5+ containers). A timetable from a school web export is a plain content-stream
 * document; for a file it cannot read the caller says so out loud instead of guessing, because a
 * silently half-imported week is worse than no import at all.
 *
 * Two safety rules are part of the design, because this runs on someone's phone: the tokenizer is
 * bounded by a step budget, and every loop is guaranteed to advance. A PDF is untrusted input and a
 * wedged interface is the one failure this app must never have.
 */
object PdfTextExtractor {

    /** One drawn run of text: what it says, where it starts, how big it is and how far it advanced. */
    data class Run(val text: String, val x: Float, val y: Float, val size: Float, val advance: Float)

    /** One page's runs, and the page read back as lines (top to bottom, left to right). */
    class Page(val runs: List<Run>) {
        val text: String = render(runs)
    }

    /**
     * A whole file. [unreadable] counts content streams that could not be read at all (encrypted, or
     * compressed in a way this reader does not know); callers use it to be honest about a file that
     * only partly worked.
     */
    data class Document(val pages: List<Page>, val unreadable: Int) {
        val text: String get() = pages.joinToString("\n") { it.text }
        val runs: List<Run> get() = pages.flatMap { it.runs }
        val isEmpty: Boolean get() = pages.all { it.runs.isEmpty() }
    }

    private const val MaxStreams = 400

    fun extract(bytes: ByteArray): Document {
        // One byte is one character in the file's own syntax, so ASCII markers can be found with plain
        // string search while the streams keep their bytes exactly (ISO-8859-1 is a lossless byte map).
        val source = String(bytes, Charsets.ISO_8859_1)
        if (source.isEmpty()) return Document(emptyList(), 0)
        val fonts = readFonts(source)
        val pages = mutableListOf<Page>()
        var unreadable = 0
        var seen = 0
        for (stream in locateStreams(source)) {
            if (seen++ >= MaxStreams) break
            if (stream.encrypted) {
                unreadable++
                continue
            }
            val content = if (stream.flate) inflate(stream.bytes) else stream.bytes
            if (content == null) {
                unreadable++
                continue
            }
            val runs = TextOperators(fonts).read(String(content, Charsets.ISO_8859_1))
            if (runs.isNotEmpty()) pages += Page(runs)
        }
        return Document(pages, unreadable)
    }

    // --- file level -----------------------------------------------------------------------------

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
            while (source.getOrNull(dataStart) == '\r' || source.getOrNull(dataStart) == '\n') dataStart++
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

    // --- fonts ----------------------------------------------------------------------------------

    /**
     * What is known about one font: its glyph-code to text map when it carries one, its 8-bit encoding
     * when it says so, and its advance widths where they are usable. Subset fonts often ship a widths
     * array full of zeros for the codes they do not use, which is why a run's advance is also learned
     * from the file's own pen movements (see [TextOperators.moveTo]).
     */
    private class FontInfo(
        val table: Map<Int, Char>?,
        val macRoman: Boolean,
        val first: Int,
        val widths: FloatArray?,
    ) {
        /** The text of one string (a hex string arrives here as glyph codes, a literal as bytes). */
        fun text(codes: String, hex: Boolean): String {
            if (hex && table != null) return codes.map { table[it.code] ?: it }.joinToString("")
            if (hex) return codes
            return if (macRoman) macRoman(codes) else winAnsi(codes)
        }
    }

    /** `MacRomanEncoding` covers the punctuation a school export uses (dashes, ellipsis, quotes). */
    private fun macRoman(value: String): String {
        val out = StringBuilder(value.length)
        for (char in value) {
            val code = char.code
            out.append(if (code < 0x80) char else MAC_ROMAN[code - 0x80])
        }
        return out.toString()
    }

    /** `WinAnsiEncoding` is Latin-1 with the 0x80–0x9F block filled in; that block is the whole diff. */
    private fun winAnsi(value: String): String {
        val out = StringBuilder(value.length)
        for (char in value) {
            val code = char.code
            out.append(if (code in 0x80..0x9F) WIN_ANSI[code - 0x80] else char)
        }
        return out.toString()
    }

    private fun readFonts(source: String): Map<String, FontInfo> {
        val objects = objectBodies(source)
        val fonts = mutableMapOf<String, FontInfo>()
        var searchFrom = 0
        while (true) {
            val start = source.indexOf("/Font", searchFrom)
            if (start < 0) break
            val open = source.indexOf("<<", start)
            if (open < 0) break
            val close = matchingBrackets(source, open)
            if (close < 0) break
            val dictionary = source.substring(open + 2, close)
            searchFrom = close + 2
            // `/F1 14 0 R` — the name the content streams use, and the object holding the font.
            for (match in NAME_REF.findAll(dictionary)) {
                val name = match.groupValues[1]
                val body = objects[match.groupValues[2].toInt()] ?: continue
                if (!body.contains("/BaseFont") && !body.contains("/Type0")) continue
                val table = TO_UNICODE_REF.find(body)?.let { reference ->
                    streamBytes(source, reference.groupValues[1].toInt())?.let { raw ->
                        inflate(raw)?.let { parseCMap(String(it, Charsets.ISO_8859_1)) }
                    }
                }
                val encoding = ENCODING.find(body)?.groupValues?.get(1).orEmpty()
                val first = FIRST_CHAR.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val widths = WIDTHS.find(body)?.groupValues?.get(1)?.let { array ->
                    val values = NUMBER.findAll(array).map { it.value.toFloat() }.toList().toFloatArray()
                    values.takeIf { it.isNotEmpty() && it.any { width -> width > 0f } }
                }
                fonts[name] = FontInfo(table, encoding.contains("MacRoman"), first, widths)
            }
        }
        return fonts
    }

    private fun objectBodies(source: String): Map<Int, String> {
        val bodies = mutableMapOf<Int, String>()
        for (match in OBJECT.findAll(source)) {
            val end = source.indexOf("endobj", match.range.last)
            bodies[match.groupValues[1].toInt()] =
                source.substring(match.range.last + 1, if (end < 0) source.length else end)
        }
        return bodies
    }

    /** The raw bytes of an object's stream, when it has one. */
    private fun streamBytes(source: String, number: Int): ByteArray? {
        val marker = source.indexOf("$number 0 obj")
        if (marker < 0) return null
        val open = source.indexOf("stream", marker)
        if (open < 0) return null
        var start = open + "stream".length
        while (source.getOrNull(start) == '\r' || source.getOrNull(start) == '\n') start++
        val end = source.indexOf("endstream", start)
        if (end < 0) return null
        return source.substring(start, end).toByteArray(Charsets.ISO_8859_1)
    }

    /** `beginbfrange` / `beginbfchar` tables: glyph code to the character it stands for. */
    private fun parseCMap(text: String): Map<Int, Char> {
        val table = mutableMapOf<Int, Char>()
        for (block in BFRANGE.findAll(text)) {
            val low = block.groupValues[1].toIntOrNull(16) ?: continue
            val high = block.groupValues[2].toIntOrNull(16) ?: continue
            val target = block.groupValues[3].toIntOrNull(16) ?: continue
            if (high < low || high - low > 0xFFFF) continue
            for (code in low..high) {
                val value = target + (code - low)
                if (value in 1..0x10FFFF) table[code] = value.toChar()
            }
        }
        for (block in BFCHAR.findAll(text)) {
            val code = block.groupValues[1].toIntOrNull(16) ?: continue
            val target = block.groupValues[2].toIntOrNull(16) ?: continue
            if (target in 1..0x10FFFF) table[code] = target.toChar()
        }
        return table
    }

    // --- content streams ------------------------------------------------------------------------

    /**
     * Replays one content stream. Only the operators that place text are honoured; everything that
     * draws lines, boxes or images is skipped, which is exactly what a timetable export needs.
     */
    private class TextOperators(private val fonts: Map<String, FontInfo>) {
        /** A string operand: glyph codes from a hex string, or bytes from a literal one. */
        private class Text(val value: String, val hex: Boolean)

        private val runs = mutableListOf<Run>()
        private val operands = mutableListOf<Any>()
        private var x = 0f
        private var lineStartX = 0f
        private var y = 0f
        private var leading = 0f
        private var size = 12f
        private var font: String? = null

        fun read(content: String): List<Run> {
            var index = 0
            // Untrusted input: the tokenizer may take a bounded number of steps and no more. A file
            // that cannot be tokenized in this budget is not a timetable, and the interface stays alive.
            var budget = 64L * content.length + 100_000L
            while (index < content.length) {
                if (budget-- <= 0L) break
                when (val char = content[index]) {
                    '(' -> {
                        val (value, next) = readLiteral(content, index)
                        operands += Text(value, hex = false)
                        index = maxOf(next, index + 1)
                    }
                    '<' -> {
                        if (content.getOrNull(index + 1) == '<') {
                            index += 2
                        } else {
                            val close = content.indexOf('>', index)
                            if (close < 0) break
                            operands += Text(readHex(content.substring(index + 1, close)), hex = true)
                            index = close + 1
                        }
                    }
                    '>' -> index += if (content.getOrNull(index + 1) == '>') 2 else 1
                    '[' -> {
                        operands += "["
                        index++
                    }
                    ']' -> {
                        operands += "]"
                        index++
                    }
                    '%' -> {
                        val end = content.indexOf('\n', index)
                        index = if (end < 0) content.length else end + 1
                    }
                    '/' -> {
                        // A name (font, colour space, graphics state): read past it, keep no text.
                        var next = index + 1
                        while (next < content.length && !content[next].isWhitespace() &&
                            content[next] !in "()[]<>/{}%"
                        ) {
                            next++
                        }
                        operands += "/name"
                        index = next
                    }
                    else -> {
                        if (char.isWhitespace() || char == '{' || char == '}') {
                            index++
                        } else {
                            var next = index
                            while (next < content.length && !content[next].isWhitespace() &&
                                content[next] !in "()[]<>/{}%"
                            ) {
                                next++
                            }
                            if (next == index) {
                                // A character that is none of the above and cannot start a token:
                                // step over it rather than spinning on it.
                                index++
                            } else {
                                val token = content.substring(index, next)
                                val number = token.toFloatOrNull()
                                if (number != null) operands += number else operate(token)
                                index = next
                            }
                        }
                    }
                }
            }
            return runs
        }

        private fun operate(operator: String) {
            when (operator) {
                "BT" -> {
                    // A text object starts its own pen: positions inside it are absolute.
                    x = 0f
                    lineStartX = 0f
                    y = 0f
                    leading = 0f
                }
                "Tf" -> {
                    (operands.lastOrNull { it is String } as? String)?.let { font = it }
                    (operands.lastOrNull { it is Float } as? Float)?.let { size = it }
                }
                "TL" -> (operands.firstOrNull() as? Float)?.let { leading = it }
                "Td", "TD" -> {
                    val dy = operands.getOrNull(operands.size - 1) as? Float
                    val dx = operands.getOrNull(operands.size - 2) as? Float
                    if (dx != null && dy != null) {
                        moveTo(x + dx, y + dy, if (operator == "TD") -dy else null, dx, dy)
                        lineStartX = x
                    }
                }
                "Tm" -> {
                    val values = operands.filterIsInstance<Float>()
                    if (values.size >= 6) {
                        moveTo(values[4], values[5], null, values[4] - x, values[5] - y)
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
                    show(operands.lastOrNull { it is Text } as? Text)
                }
                "TJ" -> {
                    // An array of strings and kerning numbers: a big negative number is a wide gap,
                    // which in a timetable is a column break and reads as a space.
                    val array = operands.dropWhile { it != "[" }.drop(1).takeWhile { it != "]" }
                    val text = StringBuilder()
                    var hex = false
                    array.forEach { element ->
                        when (element) {
                            is Text -> {
                                text.append(element.value)
                                hex = element.hex
                            }
                            is Float -> if (element <= -100f && text.isNotEmpty() && text.last() != ' ') text.append(' ')
                            else -> Unit
                        }
                    }
                    if (text.isNotEmpty()) show(Text(text.toString(), hex))
                }
            }
            operands.clear()
        }

        /**
         * The pen moved. A purely horizontal move continues the same line, and the distance it covers
         * is exactly the advance the renderer gave the run before it — the file's own measurement of
         * its own text, which is better than any width table. That is what keeps "Homer" + "oom 3MM SL"
         * in a subset font from reading as "Homer oom 3MM SL".
         */
        private fun moveTo(newX: Float, newY: Float, newLeading: Float?, dx: Float, dy: Float) {
            val previous = runs.lastOrNull()
            if (previous != null && dy == 0f && dx >= 0f && kotlin.math.abs(newY - previous.y) < 0.5f) {
                val learned = newX - previous.x
                if (learned > previous.advance) runs[runs.lastIndex] = previous.copy(advance = learned)
            }
            if (newLeading != null) leading = newLeading
            x = newX
            y = newY
        }

        private fun show(operand: Text?) {
            val value = operand?.value ?: return
            if (value.isEmpty()) return
            val info = font?.let { fonts[it] }
            val text = if (info != null) info.text(value, operand.hex) else value
            if (text.isEmpty()) return
            val advance = estimate(text, info)
            runs += Run(text, x, y, size, advance)
            x += advance
        }

        /**
         * How wide a run is. The font's own widths are used when the file ships usable ones; subset
         * fonts often ship zeros instead, and then a plain estimate stands in until [moveTo] learns
         * the real advance from the file's own pen movements.
         */
        private fun estimate(text: String, info: FontInfo?): Float {
            val widths = info?.widths
            val first = info?.first ?: 0
            if (widths != null) {
                var total = 0f
                for (char in text) {
                    val index = char.code - first
                    total += (if (index in widths.indices) widths[index] else 500f) / 1000f * size
                }
                if (total > 0f) return total
            }
            return text.length * size * 0.5f
        }
    }

    private fun readLiteral(content: String, start: Int): Pair<String, Int> {
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

    /** A hex string is a sequence of glyph codes; one byte each is what school exports use. */
    private fun readHex(hex: String): String {
        val digits = hex.filter { it.isLetterOrDigit() }
        val out = StringBuilder(digits.length / 2)
        var index = 0
        while (index + 1 < digits.length) {
            val value = digits.substring(index, index + 2).toIntOrNull(16) ?: break
            out.append(value.toChar())
            index += 2
        }
        return out.toString()
    }

    // --- laying the runs back out ---------------------------------------------------------------

    /**
     * Groups runs into lines and orders them the way a reader sees them: top to bottom (PDF measures
     * from the bottom of the page), then left to right, inserting a space where the pen really jumped.
     */
    private fun render(runs: List<Run>): String {
        if (runs.isEmpty()) return ""
        val sorted = runs.sortedWith(compareByDescending<Run> { it.y }.thenBy { it.x })
        val tolerance = (sorted.minOf { it.size } * 0.4f).coerceAtLeast(1.5f)
        val out = StringBuilder()
        var row = mutableListOf<Run>()
        var rowY = Float.NaN
        fun flush() {
            if (row.isEmpty()) return
            val line = StringBuilder()
            var pen = Float.NEGATIVE_INFINITY
            row.sortedBy { it.x }.forEach { run ->
                val gap = run.x - pen
                if (pen != Float.NEGATIVE_INFINITY && gap > run.size * 0.14f &&
                    line.isNotEmpty() && line.last() != ' ' && run.text.first() != ' '
                ) {
                    line.append(' ')
                }
                line.append(run.text)
                pen = run.x + run.advance
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

    private fun matchingBrackets(source: String, open: Int): Int {
        var depth = 0
        var index = open
        while (index < source.length - 1) {
            when {
                source[index] == '<' && source[index + 1] == '<' -> { depth++; index += 2 }
                source[index] == '>' && source[index + 1] == '>' -> {
                    depth--
                    if (depth == 0) return index
                    index += 2
                }
                else -> index++
            }
        }
        return -1
    }

    private val OBJECT = Regex("(\\d+)\\s+0\\s+obj")
    private val NAME_REF = Regex("/([A-Za-z0-9]+)\\s+(\\d+)\\s+0\\s+R")
    private val TO_UNICODE_REF = Regex("/ToUnicode\\s+(\\d+)\\s+0\\s+R")
    private val ENCODING = Regex("/Encoding\\s*/(\\w+)")
    private val FIRST_CHAR = Regex("/FirstChar\\s+(\\d+)")
    private val WIDTHS = Regex("/Widths\\s*\\[([^\\]]*)\\]", RegexOption.DOT_MATCHES_ALL)
    private val NUMBER = Regex("[-+]?\\d*\\.?\\d+")
    private val BFRANGE = Regex(
        "<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val BFCHAR = Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>")

    /**
     * The high half of MacRoman, in order from 0x80: the dashes, quotes and accented letters a school
     * export uses. Spelled out rather than asked of the platform, because this class must behave the
     * same on every Android version and is cheap to keep honest.
     */
    private const val MAC_ROMAN = "\u00C4\u00C5\u00C7\u00C9\u00D1\u00D6\u00DC\u00E1\u00E0\u00E2\u00E4\u00E3\u00E5\u00E7\u00E9\u00E8" +
        "\u00EA\u00EB\u00ED\u00EC\u00EE\u00EF\u00F1\u00F3\u00F2\u00F4\u00F6\u00F5\u00FA\u00F9\u00FB\u00FC\u2020\u00B0\u00A2\u00A3\u00A7\u2022\u00B6\u00DF\u00AE\u00A9\u2122\u00B4\u00A8\u2260\u00C6\u00D8" +
        "\u221E\u00B1\u2264\u2265\u00A5\u00B5\u2202\u2211\u220F\u03C0\u222B\u00AA\u00BA\u03A9\u00E6\u00F8\u00BF\u00A1\u00AC\u221A\u0192\u2248\u2206\u00AB\u00BB\u2026\u00A0\u00C0\u00C3\u00D5\u0152\u0153\u2013\u2014\u201C\u201D\u2018\u2019\u00F7\u25CA\u00FF\u0178\u2044\u20AC\u2039\u203A\uFB01\uFB02\u2021\u00B7\u201A\u201E\u2030\u00C2\u00CA\u00C1\u00CB\u00C8\u00CD\u00CE\u00CF\u00CC\u00D3\u00D4\uF8FF\u00D2\u00DA\u00DB\u00D9\u0131\u02C6\u02DC\u00AF\u02D8\u02D9\u02DA\u00B8\u02DD\u02DB\u02C7"

    /** WinAnsi's 0x80-0x9F block; identical to Latin-1 everywhere else. */
    private const val WIN_ANSI = "\u20AC\uFFFD\u201A\u0192\u201E\u2026\u2020\u2021\u02C6\u2030\u0160\u2039\u0152\uFFFD\u017D\uFFFD" +
        "\uFFFD\u2018\u2019\u201C\u201D\u2022\u2013\u2014\u02DC\u2122\u0161\u203A\u0153\uFFFD\u017E\u0178"
}
