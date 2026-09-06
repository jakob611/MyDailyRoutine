package com.example.mydailyroutine.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.ui.editor.*
import com.example.mydailyroutine.ui.overview.*
import com.example.mydailyroutine.ui.settings.NotificationAccess
import com.example.mydailyroutine.ui.settings.SettingsSheet
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel, access: NotificationAccess,
    requestNotifications: () -> Unit, requestExactAlarms: () -> Unit, openNotificationSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.content
    val onAction = viewModel::onAction
    val snackbars = remember { SnackbarHostState() }
    val now by minuteClock()
    var choosingDate by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(viewModel) { viewModel.effects.collect { snackbars.showSnackbar(it) } }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Column {
                        Text("My Daily Routine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("YOUR TIME. A LITTLE MORE INTENTION.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } },
                    actions = { IconButton(onClick = { onAction(TimelineAction.OpenSettings) }) { Icon(Icons.Outlined.Settings, "Settings") } },
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onAction(TimelineAction.Shift(-1)) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous ${data.mode.name.lowercase()}") }
                    Text(periodTitle(data), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onAction(TimelineAction.Today) }) { Text("Today") }
                    IconButton(onClick = { choosingDate = true }) { Icon(Icons.Outlined.CalendarMonth, "Choose date") }
                    IconButton(onClick = { onAction(TimelineAction.Shift(1)) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next ${data.mode.name.lowercase()}") }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                    TimelineMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(selected = data.mode == mode, onClick = { onAction(TimelineAction.SelectMode(mode)) },
                            shape = SegmentedButtonDefaults.itemShape(index, TimelineMode.entries.size)) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbars) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAction(TimelineAction.OpenAdd)
            }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add block") })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                data.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                data.error != null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(data.error)
                    TextButton(onClick = { onAction(TimelineAction.Retry) }) { Text("Try again") }
                }
                else -> when (data.mode) {
                    TimelineMode.DAY -> data.days[data.date]?.let { DailyTimeline(it, now, state.panels.isSaving, onAction) }
                    TimelineMode.WEEK -> WeeklyOverview(data) { onAction(TimelineAction.SelectDate(it, openDay = true)) }
                    TimelineMode.MONTH -> MonthlyOverview(data, now.toLocalDate()) { onAction(TimelineAction.SelectDate(it, openDay = true)) }
                    TimelineMode.YEAR -> YearlyOverview(data, state.preferences, now.toLocalDate()) { onAction(TimelineAction.SelectDate(it, openDay = true)) }
                }
            }
        }
    }
    if (choosingDate) AppDatePicker(data.date, onDismiss = { choosingDate = false }, onDate = {
        onAction(TimelineAction.SelectDate(it)); choosingDate = false
    })
    if (state.panels.showAdd) EntryEditorSheet(data.date, data.subjects, state.panels.editingMilestone, state.panels.isSaving,
        onDismiss = { onAction(TimelineAction.CloseAdd) }, onSave = { onAction(TimelineAction.SaveEntry(it)) },
        onNewSubject = { onAction(TimelineAction.EditSubject()) })
    if (state.panels.showSettings) SettingsSheet(state.preferences, data.subjects, state.panels.isSaving, access, onAction,
        onDismiss = { onAction(TimelineAction.CloseSettings) }, requestNotifications = requestNotifications,
        requestExactAlarms = requestExactAlarms, openNotificationSettings = openNotificationSettings)
    state.panels.editingBlock?.let { block ->
        BlockEditorDialog(block, state.panels.isSaving, onDismiss = { onAction(TimelineAction.CloseEditor) }, onSave = onAction)
    }
    state.panels.editingSubject?.let { subject ->
        SubjectEditorDialog(subject, state.panels.isSaving, onDismiss = { onAction(TimelineAction.CloseSubjectEditor) }, onSave = { onAction(TimelineAction.SaveSubject(it)) })
    }
    state.panels.pendingDelete?.let { item ->
        val recurring = item is ResolvedTimelineItem.Block && !item.isOneOff
        AlertDialog(onDismissRequest = { onAction(TimelineAction.DismissDelete) }, title = { Text(if (recurring) "Delete entire routine?" else "Delete this entry?") },
            text = { Text(if (recurring) "This deletes “${item.title}” in every week, including its exceptions and completion history. To remove just one session, choose Skip this date instead."
                else "“${item.title}” will be permanently deleted from this device.") },
            confirmButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.ConfirmDelete) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.DismissDelete) }) { Text("Keep it") } })
    }
}

private fun periodTitle(data: TimelineContent): String = when (data.mode) {
    TimelineMode.DAY -> data.date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    TimelineMode.WEEK -> TimelineViewModel.range(data.date, data.mode).let { (first, last) ->
        "${first.format(DateTimeFormatter.ofPattern("d MMM"))} – ${last.format(DateTimeFormatter.ofPattern("d MMM"))}"
    }
    TimelineMode.MONTH -> data.date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    TimelineMode.YEAR -> TimelineViewModel.range(data.date, data.mode).first.year.let { "$it / ${it + 1}" }
}

/** UI-only clock: a minute tick while STARTED, no background coroutine, wakelock, or service. */
@Composable
private fun minuteClock(): State<ZonedDateTime> {
    val owner = LocalLifecycleOwner.current
    return produceState(initialValue = ZonedDateTime.now(), owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = ZonedDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }
}
