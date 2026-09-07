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

@Composable
fun BlockEditorDialog(block: ResolvedTimelineItem.Block, busy: Boolean, onDismiss: () -> Unit, onSave: (TimelineAction.SaveBlockEdit) -> Unit) {
    var title by rememberSaveable(block.key) { mutableStateOf(block.title) }
    var start by rememberSaveable(block.key) { mutableStateOf(block.startsAt.toLocalTime().clockLabel()) }
    var end by rememberSaveable(block.key) { mutableStateOf(block.endsAt.toLocalTime().clockLabel()) }
    var whole by rememberSaveable(block.key) { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    val haptic = LocalRoutineHaptics.current
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_block_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.edit_occurrence, block.occurrenceDate))
                OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.entry_title)) }, singleLine = true, enabled = !busy)
                OutlinedTextField(start, { start = it }, label = { Text(stringResource(R.string.entry_start)) }, singleLine = true, enabled = !busy)
                OutlinedTextField(end, { end = it }, label = { Text(stringResource(R.string.entry_end)) }, singleLine = true, enabled = !busy)
                Text(stringResource(R.string.edit_times_hint), style = MaterialTheme.typography.bodySmall)
                if (!block.isOneOff) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(whole, { whole = it; haptic.tap() }, enabled = !busy)
                    Text(stringResource(R.string.edit_whole_template), style = MaterialTheme.typography.bodyMedium)
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
                onSave(TimelineAction.SaveBlockEdit(block, title.trim(), parsedStart, parsedEnd, whole))
            }
        }) { Text(stringResource(if (busy) R.string.saving else R.string.save_changes)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
