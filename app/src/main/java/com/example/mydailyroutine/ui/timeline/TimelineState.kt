package com.example.mydailyroutine.ui.timeline

import androidx.compose.runtime.Immutable
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
    val showSettings: Boolean = false,
    val editingBlock: ResolvedTimelineItem.Block? = null,
    val editingMilestone: ResolvedTimelineItem.Milestone? = null,
    val editingSubject: Subject? = null,
    val pendingDelete: ResolvedTimelineItem? = null,
    val isSaving: Boolean = false,
    val confirmDemo: Boolean = false,
)

@Immutable
data class TimelineUiState(
    val content: TimelineContent,
    val preferences: SchedulePreferences = SchedulePreferences(),
    val panels: TimelinePanels = TimelinePanels(),
    val exampleLoaded: Boolean = false,
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
)

sealed interface TimelineAction {
    data class SelectDate(val date: LocalDate, val openDay: Boolean = false) : TimelineAction
    data class SelectMode(val mode: TimelineMode) : TimelineAction
    data class Shift(val direction: Long) : TimelineAction
    data object Today : TimelineAction
    data object Retry : TimelineAction
    data object OpenAdd : TimelineAction
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
    data class Message(@StringRes val resource: Int, val minutes: Int? = null) : TimelineEffect
    data object Completed : TimelineEffect
}
