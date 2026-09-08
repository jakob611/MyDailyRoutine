package com.example.mydailyroutine.app.presentation

import com.example.mydailyroutine.core.presentation.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.core.platform.Slovenian
import com.example.mydailyroutine.features.timeline.components.DateNavigator
import com.example.mydailyroutine.features.entry.presentation.*
import com.example.mydailyroutine.features.subjects.presentation.SubjectEditorDialog
import com.example.mydailyroutine.core.designsystem.haptics.*
import com.example.mydailyroutine.features.timeline.presentation.overview.*
import com.example.mydailyroutine.features.timeline.presentation.DailyTimeline
import com.example.mydailyroutine.features.planning.presentation.*
import androidx.compose.material.icons.outlined.AutoAwesome
import com.example.mydailyroutine.features.settings.presentation.NotificationAccess
import com.example.mydailyroutine.features.settings.presentation.SettingsSheet
import com.example.mydailyroutine.core.designsystem.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineApp(viewModel: RoutineViewModel, access: NotificationAccess,
    requestNotifications: () -> Unit, requestExactAlarms: () -> Unit, openNotificationSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.content
    val context = LocalContext.current
    val haptics = rememberRoutineHaptics(state.preferences.hapticsEnabled)
    val nativeHaptics = LocalHapticFeedback.current
    val gatedHaptics = remember(state.preferences.hapticsEnabled, nativeHaptics) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (state.preferences.hapticsEnabled) nativeHaptics.performHapticFeedback(hapticFeedbackType)
            }
        }
    }
    val onAction: (TimelineAction) -> Unit = remember(viewModel, haptics) { { action ->
        if (action !is TimelineAction.ToggleComplete && action !is TimelineAction.SyncExecution) haptics.tap()
        viewModel.onAction(action)
    } }
    val snackbars = remember { SnackbarHostState() }
    val now by minuteClock()
    LaunchedEffect(now, state.execution?.startedAt) {
        if (state.execution != null) viewModel.onAction(TimelineAction.SyncExecution)
    }
    var choosingDate by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel, haptics, context) {
        viewModel.effects.collect { effect -> when (effect) {
            TimelineEffect.Completed -> haptics.complete()
            is TimelineEffect.Message -> snackbars.showSnackbar(if (effect.count != null) context.getString(effect.resource, effect.minutes, effect.count) else if (effect.minutes == null) context.getString(effect.resource) else context.getString(effect.resource, effect.minutes))
        } }
    }
    val warningKeys = data.days[data.date]?.warnings.orEmpty().map { "${it.type}:${it.itemKeys}:${it.atMinute}" }.toSet()
    var previousWarnings by remember(data.date) { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(warningKeys, data.isLoading) {
        if (!data.isLoading) {
            if (previousWarnings != null && (warningKeys - previousWarnings.orEmpty()).isNotEmpty()) haptics.warning()
            previousWarnings = warningKeys
        }
    }
    CompositionLocalProvider(LocalRoutineHaptics provides haptics, LocalHapticFeedback provides gatedHaptics) {
        Scaffold(containerColor = RoutineColors.Background,
            topBar = {
                Column {
                    TopAppBar(title = { Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary)
                    } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = RoutineColors.Background),
                        actions = {
                            IconButton(onClick = { onAction(TimelineAction.OpenPlanning) }) { Icon(Icons.Outlined.AutoAwesome, stringResource(R.string.planning_open)) }
                            IconButton(onClick = { onAction(TimelineAction.OpenSettings) }) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) } })
                    DateNavigator(periodTitle(data), onPrevious = { onAction(TimelineAction.Shift(-1)) }, onNext = { onAction(TimelineAction.Shift(1)) },
                        onToday = { onAction(TimelineAction.Today) }, onPick = { haptics.tap(); choosingDate = true })
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                        TimelineMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(selected = data.mode == mode, onClick = { onAction(TimelineAction.SelectMode(mode)) }, shape = SegmentedButtonDefaults.itemShape(index, TimelineMode.entries.size)) {
                                Text(stringResource(when (mode) { TimelineMode.DAY -> R.string.nav_day; TimelineMode.WEEK -> R.string.nav_week; TimelineMode.MONTH -> R.string.nav_month; TimelineMode.YEAR -> R.string.nav_year }))
                            }
                        }
                    }
                }
            }, snackbarHost = { SnackbarHost(snackbars) },
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = { onAction(TimelineAction.OpenAdd) }, modifier = Modifier.testTag("fast-add"), shape = RoutineShapes.Pill,
                    containerColor = RoutineColors.Amber, contentColor = RoutineColors.Background,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp),
                    icon = { Icon(Icons.Default.Add, null) }, text = { Text(stringResource(R.string.add_block)) })
            },
        ) { padding ->
            AnimatedContent(targetState = data, contentKey = { it.date to it.mode }, label = "period-switch",
                modifier = Modifier.fillMaxSize().padding(padding), transitionSpec = {
                    (slideInHorizontally(tween(TransitionMillis)) { it / 4 } + fadeIn(tween(TransitionMillis))) togetherWith
                        (slideOutHorizontally(tween(TransitionMillis)) { -it / 4 } + fadeOut(tween(TransitionMillis)))
                }) { shown ->
                Box(Modifier.fillMaxSize()) {
                    when {
                        shown.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        shown.error != null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(shown.error))
                            TextButton(onClick = { onAction(TimelineAction.Retry) }) { Text(stringResource(R.string.retry)) }
                        }
                        else -> when (shown.mode) {
                            TimelineMode.DAY -> shown.days[shown.date]?.let { DailyTimeline(it, now, state.panels.isSaving, state.preferences.health, state.preferences.planning, state.planning.backlog.size, state.execution, onAction) }
                            TimelineMode.WEEK -> WeeklyOverview(shown) { onAction(TimelineAction.SelectDate(it, true)) }
                            TimelineMode.MONTH -> MonthlyOverview(shown, now.toLocalDate()) { onAction(TimelineAction.SelectDate(it, true)) }
                            TimelineMode.YEAR -> YearlyOverview(shown, state.preferences, now.toLocalDate()) { onAction(TimelineAction.SelectDate(it, true)) }
                        }
                    }
                }
            }
        }
        if (choosingDate) AppDatePicker(data.date, onDismiss = { choosingDate = false }, onDate = { onAction(TimelineAction.SelectDate(it)); choosingDate = false })
        if (state.panels.showAdd) key(state.panels.addSession) {
            EntryEditorSheet(data.date, data.subjects, data.subjectPresets, state.planning.history, state.panels.editingMilestone, state.panels.isSaving,
                onDismiss = { onAction(TimelineAction.CloseAdd) }, onSave = { onAction(TimelineAction.SaveEntry(it)) }, onNewSubject = { onAction(TimelineAction.EditSubject()) }, defaults = state.preferences.entryDefaults, continuation = state.panels.entryContinuation)
        }
        if (state.panels.showSettings) SettingsSheet(state.preferences, data.subjects, state.panels.isSaving, access, state.exampleLoaded, state.sleep, onAction,
            onDismiss = { onAction(TimelineAction.CloseSettings) }, requestNotifications = requestNotifications, requestExactAlarms = requestExactAlarms, openNotificationSettings = openNotificationSettings)
        if (state.panels.showPlanning) PlanningSheet(state, onAction)
        if (state.panels.showTopicEditor) TopicEditorSheet(state, onAction)
        state.panels.completionTarget?.let { ActualCompletionDialog(it, state.panels.isSaving, onAction) }
        state.panels.editingBlock?.let { BlockEditorDialog(it, state.panels.isSaving, onDismiss = { onAction(TimelineAction.CloseEditor) }, onSave = onAction) }
        state.panels.editingSubject?.let { SubjectEditorDialog(it, state.panels.isSaving, onDismiss = { onAction(TimelineAction.CloseSubjectEditor) }, onSave = { subject -> onAction(TimelineAction.SaveSubject(subject)) }) }
        state.panels.pendingDelete?.let { item ->
            val recurring = item is ResolvedTimelineItem.Block && !item.isOneOff
            val group = (item as? ResolvedTimelineItem.Block)?.takeIf { it.parentRoutineId == null && it.origin == com.example.mydailyroutine.domain.routines.RoutineOrigin.USER }?.seriesKey
            val groupDays = (item as? ResolvedTimelineItem.Block)?.seriesDays.orEmpty()
            var entireSeries by remember(item.key) { mutableStateOf(true) }
            AlertDialog(onDismissRequest = { onAction(TimelineAction.DismissDelete) }, title = { Text(stringResource(if (recurring) R.string.delete_routine_title else R.string.delete_entry_title)) },
                text = { Column {
                    Text(stringResource(if (recurring) R.string.delete_routine_body else R.string.delete_entry_body, item.title))
                    if (group != null && groupDays.size > 1) Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(entireSeries,{ entireSeries=it })
                        Text(stringResource(R.string.delete_all_repeat_days))
                    }
                } },
                confirmButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(if (group != null && entireSeries) TimelineAction.DeleteSeries(group) else TimelineAction.ConfirmDelete) }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) } },
                dismissButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.DismissDelete) }) { Text(stringResource(R.string.keep)) } })
        }
        if (state.panels.confirmCancelExecution) AlertDialog(onDismissRequest = { onAction(TimelineAction.DismissCancelExecution) },
            title = { Text(stringResource(R.string.execution_cancel_title)) }, text = { Text(stringResource(R.string.execution_cancel_body)) },
            confirmButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.CancelExecution) }) { Text(stringResource(R.string.execution_cancel_confirm)) } },
            dismissButton = { TextButton(onClick = { onAction(TimelineAction.DismissCancelExecution) }) { Text(stringResource(R.string.keep)) } })
        if (state.panels.confirmDemo) AlertDialog(onDismissRequest = { onAction(TimelineAction.DismissDemo) },
            title = { Text(stringResource(R.string.demo_confirm_title)) }, text = { Text(stringResource(R.string.demo_confirm_body)) },
            confirmButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.LoadDemo) }) { Text(stringResource(R.string.demo_confirm)) } },
            dismissButton = { TextButton(enabled = !state.panels.isSaving, onClick = { onAction(TimelineAction.DismissDemo) }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun periodTitle(data: TimelineContent): String = when (data.mode) {
    TimelineMode.DAY -> data.date.format(DateTimeFormatter.ofPattern("EEE, d. MMM", Slovenian))
    TimelineMode.WEEK -> PeriodRanges.range(data.date, data.mode).let { (first, last) -> stringResource(R.string.date_range,
        first.format(DateTimeFormatter.ofPattern("d. MMM", Slovenian)), last.format(DateTimeFormatter.ofPattern("d. MMM", Slovenian))) }
    TimelineMode.MONTH -> data.date.format(DateTimeFormatter.ofPattern("LLLL yyyy", Slovenian))
    TimelineMode.YEAR -> PeriodRanges.range(data.date, data.mode).first.year.let { stringResource(R.string.academic_year, it, it + 1) }
}

/** UI-only clock: no polling service, background timer, or app-owned wakelock. */
@Composable
private fun minuteClock(): State<ZonedDateTime> {
    val owner = LocalLifecycleOwner.current
    return produceState(initialValue = ZonedDateTime.now(), owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = ZonedDateTime.now(); delay(60_000L - System.currentTimeMillis() % 60_000L) }
        }
    }
}
