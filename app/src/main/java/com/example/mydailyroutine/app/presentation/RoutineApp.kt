package com.example.mydailyroutine.app.presentation

import com.example.mydailyroutine.core.presentation.*
import com.example.mydailyroutine.core.platform.Diagnostics
import com.example.mydailyroutine.core.platform.StartupTrace
import com.example.mydailyroutine.core.platform.applyLocaleToProcessDefaults

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.mydailyroutine.R
import com.example.mydailyroutine.widget.refreshAgendaWidgets
import android.app.Activity
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.features.timeline.components.DateNavigator
import com.example.mydailyroutine.features.entry.presentation.*
import com.example.mydailyroutine.features.onboarding.presentation.OnboardingScreen
import com.example.mydailyroutine.features.subjects.presentation.SubjectEditorDialog
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheet
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.swipeToShift
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.haptics.*
import com.example.mydailyroutine.core.designsystem.sound.*
import com.example.mydailyroutine.features.timeline.presentation.overview.*
import com.example.mydailyroutine.features.timeline.presentation.DailyTimeline
import com.example.mydailyroutine.features.planning.presentation.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.mydailyroutine.core.designsystem.glass.GlassRole
import com.example.mydailyroutine.core.designsystem.motion.LocalPulse
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.effectSpec
import com.example.mydailyroutine.core.designsystem.motion.rememberAppPulse
import com.example.mydailyroutine.core.designsystem.motion.rememberReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.spatialSpec
import com.example.mydailyroutine.core.designsystem.components.GlassIconButton
import com.example.mydailyroutine.core.designsystem.glass.LocalRoutineBackdrop
import com.example.mydailyroutine.core.designsystem.glass.RoutineAmbientBackground
import com.example.mydailyroutine.core.designsystem.glass.RoutineBackdropProvider
import com.example.mydailyroutine.core.designsystem.glass.RoutineGlassSurface
import com.example.mydailyroutine.core.designsystem.glass.rememberGlassTouch
import com.example.mydailyroutine.core.designsystem.glass.routineGlassTouch
import com.example.mydailyroutine.core.designsystem.glass.routineBackdropLayer
import com.example.mydailyroutine.core.designsystem.glass.LocalGlassTilt
import com.example.mydailyroutine.core.designsystem.glass.routineGlass
import com.example.mydailyroutine.features.tasks.presentation.TasksSheet
import com.example.mydailyroutine.features.goals.presentation.GoalsScreen
import com.example.mydailyroutine.features.settings.presentation.NotificationAccess
import com.example.mydailyroutine.features.settings.presentation.SettingsSheet
import com.example.mydailyroutine.core.designsystem.theme.*
import com.kyant.backdrop.Backdrop
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineApp(viewModel: RoutineViewModel, access: NotificationAccess,
    requestNotifications: () -> Unit, requestExactAlarms: () -> Unit, openNotificationSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.content
    val context = LocalContext.current
    val haptics = rememberRoutineHaptics(state.preferences.hapticsEnabled)
    val sounds = rememberRoutineSounds(state.preferences.soundEffectsEnabled)
    val nativeHaptics = LocalHapticFeedback.current
    val gatedHaptics = remember(state.preferences.hapticsEnabled, nativeHaptics) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (state.preferences.hapticsEnabled) nativeHaptics.performHapticFeedback(hapticFeedbackType)
            }
        }
    }
    val onAction: (TimelineAction) -> Unit = remember(viewModel, haptics, sounds) { { action ->
        // One haptic per action, chosen by what the action *means* instead of one tick for everything.
        // Apple's generators are semantic — selection for a discrete step, impact for a collision,
        // notification for an outcome — and that mapping is most of why an iPhone feels precise. The
        // toggles and the execution sync stay silent here because they answer through their own
        // effects (`TimelineEffect.Completed`), and a second haptic would muddy the first.
        when (action) {
            is TimelineAction.ToggleComplete, is TimelineAction.SyncExecution, is TimelineAction.ToggleTask,
            is TimelineAction.ToggleGoalMilestone, is TimelineAction.ToggleGoalActivity -> Unit
            // A discrete step under the finger: a scale, a date, a nudge through time — or the
            // language the whole interface is about to speak.
            is TimelineAction.SelectMode, is TimelineAction.SelectDate, is TimelineAction.Shift,
            is TimelineAction.Today, is TimelineAction.SetAppLanguage -> haptics.selection()
            // A switch changing side. Android has had distinct on/off haptics since API 34, as iOS has.
            is TimelineAction.SetReminder -> haptics.toggle(action.enabled)
            is TimelineAction.SetAutomaticHealing -> haptics.toggle(action.enabled)
            is TimelineAction.SetHaptics -> haptics.toggle(action.enabled)
            is TimelineAction.SetMute -> haptics.toggle(action.muted)
            // A change committed: Apple's `.success`, two light taps.
            is TimelineAction.SaveEntry, is TimelineAction.SaveBlockEdit, is TimelineAction.SaveSubject,
            is TimelineAction.SaveTopic, is TimelineAction.SaveGoalsProject, is TimelineAction.SaveGoalActivity,
            is TimelineAction.SaveGoalMilestone, is TimelineAction.AddGoalProgress, is TimelineAction.SeedGoalProject,
            is TimelineAction.AddTask, is TimelineAction.UpdateTask, is TimelineAction.AddReserve,
            is TimelineAction.PlanMilestone, is TimelineAction.RecordActual, is TimelineAction.ImportTimetable,
            is TimelineAction.ImportSchedule, is TimelineAction.Restore, is TimelineAction.InsertRecovery,
            is TimelineAction.AutoHeal, is TimelineAction.ScheduleBacklog, is TimelineAction.TaskToSchedule,
            is TimelineAction.GoalActivityToSchedule, is TimelineAction.SaveSleep, is TimelineAction.SaveEntryDefaults,
            is TimelineAction.SetPlanningConfig, is TimelineAction.SetHealthConfig, is TimelineAction.SetPeriodicBreak,
            is TimelineAction.SetSchoolWindow, is TimelineAction.SetTeachingEnd, is TimelineAction.LoadDemo,
            is TimelineAction.ExportSchedule -> haptics.confirm()
            // Something destroyed or refused: Apple's `.error`, three taps of rising strength.
            is TimelineAction.ConfirmDelete, is TimelineAction.DeleteTopic, is TimelineAction.DeleteSubject,
            is TimelineAction.DeleteTask, is TimelineAction.DeleteBacklog, is TimelineAction.DeleteGoalsProject,
            is TimelineAction.DeleteGoalActivity, is TimelineAction.DeleteGoalMilestone,
            is TimelineAction.DeleteGoalProgress, is TimelineAction.DeleteSeries,
            is TimelineAction.ClearCompletedTasks, is TimelineAction.Skip, is TimelineAction.CancelExecution ->
                haptics.reject()
            // A surface opening under the finger: a medium impact, the one Apple's interactive glass fires.
            is TimelineAction.OpenAdd, is TimelineAction.OpenPlanning, is TimelineAction.OpenSettings,
            is TimelineAction.OpenTasks, is TimelineAction.OpenGoals, is TimelineAction.ShowTimetableImport,
            is TimelineAction.Edit, is TimelineAction.EditSubject, is TimelineAction.NewTopic,
            is TimelineAction.StartExecution, is TimelineAction.RequestDelete, is TimelineAction.RequestDemo ->
                haptics.press()
            // Everything else is a light impact: closing, dismissing, retrying.
            else -> haptics.tap()
        }
        // Sound sits only on the semantic moments, in step with the matching haptic: ear and palm
        // tell the same story at the same instant. Toggles and plain taps stay silent, so a sound
        // always means an outcome or a destination, never a tick.
        when (action) {
            // „Dodaj v moj dan“: the day accepted a block — click pack 2 under the success haptic.
            is TimelineAction.SaveEntry -> sounds.addToDay()
            // The other committed changes: interface pack 2 under the success haptic.
            is TimelineAction.SaveBlockEdit, is TimelineAction.AddTask,
            is TimelineAction.UpdateTask, is TimelineAction.RecordActual,
            is TimelineAction.ImportTimetable, is TimelineAction.ImportSchedule -> sounds.confirm()
            // Something destroyed or refused: interface pack 3 under the error haptic.
            is TimelineAction.ConfirmDelete, is TimelineAction.DeleteTopic, is TimelineAction.DeleteSubject,
            is TimelineAction.DeleteTask, is TimelineAction.DeleteBacklog, is TimelineAction.DeleteGoalsProject,
            is TimelineAction.DeleteGoalActivity, is TimelineAction.DeleteGoalMilestone,
            is TimelineAction.DeleteGoalProgress, is TimelineAction.DeleteSeries,
            is TimelineAction.ClearCompletedTasks, is TimelineAction.Skip, is TimelineAction.CancelExecution ->
                sounds.reject()
            // „Dodaj blok“ and the remaining sheets: interface pack 8 under the press haptic.
            is TimelineAction.OpenAdd, is TimelineAction.ShowTimetableImport -> sounds.open()
            // The four top-bar icon buttons: click pack 6 under the press haptic.
            is TimelineAction.OpenPlanning, is TimelineAction.OpenSettings,
            is TimelineAction.OpenTasks, is TimelineAction.OpenGoals -> sounds.topAction()
            // Dan / Teden / Mesec / Leto: interface pack 1 under the selection haptic.
            is TimelineAction.SelectMode -> sounds.select()
            else -> Unit
        }
        viewModel.onAction(action)
    } }
    // First run owns the whole window: two questions asked inside a sheet the reader can swipe away
    // would be asked twice, and there is nothing behind it worth guarding — the app holds no data yet
    // and every default the flow sets is already what the app ships with. It answers with the same
    // haptics and sounds as the rest of the app, so the first tap already feels like this app.
    if (!state.preferences.onboardingDone) {
        CompositionLocalProvider(LocalRoutineHaptics provides haptics, LocalRoutineSounds provides sounds) {
            OnboardingScreen(
                userName = state.preferences.userName,
                schoolStart = state.preferences.schoolStart,
                schoolEnd = state.preferences.schoolEnd,
                appLanguage = state.preferences.appLanguage,
                onAction = onAction,
            )
        }
        return
    }
    val snackbars = remember { SnackbarHostState() }
    val now by minuteClock()
    LaunchedEffect(now, state.execution?.startedAt) {
        if (state.execution != null) viewModel.onAction(TimelineAction.SyncExecution)
    }
    var choosingDate by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel, haptics, sounds, context) {
        viewModel.effects.collect { effect -> when (effect) {
            // The hero moment: the block's completion animation, the success haptic and the
            // confirm sound land together.
            TimelineEffect.Completed -> { haptics.complete(); sounds.confirm() }
            is TimelineEffect.Message -> snackbars.showSnackbar(if (effect.count != null) context.getString(effect.resource, effect.minutes, effect.count) else if (effect.minutes == null) context.getString(effect.resource) else context.getString(effect.resource, effect.minutes))
            // A language change is not a re-composition: resources are bound to the context, so
            // the window has to be recreated to read them. The widget renders outside this window,
            // so it refreshes first — the moment the reader sees the new language, the home
            // screen and the app speak it together.
            TimelineEffect.RestartForLocale -> {
                // The process-wide defaults were set at process start, before this choice existed;
                // re-apply them so framework-facing formatting agrees with the labels after too.
                applyLocaleToProcessDefaults()
                // The refresh is best-effort on purpose: if it fails, the next scheduled widget
                // boundary retries it, and the window below still restarts in the new language.
                try { refreshAgendaWidgets(context.applicationContext) }
                catch (error: CancellationException) { throw error }
                catch (error: Exception) { Diagnostics.warn("widget refresh before locale restart", error) }
                (context as? Activity)?.recreate()
            }
        } }
    }
    // N19: the first day that is actually on screen is the end of the first-minute measurement.
    LaunchedEffect(data.isLoading, data.error, data.mode, data.date) {
        if (!data.isLoading && data.error == null) StartupTrace.firstDayDrawn()
    }
    val warningKeys = data.days[data.date]?.warnings.orEmpty().map { "${it.type}:${it.itemKeys}:${it.atMinute}" }.toSet()
    var previousWarnings by remember(data.date) { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(warningKeys, data.isLoading) {
        if (!data.isLoading) {
            if (previousWarnings != null && (warningKeys - previousWarnings.orEmpty()).isNotEmpty()) haptics.warning()
            previousWarnings = warningKeys
        }
    }
    // Two different things were one red dot before: work that is already late and work that is due
    // today or tomorrow. The count is now written quietly, and red is kept for the second group only —
    // a deadline that is still ahead is information, not a scolding (N3).
    val today = now.toLocalDate()
    val overdueTasks = state.planning.tasks.count { task -> val due = task.dueDate; task.completedAtEpochMillis == null && due != null && due.isBefore(today) }
    val dueSoonTasks = state.planning.tasks.count { task -> val due = task.dueDate; task.completedAtEpochMillis == null && due != null && (due == today || due == today.plusDays(1)) }
    val waitingTasks = overdueTasks + dueSoonTasks
    // Read before the modifier: a semantics block is not a composable scope, so the string has to
    // exist by the time the dot is described.
    val badgeLabel = pluralStringResource(R.plurals.tasks_badge_waiting, waitingTasks, waitingTasks)
    val reduceMotion = rememberReduceMotion()
    // The "Dodaj blok" container transform measures its endpoints in root pixels: the pill the
    // finger just pressed and the window-sized root the pane is laid out inside. Measuring and
    // placing in the same space is what keeps the growing pane glued to the pill it starts from.
    var windowPx by remember { mutableStateOf(IntSize.Zero) }
    var pillBoundsPx by remember { mutableStateOf(Rect.Zero) }
    CompositionLocalProvider(LocalRoutineHaptics provides haptics, LocalRoutineSounds provides sounds,
        LocalHapticFeedback provides gatedHaptics, LocalReduceMotion provides reduceMotion,
        LocalPulse provides rememberAppPulse(reduceMotion)) {
      // One backdrop for the window: the content layer records into it and the floating chrome —
      // the top bar, the fast-add control — refracts it. They have to stay siblings of the layer,
      // never inside it, or a panel draws itself into itself.
      RoutineBackdropProvider {
        val backdrop = LocalRoutineBackdrop.current
        val density = LocalDensity.current
        // Measured, never assumed: the bar is three rows on the day view and one on goals.
        var topInset by remember { mutableStateOf(0.dp) }
        // The same for the bottom: the add control floats above the navigation bar, and that bar is
        // 0, 24 or 48 dp tall depending on the phone and on how the reader navigates. A constant was
        // correct on one device and hid the last row of a list on the next.
        var bottomInset by remember { mutableStateOf(RoutineMetrics.ListBottomInset) }
        // The bar folds while the reader scrolls and unfolds the moment they scroll back up, which
        // is what gives a content screen its room: ~48 dp of date chrome only when it is being used.
        var collapsed by remember { mutableStateOf(false) }
        // Progress of the predictive-back gesture, 0..1, while the reader drags out of Goals.
        var backProgress by remember { mutableFloatStateOf(0f) }
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
        // Drilling from a wider scale into a day pushes the scale it came from, so back walks out of
        // the day again instead of leaving the app — the stack a calendar reader expects. One entry
        // deep, because a day view never drills any further.
        // Changing scale is navigation, so it belongs on the back stack: week to month to year and
        // back out again, one press per step, instead of back leaving the app from wherever you
        // stand. Transitions caused by popping the stack are flagged so they do not push again.

        // Full-bleed stack instead of a Scaffold: a Scaffold body starts below its top bar, which
        // would leave nothing for the glass to refract. Here the content fills the window and the
        // floating chrome sits on top of it — as siblings of the layer, never inside it, or a panel
        // would draw itself into itself.
        Box(Modifier.fillMaxSize().onSizeChanged { windowPx = it }) {
            Box(Modifier.fillMaxSize().nestedScroll(headerScroll).routineBackdropLayer(backdrop)) {
                RoutineAmbientBackground(Modifier.fillMaxSize())
                AnimatedContent(targetState = state.panels.showGoals, label = "goals-switch",
                    modifier = Modifier.fillMaxSize(), transitionSpec = {
                        (slideInVertically(spatialSpec<IntOffset>(reduceMotion)) { it / 6 } + fadeIn(effectSpec<Float>(reduceMotion))) togetherWith
                            (slideOutVertically(spatialSpec<IntOffset>(reduceMotion)) { -it / 6 } + fadeOut(effectSpec<Float>(reduceMotion)))
                    }) { goalsShown ->
                    if (goalsShown) Box(Modifier.fillMaxSize().graphicsLayer {
                        // Read in the draw phase, so the gesture drives the transform directly
                        // instead of recomposing the screen sixty times a second.
                        val progress = backProgress
                        scaleX = 1f - 0.08f * progress
                        scaleY = 1f - 0.08f * progress
                        alpha = 1f - 0.30f * progress
                    }) { GoalsScreen(state.goals, state.panels.isSaving, onAction, topInset = topInset, bottomInset = bottomInset) }
                    else AnimatedContent(targetState = data, contentKey = { it.date to it.mode }, label = "period-switch",
                        // Swipe sideways for the next or previous period — the same Shift the date
                        // arrows fire, so both paths end in exactly one place. Day, week and month:
                        // the year is a summary nobody flicks through, and a horizontal drag there
                        // belongs to the month grid's own gestures.
                        modifier = Modifier.fillMaxSize().swipeToShift(
                            enabled = !state.panels.showGoals && data.mode != TimelineMode.YEAR,
                            onShift = { direction -> onAction(TimelineAction.Shift(direction.toLong())) },
                        ), transitionSpec = {
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
                                // Fade-through with a whisper of scale: a pure crossfade of two full
                                // trees reads as "it blinked and swapped"; the scale anchors the eye.
                                (fadeIn(effect) + scaleIn(effect, initialScale = 0.96f)) togetherWith
                                    (fadeOut(effect) + scaleOut(effect, targetScale = 0.96f))
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
                                        topInset = topInset, bottomInset = bottomInset, userName = state.preferences.userName,
                                        eveningFull = state.panels.eveningFullDay == day.date,
                                        skippedHidden = state.panels.skippedHiddenDay == day.date,
                                        // Tomorrow's exam or deadline is what keeps tonight's plan untouched.
                                        tomorrowHasDeadline = data.milestones.any { !it.isCompleted && it.dueDate == day.date.plusDays(1) } ||
                                            data.taskMarkers.any { !it.isCompleted && it.dueDate == day.date.plusDays(1) } ||
                                            data.goalMarkers.any { !it.isCompleted && it.dueDate == day.date.plusDays(1) }) }
                                    TimelineMode.WEEK -> WeeklyOverview(shown, onGoals = { onAction(TimelineAction.OpenGoals) }, topInset = topInset, bottomInset = bottomInset) { onAction(TimelineAction.SelectDate(it, true)) }
                                    TimelineMode.MONTH -> MonthlyOverview(shown, now.toLocalDate(), onGoals = { onAction(TimelineAction.OpenGoals) }, topInset = topInset, bottomInset = bottomInset) { onAction(TimelineAction.SelectDate(it, true)) }
                                    TimelineMode.YEAR -> YearlyOverview(shown, state.preferences, now.toLocalDate(), onGoals = { onAction(TimelineAction.OpenGoals) }, topInset = topInset, bottomInset = bottomInset) { onAction(TimelineAction.SelectDate(it, true)) }
                                }
                            }
                        }
                    }
                }
                }
            // The floating top bar: its own rounded glass pane with real space between the pane and
            // the status bar — nothing glued to the screen edge, the way the rest of the chrome
            // floats. The wrapper (status inset + gap + pane) is what the content has to clear, so
            // the wrapper is what gets measured; only the expanded bar sets the inset, and content
            // clears the *tall* bar, so when it folds mid-scroll the lists keep their padding and
            // nothing jumps — scrolled items simply travel up through the space the folded bar no
            // longer covers, under glass.
            Column(
                Modifier.align(Alignment.TopCenter)
                    // Measured *outside* the insets and the gap, so the number is the whole footprint:
                    // status bar, the gap above the pane, and the pane. The chain used to end with
                    // onSizeChanged, which reports what is left after the paddings above it — the pane
                    // alone — so on every screen the first line sat exactly the status bar deep under
                    // the open island. The order of these modifiers is the fix, not a style choice.
                    .onSizeChanged { size ->
                        if (!collapsed) topInset = with(density) { size.height.toDp() }
                    }
                    .statusBarsPadding()
                    .padding(top = RoutineSpacing.md, start = RoutineSpacing.md, end = RoutineSpacing.md),
            ) {
                RoutineGlassSurface(
                    modifier = Modifier.fillMaxWidth().testTag("app-top-bar"),
                    shape = RoutineShapes.GlassBar,
                    role = GlassRole.Bar,
                    // There is real space above the pane now, so its top edge can catch the light.
                    specular = true,
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = RoutineSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                        ) {
                            if (state.panels.showGoals) {
                                GlassIconButton(onClick = { onAction(TimelineAction.CloseGoals) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.tasks_back), Modifier.size(RoutineMetrics.IconSize))
                                }
                                RoutineLabel(stringResource(R.string.goals_title), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.weight(1f))
                            } else {
                                Row(Modifier.weight(1f).padding(horizontal = RoutineSpacing.xs)) {
                                    // Expanded: the app. Folded: the period being read, because that is
                                    // the only thing in the bar still worth the space.
                                    AnimatedContent(targetState = collapsed, label = "header-title",
                                        transitionSpec = { ContentTransform(fadeIn(effectSpec<Float>(reduceMotion)), fadeOut(effectSpec<Float>(reduceMotion)), sizeTransform = SizeTransform(clip = false)) }) { isCollapsed ->
                                        if (isCollapsed) {
                                            // Two lines, then still the auto-shrink inside RoutineLabel: the bar can be
                                            // narrow with four actions beside it, and the period it
                                            // names is the one thing that must never read "sreda, 16. sep…".
                                            RoutineLabel(periodTitle(data), style = MaterialTheme.typography.titleLarge,
                                                maxLines = RoutineTextDefaults.Body, heading = true)
                                        } else {
                                            // The brand answers the door: the LockIn mark with its wordmark
                                            // lives where the plain app-name text used to sit. This is the
                                            // screen the app opens on — deliberately not a splash screen.
                                            Image(
                                                painter = painterResource(R.drawable.logo_lockin),
                                                contentDescription = stringResource(R.string.app_name),
                                                modifier = Modifier.height(28.dp),
                                            )
                                        }
                                    }
                                }
                                // One pane of glass per action: each button is a piece of glass itself,
                                // not a transparent click target painted on the bar's frame.
                                GlassIconButton(onClick = { onAction(TimelineAction.OpenPlanning) }) { Icon(Icons.Outlined.AutoAwesome, stringResource(R.string.planning_open), Modifier.size(RoutineMetrics.IconSize)) }
                                Box {
                                    GlassIconButton(onClick = { onAction(TimelineAction.OpenTasks) }) { Icon(Icons.Outlined.Checklist, stringResource(R.string.tasks_open), Modifier.size(RoutineMetrics.IconSize)) }
                                    // The badge is the one hero moment allowed a bounce — and the one
                                    // animation that disappears entirely under remove-animations.
                                    val badgeEnter: EnterTransition =
                                        if (reduceMotion) fadeIn(snap<Float>(), initialAlpha = 1f)
                                        else scaleIn(PopSpring, initialScale = 0.4f) + fadeIn(tween<Float>(120))
                                    val badgeExit: ExitTransition =
                                        fadeOut(if (reduceMotion) snap<Float>() else tween<Float>(120))
                                    // Fully qualified on purpose: inside Box{} the RowScope receiver is
                                    // DslMarker-restricted, so the scope extension is not a candidate and
                                    // the compiler wants the top-level one named explicitly.
                                    androidx.compose.animation.AnimatedVisibility(visible = waitingTasks > 0,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(top = BadgeInset, end = BadgeInset),
                                        enter = badgeEnter, exit = badgeExit) {
                                        // A dot, not a number. The count sat on the corner of a 40 dp glass
                                        // pane and grew over the checklist glyph as soon as it reached two
                                        // digits, which is exactly the "something is hidden under something"
                                        // the design rules out. The dot keeps the colour meaning (red = a
                                        // deadline today or tomorrow, quiet grey = something is late), and the
                                        // number itself is spoken and lives one tap away in the tasks sheet.
                                        Box(
                                            Modifier.size(BadgeDot)
                                                .clip(CircleShape)
                                                .background(if (dueSoonTasks > 0) RoutineColors.Error else RoutineColors.TextMuted)
                                                .semantics { contentDescription = badgeLabel },
                                        )
                                    }
                                }
                                GlassIconButton(onClick = { onAction(TimelineAction.OpenGoals) }) { Icon(Icons.Outlined.Flag, stringResource(R.string.goals_open), Modifier.size(RoutineMetrics.IconSize)) }
                                GlassIconButton(onClick = { onAction(TimelineAction.OpenSettings) }) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings), Modifier.size(RoutineMetrics.IconSize)) }
                            }
                        }
                        // The marketing line does not earn 18 dp of every screen forever: it moved to the
                        // empty-day state, where there is room and it reads as an invitation.
                        AnimatedVisibility(
                            visible = !state.panels.showGoals && !collapsed,
                            enter = expandVertically(spatialSpec<IntSize>(reduceMotion)) + fadeIn(effectSpec<Float>(reduceMotion)),
                            exit = shrinkVertically(spatialSpec<IntSize>(reduceMotion)) + fadeOut(effectSpec<Float>(reduceMotion)),
                        ) {
                            DateNavigator(periodTitle(data),
                                onToday = { onAction(TimelineAction.Today) },
                                onPick = { haptics.tap(); choosingDate = true },
                                onShift = { direction -> onAction(TimelineAction.Shift(direction.toLong())) },
                                // "Today" means different things at different scales: the day the reader is
                                // living in, the week that contains it, the month, the school year. Each is
                                // compared the way the screen itself defines the period.
                                isCurrentPeriod = when (data.mode) {
                                    TimelineMode.DAY -> data.date == now.toLocalDate()
                                    TimelineMode.WEEK, TimelineMode.MONTH, TimelineMode.YEAR ->
                                        data.date in PeriodRanges.range(now.toLocalDate(), data.mode).let { (first, last) -> first..last }
                                })
                        }
                        AnimatedVisibility(
                            visible = !state.panels.showGoals,
                            enter = expandVertically(spatialSpec<IntSize>(reduceMotion)) + fadeIn(effectSpec<Float>(reduceMotion)),
                            exit = shrinkVertically(spatialSpec<IntSize>(reduceMotion)) + fadeOut(effectSpec<Float>(reduceMotion)),
                        ) {
                            // Kyant0's LiquidBottomTabs pattern at segment scale: one capsule of accent
                            // wash that slides between the cells on the spatial spring, instead of four
                            // backgrounds blinking at each other. The cells themselves stay transparent
                            // and clickable; the label colour flips as the capsule arrives.
                            BoxWithConstraints(
                                Modifier.fillMaxWidth()
                                    .padding(horizontal = RoutineSpacing.sm)
                                    .padding(bottom = RoutineSpacing.sm)
                                    // The strip is as tall as a finger needs, the capsule inside it keeps
                                    // the 40 dp the design asked for.
                                    .height(RoutineMetrics.TouchTarget),
                            ) {
                            val count = TimelineMode.entries.size
                            val cell = maxWidth / count
                            val slide by animateDpAsState(
                                cell * data.mode.ordinal,
                                spatialSpec<Dp>(reduceMotion),
                                label = "mode-indicator",
                            )
                            Box(
                                Modifier.width(cell).fillMaxHeight()
                                    .padding(vertical = 4.dp)
                                    // Draw-phase translation, not layout offset: the capsule glides
                                    // without re-measuring the row on every spring frame.
                                    .graphicsLayer { translationX = slide.toPx() }
                                    .background(RoutineColors.Timer.copy(alpha = 0.18f), RoutineShapes.Pill)
                                    .border(1.dp, RoutineColors.Timer.copy(alpha = 0.55f), RoutineShapes.Pill),
                            )
                            Row(Modifier.fillMaxSize()) {
                                TimelineMode.entries.forEach { mode ->
                                    val selected = data.mode == mode
                                    Box(
                                        Modifier.weight(1f).fillMaxHeight()
                                            .clickable(role = Role.Tab) { onAction(TimelineAction.SelectMode(mode)) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        RoutineLabel(
                                            text = stringResource(when (mode) { TimelineMode.DAY -> R.string.nav_day; TimelineMode.WEEK -> R.string.nav_week; TimelineMode.MONTH -> R.string.nav_month; TimelineMode.YEAR -> R.string.nav_year }),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) RoutineColors.TextPrimary else RoutineColors.TextSecondary,
                                        )
                                    }
                                }
                            }
                            }
                        }
                    }
                }
            }
            // Neutral glass with an amber wash laid *over* the refracted layer and an amber glyph on
            // top. Hue-blending the accent into the refraction looked tempting and read as a bug: the
            // button's colour then depended on whatever card happened to scroll underneath it. A wash
            // above the surface keeps the accent constant, exactly as Apple tints controls with a
            // fill over the material rather than by recolouring what the material shows.
            // Interactive in Apple's sense: it shrinks a few percent under the finger, lights up at
            // the touch point and springs back on the glass release spring. The ripple is switched off
            // on purpose — the scale *is* the state layer here, and a Material ripple on top of a
            // refracting panel is a second, contradicting answer.
            if (!state.panels.showGoals) {
                val fastAddTouch = rememberGlassTouch(reduceMotion)
                Box(
                    Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(RoutineSpacing.lg)
                        .height(56.dp).testTag("fast-add")
                        // The container transform's origin: the pane that grows into the sheet
                        // starts exactly where this pill sits, so it is measured, not estimated.
                        // Root pixels, because root is the space the morph lays the pane out in —
                        // the no-arg boundsInWindow() is hidden-deprecated in this Compose version.
                        .onGloballyPositioned { coordinates ->
                            pillBoundsPx = coordinates.boundsInRoot()
                            // Distance from the bottom of the window to the top of the pill, plus the
                            // gap the list keeps under it. Root pixels, the same space the morph uses.
                            bottomInset = with(density) {
                                (windowPx.height - coordinates.boundsInRoot().top).toDp()
                            } + RoutineSpacing.md
                        }
                        .routineGlassTouch(fastAddTouch, RoutineShapes.Pill)
                        .routineGlass(backdrop, RoutineShapes.Pill, GlassRole.Control,
                            tilt = LocalGlassTilt.current)
                        .background(RoutineColors.Primary.copy(alpha = 0.16f), RoutineShapes.Pill)
                        .clip(RoutineShapes.Pill)
                        .clickable(interactionSource = fastAddTouch.source, indication = null, role = Role.Button) {
                            onAction(TimelineAction.OpenAdd)
                        }
                        .padding(horizontal = RoutineSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                        Icon(Icons.Default.Add, null, tint = RoutineColors.Primary)
                        RoutineLabel(stringResource(R.string.add_block), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Primary)
                    }
                }
            }
            // Composed after the pill so the growing pane covers it — the morph is the pill
            // itself, not a second element next to it. It sits above the content it refracts and
            // below the sheet's window, which slides in to take the surface over.
            AddBlockMorph(
                active = state.panels.showAdd,
                pill = pillBoundsPx,
                window = windowPx,
                backdrop = backdrop,
            )
            // The Scaffold used to keep the snackbar clear of the button; in a plain stack that has to
            // be done by hand, or the two overlap at the bottom of the screen.
            SnackbarHost(
                snackbars,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(bottom = if (state.panels.showGoals) RoutineSpacing.md else RoutineMetrics.FabClearance),
            )
        }
        // ---- the back stack ------------------------------------------------------------------
        // Composed outermost-first: Compose answers with the last enabled callback, so the order of
        // these three blocks is the depth of the stack — scale, then Goals, then (in their own
        // windows, and therefore ahead of both) the sheets and dialogs.
        BackHandler(enabled = state.panels.modeBackStack.isNotEmpty() && !state.panels.showGoals &&
            !state.panels.showSettings && !state.panels.showTasks && !state.panels.showPlanning &&
            !state.panels.showAdd && state.panels.editingBlock == null,
        ) {
            onAction(TimelineAction.PopMode)
        }
        // Panels here are plain state, so the stack is derived rather than remembered: each layer on
        // screen contributes one callback and Compose answers with the innermost enabled one. Dialogs
        // and ModalBottomSheets live in their own window and therefore answer before this does, which
        // is why the only layer needing a callback is Goals — a full-screen swap inside the activity,
        // and until now the one screen where back left the app instead of closing it.
        // PredictiveBackHandler rather than BackHandler so the drag itself animates the exit.
        PredictiveBackHandler(enabled = state.panels.showGoals) { progress ->
            try {
                progress.collect { event -> backProgress = event.progress }
                onAction(TimelineAction.CloseGoals)
            } catch (cancelled: CancellationException) {
                // Gesture abandoned halfway: stay on Goals, unwind the transform, and let the
                // cancellation propagate — swallowing it would break the caller's coroutine.
                throw cancelled
            } finally {
                backProgress = 0f
            }
        }
        if (choosingDate) AppDatePicker(data.date, onDismiss = { choosingDate = false }, onDate = { onAction(TimelineAction.SelectDate(it)); choosingDate = false })
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
    }
}

