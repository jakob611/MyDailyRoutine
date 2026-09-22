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

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheetScaffold
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetPrimaryButton
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.platform.uiLocale
import com.example.mydailyroutine.domain.import.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.presentation.TimetableRow
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * Parses timetable text copied out of ManageBac (web view or PDF select-all) into weekly blocks.
 * Tolerant by design: grid exports jam several periods into one line, day names arrive in SL or EN,
 * lines that cannot be understood are simply dropped - the user reviews a preview before anything is created.
 */
object TimetablePasteParser {

    // Times arrive both ways: a Slovenian page writes 24-hour, an English export writes "8:00 AM".
    private val TimeRange = Regex(
        "(\\d{1,2})[:.](\\d{2})\\s*([AaPp]\\.?[Mm]\\.?)?\\s*[-–—]\\s*(\\d{1,2})[:.](\\d{2})\\s*([AaPp]\\.?[Mm]\\.?)",
    )

    private val DayTokens: Map<String, DayOfWeek> = mapOf(
        "pon" to DayOfWeek.MONDAY, "ponedeljek" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY, "montag" to DayOfWeek.MONDAY,
        "tor" to DayOfWeek.TUESDAY, "torek" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY,
        "sre" to DayOfWeek.WEDNESDAY, "sreda" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "crt" to DayOfWeek.THURSDAY, "cet" to DayOfWeek.THURSDAY, "cetrtek" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY,
        "pet" to DayOfWeek.FRIDAY, "petek" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "sob" to DayOfWeek.SATURDAY, "sobota" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
    )

    private fun normalize(token: String): String = token.lowercase()
        .replace("č", "c").replace("š", "s").replace("ž", "z")
        .replace("ć", "c").replace("đ", "d")

    internal fun dayOf(line: String, firstColumnDay: DayOfWeek = DayOfWeek.MONDAY): DayOfWeek? {
        val tokens = line.split(Regex("\\s+"))
        tokens.firstNotNullOfOrNull { token -> DayTokens[normalize(token)] }?.let { return it }
        // "Day 3": a school that numbers its columns means the third column of its own week, and which
        // weekday that is depends on the school — so the caller says where the first column starts.
        tokens.forEachIndexed { index, token ->
            if (normalize(token) !in setOf("day", "dan")) return@forEachIndexed
            val number = tokens.getOrNull(index + 1)?.trim()?.toIntOrNull() ?: return@forEachIndexed
            if (number in 1..7) return DayOfWeek.of((firstColumnDay.value - 1 + number - 1) % 7 + 1)
        }
        return null
    }

