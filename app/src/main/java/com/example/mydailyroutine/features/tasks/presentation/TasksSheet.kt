package com.example.mydailyroutine.features.tasks.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.platform.Slovenian
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.domain.model.Task
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val taskDateFormat = DateTimeFormatter.ofPattern("d. MMM", Slovenian)

/** Homework/errand checklist. Fast entry, relative due labels, one-tap completion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksSheet(tasks: List<Task>, subjects: List<Subject>, busy: Boolean, onAction: (TimelineAction) -> Unit) {
    val today = LocalDate.now()
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var newDueEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var newSubject by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingNewDate by rememberSaveable { mutableStateOf(false) }
    var newSubjectMenu by rememberSaveable { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val open = tasks.filter { it.completedAtEpochMillis == null }
    val overdue = open.filter { it.dueDate != null && it.dueDate.isBefore(today) }.sortedBy { it.dueDate }
    val dueToday = open.filter { it.dueDate == today }.sortedBy { it.createdAtEpochMillis }
    val upcoming = open.filter { it.dueDate != null && it.dueDate.isAfter(today) }.sortedBy { it.dueDate }
    val noDue = open.filter { it.dueDate == null }.sortedByDescending { it.createdAtEpochMillis }
    val done = tasks.filter { it.completedAtEpochMillis != null }.sortedByDescending { it.completedAtEpochMillis }
    fun submitNew() {
        val clean = newTitle.trim()
        if (clean.isEmpty() || busy) return
        onAction(TimelineAction.AddTask(clean, newDueEpoch?.let(LocalDate::ofEpochDay), newSubject))
        newTitle = ""; newDueEpoch = null; newSubject = null
    }
    ModalBottomSheet(onDismissRequest = { onAction(TimelineAction.CloseTasks) }, shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.Surface1, tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp, 0.dp, 24.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "tasks-header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.tasks_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.tasks_open_count, open.size), style = MaterialTheme.typography.labelLarge, color = RoutineColors.TextSecondary)
                    }
                    OutlinedTextField(newTitle, { newTitle = it.take(120) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        placeholder = { Text(stringResource(R.string.tasks_quick_add_hint)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { submitNew() }))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingNewDate = true }) {
                            Icon(Icons.Outlined.CalendarMonth, null)
                            Spacer(Modifier.width(6.dp))
                            Text(newDueEpoch?.let { LocalDate.ofEpochDay(it).format(taskDateFormat) } ?: stringResource(R.string.tasks_no_due), maxLines = 1)
                        }
                        if (newDueEpoch != null) TextButton(enabled = !busy, onClick = { newDueEpoch = null }) {
                            Text(stringResource(R.string.tasks_clear_date), color = RoutineColors.Crimson)
                        }
                        Box {
                            OutlinedButton(enabled = !busy, onClick = { newSubjectMenu = true }) {
                                Text(newSubject?.let(subjectsById::get)?.name ?: stringResource(R.string.tasks_subject_any), maxLines = 1)
                            }
                            DropdownMenu(expanded = newSubjectMenu, onDismissRequest = { newSubjectMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.tasks_subject_none)) }, onClick = { newSubject = null; newSubjectMenu = false })
                                subjects.forEach { subject ->
                                    DropdownMenuItem(text = { Text(subject.name) }, onClick = { newSubject = subject.id; newSubjectMenu = false })
                                }
                            }
                        }
                        FilledButton(enabled = !busy && newTitle.isNotBlank(), onClick = { submitNew() }) { Text(stringResource(R.string.tasks_add)) }
                    }
                }
            }
            if (open.isEmpty()) item(key = "tasks-empty") {
                Text(stringResource(R.string.tasks_empty), style = MaterialTheme.typography.bodyMedium, color = RoutineColors.TextSecondary)
            }
            if (overdue.isNotEmpty()) {
                item(key = "tasks-h-overdue") { TaskSectionHeader(R.string.tasks_section_overdue, RoutineColors.Crimson) }
                items(overdue, key = { "task:${it.id}" }) { task ->
                    TaskRow(task, subjects, subjectsById, today, busy, expandedId == task.id,
                        onToggle = { onAction(TimelineAction.ToggleTask(task.id)) },
                        onExpand = { expandedId = if (expandedId == task.id) null else task.id },
                        onRequestDelete = { deleteId = task.id }, onAction = onAction)
                }
            }
            if (dueToday.isNotEmpty()) {
                item(key = "tasks-h-today") { TaskSectionHeader(R.string.tasks_section_today, RoutineColors.Amber) }
                items(dueToday, key = { "task:${it.id}" }) { task ->
                    TaskRow(task, subjects, subjectsById, today, busy, expandedId == task.id,
                        onToggle = { onAction(TimelineAction.ToggleTask(task.id)) },
                        onExpand = { expandedId = if (expandedId == task.id) null else task.id },
                        onRequestDelete = { deleteId = task.id }, onAction = onAction)
                }
            }
            if (upcoming.isNotEmpty()) {
                item(key = "tasks-h-upcoming") { TaskSectionHeader(R.string.tasks_section_upcoming, RoutineColors.Cobalt) }
                items(upcoming, key = { "task:${it.id}" }) { task ->
                    TaskRow(task, subjects, subjectsById, today, busy, expandedId == task.id,
                        onToggle = { onAction(TimelineAction.ToggleTask(task.id)) },
                        onExpand = { expandedId = if (expandedId == task.id) null else task.id },
                        onRequestDelete = { deleteId = task.id }, onAction = onAction)
                }
            }
            if (noDue.isNotEmpty()) {
                item(key = "tasks-h-nodue") { TaskSectionHeader(R.string.tasks_section_no_due, RoutineColors.TextMuted) }
                items(noDue, key = { "task:${it.id}" }) { task ->
                    TaskRow(task, subjects, subjectsById, today, busy, expandedId == task.id,
                        onToggle = { onAction(TimelineAction.ToggleTask(task.id)) },
                        onExpand = { expandedId = if (expandedId == task.id) null else task.id },
                        onRequestDelete = { deleteId = task.id }, onAction = onAction)
                }
            }
            if (done.isNotEmpty()) item(key = "tasks-h-done") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tasks_section_done, done.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.ClearCompletedTasks) }) { Text(stringResource(R.string.tasks_clear_done)) }
                }
            }
            items(done, key = { "done:${it.id}" }) { task ->
                OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Checkbox(true, { if (!busy) onAction(TimelineAction.ToggleTask(task.id)) }, enabled = !busy)
                        Text(task.title, style = MaterialTheme.typography.bodyMedium, color = RoutineColors.TextMuted, modifier = Modifier.weight(1f), maxLines = 2)
                        task.dueDate?.let { Text(it.format(taskDateFormat), style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextMuted) }
                    }
                }
            }
        }
    }
    if (pickingNewDate) AppDatePicker(newDueEpoch?.let(LocalDate::ofEpochDay) ?: today,
        onDismiss = { pickingNewDate = false }, onDate = { newDueEpoch = it.toEpochDay(); pickingNewDate = false })
    tasks.firstOrNull { it.id == deleteId }?.let { target ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text(stringResource(R.string.tasks_delete_title)) },
            text = { Text(stringResource(R.string.tasks_delete_body, target.title)) },
            confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteTask(target.id)); deleteId = null }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.keep)) } })
    }
}

@Composable
private fun TaskSectionHeader(res: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(stringResource(res), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TaskRow(task: Task, subjects: List<Subject>, subjectsById: Map<Long, Subject>, today: LocalDate, busy: Boolean,
                    expanded: Boolean, onToggle: () -> Unit, onExpand: () -> Unit, onRequestDelete: () -> Unit,
                    onAction: (TimelineAction) -> Unit) {
    var editTitle by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var editDueEpoch by rememberSaveable(task.id) { mutableStateOf(task.dueDate?.toEpochDay()) }
    var editSubject by rememberSaveable(task.id) { mutableStateOf(task.subjectId) }
    var editNote by rememberSaveable(task.id) { mutableStateOf(task.note.orEmpty()) }
    var pickingEditDate by rememberSaveable { mutableStateOf(false) }
    var subjectMenu by rememberSaveable { mutableStateOf(false) }
    val isDone = task.completedAtEpochMillis != null
    OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onExpand), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Checkbox(isDone, { if (!busy) onToggle() }, enabled = !busy)
                task.subjectId?.let(subjectsById::get)?.let { subject ->
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(subject.colorHex.toInt())))
                }
                Text(task.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 2)
                task.dueDate?.let { date ->
                    Text(taskDueLabel(date, today), style = MaterialTheme.typography.labelMedium,
                        color = if (!isDone && date.isBefore(today)) RoutineColors.Crimson else RoutineColors.TextSecondary)
                }
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editTitle, { editTitle = it.take(120) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        label = { Text(stringResource(R.string.tasks_edit_title)) })
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { pickingEditDate = true }) {
                            Icon(Icons.Outlined.CalendarMonth, null)
                            Spacer(Modifier.width(6.dp))
                            Text(editDueEpoch?.let { LocalDate.ofEpochDay(it).format(taskDateFormat) } ?: stringResource(R.string.tasks_no_due), maxLines = 1)
                        }
                        if (editDueEpoch != null) TextButton(enabled = !busy, onClick = { editDueEpoch = null }) {
                            Text(stringResource(R.string.tasks_clear_date), color = RoutineColors.Crimson)
                        }
                        Box {
                            OutlinedButton(enabled = !busy, onClick = { subjectMenu = true }) {
                                Text(editSubject?.let(subjectsById::get)?.name ?: stringResource(R.string.tasks_subject_any), maxLines = 1)
                            }
                            DropdownMenu(expanded = subjectMenu, onDismissRequest = { subjectMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.tasks_subject_none)) }, onClick = { editSubject = null; subjectMenu = false })
                                subjects.forEach { subject ->
                                    DropdownMenuItem(text = { Text(subject.name) }, onClick = { editSubject = subject.id; subjectMenu = false })
                                }
                            }
                        }
                    }
                    OutlinedTextField(editNote, { editNote = it.take(2000) }, modifier = Modifier.fillMaxWidth(), enabled = !busy, minLines = 2,
                        label = { Text(stringResource(R.string.tasks_note)) }, placeholder = { Text(stringResource(R.string.tasks_note_hint)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(enabled = !busy && editTitle.isNotBlank(), onClick = {
                            onAction(TimelineAction.UpdateTask(task.copy(title = editTitle.trim(), dueDate = editDueEpoch?.let(LocalDate::ofEpochDay),
                                subjectId = editSubject, note = editNote.trim().takeIf { it.isNotEmpty() })))
                        }) { Text(stringResource(R.string.save)) }
                        TextButton(enabled = !busy, onClick = { onAction(TimelineAction.TaskToSchedule(task)) }) { Text(stringResource(R.string.tasks_schedule)) }
                        Spacer(Modifier.weight(1f))
                        TextButton(enabled = !busy, onClick = onRequestDelete) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                    }
                }
            }
        }
    }
    if (pickingEditDate) AppDatePicker(editDueEpoch?.let(LocalDate::ofEpochDay) ?: today,
        onDismiss = { pickingEditDate = false }, onDate = { editDueEpoch = it.toEpochDay(); pickingEditDate = false })
}

@Composable
private fun taskDueLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> stringResource(R.string.tasks_today_short)
    date == today.plusDays(1) -> stringResource(R.string.tasks_tomorrow)
    date.isAfter(today) -> stringResource(R.string.tasks_in_days, ChronoUnit.DAYS.between(today, date).toInt())
    else -> stringResource(R.string.tasks_ago_days, ChronoUnit.DAYS.between(date, today).toInt())
}
