package com.example.mydailyroutine.app.presentation

import com.example.mydailyroutine.core.presentation.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import com.example.mydailyroutine.features.timeline.components.DateNavigator
import com.example.mydailyroutine.features.entry.presentation.*
import com.example.mydailyroutine.features.subjects.presentation.SubjectEditorDialog
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheet
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.haptics.*
import com.example.mydailyroutine.features.timeline.presentation.overview.*
import com.example.mydailyroutine.features.timeline.presentation.DailyTimeline
import com.example.mydailyroutine.features.planning.presentation.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.Role
import com.example.mydailyroutine.core.designsystem.glass.GlassRole
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.effectSpec
import com.example.mydailyroutine.core.designsystem.motion.rememberReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.spatialSpec
import com.example.mydailyroutine.core.designsystem.glass.LocalRoutineBackdrop
import com.example.mydailyroutine.core.designsystem.glass.RoutineAmbientBackground
import com.example.mydailyroutine.core.designsystem.glass.RoutineBackdropProvider
import com.example.mydailyroutine.core.designsystem.glass.RoutineGlassSurface
import com.example.mydailyroutine.core.designsystem.glass.routineBackdropLayer
import com.example.mydailyroutine.core.designsystem.glass.routineGlass
import com.example.mydailyroutine.features.tasks.presentation.TasksSheet
import com.example.mydailyroutine.features.goals.presentation.GoalsScreen
import com.example.mydailyroutine.features.settings.presentation.NotificationAccess
import com.example.mydailyroutine.features.settings.presentation.SettingsSheet
import com.example.mydailyroutine.core.designsystem.theme.*
import java.time.ZonedDateTime
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
        if (action !is TimelineAction.ToggleComplete && action !is TimelineAction.SyncExecution && action !is TimelineAction.ToggleTask &&
            action !is TimelineAction.ToggleGoalMilestone && action !is TimelineAction.ToggleGoalActivity) haptics.tap()
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
    val overdueTasks = state.planning.tasks.count { task -> val due = task.dueDate; task.completedAtEpochMillis == null && due != null && due.isBefore(now.toLocalDate()) }
    val reduceMotion = rememberReduceMotion()
    CompositionLocalProvider(LocalRoutineHaptics provides haptics, LocalHapticFeedback provides gatedHaptics,
        LocalReduceMotion provides reduceMotion) {
      // One backdrop for the window: the content layer records into it and the floating chrome —
      // the top bar, the fast-add control — refracts it. They have to stay siblings of the layer,
      // never inside it, or a panel draws itself into itself.
      RoutineBackdropProvider {
        val backdrop = LocalRoutineBackdrop.current
        val density = LocalDensity.current
        // Measured, never assumed: the bar is three rows on the day view and one on goals.
        var topInset by remember { mutableStateOf(0.dp) }
        // The bar folds while the reader scrolls and unfolds the moment they scroll back up, which
        // is what gives a content screen its room: ~48 dp of date chrome only when it is being used.
        var collapsed by remember { mutableStateOf(false) }
        val headerScroll = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -1f) collapsed = true
                    else if (available.y > 1f) collapsed = false
                    return Offset.Zero
                }
            }
        }
        // Coming back from goals, or changing scale, always lands on an open bar: the reader just
        // chose something, and the controls they chose it with should still be on screen.
        LaunchedEffect(data.mode, state.panels.showGoals) { collapsed = false }
        // Full-bleed stack instead of a Scaffold: a Scaffold body starts below its top bar, which
        // would leave nothing for the glass to refract. Here the content fills the window and the
        // floating chrome sits on top of it — as siblings of the layer, never inside it, or a panel
        // would draw itself into itself.
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().nestedScroll(headerScroll).routineBackdropLayer(backdrop)) {
                RoutineAmbientBackground(Modifier.fillMaxSize())
                AnimatedContent(targetState = state.panels.showGoals, label = "goals-switch",
                    modifier = Modifier.fillMaxSize(), transitionSpec = {
                        (slideInVertically(spatialSpec(reduceMotion)) { it / 6 } + fadeIn(effectSpec(reduceMotion))) togetherWith
                            (slideOutVertically(spatialSpec(reduceMotion)) { -it / 6 } + fadeOut(effectSpec(reduceMotion)))
                    }) { goalsShown ->
                    if (goalsShown) GoalsScreen(state.goals, state.panels.isSaving, onAction, topInset = topInset)
                    else AnimatedContent(targetState = data, contentKey = { it.date to it.mode }, label = "period-switch",
                        modifier = Modifier.fillMaxSize(), transitionSpec = {
                            // Moving through time slides along a shared horizontal axis in the
                            // direction of travel; changing scale (day -> week) shares no geometry
                            // with what it replaces, so it cross-fades instead of pretending to slide.
                            val spatial = spatialSpec<IntOffset>(reduceMotion)
                            val effect = effectSpec<Float>(reduceMotion)
                            if (targetState.mode == initialState.mode) {
                                val forward = if (targetState.date >= initialState.date) 1 else -1
                                (slideInHorizontally(spatial) { it / 4 * forward } + fadeIn(effect)) togetherWith
                                    (slideOutHorizontally(spatial) { -it / 4 * forward } + fadeOut(effect))
                            } else {
                                fadeIn(effect) togetherWith fadeOut(effect)
                            }
                        }) { shown ->
                        Box(Modifier.fillMaxSize()) {
                            when {
                                shown.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                                shown.error != null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    RoutineText(stringResource(shown.error), style = MaterialTheme.typography.bodyLarge,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = RoutineTextDefaults.Paragraph)
                                    TextButton(onClick = { onAction(TimelineAction.Retry) }) {
                                        RoutineLabel(stringResource(R.string.retry), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                else -> when (shown.mode) {
                                    TimelineMode.DAY -> shown.days[shown.date]?.let { day -> DailyTimeline(day, now, state.panels.isSaving, state.preferences.health, state.preferences.planning, state.planning.backlog.size, state.execution,
                                        state.planning.tasks.filter { task -> val due = task.dueDate; task.completedAtEpochMillis == null && due != null && (due == day.date || (day.date == now.toLocalDate() && due.isBefore(now.toLocalDate()))) }, onAction,
                                        topInset = topInset) }
                                    TimelineMode.WEEK -> WeeklyOverview(shown, onGoals = { onAction(TimelineAction.OpenGoals) }, topInset = topInset) { onAction(TimelineAction.SelectDate(it, true)) }
                                    TimelineMode.MONTH -> MonthlyOverview(shown, now.toLocalDate(), onGoals = { onAction(TimelineAction.OpenGoals) }, topInset = topInset) { onAction(TimelineAction.SelectDate(it, true)) }
                                    TimelineMode.YEAR -> YearlyOverview(shown, state.preferences, now.toLocalDate(), onGoals = { onAction(TimelineAction.OpenGoals) }, topInset = topInset) { onAction(TimelineAction.SelectDate(it, true)) }
                                }
                            }
                        }
                    }
                }
                }
            RoutineGlassSurface(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().testTag("app-top-bar")
                    // Only the expanded bar sets the inset. Content clears the *tall* bar, so when it
                    // folds mid-scroll the lists keep their padding and nothing jumps; scrolled items
                    // simply travel up through the space the folded bar no longer covers, under glass.
                    .onSizeChanged { height -> if (!collapsed) topInset = with(density) { height.toDp() } },
                shape = RoutineShapes.GlassTopBar,
                role = GlassRole.Bar,
            ) {
                Column {
                    TopAppBar(
                        title = {
                            if (state.panels.showGoals) {
                                RoutineLabel(stringResource(R.string.goals_title), style = MaterialTheme.typography.titleLarge)
                            } else {
                                // Expanded: the app. Folded: the period being read, because that is
                                // the only thing in the bar still worth the space.
                                AnimatedContent(targetState = collapsed, label = "header-title",
                                    transitionSpec = { ContentTransform(fadeIn(effectSpec(reduceMotion)), fadeOut(effectSpec(reduceMotion)), sizeTransform = SizeTransform(clip = false)) }) { isCollapsed ->
                                    RoutineLabel(
                                        text = if (isCollapsed) periodTitle(data) else stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (state.panels.showGoals) {
                                IconButton(onClick = { onAction(TimelineAction.CloseGoals) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.tasks_back))
                                }
                            }
                        },
                        actions = {
                            if (!state.panels.showGoals) {
                                IconButton(onClick = { onAction(TimelineAction.OpenPlanning) }) { Icon(Icons.Outlined.AutoAwesome, stringResource(R.string.planning_open)) }
                                Box {
                                    IconButton(onClick = { onAction(TimelineAction.OpenTasks) }) { Icon(Icons.Outlined.Checklist, stringResource(R.string.tasks_open)) }
                                    AnimatedVisibility(visible = overdueTasks > 0, modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                                        enter = scaleIn(if (reduceMotion) snap() else PopSpring, initialScale = if (reduceMotion) 1f else 0.4f) + fadeIn(effectSpec(reduceMotion, 120)),
                                        exit = fadeOut(effectSpec(reduceMotion, 120))) {
                                        Box(Modifier.size(12.dp).padding(2.dp).clip(CircleShape).background(RoutineColors.Crimson))
                                    }
                                }
                                IconButton(onClick = { onAction(TimelineAction.OpenGoals) }) { Icon(Icons.Outlined.Flag, stringResource(R.string.goals_open)) }
                                IconButton(onClick = { onAction(TimelineAction.OpenSettings) }) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                    // The marketing line does not earn 18 dp of every screen forever: it moved to the
                    // empty-day state, where there is room and it reads as an invitation.
                    AnimatedVisibility(
                        visible = !state.panels.showGoals && !collapsed,
                        enter = expandVertically(spatialSpec(reduceMotion)) + fadeIn(effectSpec(reduceMotion)),
                        exit = shrinkVertically(spatialSpec(reduceMotion)) + fadeOut(effectSpec(reduceMotion)),
                    ) {
                        DateNavigator(periodTitle(data), onPrevious = { onAction(TimelineAction.Shift(-1)) }, onNext = { onAction(TimelineAction.Shift(1)) },
                            onToday = { onAction(TimelineAction.Today) }, onPick = { haptics.tap(); choosingDate = true })
                    }
                    AnimatedVisibility(
                        visible = !state.panels.showGoals,
                        enter = expandVertically(spatialSpec(reduceMotion)) + fadeIn(effectSpec(reduceMotion)),
                        exit = shrinkVertically(spatialSpec(reduceMotion)) + fadeOut(effectSpec(reduceMotion)),
                    ) {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                            TimelineMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(selected = data.mode == mode, onClick = { onAction(TimelineAction.SelectMode(mode)) }, shape = SegmentedButtonDefaults.itemShape(index, TimelineMode.entries.size)) {
                                    RoutineLabel(
                                        text = stringResource(when (mode) { TimelineMode.DAY -> R.string.nav_day; TimelineMode.WEEK -> R.string.nav_week; TimelineMode.MONTH -> R.string.nav_month; TimelineMode.YEAR -> R.string.nav_year }),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Amber glass: hue-blended, so the refracted content keeps its shading under the accent.
            if (!state.panels.showGoals) Box(
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(RoutineSpacing.lg)
                    .height(56.dp).testTag("fast-add")
                    .routineGlass(backdrop, RoutineShapes.Pill, GlassRole.Control, RoutineColors.Amber, hue = true)
                    .clip(RoutineShapes.Pill)
                    .clickable(role = Role.Button) { onAction(TimelineAction.OpenAdd) }
                    .padding(horizontal = RoutineSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                    Icon(Icons.Default.Add, null, tint = RoutineColors.Background)
                    RoutineLabel(stringResource(R.string.add_block), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Background)
                }
            }
            // The Scaffold used to keep the snackbar clear of the button; in a plain stack that has to
            // be done by hand, or the two overlap at the bottom of the screen.
            SnackbarHost(
                snackbars,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(bottom = if (state.panels.showGoals) RoutineSpacing.md else RoutineMetrics.FabClearance),
            )
        }
        if (choosingDate) AppDatePicker(data.date, onDismiss = { choosingDate = false }, onDate = { onAction(TimelineAction.SelectDate(it)); choosingDate = false })
        RoutineSheet(state.panels.showAdd) { sheetState -> key(state.panels.addSession) {
            EntryEditorSheet(data.date, data.subjects, data.subjectPresets, state.planning.history, state.panels.editingMilestone, state.panels.isSaving, sheetState,
                onDismiss = { onAction(TimelineAction.CloseAdd) }, onSave = { onAction(TimelineAction.SaveEntry(it)) }, onNewSubject = { onAction(TimelineAction.EditSubject()) }, onEditSubject = { subject -> onAction(TimelineAction.EditSubject(subject)) }, defaults = state.preferences.entryDefaults, continuation = state.panels.entryContinuation, prefillTitle = state.panels.entryPrefillTitle)
        } }
        RoutineSheet(state.panels.showSettings) { sheetState -> SettingsSheet(state.preferences, data.subjects, state.panels.isSaving, access, state.exampleLoaded, state.sleep, onAction = onAction,
            exportJson = state.panels.exportJson,
            onDismiss = { onAction(TimelineAction.CloseSettings) }, requestNotifications = requestNotifications, requestExactAlarms = requestExactAlarms, openNotificationSettings = openNotificationSettings, sheetState = sheetState) }
        RoutineSheet(state.panels.showPlanning) { sheetState -> PlanningSheet(state, onAction, sheetState = sheetState) }
        RoutineSheet(state.panels.showTasks) { sheetState -> TasksSheet(state.planning.tasks, data.subjects, state.panels.isSaving, sheetState, state.panels.sharedTaskTitle, state.panels.sharedTaskDue, onAction) }
        RoutineSheet(state.panels.showTimetableImport) { sheetState -> TimetableImportSheet(data.subjects, state.panels.isSaving, sheetState, onDismiss = { onAction(TimelineAction.CloseTimetableImport) }, onImport = { rows -> onAction(TimelineAction.ImportTimetable(rows)) }) }
        RoutineSheet(state.panels.showTopicEditor) { sheetState -> TopicEditorSheet(state, onAction, sheetState = sheetState) }
        state.panels.completionTarget?.let { ActualCompletionDialog(it, state.panels.isSaving, onAction) }
        state.panels.editingBlock?.let { BlockEditorSheet(it, state.panels.isSaving, onDismiss = { onAction(TimelineAction.CloseEditor) }, onSave = onAction) }
        state.panels.editingSubject?.let { editing -> SubjectEditorDialog(editing, state.panels.isSaving, onDismiss = { onAction(TimelineAction.CloseSubjectEditor) },
            onSave = { subject -> onAction(TimelineAction.SaveSubject(subject)) },
            onDelete = if (editing.id == 0L) null else { { onAction(TimelineAction.DeleteSubject(editing.id)); onAction(TimelineAction.CloseSubjectEditor) } }) }
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
                    RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Crimson)
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
                RoutineLabel(stringResource(R.string.execution_cancel_confirm), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Crimson)
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
    }
}

/** Header text for the period being shown. Dates come from [RoutineDate] and never from an inline
 *  formatter, so the same period reads the same everywhere in the app. */
@Composable
private fun periodTitle(data: TimelineContent): String = when (data.mode) {
    TimelineMode.DAY -> RoutineDate.withWeekday(data.date)
    TimelineMode.WEEK -> PeriodRanges.range(data.date, data.mode).let { (first, last) -> RoutineDate.range(first, last) }
    TimelineMode.MONTH -> RoutineDate.monthAndYear(data.date)
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
