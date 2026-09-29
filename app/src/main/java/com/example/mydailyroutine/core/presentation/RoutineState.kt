package com.example.mydailyroutine.core.presentation

import androidx.compose.runtime.Immutable
import com.example.mydailyroutine.domain.routines.*
import com.example.mydailyroutine.domain.planning.*
import com.example.mydailyroutine.domain.learning.*
import androidx.annotation.StringRes
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.PeriodicBreakConfig
import com.example.mydailyroutine.domain.presets.QuickAddPreset
import com.example.mydailyroutine.domain.health.DailyMetrics
import com.example.mydailyroutine.domain.health.WarningType
import com.example.mydailyroutine.domain.model.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

enum class TimelineMode { DAY, WEEK, MONTH, YEAR }
enum class EntryKind { BLOCK, DEADLINE, EXAM }

@Immutable
data class WarningUi(val type: WarningType, val itemKeys: PersistentSet<String>, val atMinute: Int, val recoveryMinutes: Int)
@Immutable
data class CancelledOccurrenceUi(val routineId: Long, val date: LocalDate, val title: String)
@Immutable
data class DayUi(
    val date: LocalDate,
    val items: PersistentList<ResolvedTimelineItem>,
    val warnings: PersistentList<WarningUi>,
    val calendar: PersistentList<CalendarEntry>,
    val cancelled: PersistentList<CancelledOccurrenceUi>,
    val metrics: DailyMetrics,
)

@Immutable
data class TimelineContent(
    val date: LocalDate,
    val mode: TimelineMode = TimelineMode.DAY,
    val isLoading: Boolean = true,
    @StringRes val error: Int? = null,
    val days: PersistentMap<LocalDate, DayUi> = persistentMapOf(),
    val subjects: PersistentList<Subject> = persistentListOf(),
    val subjectPresets: PersistentList<QuickAddPreset> = persistentListOf(),
    val calendar: PersistentList<CalendarEntry> = persistentListOf(),
    val milestones: PersistentList<Milestone> = persistentListOf(),
    val taskMarkers: PersistentList<Milestone> = persistentListOf(),
    // CAS/EE milestones mapped onto the marker display for the days inside the selected range (GoalsScreen owns editing).
    val goalMarkers: PersistentList<Milestone> = persistentListOf(),
)

@Immutable
data class TimelinePanels(
    val showAdd: Boolean = false,
    val addSession: Int = 0,
    val entryContinuation: EntryContinuation? = null,
    val showSettings: Boolean = false,
    val editingBlock: ResolvedTimelineItem.Block? = null,
    val editingMilestone: ResolvedTimelineItem.Milestone? = null,
    val editingSubject: Subject? = null,
    val pendingDelete: ResolvedTimelineItem? = null,
    val isSaving: Boolean = false,
    val confirmDemo: Boolean = false,
    val showPlanning: Boolean = false,
    val completionTarget: ResolvedTimelineItem.Block? = null,
    val showTopicEditor: Boolean = false,
    val confirmCancelExecution: Boolean = false,
    val exportJson: String? = null,
    val showTasks: Boolean = false,
    val showGoals: Boolean = false,
    /** Scales already visited on this visit: back walks them in reverse instead of leaving. */
    val modeBackStack: List<TimelineMode> = emptyList(),
    val entryPrefillTitle: String? = null,
    /** The kind and length the editor opens with; null means "the editor's own default". */
    val entryPrefill: EntryPrefill? = null,
    /**
     * The date for which the reader asked to see the whole evening even though the minimal-state
     * protocol would have quieted it (N15). Remembered for that date only, and only for this run:
     * the schema is locked, so no column is added for a decision that lasts an evening.
     */
    val eveningFullDay: LocalDate? = null,
    /** The date for which the reader put the skipped blocks aside until tomorrow (N6). */
    val skippedHiddenDay: LocalDate? = null,
    // A task captured via the system share sheet pre-fills the Tasks quick-add; cleared on save or close.
    val sharedTaskTitle: String? = null,
    val sharedTaskDue: Long? = null,
    val showTimetableImport: Boolean = false,
)

