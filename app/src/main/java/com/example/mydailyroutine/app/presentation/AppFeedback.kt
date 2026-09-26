package com.example.mydailyroutine.app.presentation

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.mydailyroutine.core.designsystem.haptics.*
import com.example.mydailyroutine.core.designsystem.sound.*
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.platform.Diagnostics
import com.example.mydailyroutine.core.platform.applyLocaleToProcessDefaults
import com.example.mydailyroutine.core.presentation.*
import com.example.mydailyroutine.features.entry.presentation.*
import com.example.mydailyroutine.features.planning.presentation.*
import com.example.mydailyroutine.features.timeline.presentation.overview.*
import com.example.mydailyroutine.widget.refreshAgendaWidgets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * How the app answers: one haptic and at most one sound per action, chosen by what the action
 * *means*.
 *
 * Apple's generators are semantic — selection for a discrete step, impact for a collision,
 * notification for an outcome — and that mapping is most of why an iPhone feels precise. Wrapping
 * the view model here rather than sprinkling `haptics.tap()` through the screens is what keeps one
 * action from ever answering twice.
 */
@Composable
fun rememberFeedbackAction(
    viewModel: RoutineViewModel,
    haptics: RoutineHaptics,
    sounds: RoutineSounds,
): (TimelineAction) -> Unit =
    remember(viewModel, haptics, sounds) { { action ->
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

/**
 * Collects the one-shot effects: the completion moment, the messages, the restart a language
 * change needs.
 *
 * Composed above the first-run branch on purpose. The language chip in onboarding sends
 * `RestartForLocale`, and an effect nobody collects is an effect that sits in the channel: the
 * onboarding screen would stay in the old language — while its own label promises the choice
 * applies at once — and the activity would then recreate itself the moment the reader finished,
 * in the middle of the first impression.
 */
@Composable
fun RoutineEffects(
    viewModel: RoutineViewModel,
    haptics: RoutineHaptics,
    sounds: RoutineSounds,
    snackbars: SnackbarHostState,
) {
    val context = LocalContext.current
    // Above the first-run branch on purpose. The language chip in onboarding sends
    // RestartForLocale, and an effect nobody collects is an effect that sits in the channel:
    // the onboarding screen would stay in the old language — while its own label promises the
    // choice applies at once — and the activity would then recreate itself the moment the
    // reader finished, in the middle of the first impression.
    LaunchedEffect(viewModel, haptics, sounds, context) {
        val scope = this
        viewModel.effects.collect { effect -> when (effect) {
            // The hero moment: the block's completion animation, the success haptic and the
            // confirm sound land together.
            TimelineEffect.Completed -> { haptics.complete(); sounds.confirm() }
            // Shown in its own coroutine: during onboarding no host is composed yet, and a
            // suspended showSnackbar would hold the collector — and with it the restart the
            // language chip asks for — until something dismissed a snackbar nobody can see.
            is TimelineEffect.Message -> scope.launch { snackbars.showSnackbar(if (effect.count != null) context.getString(effect.resource, effect.minutes, effect.count) else if (effect.minutes == null) context.getString(effect.resource) else context.getString(effect.resource, effect.minutes)) }
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
}
