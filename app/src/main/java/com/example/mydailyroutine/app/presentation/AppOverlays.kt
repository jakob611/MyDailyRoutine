package com.example.mydailyroutine.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheet
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.haptics.*
import com.example.mydailyroutine.core.designsystem.sound.*
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.presentation.*
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.features.entry.presentation.*
import com.example.mydailyroutine.features.planning.presentation.*
import com.example.mydailyroutine.features.settings.presentation.NotificationAccess
import com.example.mydailyroutine.features.settings.presentation.SettingsSheet
import com.example.mydailyroutine.features.subjects.presentation.SubjectEditorDialog
import com.example.mydailyroutine.features.tasks.presentation.TasksSheet
import com.example.mydailyroutine.features.timeline.presentation.overview.*

/**
 * Every sheet and dialog the main screen can raise, in one place.
 *
 * They are siblings of the screen, not part of it: each one is its own window or its own dialog,
 * each is driven purely by [TimelineUiState.panels], and none of them can affect the layout
 * underneath. Keeping them here is what stops the screen's own composable from being read as a
 * seventy-line list of things that are usually not on screen at all.
 */
@Composable
fun RoutineOverlays(
    state: TimelineUiState,
    access: NotificationAccess,
    choosingDate: Boolean,
    onChoosingDate: (Boolean) -> Unit,
    onAction: (TimelineAction) -> Unit,
    requestNotifications: () -> Unit,
    requestExactAlarms: () -> Unit,
    openNotificationSettings: () -> Unit,
) {
    val data = state.content
    if (choosingDate) AppDatePicker(data.date, onDismiss = { onChoosingDate(false) }, onDate = { onAction(TimelineAction.SelectDate(it)); onChoosingDate(false) })
    RoutineSheet(state.panels.showAdd) { sheetState -> key(state.panels.addSession) {
        EntryEditorSheet(data.date, data.subjects, data.subjectPresets, state.planning.history, state.panels.editingMilestone, state.panels.isSaving, sheetState,
            onDismiss = { onAction(TimelineAction.CloseAdd) }, onSave = { onAction(TimelineAction.SaveEntry(it)) }, onNewSubject = { onAction(TimelineAction.EditSubject()) }, onEditSubject = { subject -> onAction(TimelineAction.EditSubject(subject)) }, defaults = state.preferences.entryDefaults, continuation = state.panels.entryContinuation, prefillTitle = state.panels.entryPrefillTitle,
            prefill = state.panels.entryPrefill)
    } }
    RoutineSheet(state.panels.showSettings) { sheetState -> SettingsSheet(state.preferences, data.subjects, state.panels.isSaving, access, state.exampleLoaded, state.sleep, onAction = onAction,
        exportJson = state.panels.exportJson,
        onDismiss = { onAction(TimelineAction.CloseSettings) }, requestNotifications = requestNotifications, requestExactAlarms = requestExactAlarms, openNotificationSettings = openNotificationSettings, sheetState = sheetState) }
    RoutineSheet(state.panels.showPlanning) { sheetState -> PlanningSheet(state, onAction, sheetState = sheetState) }
    RoutineSheet(state.panels.showTasks) { sheetState -> TasksSheet(state.planning.tasks, data.subjects, state.panels.isSaving, sheetState, state.panels.sharedTaskTitle, state.panels.sharedTaskDue, onAction) }
    RoutineSheet(state.panels.showTimetableImport) { sheetState -> TimetableImportSheet(data.subjects, state.panels.isSaving, sheetState, onDismiss = { onAction(TimelineAction.CloseTimetableImport) }, onImport = { rows -> onAction(TimelineAction.ImportTimetable(rows)) }) }
    RoutineSheet(state.panels.showTopicEditor) { sheetState -> TopicEditorSheet(state, onAction, sheetState = sheetState) }
    state.panels.completionTarget?.let { ActualCompletionDialog(it, state.panels.isSaving, onAction) }
    RoutineSheet(state.panels.editingBlock != null) { sheetState ->
        EntryEditorSheet(data.date, data.subjects, data.subjectPresets, state.planning.history, null,
            state.panels.isSaving, sheetState,
            onDismiss = { onAction(TimelineAction.CloseEditor) },
            onSave = { onAction(TimelineAction.SaveEntry(it)) },
            onNewSubject = { onAction(TimelineAction.EditSubject()) },
            occurrence = state.panels.editingBlock,
            onSaveBlock = { onAction(it) },
        )
    }
    state.panels.editingSubject?.let { editing -> SubjectEditorDialog(editing, state.panels.isSaving, onDismiss = { onAction(TimelineAction.CloseSubjectEditor) },
        onSave = { subject -> onAction(TimelineAction.SaveSubject(subject)) },
        onDelete = if (editing.id == 0L) null else { { onAction(TimelineAction.DeleteSubject(editing.id)); onAction(TimelineAction.CloseSubjectEditor) } },
        // A new subject takes a colour no other subject is using; see SubjectPalette.firstFree.
        takenColors = data.subjects.map { it.colorHex }) }
    state.panels.pendingDelete?.let { item ->
        val recurring = item is ResolvedTimelineItem.Block && !item.isOneOff
        val group = (item as? ResolvedTimelineItem.Block)?.takeIf { it.parentRoutineId == null && it.origin == com.example.mydailyroutine.domain.routines.RoutineOrigin.USER }?.seriesKey
        val groupDays = (item as? ResolvedTimelineItem.Block)?.seriesDays.orEmpty()
        var entireSeries by remember(item.key) { mutableStateOf(true) }
        AlertDialog(onDismissRequest = { onAction(TimelineAction.DismissDelete) },
            title = { RoutineText(stringResource(if (recurring) R.string.delete_routine_title else R.string.delete_entry_title),
                style = MaterialTheme.typography.headlineSmall, maxLines = RoutineTextDefaults.Body) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                RoutineText(stringResource(if (recurring) R.string.delete_routine_body else R.string.delete_entry_body, item.title),
                    maxLines = RoutineTextDefaults.Paragraph)
                if (group != null && groupDays.size > 1) SettingRow(
                    title = stringResource(R.string.delete_all_repeat_days),
                    control = { Checkbox(entireSeries, { entireSeries = it }, modifier = Modifier.size(RoutineMetrics.ActionMinWidth)) },
                )
            } },
            confirmButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(if (group != null && entireSeries) TimelineAction.DeleteSeries(group) else TimelineAction.ConfirmDelete) }) {
                RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Error)
            } },
            dismissButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.DismissDelete) }) {
                RoutineLabel(stringResource(R.string.keep), style = MaterialTheme.typography.labelLarge)
            } })
    }
    if (state.panels.confirmCancelExecution) AlertDialog(onDismissRequest = { onAction(TimelineAction.DismissCancelExecution) },
        title = { RoutineText(stringResource(R.string.execution_cancel_title), style = MaterialTheme.typography.headlineSmall,
            maxLines = RoutineTextDefaults.Body) },
        text = { RoutineText(stringResource(R.string.execution_cancel_body), maxLines = RoutineTextDefaults.Paragraph) },
        confirmButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.CancelExecution) }) {
            RoutineLabel(stringResource(R.string.execution_cancel_confirm), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Error)
        } },
        dismissButton = { TextButton(onClick = { onAction(TimelineAction.DismissCancelExecution) }) {
            RoutineLabel(stringResource(R.string.keep), style = MaterialTheme.typography.labelLarge)
        } })
    if (state.panels.confirmDemo) AlertDialog(onDismissRequest = { onAction(TimelineAction.DismissDemo) },
        title = { RoutineText(stringResource(R.string.demo_confirm_title), style = MaterialTheme.typography.headlineSmall,
            maxLines = RoutineTextDefaults.Body) },
        text = { RoutineText(stringResource(R.string.demo_confirm_body), maxLines = RoutineTextDefaults.Paragraph) },
        confirmButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.LoadDemo) }) {
            RoutineLabel(stringResource(R.string.demo_confirm), style = MaterialTheme.typography.labelLarge)
        } },
        dismissButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.DismissDemo) }) {
            RoutineLabel(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
        } })
}