/**
 * What the editor should already be filled with when it opens.
 *
 * Two readers decided this: the one who writes the same kind of block twice in a row and used to be
 * handed a 90-minute default every time (N4), and the one who installs the app, sees an empty day and
 * should reach a first real block in one tap (N11). The shape is deliberately only a kind and a
 * length: the start time is still the current time, which the editor already rounds up to the next
 * quarter of an hour.
 */
@Immutable
data class EntryPrefill(val category: RoutineCategory, val durationMinutes: Int)

/**
 * Length of the block an empty day offers: long enough to be a real piece of work, short enough that
 * a reader who has never planned a day will actually start it.
 */
const val FirstBlockMinutes = 45

@Immutable
data class TimelineUiState(
    val content: TimelineContent,
    val preferences: SchedulePreferences = SchedulePreferences(),
    val panels: TimelinePanels = TimelinePanels(),
    val exampleLoaded: Boolean = false,
    val planning: PlanningUiState = PlanningUiState(),
    val execution: com.example.mydailyroutine.domain.execution.ActiveExecution? = null,
    val sleep: SleepSchedule = SleepSchedule(),
    val goals: GoalsUiState = GoalsUiState(),
)

@Immutable
data class EntryDraft(
    val title: String,
    val subjectId: Long?,
    val kind: EntryKind,
    val date: LocalDate,
    val start: LocalTime?,
    val end: LocalTime?,
    val category: RoutineCategory,
    val repeatWeekly: Boolean,
    val notificationsEnabled: Boolean,
    val existingMilestoneId: Long = 0,
    val isCompleted: Boolean = false,
    val minDurationMinutes: Int? = null,
    val elasticity: Double = 1.0,
    val priorityWeight: Double = 3.0,
    val isFixedCommitment: Boolean = false,
    val calibrateDuration: Boolean = true,
    val estimatedEffortHours: Double = 0.0,
    val isTerminalExam: Boolean = false,
    val repeatDaysMask: Int = 0,
    val afterLessonBreakMinutes: Int = 0,
    val breakTitle: String = "",
    val keepOpen: Boolean = false,
)

/** One lesson detected from pasted timetable text; the import creates a weekly SCHOOL block per row. */
@Immutable
data class TimetableRow(val day: java.time.DayOfWeek, val startMinute: Int, val endMinute: Int, val title: String)

sealed interface TimelineAction {
    data class SelectDate(val date: LocalDate, val openDay: Boolean = false) : TimelineAction
    data class SelectMode(val mode: TimelineMode) : TimelineAction
    data object PopMode : TimelineAction
    data class Shift(val direction: Long) : TimelineAction
    data object Today : TimelineAction
    data object Retry : TimelineAction
    /**
     * The first-run flow is done: the reader's name, the school window, and whether the example was
     * loaded come back as one action, so a half-finished flow can never leave half a setting behind.
     */
    data class FinishOnboarding(val userName: String, val schoolStart: LocalTime, val schoolEnd: LocalTime, val loadExample: Boolean) : TimelineAction
    data object OpenAdd : TimelineAction

    /**
     * The one-tap first block of an empty day: the editor opens with a short focus block at the
     * current time already in it. It is an action of its own rather than an argument on [OpenAdd],
     * because the empty-day card and the floating button should not have to agree on that argument.
     */
    data object OpenFirstBlock : TimelineAction

    /** "Pokaži vse": the reader wants the evening as it was planned, for this date (N15). */
    data class ShowEveningFull(val date: LocalDate) : TimelineAction

