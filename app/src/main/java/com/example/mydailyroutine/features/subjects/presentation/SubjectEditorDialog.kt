package com.example.mydailyroutine.features.subjects.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.domain.model.Subject

private val colorLabels = listOf(R.string.color_cobalt, R.string.color_sage, R.string.color_amber, R.string.color_crimson, R.string.color_violet, R.string.color_slate)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectEditorDialog(subject: Subject, busy: Boolean, onDismiss: () -> Unit, onSave: (Subject) -> Unit, onDelete: (() -> Unit)? = null) {
    var name by rememberSaveable(subject.id) { mutableStateOf(subject.name) }
    var duration by rememberSaveable(subject.id) { mutableStateOf(subject.defaultDurationMinutes.toString()) }
    var color by rememberSaveable(subject.id) { mutableLongStateOf(subject.colorHex) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    val validDuration = duration.toIntOrNull()?.takeIf { it in 1..1439 }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (subject.id == 0L) R.string.new_subject_title else R.string.edit_subject_title)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(name, { name = it.take(120) }, label = { Text(stringResource(R.string.subject_name)) }, singleLine = true, enabled = !busy)
            OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.subject_duration)) },
                supportingText = { Text(stringResource(R.string.subject_duration_hint)) }, singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text(stringResource(R.string.subject_color), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutineColors.subjectSwatches.forEachIndexed { index, value ->
                    val label = stringResource(colorLabels[index])
                    val description = if (color == value) stringResource(R.string.selected_color, label) else label
                    Box(Modifier.size(48.dp).border(BorderStroke(if (color == value) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                        .padding(5.dp).background(Color(value.toInt()), CircleShape).clickable(enabled = !busy) { color = value; haptics.tap() }
                        .semantics { contentDescription = description })
                }
            }
        }
    }, confirmButton = {
        TextButton(enabled = !busy && name.isNotBlank() && validDuration != null,
            onClick = { validDuration?.let { onSave(subject.copy(name = name.trim(), colorHex = color, defaultDurationMinutes = it)) } }) { Text(stringResource(if (busy) R.string.saving else R.string.save_subject)) }
    }, dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (subject.id > 0L && onDelete != null) TextButton(enabled = !busy, onClick = { haptics.warning(); confirmingDelete = true }) { Text(stringResource(R.string.delete_subject), color = RoutineColors.Crimson) }
            TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    })
    if (confirmingDelete) AlertDialog(onDismissRequest = { confirmingDelete = false },
        title = { Text(stringResource(R.string.delete_subject_title, subject.name)) },
        text = { Text(stringResource(R.string.delete_subject_body)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { confirmingDelete = false; onDelete?.invoke() }) { Text(stringResource(R.string.delete_subject), color = RoutineColors.Crimson) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.cancel)) } })
}
