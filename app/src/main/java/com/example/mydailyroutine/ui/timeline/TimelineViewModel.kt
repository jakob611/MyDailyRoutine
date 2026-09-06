package com.example.mydailyroutine.ui.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mydailyroutine.domain.health.ScheduleHealthEngine
import com.example.mydailyroutine.domain.health.ScheduleMetrics
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.repository.PreferencesRepository
import com.example.mydailyroutine.domain.repository.TimelineRepository
import com.example.mydailyroutine.domain.repository.TimelineResolver
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val repository: TimelineRepository,
    private val settings: PreferencesRepository,
    private val savedState: SavedStateHandle,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
    private val initialDate = today()
    private val selectedDate = savedState.getStateFlow("date", initialDate.toEpochDay())
    private val selectedMode = savedState.getStateFlow("mode", TimelineMode.DAY.name)
    private val retry = MutableStateFlow(0)
    private val panels = MutableStateFlow(TimelinePanels())
    private val operationLock = Mutex()
    private val messages = Channel<String>(Channel.BUFFERED)
    val effects: Flow<String> = messages.receiveAsFlow()
    private val resolver = TimelineResolver()
    private val healthEngine = ScheduleHealthEngine()

    private val content = combine(selectedDate, selectedMode, retry) { epoch, mode, _ ->
        LocalDate.ofEpochDay(epoch) to TimelineMode.valueOf(mode)
    }.flatMapLatest { (date, mode) ->
        val (from, through) = range(date, mode)
        repository.observeSnapshot(from, through)
            .map { snapshot -> withContext(Dispatchers.Default) { resolveContent(date, mode, snapshot) } }
            .onStart { emit(TimelineContent(date, mode)) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(TimelineContent(date, mode, isLoading = false, error = "Could not open your local schedule. Your data has not been cleared."))
            }
    }

    val state: StateFlow<TimelineUiState> = combine(content, settings.preferences, panels) { data, preferences, panels ->
        TimelineUiState(data, preferences, panels)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState(TimelineContent(initialDate)))

    private suspend fun resolveContent(date: LocalDate, mode: TimelineMode, snapshot: ScheduleSnapshot): TimelineContent {
        val prepared = resolver.prepare(snapshot)
        val calendarByDate = snapshot.calendar.groupBy { it.date }
        val days = linkedMapOf<LocalDate, DayUi>()
        var current = snapshot.from
        while (current <= snapshot.through) {
            currentCoroutineContext().ensureActive()
            val items = prepared.forDate(current)
            days[current] = DayUi(
                date = current,
                items = items.toPersistentList(),
                warnings = healthEngine.evaluate(items).map { WarningUi(it.type, it.message, it.relatedItemKeys.toPersistentSet()) }.toPersistentList(),
                calendar = calendarByDate[current].orEmpty().toPersistentList(),
                cancelled = prepared.cancelledForDate(current).map {
                    CancelledOccurrenceUi(it.routineBlockId, it.occurrenceDate, it.title)
                }.toPersistentList(),
                metrics = ScheduleMetrics.forDay(items),
            )
            current = current.plusDays(1)
        }
        return TimelineContent(
            date = date, mode = mode, isLoading = false, days = days.toPersistentMap(),
            subjects = snapshot.subjects.toPersistentList(),
            calendar = snapshot.calendar.filter { it.date in snapshot.from..snapshot.through }.toPersistentList(),
            milestones = snapshot.milestones.sortedWith(compareBy<Milestone> { it.dueDate }.thenBy { it.dueTime ?: java.time.LocalTime.MIN }).toPersistentList(),
        )
    }

    fun onAction(action: TimelineAction) {
        when (action) {
            is TimelineAction.SelectDate -> {
                savedState["date"] = action.date.coerceIn(ScheduleValidation.firstUiDate, ScheduleValidation.lastUiDate).toEpochDay()
                if (action.openDay) savedState["mode"] = TimelineMode.DAY.name
            }
            is TimelineAction.SelectMode -> savedState.set("mode", action.mode.name)
            is TimelineAction.Shift -> {
                val current = LocalDate.ofEpochDay(selectedDate.value)
                val target = when (TimelineMode.valueOf(selectedMode.value)) {
                    TimelineMode.DAY -> current.plusDays(action.direction)
                    TimelineMode.WEEK -> current.plusWeeks(action.direction)
                    TimelineMode.MONTH -> current.plusMonths(action.direction)
                    TimelineMode.YEAR -> current.plusYears(action.direction)
                }
                savedState["date"] = target.coerceIn(ScheduleValidation.firstUiDate, ScheduleValidation.lastUiDate).toEpochDay()
            }
            TimelineAction.Today -> savedState.set("date", today().toEpochDay())
            TimelineAction.Retry -> retry.update { it + 1 }
            TimelineAction.OpenAdd -> panels.update { it.copy(showAdd = true, editingMilestone = null) }
            TimelineAction.OpenSettings -> panels.update { it.copy(showSettings = true) }
            TimelineAction.CloseAdd -> if (!panels.value.isSaving) panels.update { it.copy(showAdd = false, editingMilestone = null) }
            TimelineAction.CloseSettings -> if (!panels.value.isSaving) panels.update { it.copy(showSettings = false) }
            TimelineAction.CloseEditor -> if (!panels.value.isSaving) panels.update { it.copy(editingBlock = null) }
            is TimelineAction.Edit -> when (val item = action.item) {
                is ResolvedTimelineItem.Block -> panels.update { it.copy(editingBlock = item) }
                is ResolvedTimelineItem.Milestone -> panels.update { it.copy(editingMilestone = item, showAdd = true) }
            }
            is TimelineAction.SaveEntry -> saveEntry(action.draft)
            is TimelineAction.SaveBlockEdit -> perform {
                repository.editBlock(action.item.routineBlockId, action.item.occurrenceDate, action.title, action.start, action.end, action.wholeTemplate)
                panels.update { it.copy(editingBlock = null) }
                messages.send("Schedule updated")
            }
            is TimelineAction.ToggleComplete -> perform {
                when (val item = action.item) {
                    is ResolvedTimelineItem.Block -> repository.setCompleted(item.routineBlockId, item.occurrenceDate, !item.isCompleted)
                    is ResolvedTimelineItem.Milestone -> repository.setMilestoneCompleted(item.milestoneId, !item.isCompleted)
                }
            }
            is TimelineAction.SetReminder -> perform { repository.setNotificationEnabled(action.routineId, action.enabled) }
            is TimelineAction.Skip -> perform {
                repository.cancelOccurrence(action.item.routineBlockId, action.item.occurrenceDate)
                messages.send("Occurrence skipped. Restore it below the timeline.")
            }
            is TimelineAction.Restore -> perform { repository.restoreOccurrence(action.routineId, action.date) }
            is TimelineAction.ResetOverride -> perform { repository.resetOverride(action.item.routineBlockId, action.item.occurrenceDate) }
            is TimelineAction.RequestDelete -> panels.update { it.copy(pendingDelete = action.item) }
            TimelineAction.DismissDelete -> panels.update { it.copy(pendingDelete = null) }
            TimelineAction.ConfirmDelete -> panels.value.pendingDelete?.let { target -> perform {
                when (target) {
                    is ResolvedTimelineItem.Block -> repository.deleteRoutine(target.routineBlockId)
                    is ResolvedTimelineItem.Milestone -> repository.deleteMilestone(target.milestoneId)
                }
                panels.update { it.copy(pendingDelete = null) }
                messages.send("Deleted")
            } }
            is TimelineAction.EditSubject -> panels.update {
                it.copy(editingSubject = action.subject ?: Subject(name = "", colorHex = 0xFF6478C8, defaultDurationMinutes = 45))
            }
            TimelineAction.CloseSubjectEditor -> if (!panels.value.isSaving) panels.update { it.copy(editingSubject = null) }
            is TimelineAction.SaveSubject -> perform {
                repository.saveSubject(action.subject)
                panels.update { it.copy(editingSubject = null) }
            }
            is TimelineAction.DeleteSubject -> perform { repository.deleteSubject(action.id) }
            is TimelineAction.SetMute -> perform { settings.setMuteDuringSchoolHours(action.muted) }
            is TimelineAction.SetSchoolWindow -> perform {
                settings.setSchoolWindow(action.start, action.end)
                messages.send("Quiet window saved")
            }
            is TimelineAction.SetTeachingEnd -> perform {
                settings.setTeachingEndDate(action.date)
                messages.send("School-year end date saved")
            }
        }
    }

    private fun saveEntry(draft: EntryDraft) = perform {
        when (draft.kind) {
            EntryKind.BLOCK -> repository.saveRoutine(RoutineBlueprint(
                subjectId = draft.subjectId, title = draft.title, category = draft.category,
                dayOfWeek = draft.date.dayOfWeek, startTime = requireNotNull(draft.start), endTime = requireNotNull(draft.end),
                isNotificationEnabled = draft.notificationsEnabled, validFrom = draft.date,
                validUntil = if (draft.repeatWeekly) null else draft.date,
            ))
            EntryKind.DEADLINE, EntryKind.EXAM -> repository.saveMilestone(Milestone(
                id = draft.existingMilestoneId, subjectId = draft.subjectId, title = draft.title,
                dueDate = draft.date, dueTime = draft.start, isExam = draft.kind == EntryKind.EXAM,
                isCompleted = draft.isCompleted,
            ))
        }
        savedState["date"] = draft.date.toEpochDay()
        panels.update { it.copy(showAdd = false, editingMilestone = null) }
        messages.send(if (draft.existingMilestoneId == 0L) "Added to your schedule" else "Milestone updated")
    }

    private fun perform(operation: suspend () -> Unit) {
        if (!operationLock.tryLock()) return
        panels.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try { operation() }
            catch (error: CancellationException) { throw error }
            catch (error: IllegalArgumentException) { messages.send(error.message ?: "Please check the entered values.") }
            catch (_: Exception) { messages.send("Could not save this change. Check the entry and try again; existing data was not cleared.") }
            finally {
                panels.update { it.copy(isSaving = false) }
                operationLock.unlock()
            }
        }
    }

    companion object {
        fun range(date: LocalDate, mode: TimelineMode): Pair<LocalDate, LocalDate> = when (mode) {
            TimelineMode.DAY -> date to date
            TimelineMode.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(6) }
            TimelineMode.MONTH -> YearMonth.from(date).atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(41) }
            TimelineMode.YEAR -> LocalDate.of(if (date.monthValue >= 9) date.year else date.year - 1, 9, 1).let { it to it.plusYears(1).minusDays(1) }
        }
    }
}
