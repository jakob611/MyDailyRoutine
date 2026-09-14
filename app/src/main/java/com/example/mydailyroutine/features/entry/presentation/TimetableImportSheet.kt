package com.example.mydailyroutine.features.entry.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.platform.Slovenian
import com.example.mydailyroutine.core.presentation.TimetableRow
import com.example.mydailyroutine.core.presentation.clockLabel
import com.example.mydailyroutine.domain.model.Subject
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * Parses pasted ManageBac timetable lines into rows: one lesson per "day + time range + title".
 * Handles a day token anywhere in the line and several ranges per line (a copied grid row).
 */
object TimetablePasteParser {
    private val TimeRange = Regex("""(\d{1,2})[:.](\d{2})\s*[–—-]\s*(\d{1,2})[:.](\d{2})""")
    private val Days = mapOf(
        DayOfWeek.MONDAY to listOf("pon", "poned", "ponedeljek", "mon", "monday"),
        DayOfWeek.TUESDAY to listOf("tor", "torek", "tue", "tues", "tuesday"),
        DayOfWeek.WEDNESDAY to listOf("sre", "sred", "sreda", "wed", "wednesday"),
        DayOfWeek.THURSDAY to listOf("cet", "crt", "cetrt", "cetrtek", "thu", "thur", "thurs", "thursday"),
        DayOfWeek.FRIDAY to listOf("pet", "pete", "petek", "fri", "friday"),
        DayOfWeek.SATURDAY to listOf("sob", "sobota", "sat", "saturday"),
        DayOfWeek.SUNDAY to listOf("ned", "nedelj", "nedelja", "sun", "sunday"),
    )
    private val DayTokens = Days.flatMap { (day, names) -> names.map { name -> name to day } }.toMap()

    private fun normalize(token: String): String = token.lowercase()
        .replace('č', 'c').replace('š', 's').replace('ž', 'z').replace('đ', 'd')
        .trim('.', ',', ':', '’')

    internal fun dayOf(line: String): DayOfWeek? =
        line.split(Regex("""\s+""")).firstNotNullOfOrNull { DayTokens[normalize(it)] }

    private fun subjectOf(segment: String): String = segment
        .split(Regex("""\s+""")).filterNot { it.isEmpty() || DayTokens.containsKey(normalize(it)) }
        .joinToString(" ").trim('-', '–', '—', ':', ';', ',').replace(Regex("""\s{2,}""), " ").trim()

    fun parse(text: String): List<TimetableRow> {
        val rows = mutableListOf<TimetableRow>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val day = dayOf(line) ?: return@forEach
            val matches = TimeRange.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            matches.forEachIndexed { index, match ->
                val start = match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
                val end = match.groupValues[3].toInt() * 60 + match.groupValues[4].toInt()
                if (start !in 0..1439 || end > 1440 || end <= start) return@forEachIndexed
                val nextStart = matches.getOrNull(index + 1)?.range?.first ?: line.length
                var title = subjectOf(line.substring(match.range.last + 1, nextStart))
                if (title.isBlank() && index == 0) title = subjectOf(line.substring(0, match.range.first))
                if (title.isNotBlank()) rows += TimetableRow(day, start, end, title.take(80))
            }
        }
        return rows.distinct().sortedWith(compareBy({ it.day.value }, { it.startMinute }))
    }
}

/** Review a pasted timetable before creating it; the school feed never writes back, this is a one-time import. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableImportSheet(subjects: List<Subject>, busy: Boolean, sheetState: SheetState, onDismiss: () -> Unit, onImport: (List<TimetableRow>) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<TimetableRow>?>(null) }
    val haptics = LocalRoutineHaptics.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.Surface1, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.timetable_import_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.timetable_import_hint), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
            OutlinedTextField(text, { value -> text = value; preview = null }, enabled = !busy, minLines = 6, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.timetable_import_paste)) })
            val rows = preview
            if (rows != null) {
                if (rows.isEmpty()) Text(stringResource(R.string.timetable_import_none), style = MaterialTheme.typography.bodySmall, color = RoutineColors.Warning)
                rows.forEach { row ->
                    val subject = subjects.firstOrNull { row.title.contains(it.name, ignoreCase = true) || it.name.equals(row.title, ignoreCase = true) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(subject?.let { Color(it.colorHex.toInt()) } ?: RoutineColors.TextMuted))
                        Text(row.day.getDisplayName(TextStyle.SHORT, Slovenian), style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary)
                        Text(LocalTime.of(row.startMinute / 60, row.startMinute % 60).clockLabel() + "–" +
                            LocalTime.of(minOf(row.endMinute, 1439) / 60, minOf(row.endMinute, 1439) % 60).clockLabel(),
                            style = MaterialTheme.typography.labelMedium)
                        Text(row.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy && text.isNotBlank(), onClick = { haptics.tap(); preview = TimetablePasteParser.parse(text) }) {
                    Text(stringResource(R.string.timetable_import_preview))
                }
                FilledTonalButton(modifier = Modifier.weight(1f), enabled = !busy && !rows.isNullOrEmpty(), onClick = { haptics.tap(); rows?.let(onImport) }) {
                    Text(stringResource(R.string.timetable_import_confirm, rows?.size ?: 0))
                }
            }
        }
    }
}