    /** "Skrij za danes": the skipped blocks leave the screen for this date, not for good (N6). */
    data class HideSkipped(val date: LocalDate) : TimelineAction
    data object OpenPlanning : TimelineAction
    data class StartExecution(val block: ResolvedTimelineItem.Block) : TimelineAction
    data class StartExecutionById(val id: Long, val date: LocalDate) : TimelineAction
    data class RequestActual(val item: ResolvedTimelineItem.Block) : TimelineAction
    data class RequestActualById(val id: Long) : TimelineAction
    data object FinishExecution : TimelineAction
    data object CancelExecution : TimelineAction
    data object RequestCancelExecution : TimelineAction
    data object DismissCancelExecution : TimelineAction
    data object SyncExecution : TimelineAction
    data class SetAutomaticHealing(val enabled: Boolean) : TimelineAction
    data object ClosePlanning : TimelineAction
    data object NewTopic : TimelineAction
    data object CloseTopic : TimelineAction
    data class SaveTopic(val topic: StudyTopic, val titlePattern: String) : TimelineAction
    data class DeleteTopic(val id: Long) : TimelineAction
    data class AutoHeal(val date: LocalDate, val actualStartMinutes: Int) : TimelineAction
    data class AddReserve(val date: LocalDate, val title: String) : TimelineAction
    data class ScheduleBacklog(val id: Long, val date: LocalDate) : TimelineAction
    data class DeleteBacklog(val id: Long) : TimelineAction
    data class PlanMilestone(val id: Long, val initialDate: LocalDate, val titlePattern: String, val reserveTitle: String, val category: RoutineCategory = RoutineCategory.FOCUS_ANALYTICAL, val stages: List<PreparationStage> = emptyList()) : TimelineAction
    data class RecordActual(val item: ResolvedTimelineItem.Block, val minutes: Int) : TimelineAction
    data object CloseActual : TimelineAction
    data class SetPlanningConfig(val config: PlanningConfig) : TimelineAction
    data object OpenSettings : TimelineAction
    data object CloseAdd : TimelineAction
    data object CloseSettings : TimelineAction
    data object CloseEditor : TimelineAction
    data class SaveEntry(val draft: EntryDraft) : TimelineAction
    data class Edit(val item: ResolvedTimelineItem) : TimelineAction
    data class SaveBlockEdit(val item: ResolvedTimelineItem.Block, val title: String, val start: LocalTime, val end: LocalTime, val wholeTemplate: Boolean, val allSeriesDays: Boolean = false) : TimelineAction
    data class ToggleComplete(val item: ResolvedTimelineItem) : TimelineAction
    data class SetReminder(val routineId: Long, val enabled: Boolean) : TimelineAction
    data class Skip(val item: ResolvedTimelineItem.Block) : TimelineAction
    data class Restore(val routineId: Long, val date: LocalDate) : TimelineAction
    data class ResetOverride(val item: ResolvedTimelineItem.Block) : TimelineAction
    data class RequestDelete(val item: ResolvedTimelineItem) : TimelineAction
    data object DismissDelete : TimelineAction
    data object ConfirmDelete : TimelineAction
    data class EditSubject(val subject: Subject? = null) : TimelineAction
    data object CloseSubjectEditor : TimelineAction
    data class SaveSubject(val subject: Subject) : TimelineAction
    data class DeleteSubject(val id: Long) : TimelineAction
    data class InsertRecovery(val type: WarningType, val anchorKey: String, val recoveryTitle: String, val continuationSuffix: String) : TimelineAction
    data class SetHaptics(val enabled: Boolean) : TimelineAction
    data class SetSoundEffects(val enabled: Boolean) : TimelineAction
    /**
     * The reader's explicit interface language: [com.example.mydailyroutine.domain.model.AppLanguage]
     * tags, or `null` for "follow the device". Sent from the settings sheet and the first
     * onboarding screen; applying it restarts the activity ([TimelineEffect.RestartForLocale]).
     */
    data class SetAppLanguage(val language: String?) : TimelineAction
    data class SetHealthConfig(val config: HealthConfig) : TimelineAction
    data class SetPeriodicBreak(val config: PeriodicBreakConfig) : TimelineAction
    data object RequestDemo : TimelineAction
    data object DismissDemo : TimelineAction
    data object LoadDemo : TimelineAction
    data class DeleteSeries(val key: String) : TimelineAction
    data class SaveSleep(val value: SleepSchedule, val sleepTitle: String, val morningTitle: String) : TimelineAction
    data class SaveEntryDefaults(val defaults: EntryDefaults) : TimelineAction
    data class SetMute(val muted: Boolean) : TimelineAction
    data class SetRecoveryNotifications(val enabled: Boolean) : TimelineAction
    data class SetSchoolWindow(val start: LocalTime, val end: LocalTime) : TimelineAction
    data class SetTeachingEnd(val date: LocalDate) : TimelineAction
    data object ExportSchedule : TimelineAction
    data class ImportSchedule(val json: String) : TimelineAction
    data object OpenTasks : TimelineAction
    data object CloseTasks : TimelineAction
    data class OpenSharedTask(val title: String, val dueEpochDay: Long?) : TimelineAction
    data object ShowTimetableImport : TimelineAction
    data object CloseTimetableImport : TimelineAction
    data class ImportTimetable(val rows: List<TimetableRow>) : TimelineAction
    data class AddTask(val title: String, val dueDate: LocalDate?, val subjectId: Long?) : TimelineAction
    data class UpdateTask(val task: Task) : TimelineAction
    data class ToggleTask(val id: Long) : TimelineAction
    data class DeleteTask(val id: Long) : TimelineAction
    data object ClearCompletedTasks : TimelineAction
    data class TaskToSchedule(val task: Task) : TimelineAction
    data object OpenGoals : TimelineAction
    data object CloseGoals : TimelineAction
    data class SaveGoalsProject(val project: GoalsProject) : TimelineAction
    data class DeleteGoalsProject(val id: Long) : TimelineAction
    data class SaveGoalActivity(val activity: GoalActivity) : TimelineAction
    data class ToggleGoalActivity(val id: Long) : TimelineAction
    data class DeleteGoalActivity(val id: Long) : TimelineAction
    data class SaveGoalMilestone(val milestone: GoalMilestone) : TimelineAction
    data class ToggleGoalMilestone(val id: Long) : TimelineAction
    data class DeleteGoalMilestone(val id: Long) : TimelineAction
    data class AddGoalProgress(val entry: GoalProgress) : TimelineAction
    data class DeleteGoalProgress(val id: Long) : TimelineAction
    data class GoalActivityToSchedule(val activity: GoalActivity) : TimelineAction
    data class SeedGoalProject(val kind: String, val projectName: String, val activityNames: List<String>, val milestoneNames: List<String>) : TimelineAction
}

