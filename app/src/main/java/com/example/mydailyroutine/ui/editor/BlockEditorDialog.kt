package com.example.mydailyroutine.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.ui.timeline.TimelineAction
import com.example.mydailyroutine.ui.timeline.clockLabel

@Composable
fun BlockEditorDialog(block: ResolvedTimelineItem.Block, busy: Boolean, onDismiss: () -> Unit, onSave: (TimelineAction.SaveBlockEdit) -> Unit) {
    var title by rememberSaveable(block.key) { mutableStateOf(block.title) }
    var start by rememberSaveable(block.key) { mutableStateOf(block.startsAt.toLocalTime().clockLabel()) }
    var end by rememberSaveable(block.key) { mutableStateOf(block.endsAt.toLocalTime().clockLabel()) }
    var whole by rememberSaveable(block.key) { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Move / rename") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Occurrence: ${block.occurrenceDate}")
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("Title") }, singleLine = true, enabled = !busy)
                OutlinedTextField(start, { start = it }, label = { Text("Start · HH:mm") }, singleLine = true, enabled = !busy)
                OutlinedTextField(end, { end = it }, label = { Text("End · HH:mm") }, singleLine = true, enabled = !busy)
                Text("An earlier end continues tomorrow. Equal start and end times are not allowed.", style = MaterialTheme.typography.bodySmall)
                if (!block.isOneOff) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(whole, { whole = it }, enabled = !busy)
                    Text("Edit the weekly blueprint", style = MaterialTheme.typography.bodyMedium)
                }
                Text(if (whole) "Existing date-specific exceptions are preserved. This changes the blueprint in all weeks, including past weeks."
                    else "Only this occurrence changes; other weeks stay as planned.", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            val parsedStart = ScheduleValidation.parseTime(start)
            val parsedEnd = ScheduleValidation.parseTime(end)
            if (title.isBlank() || parsedStart == null || parsedEnd == null || parsedStart == parsedEnd) {
                error = "Enter a title and two different HH:mm times."
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSave(TimelineAction.SaveBlockEdit(block, title.trim(), parsedStart, parsedEnd, whole))
            }
        }) { Text(if (busy) "Saving…" else "Save changes") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
    )
}
