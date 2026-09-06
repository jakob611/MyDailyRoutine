package com.example.mydailyroutine.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.ui.timeline.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private data class QuickPreset(val label: String, val title: String, val minutes: Int, val category: RoutineCategory, val kind: EntryKind = EntryKind.BLOCK)
private val presets = listOf(
    QuickPreset("90 min Deep Work", "Deep work", 90, RoutineCategory.FOCUS_STUDY),
    QuickPreset("45 min Pomodoro", "Pomodoro", 45, RoutineCategory.FOCUS_STUDY),
    QuickPreset("15 min Walk", "Screen-free walk", 15, RoutineCategory.REST_BREAK),
    QuickPreset("IB Revision", "IB revision", 90, RoutineCategory.FOCUS_STUDY),
    QuickPreset("Exam", "Exam", 60, RoutineCategory.SCHOOL, EntryKind.EXAM),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorSheet(
    selectedDate: LocalDate, subjects: List<Subject>, editing: ResolvedTimelineItem.Milestone?, busy: Boolean,
    onDismiss: () -> Unit, onSave: (EntryDraft) -> Unit, onNewSubject: () -> Unit,
) {
    val initial = remember(selectedDate, editing?.key) {
        val now = LocalDateTime.now()
        if (editing != null) editing.date.atTime(editing.dueTime ?: LocalTime.of(16, 0))
        else if (selectedDate == now.toLocalDate()) now.plusMinutes((15 - now.minute % 15).toLong()).withSecond(0).withNano(0)
        else selectedDate.atTime(16, 0)
    }
    var title by rememberSaveable(editing?.key) { mutableStateOf(editing?.title ?: "") }
    var dateText by rememberSaveable(editing?.key) { mutableStateOf(initial.toLocalDate().toString()) }
    var startText by rememberSaveable(editing?.key) { mutableStateOf(initial.toLocalTime().clockLabel()) }
    var endText by rememberSaveable(editing?.key) { mutableStateOf(initial.toLocalTime().plusMinutes(90).clockLabel()) }
    var category by rememberSaveable { mutableStateOf(RoutineCategory.FOCUS_STUDY) }
    var kind by rememberSaveable(editing?.key) { mutableStateOf(if (editing == null) EntryKind.BLOCK else if (editing.isExam) EntryKind.EXAM else EntryKind.DEADLINE) }
    var subjectId by rememberSaveable(editing?.key) { mutableStateOf(editing?.subject?.id) }
    var weekly by rememberSaveable { mutableStateOf(false) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var allDay by rememberSaveable(editing?.key) { mutableStateOf(editing != null && editing.dueTime == null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (editing == null) "Make a little space." else "Edit milestone", style = MaterialTheme.typography.headlineSmall)
            Text("A plan for your time. Stored only on this device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (editing == null) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets, key = { it.label }) { preset ->
                        SuggestionChip(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            title = preset.title; kind = preset.kind; category = preset.category
                            endText = (ScheduleValidation.parseTime(startText) ?: LocalTime.of(16, 0)).plusMinutes(preset.minutes.toLong()).clockLabel()
                            allDay = false; error = null
                        }, label = { Text(preset.label) })
                    }
                }
            }
            Text("Saved subjects", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects, key = { it.id }) { subject ->
                    FilterChip(selected = subjectId == subject.id, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (subjectId == subject.id) subjectId = null else {
                            if (title.isBlank() || title == subjects.firstOrNull { it.id == subjectId }?.name) title = subject.name
                            subjectId = subject.id
                            endText = (ScheduleValidation.parseTime(startText) ?: LocalTime.of(16, 0)).plusMinutes(subject.defaultDurationMinutes.toLong()).clockLabel()
                        }
                    }, label = { Text(subject.name) })
                }
                item { SuggestionChip(onClick = onNewSubject, label = { Text("+ Subject") }) }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val choices = if (editing == null) EntryKind.entries else listOf(EntryKind.DEADLINE, EntryKind.EXAM)
                choices.forEachIndexed { index, choice ->
                    SegmentedButton(selected = kind == choice, onClick = { kind = choice; error = null },
                        shape = SegmentedButtonDefaults.itemShape(index, choices.size)) {
                        Text(when (choice) { EntryKind.BLOCK -> "Block"; EntryKind.DEADLINE -> "Deadline"; EntryKind.EXAM -> "Exam" })
                    }
                }
            }
            OutlinedTextField(value = title, onValueChange = { title = it.take(120); error = null }, label = { Text("Title") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy)
            OutlinedTextField(value = dateText, onValueChange = { dateText = it; error = null }, label = { Text("Date · YYYY-MM-DD") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                trailingIcon = { IconButton(onClick = { pickingDate = true }, enabled = !busy) { Icon(Icons.Outlined.CalendarMonth, "Choose date") } })
            if (kind != EntryKind.BLOCK) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = allDay, onCheckedChange = { allDay = it }, enabled = !busy)
                Text("All-day marker (no fixed time)")
            }
            AnimatedVisibility(kind == EntryKind.BLOCK || !allDay) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = startText, onValueChange = { startText = it; error = null }, label = { Text(if (kind == EntryKind.BLOCK) "Start · HH:mm" else "Due · HH:mm") },
                        singleLine = true, modifier = Modifier.weight(1f), enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                    if (kind == EntryKind.BLOCK) OutlinedTextField(value = endText, onValueChange = { endText = it; error = null }, label = { Text("End · HH:mm") },
                        singleLine = true, modifier = Modifier.weight(1f), enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                }
            }
            if (kind == EntryKind.BLOCK) {
                Text("An end earlier than the start continues into the next day.", style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RoutineCategory.entries, key = { it.name }) { option ->
                        FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option.label()) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Repeat weekly", style = MaterialTheme.typography.titleSmall)
                        Text(if (weekly) "From this date forward, on the same weekday" else "Only this date — your weekly plan stays unchanged", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = weekly, onCheckedChange = { weekly = it }, enabled = !busy)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reminder", style = MaterialTheme.typography.titleSmall)
                        Text(if (category == RoutineCategory.REST_BREAK) "At the start of recovery" else "5 minutes before the block", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = notifications, onCheckedChange = { notifications = it }, enabled = !busy)
                }
            } else Text("Markers highlight a deadline without reserving study time. Add a separate block if you need time to prepare. Markers do not send block reminders.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
                val date = ScheduleValidation.parseDate(dateText)
                val start = if (kind != EntryKind.BLOCK && allDay) null else ScheduleValidation.parseTime(startText)
                val end = if (kind == EntryKind.BLOCK) ScheduleValidation.parseTime(endText) else null
                error = when {
                    title.isBlank() -> "Give this entry a title."
                    date == null -> "Use a valid date in 1900–2100, for example 2026-09-07."
                    (kind == EntryKind.BLOCK || !allDay) && start == null -> "Enter a 24-hour time, for example 16:30."
                    kind == EntryKind.BLOCK && (end == null || end == start) -> "Use different, valid start and end times."
                    else -> null
                }
                if (error == null && date != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(EntryDraft(title.trim(), subjectId?.takeIf { id -> subjects.any { it.id == id } }, kind, date, start, end, category, weekly, notifications,
                        existingMilestoneId = editing?.milestoneId ?: 0, isCompleted = editing?.isCompleted ?: false))
                }
            }) { Text(if (busy) "Saving…" else if (editing == null) "Add to my day" else "Save milestone") }
        }
    }
    if (pickingDate) AppDatePicker(ScheduleValidation.parseDate(dateText) ?: selectedDate,
        onDismiss = { pickingDate = false }, onDate = { dateText = it.toString(); pickingDate = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePicker(date: LocalDate, onDismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    // Material DatePicker speaks UTC epoch milliseconds, not local-zone midnight instants.
    val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = state.selectedDateMillis != null, onClick = {
            state.selectedDateMillis?.let { onDate(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L))) }
        }) { Text("Choose") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}