sealed interface TimelineEffect {
    data class Message(@StringRes val resource: Int, val minutes: Int? = null, val count: Int? = null) : TimelineEffect
    data object Completed : TimelineEffect
    /**
     * The interface language changed: the reader must see the new language from the next frame,
     * which means a recreated activity (resources are per-context, not per-composition) and a
     * refreshed widget, because the widget renders outside this window.
     */
    data object RestartForLocale : TimelineEffect
}

@Immutable
data class PlanningUiState(
    val backlog: PersistentList<BacklogEntry> = persistentListOf(),
    val history: PersistentList<HistoricalVelocity> = persistentListOf(),
    val topics: PersistentList<StudyTopic> = persistentListOf(),
    val milestones: PersistentList<Milestone> = persistentListOf(),
    val tasks: PersistentList<Task> = persistentListOf(),
)

@Immutable
data class GoalsUiState(
    val projects: PersistentList<GoalsProject> = persistentListOf(),
    val activities: PersistentList<GoalActivity> = persistentListOf(),
    val milestones: PersistentList<GoalMilestone> = persistentListOf(),
    val progress: PersistentList<GoalProgress> = persistentListOf(),
)

@Immutable
data class EntryContinuation(val start: java.time.LocalDateTime, val durationMinutes: Int,
    val weekly: Boolean, val weekdaysMask: Int, val breakMinutes: Int)
