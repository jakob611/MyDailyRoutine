package com.example.mydailyroutine.features.goals.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheetScaffold
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.components.SheetPrimaryButton
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.model.GoalActivity
import com.example.mydailyroutine.domain.model.GoalMilestone
import com.example.mydailyroutine.domain.model.GoalProgress
import com.example.mydailyroutine.domain.model.GoalsProject
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import java.time.LocalDate

// The three forms behind the goals screen — an activity, a milestone, a project — and the
// confirmation that guards a delete.
//
// They are leaves: each is opened by its `*EditorHost` in `GoalsScreen`, each owns nothing but the
// draft the reader is typing, and none of them can affect the screen underneath. Four hundred and
// eighty lines of form that is usually not on screen at all used to sit in the middle of the file
// that lays the screen out.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ActivityEditorSheet(
    project: GoalsProject,
    projects: List<GoalsProject>,
    onPickProject: (GoalsProject) -> Unit,
    initial: GoalActivity?,
    busy: Boolean,
    progress: List<GoalProgress>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
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
    // The three CAS categories and the EE stages are different vocabularies: pointing a new activity
    // at the other plan drops a category that plan has no place for.
    LaunchedEffect(project.id) {
        if (!saved) {
            category = if (project.kind == "EE") "STAGE" else null
            // "This is also a CAS project" is a CAS question; it has no meaning under the EE plan.
            if (project.kind != "CAS") casProject = false
        }
    }
    val closeLabel = stringResource(R.string.close)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.SheetSurface,
        tonalElevation = 0.dp,
        sheetState = sheetState,
    ) {
        RoutineSheetScaffold(
            title = stringResource(if (saved) R.string.goals_edit_activity else R.string.goals_new_activity),
            closeLabel = closeLabel,
            onClose = onDismiss,
            modifier = Modifier.testTag("activity-editor"),
            footer = {
                SheetPrimaryButton(
                    label = stringResource(if (busy) R.string.saving else R.string.save),
                    enabled = !busy && title.isNotBlank(),
                    onClick = {
                        onAction(
                            TimelineAction.SaveGoalActivity(
                                GoalActivity(
                                    id = initial?.id ?: 0, projectId = project.id, title = title.trim(), category = category,
                                    start = start, end = end, note = note.trim().takeIf { it.isNotEmpty() },
                                    isCasProject = casProject, isDone = done, isScheduled = initial?.isScheduled ?: false,
                                ),
                            ),
                        )
                        onDismiss()
                    },
                )
                if (saved && initial != null) {
                    SheetSecondaryButton(
                        label = stringResource(R.string.delete),
                        enabled = !busy,
                        contentColor = RoutineColors.Error,
                        onClick = { confirmingDelete = true },
                    )
                }
            },
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                modifier = Modifier.fillMaxWidth().testTag("activity-title"),
                singleLine = true,
                enabled = !busy,
                label = { RoutineText(stringResource(R.string.goals_activity_title)) },
            )
            if (projects.size > 1 && !saved) {
                ProjectPicker(projects, project.id, enabled = !busy, onPick = onPickProject)
            }
            if (project.kind == "CAS") {
                ActionRow {
                    listOf(
                        "CREATIVITY" to R.string.goals_category_creativity,
                        "ACTIVITY" to R.string.goals_category_activity,
                        "SERVICE" to R.string.goals_category_service,
                    ).forEach { (value, res) ->
                        if (category == value) {
                            FilledTonalButton(onClick = { category = null }) {
                                RoutineLabel(stringResource(res), style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(onClick = { category = value }) {
                                RoutineLabel(stringResource(res), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                SettingRow(
                    title = stringResource(R.string.goals_cas_project_flag),
                    description = null,
                    control = {
                        Checkbox(checked = casProject, onCheckedChange = { casProject = it }, enabled = !busy,
                            modifier = Modifier.size(RoutineMetrics.ActionMinWidth))
                    },
                )
            }
            ActionRow {
                OutlinedButton(
                    enabled = !busy,
                    shape = RoutineShapes.Pill,
                    onClick = { pickingStart = true },
                ) {
                    RoutineLabel("${stringResource(R.string.goals_start_date)} · ${RoutineDate.normal(start)}",
                        style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    enabled = !busy,
                    shape = RoutineShapes.Pill,
                    onClick = { pickingEnd = true },
                ) {
                    RoutineLabel("${stringResource(R.string.goals_end_date)} · ${RoutineDate.normal(end)}",
                        style = MaterialTheme.typography.labelLarge)
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(2000) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !busy,
                label = { RoutineText(stringResource(R.string.goals_activity_note)) },
                placeholder = { RoutineText(stringResource(R.string.goals_activity_note_hint), maxLines = RoutineTextDefaults.Body) },
            )
            SettingRow(
                title = stringResource(R.string.goals_done),
                control = {
                    Checkbox(checked = done, onCheckedChange = { done = it }, enabled = !busy,
                        modifier = Modifier.size(RoutineMetrics.ActionMinWidth))
                },
            )
            if (saved && initial != null) {
                val hours = progress.filter { it.kind == "hour" && it.activityId == initial.id }.sumOf { it.amount }
                RoutineText(stringResource(R.string.goals_hours_total_log, goalsHoursLabel(hours)),
                    style = MaterialTheme.typography.titleMedium, maxLines = RoutineTextDefaults.Body)
                RoutineLabel(stringResource(R.string.goals_hours_log), style = MaterialTheme.typography.labelMedium,
                    color = RoutineColors.TextSecondary)
                ActionRow {
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 0.5)) }) {
                        RoutineLabel(stringResource(R.string.goals_minutes_step, 30), style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 1.0)) }) {
                        RoutineLabel(stringResource(R.string.goals_hours_step, 1), style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 2.0)) }) {
                        RoutineLabel(stringResource(R.string.goals_hours_step, 2), style = MaterialTheme.typography.labelLarge)
                    }
                }
                RoutineText(stringResource(R.string.goals_reflections), style = MaterialTheme.typography.titleMedium,
                    maxLines = RoutineTextDefaults.Body)
                val reflections = progress.filter { it.kind == "reflection" && it.activityId == initial.id }
                if (reflections.isEmpty()) {
                    RoutineText(stringResource(R.string.goals_reflections_empty), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
                }
                reflections.forEach { entry ->
                    OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.CardBorder),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                        ) {
                            RoutineLabel(RoutineDate.normal(entry.date), style = MaterialTheme.typography.labelSmall,
                                color = RoutineColors.TextSecondary)
                            RoutineText(entry.note.orEmpty(), style = MaterialTheme.typography.bodySmall,
                                maxLines = RoutineTextDefaults.Paragraph)
                            ActionRow {
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalProgress(entry.id)) }) {
                                    RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge,
                                        color = RoutineColors.Error)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = reflectionText,
                    onValueChange = { reflectionText = it.take(2000) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !busy,
                    label = { RoutineText(stringResource(R.string.goals_add_reflection)) },
                    placeholder = { RoutineText(stringResource(R.string.goals_reflection_hint), maxLines = RoutineTextDefaults.Body) },
                )
                ActionRow {
                    FilledTonalButton(
                        enabled = !busy && reflectionText.isNotBlank(),
                        onClick = {
                            onAction(
                                TimelineAction.AddGoalProgress(
                                    GoalProgress(projectId = project.id, activityId = initial.id, kind = "reflection",
                                        amount = 1.0, note = reflectionText.trim(), date = LocalDate.now()),
                                ),
                            )
                            reflectionText = ""
                        },
                    ) { RoutineLabel(stringResource(R.string.goals_save_reflection), style = MaterialTheme.typography.labelLarge) }
                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.GoalActivityToSchedule(initial)) }) {
                        RoutineLabel(stringResource(R.string.goals_schedule_activity), style = MaterialTheme.typography.labelLarge)
                    }
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
    if (confirmingDelete && initial != null) {
        DeleteConfirmation(
            title = stringResource(R.string.goals_delete_activity_title),
            body = stringResource(R.string.goals_delete_activity_body, initial.title),
            busy = busy,
            onCancel = { confirmingDelete = false },
            onConfirm = { confirmingDelete = false; onDismiss() },
            onDelete = { onAction(TimelineAction.DeleteGoalActivity(initial.id)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MilestoneEditorSheet(
    project: GoalsProject,
    projects: List<GoalsProject>,
    onPickProject: (GoalsProject) -> Unit,
    initial: GoalMilestone?,
    busy: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var epoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.dueDate ?: LocalDate.now()).toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val closeLabel = stringResource(R.string.close)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.SheetSurface,
        tonalElevation = 0.dp,
        sheetState = sheetState,
    ) {
        RoutineSheetScaffold(
            title = stringResource(if (initial == null) R.string.goals_add_milestone else R.string.goals_edit_milestone),
            closeLabel = closeLabel,
            onClose = onDismiss,
            modifier = Modifier.testTag("milestone-editor"),
            footer = {
                SheetPrimaryButton(
                    label = stringResource(if (busy) R.string.saving else R.string.save),
                    enabled = !busy && title.isNotBlank(),
                    onClick = {
                        onAction(
                            TimelineAction.SaveGoalMilestone(
                                GoalMilestone(id = initial?.id ?: 0, projectId = project.id, title = title.trim(),
                                    dueDate = LocalDate.ofEpochDay(epoch), isDone = initial?.isDone ?: false),
                            ),
                        )
                        onDismiss()
                    },
                )
                if (initial != null && initial.id > 0) {
                    SheetSecondaryButton(
                        label = stringResource(R.string.delete),
                        enabled = !busy,
                        contentColor = RoutineColors.Error,
                        onClick = { confirmingDelete = true },
                    )
                }
            },
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !busy,
                label = { RoutineText(stringResource(R.string.goals_milestone_title)) },
            )
            if (projects.size > 1 && initial == null) {
                ProjectPicker(projects, project.id, enabled = !busy, onPick = onPickProject)
            }
            OutlinedButton(enabled = !busy, shape = RoutineShapes.Pill, onClick = { picking = true }) {
                RoutineLabel(RoutineDate.normalYear(LocalDate.ofEpochDay(epoch)), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    if (picking) AppDatePicker(LocalDate.ofEpochDay(epoch), onDismiss = { picking = false }, onDate = { epoch = it.toEpochDay(); picking = false })
    if (confirmingDelete && initial != null) {
        DeleteConfirmation(
            title = stringResource(R.string.goals_delete_milestone_title),
            body = stringResource(R.string.goals_delete_milestone_body, initial.title),
            busy = busy,
            onCancel = { confirmingDelete = false },
            onConfirm = { confirmingDelete = false; onDismiss() },
            onDelete = { onAction(TimelineAction.DeleteGoalMilestone(initial.id)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectEditorSheet(
    initial: GoalsProject?,
    busy: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
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
    val closeLabel = stringResource(R.string.close)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.SheetSurface,
        tonalElevation = 0.dp,
        sheetState = sheetState,
    ) {
        RoutineSheetScaffold(
            title = stringResource(if (initial == null) R.string.goals_new_project_title else R.string.goals_edit_project),
            closeLabel = closeLabel,
            onClose = onDismiss,
            modifier = Modifier.testTag("project-editor"),
            footer = {
                SheetPrimaryButton(
                    label = stringResource(if (busy) R.string.saving else R.string.save),
                    enabled = !busy && name.isNotBlank(),
                    onClick = {
                        onAction(
                            TimelineAction.SaveGoalsProject(
                                GoalsProject(id = initial?.id ?: 0, name = name.trim(), kind = initial?.kind ?: "CUSTOM",
                                    start = start, end = end, targetHours = hours.trim().toDoubleOrNull(),
                                    targetWords = words.trim().toIntOrNull()),
                            ),
                        )
                        onDismiss()
                    },
                )
                if (initial != null && initial.id > 0) {
                    SheetSecondaryButton(
                        label = stringResource(R.string.delete),
                        enabled = !busy,
                        contentColor = RoutineColors.Error,
                        onClick = { confirmingDelete = true },
                    )
                }
            },
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !busy,
                label = { RoutineText(stringResource(R.string.goals_project_name)) },
            )
            ActionRow {
                OutlinedButton(
                    enabled = !busy,
                    shape = RoutineShapes.Pill,
                    onClick = { pickingStart = true },
                ) {
                    RoutineLabel("${stringResource(R.string.goals_start_date)} · ${RoutineDate.normal(start)}",
                        style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    enabled = !busy,
                    shape = RoutineShapes.Pill,
                    onClick = { pickingEnd = true },
                ) {
                    RoutineLabel("${stringResource(R.string.goals_end_date)} · ${RoutineDate.normal(end)}",
                        style = MaterialTheme.typography.labelLarge)
                }
            }
            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it.filter { character -> character.isDigit() || character == '.' }.take(7) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !busy,
                label = { RoutineText(stringResource(R.string.goals_target_hours)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = words,
                onValueChange = { words = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !busy,
                label = { RoutineText(stringResource(R.string.goals_target_words)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            RoutineText(stringResource(R.string.goals_period_note), style = MaterialTheme.typography.labelSmall,
                color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
        }
    }
    if (pickingStart) AppDatePicker(start, onDismiss = { pickingStart = false }, onDate = {
        if (it.toEpochDay() > endEpoch) endEpoch = it.toEpochDay(); startEpoch = it.toEpochDay(); pickingStart = false
    })
    if (pickingEnd) AppDatePicker(end, onDismiss = { pickingEnd = false }, onDate = {
        if (it.toEpochDay() < startEpoch) startEpoch = it.toEpochDay(); endEpoch = it.toEpochDay(); pickingEnd = false
    })
    if (confirmingDelete && initial != null) {
        DeleteConfirmation(
            title = stringResource(R.string.goals_delete_project_title),
            body = stringResource(R.string.goals_delete_project_body, initial.name),
            busy = busy,
            onCancel = { confirmingDelete = false },
            onConfirm = { confirmingDelete = false; onDismiss() },
            onDelete = { onAction(TimelineAction.DeleteGoalsProject(initial.id)) },
        )
    }
}

/**
 * One destructive-confirmation dialog for the whole goals feature. The haptic warning, the button
 * pair and the wording are identical everywhere instead of three near-copies.
 */
@Composable
private fun DeleteConfirmation(
    title: String,
    body: String,
    busy: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalRoutineHaptics.current
    AlertDialog(
        onDismissRequest = onCancel,
        title = { RoutineText(title, style = MaterialTheme.typography.headlineSmall, maxLines = RoutineTextDefaults.Body) },
        text = { RoutineText(body, maxLines = RoutineTextDefaults.Paragraph) },
        confirmButton = {
            TextButton(
                enabled = !busy,
                // Deleting dispatches an action, and the wrapper answers every destructive action with the
                // error haptic. Warning here as well would be two vibrations for one decision.
                onClick = { onDelete(); onConfirm() },
            ) { RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Error) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                RoutineLabel(stringResource(R.string.keep), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
