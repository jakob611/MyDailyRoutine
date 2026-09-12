package com.example.mydailyroutine.features.goals.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.domain.model.GoalActivity
import com.example.mydailyroutine.domain.model.GoalMilestone
import com.example.mydailyroutine.domain.model.GoalProgress
import com.example.mydailyroutine.domain.model.GoalsProject
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import java.time.LocalDate
import kotlin.math.roundToInt

private fun logProgress(projectId: Long, activityId: Long, hours: Double): TimelineAction =
    TimelineAction.AddGoalProgress(GoalProgress(projectId = projectId, activityId = activityId, kind = "hour", amount = hours, date = LocalDate.now()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityEditorSheet(project: GoalsProject, initial: GoalActivity?, busy: Boolean, progress: List<GoalProgress>,
                                onDismiss: () -> Unit, onAction: (TimelineAction) -> Unit) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var category by rememberSaveable(initial?.id) { mutableStateOf(initial?.category ?: if (project.kind == "EE") "STAGE" else null) }
    var startEpoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.start ?: LocalDate.now()).toEpochDay()) }
    var endEpoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.end ?: LocalDate.now().plusDays(30)).toEpochDay()) }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.note.orEmpty()) }
    var casProject by rememberSaveable(initial?.id) { mutableStateOf(initial?.isCasProject ?: false) }
    var done by rememberSaveable(initial?.id) { mutableStateOf(initial?.isDone ?: false) }
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var reflectionText by rememberSaveable(initial?.id) { mutableStateOf("") }
    val start = LocalDate.ofEpochDay(startEpoch)
    val end = LocalDate.ofEpochDay(endEpoch).let { if (it.isBefore(start)) start else it }
    val saved = initial != null && initial.id > 0
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1,
        tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (saved) R.string.goals_edit_activity else R.string.goals_new_activity), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(title, { title = it.take(80) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_activity_title)) })
            if (project.kind == "CAS") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CREATIVITY" to R.string.goals_category_creativity, "ACTIVITY" to R.string.goals_category_activity, "SERVICE" to R.string.goals_category_service).forEach { (value, res) ->
                        if (category == value) FilledTonalButton(onClick = { category = null }) { Text(stringResource(res)) }
                        else OutlinedButton(onClick = { category = value }) { Text(stringResource(res)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(casProject, { casProject = it }, enabled = !busy)
                    Text(stringResource(R.string.goals_cas_project_flag), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingStart = true }) {
                    Text("${stringResource(R.string.goals_start_date)} · ${start.format(GoalShortFormat)}", maxLines = 1)
                }
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingEnd = true }) {
                    Text("${stringResource(R.string.goals_end_date)} · ${end.format(GoalShortFormat)}", maxLines = 1)
                }
            }
            OutlinedTextField(note, { note = it.take(2000) }, modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !busy,
                label = { Text(stringResource(R.string.goals_activity_note)) }, placeholder = { Text(stringResource(R.string.goals_activity_note_hint)) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(done, { done = it }, enabled = !busy)
                Text(stringResource(R.string.goals_done))
            }
            if (saved && initial != null) {
                val hours = progress.filter { it.kind == "hour" && it.activityId == initial.id }.sumOf { it.amount }
                Text(stringResource(R.string.goals_hours_total_log, hours.roundToInt()), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.goals_hours_log), style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary, modifier = Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 0.5)) }) { Text(stringResource(R.string.goals_minutes_step, 30)) }
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 1.0)) }) { Text(stringResource(R.string.goals_hours_step, 1)) }
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 2.0)) }) { Text(stringResource(R.string.goals_hours_step, 2)) }
                }
                Text(stringResource(R.string.goals_reflections), style = MaterialTheme.typography.titleMedium)
                val reflections = progress.filter { it.kind == "reflection" && it.activityId == initial.id }
                if (reflections.isEmpty()) Text(stringResource(R.string.goals_reflections_empty), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                reflections.forEach { entry ->
                    OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.date.format(GoalShortFormat), style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary)
                            Text(entry.note.orEmpty(), style = MaterialTheme.typography.bodySmall)
                            Row {
                                Spacer(Modifier.weight(1f))
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalProgress(entry.id)) }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                            }
                        }
                    }
                }
                OutlinedTextField(reflectionText, { reflectionText = it.take(2000) }, modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !busy,
                    label = { Text(stringResource(R.string.goals_add_reflection)) }, placeholder = { Text(stringResource(R.string.goals_reflection_hint)) })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(enabled = !busy && reflectionText.isNotBlank(), onClick = {
                        onAction(TimelineAction.AddGoalProgress(GoalProgress(projectId = project.id, activityId = initial.id, kind = "reflection", amount = 1.0, note = reflectionText.trim(), date = LocalDate.now())))
                        reflectionText = ""
                    }) { Text(stringResource(R.string.goals_save_reflection)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.GoalActivityToSchedule(initial)) }) { Text(stringResource(R.string.goals_schedule_activity)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !busy && title.isNotBlank(), onClick = {
                    onAction(TimelineAction.SaveGoalActivity(GoalActivity(id = initial?.id ?: 0, projectId = project.id, title = title.trim(), category = category,
                        start = start, end = end, note = note.trim().takeIf { it.isNotEmpty() }, isCasProject = casProject, isDone = done, isScheduled = initial?.isScheduled ?: false)))
                    onDismiss()
                }) { Text(stringResource(R.string.save)) }
                if (initial != null && initial.id > 0) {
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { confirmingDelete = true }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                }
            }
        }
    }
    if (pickingStart) AppDatePicker(start, onDismiss = { pickingStart = false }, onDate = {
        if (it.toEpochDay() > endEpoch) endEpoch = it.toEpochDay(); startEpoch = it.toEpochDay(); pickingStart = false
    })
    if (pickingEnd) AppDatePicker(end, onDismiss = { pickingEnd = false }, onDate = {
        if (it.toEpochDay() < startEpoch) startEpoch = it.toEpochDay(); endEpoch = it.toEpochDay(); pickingEnd = false
    })
    if (confirmingDelete && initial != null) AlertDialog(onDismissRequest = { confirmingDelete = false },
        title = { Text(stringResource(R.string.goals_delete_activity_title)) },
        text = { Text(stringResource(R.string.goals_delete_activity_body, initial.title)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalActivity(initial.id)); confirmingDelete = false; onDismiss() }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.keep)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneEditorSheet(project: GoalsProject, initial: GoalMilestone?, busy: Boolean, onDismiss: () -> Unit, onAction: (TimelineAction) -> Unit) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var epoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.dueDate ?: LocalDate.now()).toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1,
        tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (initial == null) R.string.goals_add_milestone else R.string.goals_edit_milestone), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(title, { title = it.take(80) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_milestone_title)) })
            OutlinedButton(enabled = !busy, onClick = { picking = true }) { Text(LocalDate.ofEpochDay(epoch).format(GoalDateFormat)) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !busy && title.isNotBlank(), onClick = {
                    onAction(TimelineAction.SaveGoalMilestone(GoalMilestone(id = initial?.id ?: 0, projectId = project.id, title = title.trim(),
                        dueDate = LocalDate.ofEpochDay(epoch), isDone = initial?.isDone ?: false)))
                    onDismiss()
                }) { Text(stringResource(R.string.save)) }
                if (initial != null && initial.id > 0) {
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { confirmingDelete = true }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                }
            }
        }
    }
    if (picking) AppDatePicker(LocalDate.ofEpochDay(epoch), onDismiss = { picking = false }, onDate = { epoch = it.toEpochDay(); picking = false })
    if (confirmingDelete && initial != null) AlertDialog(onDismissRequest = { confirmingDelete = false },
        title = { Text(stringResource(R.string.goals_delete_milestone_title)) },
        text = { Text(stringResource(R.string.goals_delete_milestone_body, initial.title)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalMilestone(initial.id)); confirmingDelete = false; onDismiss() }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.keep)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectEditorSheet(initial: GoalsProject?, busy: Boolean, onDismiss: () -> Unit, onAction: (TimelineAction) -> Unit) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var startEpoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.start ?: LocalDate.now()).toEpochDay()) }
    var endEpoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.end ?: LocalDate.now().plusMonths(18)).toEpochDay()) }
    var hours by rememberSaveable(initial?.id) { mutableStateOf(initial?.targetHours?.toString() ?: "150") }
    var words by rememberSaveable(initial?.id) { mutableStateOf(initial?.targetWords?.toString() ?: "") }
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val start = LocalDate.ofEpochDay(startEpoch)
    val end = LocalDate.ofEpochDay(endEpoch).let { if (it.isBefore(start)) start else it }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1,
        tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (initial == null) R.string.goals_new_project_title else R.string.goals_edit_project), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(name, { name = it.take(60) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_project_name)) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingStart = true }) {
                    Text("${stringResource(R.string.goals_start_date)} · ${start.format(GoalShortFormat)}", maxLines = 1)
                }
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingEnd = true }) {
                    Text("${stringResource(R.string.goals_end_date)} · ${end.format(GoalShortFormat)}", maxLines = 1)
                }
            }
            OutlinedTextField(hours, { hours = it.filter { c -> c.isDigit() || c == '.' }.take(7) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_target_hours)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(words, { words = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_target_words)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text(stringResource(R.string.goals_period_note), style = MaterialTheme.typography.labelSmall, color = RoutineColors.Warning)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !busy && name.isNotBlank(), onClick = {
                    onAction(TimelineAction.SaveGoalsProject(GoalsProject(id = initial?.id ?: 0, name = name.trim(), kind = initial?.kind ?: "CUSTOM",
                        start = start, end = end, targetHours = hours.trim().toDoubleOrNull(), targetWords = words.trim().toIntOrNull())))
                    onDismiss()
                }) { Text(stringResource(R.string.save)) }
                if (initial != null && initial.id > 0) {
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { confirmingDelete = true }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                }
            }
        }
    }
    if (pickingStart) AppDatePicker(start, onDismiss = { pickingStart = false }, onDate = {
        if (it.toEpochDay() > endEpoch) endEpoch = it.toEpochDay(); startEpoch = it.toEpochDay(); pickingStart = false
    })
    if (pickingEnd) AppDatePicker(end, onDismiss = { pickingEnd = false }, onDate = {
        if (it.toEpochDay() < startEpoch) startEpoch = it.toEpochDay(); endEpoch = it.toEpochDay(); pickingEnd = false
    })
    if (confirmingDelete && initial != null) AlertDialog(onDismissRequest = { confirmingDelete = false },
        title = { Text(stringResource(R.string.goals_delete_project_title)) },
        text = { Text(stringResource(R.string.goals_delete_project_body, initial.name)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalsProject(initial.id)); confirmingDelete = false; onDismiss() }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.keep)) } })
}