    internal fun subjectOf(segment: String): String {
        val words = segment.split(Regex("\\s+")).filter { it.isNotEmpty() }
        // A day marker is either a weekday name or the "Day 3" pair a numbered grid writes; neither
        // belongs in the title that gets imported.
        val kept = mutableListOf<String>()
        var index = 0
        while (index < words.size) {
            val token = normalize(words[index])
            if (token in setOf("day", "dan") && words.getOrNull(index + 1)?.toIntOrNull() != null) {
                index += 2
                continue
            }
            if (!DayTokens.containsKey(token)) kept += words[index]
            index++
        }
        return kept.joinToString(" ")
        .trim('-', '–', '—', ':', ';', ',')
        .trim()
        .replace(Regex("\\s{2,}"), " ")

    /** A clock time as minutes since midnight; a 12-hour time only means something with its meridian. */
    private fun clockMinutes(hour: String, minute: String, meridian: String): Int {
        var value = (hour.toIntOrNull() ?: 0) * 60 + (minute.toIntOrNull() ?: 0)
        val flag = meridian.replace(".", "").lowercase()
        if (flag == "pm" && value < 12 * 60) value += 12 * 60
        if (flag == "am" && value >= 12 * 60) value -= 12 * 60
        return value
    }

    fun parse(text: String, firstColumnDay: DayOfWeek = DayOfWeek.MONDAY): List<TimetableRow> {
        val rows = mutableListOf<TimetableRow>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val day = dayOf(line, firstColumnDay) ?: return@forEach
            val matches = TimeRange.findAll(line).toList()
            matches.forEachIndexed { index, match ->
                val start = clockMinutes(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                val end = clockMinutes(match.groupValues[4], match.groupValues[5], match.groupValues[6])
                if (end > start && end <= 24 * 60) {
                    val after = match.range.last + 1
                    val until = matches.getOrNull(index + 1)?.range?.first ?: line.length
                    var title = subjectOf(line.substring(after, minOf(after + 60, until)))
                    if (title.isBlank()) {
                        val beforeStart = matches.getOrNull(index - 1)?.range?.last?.plus(1) ?: 0
                        title = subjectOf(line.substring(beforeStart, match.range.first))
                    }
                    if (title.isNotBlank()) rows += TimetableRow(day, start, end, title.take(60))
                }
            }
        }
        return rows.distinct().sortedWith(compareBy({ it.day }, { it.startMinute }))
    }
}

/** Review a pasted timetable before creating it; nothing writes back to the school - this is a one-time import. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableImportSheet(
    subjects: List<Subject>,
    busy: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onImport: (List<TimetableRow>) -> Unit,
) {
    val haptics = LocalRoutineHaptics.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<TimetableRow>?>(null) }
    // Where a PDF import leaves its answer: how many rows came out, or why none did.
    var pdfNotice by remember { mutableStateOf<PdfNotice?>(null) }
    var reading by remember { mutableStateOf(false) }
    // The file that was read, kept so the reader can re-map its columns without picking it again.
    var document by remember { mutableStateOf<PdfTextExtractor.Document?>(null) }
    // ManageBac numbers its columns ("Day 1", "Day 2") instead of naming weekdays, and the app cannot
    // know the school's convention. Monday is the honest default; the row of days below the preview
    // is how the reader says otherwise without leaving the sheet.
    var firstColumnDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var numberedDays by remember { mutableStateOf(false) }

    /** Reads the drawn grid first, and only falls back to plain text when there is no grid. */
    fun reviewDocument(extracted: PdfTextExtractor.Document, day: DayOfWeek = firstColumnDay) {
        val grid = TimetableGridParser.parse(extracted, day)
        numberedDays = grid.numberedDays
        val fromGrid = grid.rows
        val fromText = if (fromGrid.isEmpty()) TimetablePasteParser.parse(extracted.text, day) else emptyList()
        val rowsFound = if (fromGrid.isNotEmpty()) fromGrid else fromText
        preview = rowsFound
        pdfNotice = if (rowsFound.isEmpty()) PdfNotice.EMPTY else PdfNotice.ROWS(rowsFound.size)
        if (rowsFound.isEmpty()) haptics.warning() else haptics.confirm()
    }

