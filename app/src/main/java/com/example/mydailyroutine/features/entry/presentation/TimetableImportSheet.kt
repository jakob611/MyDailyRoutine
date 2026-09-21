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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.presentation.TimetableRow
import java.time.DayOfWeek

/**
 * Parses timetable text copied out of ManageBac (web view or PDF select-all) into weekly blocks.
 * Tolerant by design: grid exports jam several periods into one line, day names arrive in SL or EN,
 * lines that cannot be understood are simply dropped - the user reviews a preview before anything is created.
 */
object TimetablePasteParser {

    private val TimeRange = Regex("(\\d{1,2})[:.](\\d{2})\\s*[-–—]\\s*(\\d{1,2})[:.](\\d{2})")

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

    internal fun dayOf(line: String): DayOfWeek? =
        line.split(Regex("\\s+")).firstNotNullOfOrNull { token -> DayTokens[normalize(token)] }

    internal fun subjectOf(segment: String): String = segment
        .split(Regex("\\s+"))
        .filterNot { it.isEmpty() || DayTokens.containsKey(normalize(it)) }
        .joinToString(" ")
        .trim('-', '–', '—', ':', ';', ',')
        .trim()
        .replace(Regex("\\s{2,}"), " ")

    fun parse(text: String): List<TimetableRow> {
        val rows = mutableListOf<TimetableRow>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val day = dayOf(line) ?: return@forEach
            val matches = TimeRange.findAll(line).toList()
            matches.forEachIndexed { index, match ->
                val start = match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
                val end = match.groupValues[3].toInt() * 60 + match.groupValues[4].toInt()
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
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<TimetableRow>?>(null) }
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
                onValueChange = { value -> text = value; preview = null },
                modifier = Modifier.fillMaxWidth(),
                label = { RoutineText(stringResource(R.string.timetable_import_paste)) },
                minLines = 5,
                maxLines = 10,
                shape = RoutineShapes.Card,
            )
            SheetSecondaryButton(
                label = stringResource(R.string.timetable_import_preview),
                enabled = text.isNotBlank(),
                onClick = {
                    val parsed = TimetablePasteParser.parse(text)
                    preview = parsed
                    // Apple's notification family, used the way it is meant to be: success when the
                    // paste yielded rows, warning when it did not.
                    if (parsed.isNullOrEmpty()) haptics.warning() else haptics.confirm()
                },
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
