package com.example.mydailyroutine.features.tasks.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import com.example.mydailyroutine.core.designsystem.components.RoutineCompletionCheckbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheetListScaffold
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.spatialSpec
import com.example.mydailyroutine.core.designsystem.theme.TransitionMillis
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.domain.model.Task
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import java.time.LocalDate
import java.time.temporal.ChronoUnit


/**
 * Homework/errand checklist: fast entry, relative due labels, one-tap completion.
 *
 * The quick-add row and the per-task edit row share one [TaskAttributePickers] component, so the due
 * date and subject pickers are literally the same control in both places instead of two near-copies
 * that drift apart.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TasksSheet(
    tasks: List<Task>,
    subjects: List<Subject>,
    busy: Boolean,
    sheetState: SheetState,
    prefillTitle: String? = null,
    prefillDueEpoch: Long? = null,
    onAction: (TimelineAction) -> Unit,
) {
    val today = LocalDate.now()
    val haptics = LocalRoutineHaptics.current
    var showDone by rememberSaveable { mutableStateOf(false) }
    val subjectsById = remember(subjects) { subjects.associateBy { it.id } }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var newDueEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var newSubject by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingNewDate by rememberSaveable { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(prefillTitle) {
        if (!prefillTitle.isNullOrBlank()) {
            newTitle = prefillTitle
            prefillDueEpoch?.let { newDueEpoch = it }
        }
    }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val open = tasks.filter { it.completedAtEpochMillis == null }
    val overdue = open.filter { task -> task.dueDate?.let { due -> due.isBefore(today) } == true }.sortedBy { it.dueDate }
    val dueToday = open.filter { it.dueDate == today }.sortedBy { it.createdAtEpochMillis }
    val upcoming = open.filter { task -> task.dueDate?.let { due -> due.isAfter(today) } == true }.sortedBy { it.dueDate }
    val noDue = open.filter { it.dueDate == null }.sortedByDescending { it.createdAtEpochMillis }
    val done = tasks.filter { it.completedAtEpochMillis != null }.sortedByDescending { it.completedAtEpochMillis }
    fun submitNew() {
        val clean = newTitle.trim()
        if (clean.isEmpty() || busy) return
        onAction(TimelineAction.AddTask(clean, newDueEpoch?.let(LocalDate::ofEpochDay), newSubject))
        newTitle = ""; newDueEpoch = null; newSubject = null
    }
    val closeLabel = stringResource(R.string.close)
    ModalBottomSheet(
        onDismissRequest = { onAction(TimelineAction.CloseTasks) },
        shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.SheetSurface,
        tonalElevation = 0.dp,
        sheetState = sheetState,
    ) {
        RoutineSheetListScaffold(
            title = stringResource(R.string.tasks_title),
            subtitle = stringResource(R.string.tasks_open_count, open.size),
            closeLabel = closeLabel,
            onClose = { onAction(TimelineAction.CloseTasks) },
            modifier = Modifier.testTag("tasks-sheet"),
        ) {
            item(key = "tasks-quick-add-row") {
                Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it.take(120) },
                        modifier = Modifier.fillMaxWidth().testTag("task-quick-add"),
                        singleLine = true,
                        enabled = !busy,
                        placeholder = { RoutineText(stringResource(R.string.tasks_quick_add_hint)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitNew() }),
                        trailingIcon = {
                            if (newTitle.isNotBlank()) {
                                IconButton(enabled = !busy, onClick = { submitNew() }) {
                                    Icon(Icons.Default.Check, stringResource(R.string.tasks_add))
                                }
                            }
                        },
                    )
                    TaskAttributePickers(
                        dueEpoch = newDueEpoch,
                        subjectId = newSubject,
                        subjects = subjects,
                        subjectsById = subjectsById,
                        today = today,
                        busy = busy,
                        onDueEpoch = { newDueEpoch = it },
                        onSubjectId = { newSubject = it },
                        onPickDate = { pickingNewDate = true },
                    )
                }
            }
            if (open.isEmpty()) {
                item(key = "tasks-empty") {
                    RoutineText(stringResource(R.string.tasks_empty), style = MaterialTheme.typography.bodyMedium,
                        color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
                }
            }
            taskSection("overdue", R.string.tasks_section_overdue, RoutineColors.Error, overdue, subjects, subjectsById, today, busy, expandedId,
                onAction = onAction, onExpand = { id -> expandedId = if (expandedId == id) null else id }, onRequestDelete = { deleteId = it })
            taskSection("today", R.string.tasks_section_today, RoutineColors.Warning, dueToday, subjects, subjectsById, today, busy, expandedId,
                onAction = onAction, onExpand = { id -> expandedId = if (expandedId == id) null else id }, onRequestDelete = { deleteId = it })
            taskSection("upcoming", R.string.tasks_section_upcoming, RoutineColors.Cobalt, upcoming, subjects, subjectsById, today, busy, expandedId,
                onAction = onAction, onExpand = { id -> expandedId = if (expandedId == id) null else id }, onRequestDelete = { deleteId = it })
            taskSection("nodue", R.string.tasks_section_no_due, RoutineColors.TextSecondary, noDue, subjects, subjectsById, today, busy, expandedId,
                onAction = onAction, onExpand = { id -> expandedId = if (expandedId == id) null else id }, onRequestDelete = { deleteId = it })
            if (done.isNotEmpty()) {
                item(key = "tasks-done-header") {
                    val doneChevron by animateFloatAsState(if (showDone) 180f else 0f, spatialSpec<Float>(LocalReduceMotion.current), label = "done-chevron")
                    Row(
                    Modifier.fillMaxWidth().animateItem(placementSpec = taskListSpec())
                        .clickable(enabled = !busy) { haptics.toggle(!showDone); showDone = !showDone },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    RoutineText(
                        text = stringResource(R.string.tasks_section_done, done.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = RoutineTextDefaults.Body,
                    )
                    Icon(Icons.Outlined.ExpandMore, null, tint = RoutineColors.TextSecondary,
                        modifier = Modifier.size(20.dp).rotate(doneChevron))
                    TextButton(enabled = !busy, onClick = { haptics.warning(); onAction(TimelineAction.ClearCompletedTasks) }) {
                        RoutineLabel(stringResource(R.string.tasks_clear_done), style = MaterialTheme.typography.labelLarge)
                    }
                }
                }
            }
            if (showDone) {
                items(done, key = { "done:${it.id}" }) { task ->
                    OutlinedCard(
                        modifier = Modifier.animateItem(placementSpec = taskListSpec()).fillMaxWidth(),
                        shape = RoutineShapes.Card,
                        border = BorderStroke(1.dp, RoutineColors.Border),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = RoutineSpacing.md, vertical = RoutineSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                        ) {
                            RoutineCompletionCheckbox(
                                checked = true,
                                onCheckedChange = { if (!busy) { haptics.tap(); onAction(TimelineAction.ToggleTask(task.id)) } },
                                enabled = !busy,
                                modifier = Modifier.size(RoutineMetrics.ActionMinWidth),
                            )
                            RoutineText(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoutineColors.TextSecondary,
                                modifier = Modifier.weight(1f),
                                maxLines = RoutineTextDefaults.Body,
                            )
                            task.dueDate?.let {
                                RoutineLabel(RoutineDate.normal(it), style = MaterialTheme.typography.labelSmall,
                                    color = RoutineColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
    if (pickingNewDate) {
        AppDatePicker(
            newDueEpoch?.let(LocalDate::ofEpochDay) ?: today,
            onDismiss = { pickingNewDate = false },
            onDate = { newDueEpoch = it.toEpochDay(); pickingNewDate = false },
        )
    }
    tasks.firstOrNull { it.id == deleteId }?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { RoutineText(stringResource(R.string.tasks_delete_title), style = MaterialTheme.typography.headlineSmall) },
            text = { RoutineText(stringResource(R.string.tasks_delete_body, target.title), maxLines = RoutineTextDefaults.Paragraph) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteTask(target.id)); deleteId = null }) {
                    RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) {
                    RoutineLabel(stringResource(R.string.keep), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

/**
 * Due date + subject pickers, used unchanged by the quick-add row and by every expanded task row:
 * both buttons keep their full one-line label and reflow onto a second line when the sheet is narrow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskAttributePickers(
    dueEpoch: Long?,
    subjectId: Long?,
    subjects: List<Subject>,
    subjectsById: Map<Long, Subject>,
    today: LocalDate,
    busy: Boolean,
    onDueEpoch: (Long?) -> Unit,
    onSubjectId: (Long?) -> Unit,
    onPickDate: () -> Unit,
) {
    var subjectMenu by rememberSaveable { mutableStateOf(false) }
    ActionRow {
        OutlinedButton(enabled = !busy, shape = RoutineShapes.Pill, onClick = onPickDate) {
            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp))
            Spacer(Modifier.width(RoutineSpacing.sm))
            RoutineLabel(
                text = taskDueButtonLabel(dueEpoch?.let(LocalDate::ofEpochDay), today),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (dueEpoch != null) {
            IconButton(
                modifier = Modifier.size(RoutineMetrics.ActionMinWidth),
                enabled = !busy,
                onClick = { onDueEpoch(null) },
            ) {
                Icon(Icons.Outlined.Close, stringResource(R.string.tasks_clear_date), tint = RoutineColors.Error)
            }
        }
        Box {
            OutlinedButton(enabled = !busy, shape = RoutineShapes.Pill, onClick = { subjectMenu = true }) {
                RoutineLabel(
                    text = subjectId?.let(subjectsById::get)?.name ?: stringResource(R.string.tasks_subject_any),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            DropdownMenu(expanded = subjectMenu, onDismissRequest = { subjectMenu = false }) {
                DropdownMenuItem(
                    text = { RoutineText(stringResource(R.string.tasks_subject_none), maxLines = RoutineTextDefaults.Body) },
                    onClick = { onSubjectId(null); subjectMenu = false },
                )
                subjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { RoutineText(subject.name, maxLines = RoutineTextDefaults.Body) },
                        onClick = { onSubjectId(subject.id); subjectMenu = false },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.taskSection(
    key: String,
    titleRes: Int,
    color: Color,
    tasks: List<Task>,
    subjects: List<Subject>,
    subjectsById: Map<Long, Subject>,
    today: LocalDate,
    busy: Boolean,
    expandedId: Long?,
    onAction: (TimelineAction) -> Unit,
    onExpand: (Long) -> Unit,
    onRequestDelete: (Long) -> Unit,
) {
    if (tasks.isEmpty()) return
    item(key = "tasks-h-$key") {
        Box(Modifier.animateItem(placementSpec = taskListSpec()).padding(top = RoutineSpacing.sm)) {
            TaskSectionHeader(titleRes, color)
        }
    }
    items(tasks, key = { "task:${it.id}" }) { task ->
        Box(Modifier.animateItem(placementSpec = taskListSpec())) {
            TaskRow(task, subjects, subjectsById, today, busy, expandedId == task.id,
                onToggle = { onAction(TimelineAction.ToggleTask(task.id)) },
                onExpand = { onExpand(task.id) },
                onRequestDelete = { onRequestDelete(task.id) }, onAction = onAction)
        }
    }
}

@Composable
private fun TaskSectionHeader(res: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        RoutineText(stringResource(res), style = MaterialTheme.typography.titleMedium, maxLines = RoutineTextDefaults.Body)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskRow(
    task: Task,
    subjects: List<Subject>,
    subjectsById: Map<Long, Subject>,
    today: LocalDate,
    busy: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onRequestDelete: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    var editTitle by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var editDueEpoch by rememberSaveable(task.id) { mutableStateOf(task.dueDate?.toEpochDay()) }
    var editSubject by rememberSaveable(task.id) { mutableStateOf(task.subjectId) }
    var editNote by rememberSaveable(task.id) { mutableStateOf(task.note.orEmpty()) }
    var pickingEditDate by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, spatialSpec<Float>(LocalReduceMotion.current), label = "task-chevron")
    OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = RoutineSpacing.md, vertical = RoutineSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { haptics.tap(); onExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
            ) {
                RoutineCompletionCheckbox(
                    checked = task.completedAtEpochMillis != null,
                    onCheckedChange = { if (!busy) onToggle() },
                    enabled = !busy,
                    modifier = Modifier.size(RoutineMetrics.ActionMinWidth),
                )
                task.subjectId?.let(subjectsById::get)?.let { subject ->
                    Box(Modifier.size(RoutineSpacing.sm).clip(CircleShape).background(Color(subject.colorHex.toInt())))
                }
                RoutineText(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = RoutineTextDefaults.Body,
                )
                task.dueDate?.let { date ->
                    RoutineLabel(
                        text = taskDueLabel(date, today),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (task.completedAtEpochMillis == null && date.isBefore(today)) RoutineColors.Error
                        else RoutineColors.TextSecondary,
                    )
                }
                Icon(Icons.Outlined.ExpandMore, null, tint = RoutineColors.TextSecondary,
                    modifier = Modifier.size(20.dp).rotate(chevron))
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(TransitionMillis)) + expandVertically(tween(TransitionMillis)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it.take(120) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !busy,
                        label = { RoutineText(stringResource(R.string.tasks_edit_title)) },
                    )
                    TaskAttributePickers(
                        dueEpoch = editDueEpoch,
                        subjectId = editSubject,
                        subjects = subjects,
                        subjectsById = subjectsById,
                        today = today,
                        busy = busy,
                        onDueEpoch = { editDueEpoch = it },
                        onSubjectId = { editSubject = it },
                        onPickDate = { pickingEditDate = true },
                    )
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it.take(2000) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        minLines = 2,
                        label = { RoutineText(stringResource(R.string.tasks_note)) },
                        placeholder = { RoutineText(stringResource(R.string.tasks_note_hint)) },
                    )
                    ActionRow {
                        TextButton(
                            enabled = !busy && editTitle.isNotBlank(),
                            onClick = {
                                onAction(
                                    TimelineAction.UpdateTask(
                                        task.copy(
                                            title = editTitle.trim(),
                                            dueDate = editDueEpoch?.let(LocalDate::ofEpochDay),
                                            subjectId = editSubject,
                                            note = editNote.trim().takeIf { it.isNotEmpty() },
                                        ),
                                    ),
                                )
                            },
                        ) { RoutineLabel(stringResource(R.string.save), style = MaterialTheme.typography.labelLarge) }
                        TextButton(enabled = !busy, onClick = { onAction(TimelineAction.TaskToSchedule(task)) }) {
                            RoutineLabel(stringResource(R.string.tasks_schedule), style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(enabled = !busy, onClick = { haptics.warning(); onRequestDelete() }) {
                            RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge,
                                color = RoutineColors.Error)
                        }
                    }
                }
            }
        }
    }
    if (pickingEditDate) {
        AppDatePicker(
            editDueEpoch?.let(LocalDate::ofEpochDay) ?: today,
            onDismiss = { pickingEditDate = false },
            onDate = { editDueEpoch = it.toEpochDay(); pickingEditDate = false },
        )
    }
}

/**
 * Re-ordering a task list is the one animation the reader causes by tapping, so it is a spring that
 * can be retargeted mid-flight — and it disappears entirely under remove-animations, where the row
 * simply lands in its new place.
 */
@Composable
private fun taskListSpec() = spatialSpec<androidx.compose.ui.unit.IntOffset>(LocalReduceMotion.current)

@Composable
private fun taskDueButtonLabel(date: LocalDate?, today: LocalDate): String = when {
    date == null -> stringResource(R.string.tasks_no_due)
    date.isAfter(today.plusDays(7)) || date.isBefore(today) -> RoutineDate.normal(date)
    else -> taskDueLabel(date, today)
}

@Composable
private fun taskDueLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> stringResource(R.string.tasks_today_short)
    date == today.plusDays(1) -> stringResource(R.string.tasks_tomorrow)
    date.isAfter(today) -> ChronoUnit.DAYS.between(today, date).toInt().let { pluralStringResource(R.plurals.tasks_in_days, it, it) }
    else -> ChronoUnit.DAYS.between(date, today).toInt().let { pluralStringResource(R.plurals.tasks_ago_days, it, it) }
}