    fun review(candidate: String) {
        val parsed = TimetablePasteParser.parse(candidate)
        preview = parsed
        if (parsed.isNullOrEmpty()) haptics.warning() else haptics.confirm()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            reading = true
            val extracted = runCatching { readTimetablePdf(context, uri) }.getOrNull()
            reading = false
            if (extracted == null) {
                pdfNotice = PdfNotice.FAILED
                haptics.warning()
                return@launch
            }
            document = extracted
            reviewDocument(extracted)
            if (extracted.isEmpty) {
                // Nothing readable in the file at all: leave the paste box alone, because the reader
                // can still select all in the PDF and paste it here by hand.
                pdfNotice = PdfNotice.EMPTY
                return@launch
            }
            if (preview.isNullOrEmpty()) {
                pdfNotice = PdfNotice.EMPTY
                haptics.warning()
                return@launch
            }
            // The text goes into the paste box as well: the grid is what gets imported, the lines are
            // what the reader can see and fix if the school's file did something unexpected.
            text = extracted.text
        }
    }
    val rows = preview
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.SheetSurface, tonalElevation = 0.dp) {
        RoutineSheetScaffold(
            title = stringResource(R.string.timetable_import_title),
            subtitle = stringResource(R.string.timetable_import_hint),
            closeLabel = stringResource(R.string.close),
            onClose = onDismiss,
            modifier = Modifier.testTag("timetable-import"),
            footer = {
                if (rows != null && rows.isNotEmpty()) {
                    SheetPrimaryButton(
                        label = pluralStringResource(R.plurals.timetable_import_create, rows.size, rows.size),
                        enabled = !busy,
                        // The import action reaches the wrapper, which fires the success haptic for every committed
                        // change. A second one here would just blur it.
                        onClick = { onImport(rows) },
                    )
                }
            },
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    text = value
                    preview = null
                    document = null
                    numberedDays = false
                    pdfNotice = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { RoutineText(stringResource(R.string.timetable_import_paste)) },
                minLines = 5,
                maxLines = 10,
                shape = RoutineShapes.Card,
            )
            // The second way in: the school's own PDF, read on the device. The file never leaves the
            // phone and the text lands in the same paste box, so there is exactly one review path.
            RoutineText(stringResource(R.string.timetable_import_pdf_hint), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextMuted, maxLines = RoutineTextDefaults.Paragraph)
            SheetSecondaryButton(
                label = stringResource(R.string.timetable_import_pdf),
                enabled = !reading,
                onClick = { haptics.press(); picker.launch(arrayOf("application/pdf")) },
            )
            if (numberedDays && !rows.isNullOrEmpty()) {
                RoutineText(stringResource(R.string.timetable_import_pdf_days),
                    style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary,
                    maxLines = RoutineTextDefaults.Paragraph)
                Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                    modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    DayOfWeek.entries.forEach { day ->
                        val label = day.getDisplayName(TextStyle.SHORT_STANDALONE, uiLocale())
                        FilterChip(
                            selected = day == firstColumnDay,
                            onClick = {
                                haptics.selection()
                                firstColumnDay = day
                                document?.let { reviewDocument(it, day) }
                            },
                            shape = RoutineShapes.Chip,
                            modifier = Modifier.testTag("import-first-day-${day.name.lowercase()}"),
                            label = { RoutineLabel(label, style = MaterialTheme.typography.labelLarge) },
                        )
                    }
                }
            }
            when (val notice = pdfNotice) {
                is PdfNotice.ROWS -> RoutineText(stringResource(R.string.timetable_import_pdf_done, notice.count),
                    style = MaterialTheme.typography.bodySmall, color = RoutineColors.Success,
                    maxLines = RoutineTextDefaults.Paragraph)
                PdfNotice.EMPTY -> RoutineText(stringResource(R.string.timetable_import_pdf_empty),
                    style = MaterialTheme.typography.bodySmall, color = RoutineColors.Warning,
                    maxLines = RoutineTextDefaults.Paragraph)
                PdfNotice.FAILED -> RoutineText(stringResource(R.string.timetable_import_pdf_failed),
                    style = MaterialTheme.typography.bodySmall, color = RoutineColors.Warning,
                    maxLines = RoutineTextDefaults.Paragraph)
                null -> Unit
            }
            SheetSecondaryButton(
                label = stringResource(R.string.timetable_import_preview),
                enabled = text.isNotBlank(),
                onClick = { review(text) },
            )
            if (rows != null) {
                if (rows.isEmpty()) {
                    RoutineText(stringResource(R.string.timetable_import_none), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
                } else {
                    // The count lives on the import button, so the preview list needs no second headline.
                    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                        rows.forEach { row ->
                            val matchesSubject = subjects.any { subject ->
                                val name = normalizeToken(subject.name)
                                val title = normalizeToken(row.title)
                                name.isNotBlank() && title.contains(name)
                            }
                            Row(
                                Modifier.fillMaxWidth().background(
                                    if (matchesSubject) RoutineColors.School.container else RoutineColors.Surface2,
                                    RoutineShapes.Chip,
                                ).padding(horizontal = RoutineSpacing.md, vertical = RoutineSpacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RoutineLabel(
                                    text = row.day.getDisplayName(java.time.format.TextStyle.SHORT, uiLocale()),
                                    modifier = Modifier.widthIn(min = 40.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (matchesSubject) RoutineColors.School.content else RoutineColors.TextMuted,
                                )
                                RoutineLabel(
                                    text = "%02d:%02d – %02d:%02d".format(
                                        row.startMinute / 60, row.startMinute % 60,
                                        row.endMinute / 60, row.endMinute % 60,
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (matchesSubject) RoutineColors.School.content else RoutineColors.TextSecondary,
                                )
                                RoutineText(row.title, style = MaterialTheme.typography.bodySmall, maxLines = RoutineTextDefaults.Title,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun normalizeToken(value: String): String = value.lowercase()
    .replace("č", "c").replace("š", "s").replace("ž", "z")
    .replace("ć", "c").replace("đ", "d")
    .trim()

/**
 * What the last PDF import had to say. Kept as three explicit cases instead of a string, so the
 * screen cannot end up showing a message that belongs to a different file.
 */
private sealed interface PdfNotice {
    data class ROWS(val count: Int) : PdfNotice
    data object EMPTY : PdfNotice
    data object FAILED : PdfNotice
}

/**
 * Reads a timetable PDF through the system file picker. Bounded on purpose: a picker can hand back a
 * 200 MB scan, and the reader has no use for more than a few megabytes of content streams.
 */
private suspend fun readTimetablePdf(context: Context, uri: Uri): PdfTextExtractor.Document =
    withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(chunk)
                if (read <= 0) break
                total += read
                if (total > MaxPdfBytes) throw IllegalArgumentException("PDF too large")
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        } ?: throw IllegalArgumentException("no stream")
        PdfTextExtractor.extract(bytes)
    }

/** Sixteen megabytes: far more than a timetable needs, far less than a phone can choke on. */
private const val MaxPdfBytes = 16 * 1024 * 1024
