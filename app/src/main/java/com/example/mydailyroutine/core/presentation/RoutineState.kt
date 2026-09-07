package com.example.mydailyroutine.core.presentation

import androidx.compose.runtime.Immutable
import com.example.mydailyroutine.domain.planning.*
import com.example.mydailyroutine.domain.learning.*
import androidx.annotation.StringRes
import com.example.mydailyroutine.domain.health.HealthConfig
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
)

@Immutable
data class TimelinePanels(
    val showAdd: Boolean = false,
    val addSession: Int = 0,
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
)

@Immutable
data class TimelineUiState(
    val content: TimelineContent,
    val preferences: SchedulePreferences = SchedulePreferences(),
    val panels: TimelinePanels = TimelinePanels(),
    val exampleLoaded: Boolean = false,
    val planning: PlanningUiState = PlanningUiState(),
    val execution: com.example.mydailyroutine.domain.execution.ActiveExecution? = null,
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
)

sealed interface TimelineAction {
    data class SelectDate(val date: LocalDate, val openDay: Boolean = false) : TimelineAction
    data class SelectMode(val mode: TimelineMode) : TimelineAction
    data class Shift(val direction: Long) : TimelineAction
    data object Today : TimelineAction
    data object Retry : TimelineAction
    data object OpenAdd : TimelineAction
    data object OpenPlanning : TimelineAction
    data class StartExecution(val block: ResolvedTimelineItem.Block) : TimelineAction
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
    data class SaveBlockEdit(val item: ResolvedTimelineItem.Block, val title: String, val start: LocalTime, val end: LocalTime, val wholeTemplate: Boolean) : TimelineAction
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
    data class SetHealthConfig(val config: HealthConfig) : TimelineAction
    data object RequestDemo : TimelineAction
    data object DismissDemo : TimelineAction
    data object LoadDemo : TimelineAction
    data class SetMute(val muted: Boolean) : TimelineAction
    data class SetSchoolWindow(val start: LocalTime, val end: LocalTime) : TimelineAction
    data class SetTeachingEnd(val date: LocalDate) : TimelineAction
}

sealed interface TimelineEffect {
    data class Message(@StringRes val resource: Int, val minutes: Int? = null, val count: Int? = null) : TimelineEffect
    data object Completed : TimelineEffect
}

@Immutable
data class PlanningUiState(
    val backlog: PersistentList<BacklogEntry> = persistentListOf(),
    val history: PersistentList<HistoricalVelocity> = persistentListOf(),
    val topics: PersistentList<StudyTopic> = persistentListOf(),
    val milestones: PersistentList<Milestone> = persistentListOf(),
)