/**
 * The "Dodaj blok" container transform.
 *
 * While the editor sheet slides in from below, a pane of liquid glass grows out of the pill the
 * finger just pressed — same backdrop, same accent wash, its corners unwinding from the capsule
 * to the sheet's rounded top — and then hands the surface over: the pane fades away in the last
 * half of the morph, exactly as the sheet's own glass arrives to take it over. The sheet lives in
 * its own window and cannot share the pill's geometry, so the transform is drawn here, in the app
 * window, as a sibling of the content layer it refracts.
 *
 * Under the system's remove-animations setting the morph never plays: the sheet simply appears,
 * the way every other transition in the app behaves.
 */
@Composable
private fun AddBlockMorph(
    active: Boolean,
    pill: Rect,
    window: IntSize,
    backdrop: Backdrop?,
) {
    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (!active) {
            progress = 0f
            return@LaunchedEffect
        }
        // First launch after a cold start: the pill has not been measured yet, and a morph from
        // nothing is just a flicker. The sheet opens normally instead.
        if (reduceMotion || backdrop == null || window.width == 0 || pill.width <= 0f) return@LaunchedEffect
        progress = 0f
        animate(0f, 1f, animationSpec = tween(420, easing = FastOutSlowInEasing)) { value, _ -> progress = value }
        // The sheet owns the surface now; leave composition entirely so the pane stops sampling.
        progress = 0f
    }
    if (progress <= 0f) return
    val p = progress
    // The entry editor fills 92% of the window, anchored to the bottom: that is where the pane
    // grows into, and its top corners come from the same shape the sheet uses.
    val sheetTop = window.height * 0.08f
    val sheetTopRadius = with(density) { 24.dp.toPx() }
    val pillRadius = pill.height / 2f
    // Pixel precision for the growing pane: Dp would round through density and lose half a pixel.
    val paneLeft = (pill.left * (1f - p)).roundToInt()
    val paneTop = lerp(pill.top, sheetTop, p).roundToInt()
    val paneWidth = lerp(pill.width, window.width.toFloat(), p).roundToInt().coerceAtLeast(0)
    val paneHeight = (window.height - paneTop).coerceAtLeast(0)
    val shape = RoundedCornerShape(
        topStart = with(density) { lerp(pillRadius, sheetTopRadius, p).toDp() },
        topEnd = with(density) { lerp(pillRadius, sheetTopRadius, p).toDp() },
        bottomStart = with(density) { lerp(pillRadius, 0f, p).toDp() },
        bottomEnd = with(density) { lerp(pillRadius, 0f, p).toDp() },
    )
    Box(
        Modifier.fillMaxSize()
            // Visible for the first 55% of the morph, then gone — the handover hides the point
            // where the sheet's own spring and the morph's easing stop agreeing.
            .graphicsLayer { alpha = 1f - ((p - 0.55f) / 0.45f).coerceIn(0f, 1f) }
            // Measured placement: the pane is laid out at its pixel rectangle inside the window
            // instead of being nudged there, which is the layout invariant the whole app keeps.
            .layout { measurable, constraints ->
                val pane = measurable.measure(Constraints.fixed(paneWidth, paneHeight))
                layout(constraints.constrainWidth(window.width), constraints.constrainHeight(window.height)) {
                    pane.place(paneLeft, paneTop)
                }
            }
            .routineGlass(backdrop, shape, GlassRole.Bar, specular = true, tilt = LocalGlassTilt.current)
            .background(RoutineColors.Primary.copy(alpha = 0.16f), shape)
            .clip(shape),
    )
}

/** Size of the due-dot on the tasks button, and its distance from the button's corner. */
private val BadgeDot = 8.dp
private val BadgeInset = 6.dp

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

