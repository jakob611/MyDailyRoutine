package com.example.mydailyroutine.ui.editor

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
import com.example.mydailyroutine.domain.model.Subject

private val subjectPalette = listOf(
    "Indigo" to 0xFF6478C8L, "Teal" to 0xFF28857AL, "Rose" to 0xFFB65D74L,
    "Amber" to 0xFFAA7827L, "Violet" to 0xFF8864B3L, "Blue" to 0xFF357FAC,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectEditorDialog(subject: Subject, busy: Boolean, onDismiss: () -> Unit, onSave: (Subject) -> Unit) {
    var name by rememberSaveable(subject.id) { mutableStateOf(subject.name) }
    var duration by rememberSaveable(subject.id) { mutableStateOf(subject.defaultDurationMinutes.toString()) }
    var color by rememberSaveable(subject.id) { mutableLongStateOf(subject.colorHex) }
    val validDuration = duration.toIntOrNull()?.takeIf { it in 1..1439 }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (subject.id == 0L) "New subject" else "Edit subject") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Name, e.g. Math HL") }, singleLine = true, enabled = !busy)
            OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(4) }, label = { Text("Default duration in minutes") },
                supportingText = { Text("1–1439 minutes") }, singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("Subject color", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subjectPalette.forEach { (label, value) ->
                    Box(Modifier.size(48.dp).border(BorderStroke(if (color == value) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                        .padding(5.dp).background(Color(value.toInt()), CircleShape).clickable(enabled = !busy) { color = value }
                        .semantics { contentDescription = "$label${if (color == value) ", selected" else ""}" })
                }
            }
        }
    }, confirmButton = {
        TextButton(enabled = !busy && name.isNotBlank() && validDuration != null,
            onClick = { validDuration?.let { onSave(subject.copy(name = name.trim(), colorHex = color, defaultDurationMinutes = it)) } }) { Text(if (busy) "Saving…" else "Save subject") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } })
}
