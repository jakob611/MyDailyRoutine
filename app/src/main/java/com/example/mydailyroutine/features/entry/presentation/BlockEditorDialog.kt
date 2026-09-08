package com.example.mydailyroutine.features.entry.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.clockLabel
import com.example.mydailyroutine.domain.routines.TimeEntryState
import com.example.mydailyroutine.domain.model.nominalMinutes

@Composable
fun BlockEditorDialog(block: ResolvedTimelineItem.Block, busy: Boolean, onDismiss: () -> Unit, onSave: (TimelineAction.SaveBlockEdit) -> Unit) {
    var title by rememberSaveable(block.key) { mutableStateOf(block.title) }
    var times by rememberSaveable(block.key,stateSaver=TimeEntrySaver) { mutableStateOf(TimeEntryState.at(block.startsAt.toLocalTime(),
        nominalMinutes(block.startsAt.toLocalTime(),block.endsAt.toLocalTime()).coerceIn(1,1439))) }
    val start=times.startText
    val end=times.endText
    var allDays by rememberSaveable(block.key) { mutableStateOf(true) }
    var whole by rememberSaveable(block.key) { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    val haptic = LocalRoutineHaptics.current
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_block_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.edit_occurrence, block.occurrenceDate))
                OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.entry_title)) }, singleLine = true, enabled = !busy)
                OutlinedTextField(start, { times = times.withStart(it) }, label = { Text(stringResource(R.string.entry_start)) }, singleLine = true, enabled = !busy)
                OutlinedTextField(end, { times = times.withEnd(it) }, label = { Text(stringResource(R.string.entry_end)) }, singleLine = true, enabled = !busy)
                Text(stringResource(R.string.edit_times_hint), style = MaterialTheme.typography.bodySmall)
                if (!block.isOneOff) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(whole, { whole = it; haptic.tap() }, enabled = !busy)
                    Text(stringResource(R.string.edit_whole_template), style = MaterialTheme.typography.bodyMedium)
                }
                if (whole && block.seriesKey != null && block.seriesDays.size > 1) Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(allDays,{ allDays=it;haptic.tap() },enabled=!busy)
                    Text(stringResource(R.string.edit_all_repeat_days))
                }
                Text(stringResource(if (whole) R.string.edit_whole_hint else R.string.edit_once_hint), style = MaterialTheme.typography.bodySmall)
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            val parsedStart = ScheduleValidation.parseTime(start)
            val parsedEnd = ScheduleValidation.parseTime(end)
            if (title.isBlank() || parsedStart == null || parsedEnd == null || parsedStart == parsedEnd) {
                error = R.string.error_block_edit
            } else {
                onSave(TimelineAction.SaveBlockEdit(block, title.trim(), parsedStart, parsedEnd, whole, allDays && block.seriesKey != null))
            }
        }) { Text(stringResource(if (busy) R.string.saving else R.string.save_changes)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
