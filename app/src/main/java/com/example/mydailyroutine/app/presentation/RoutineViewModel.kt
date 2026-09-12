package com.example.mydailyroutine.app.presentation

import com.example.mydailyroutine.core.presentation.*

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.health.*
import com.example.mydailyroutine.domain.presets.PresetFactory
import com.example.mydailyroutine.domain.repository.PlanningRepository
import com.example.mydailyroutine.domain.repository.GoalsRepository
import com.example.mydailyroutine.domain.model.nominalMinutes
import com.example.mydailyroutine.domain.routines.*
import com.example.mydailyroutine.domain.repository.ExampleDataRepository
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.domain.health.ScheduleMetrics
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.repository.PreferencesRepository
import com.example.mydailyroutine.domain.repository.TimelineRepository
import com.example.mydailyroutine.domain.repository.ScheduleBackupRepository
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
class RoutineViewModel(
    private val repository: TimelineRepository,
    private val settings: PreferencesRepository,
    private val savedState: SavedStateHandle,
    private val exampleData: ExampleDataRepository,
    private val planningRepository: PlanningRepository,
    private val executionRepository: com.example.mydailyroutine.domain.execution.ExecutionRepository,
    private val patternsRepository: RoutinePatternsRepository,
    private val backup: ScheduleBackupRepository,
    private val goals: GoalsRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
    private val initialDate = today()
    private val selectedDate = savedState.getStateFlow("date", initialDate.toEpochDay())
    private val selectedMode = savedState.getStateFlow("mode", TimelineMode.DAY.name)
    private val retry = MutableStateFlow(0)
    private val panels = MutableStateFlow(TimelinePanels())
    private val operationLock = Mutex()
    private val messages = Channel<TimelineEffect>(Channel.BUFFERED)
    val effects: Flow<TimelineEffect> = messages.receiveAsFlow()
    private val resolver = TimelineResolver()
    private val healthEngine = ScheduleHealthEngine()

    private val content = combine(selectedDate, selectedMode, retry) { epoch, mode, _ ->
        LocalDate.ofEpochDay(epoch) to TimelineMode.valueOf(mode)
    }.flatMapLatest { (date, mode) ->
        val (from, through) = PeriodRanges.range(date, mode)
        combine(repository.observeSnapshot(from, through), settings.preferences.map { it.health to it.periodicBreak }.distinctUntilChanged()) { snapshot, (health, periodic) ->
            withContext(Dispatchers.Default) { resolveContent(date, mode, snapshot, health, periodic) }
        }
            .onStart { emit(TimelineContent(date, mode)) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(TimelineContent(date, mode, isLoading = false, error = R.string.error_load))
            }
    }

    private val planning = combine(planningRepository.backlog, planningRepository.history, planningRepository.topics, planningRepository.milestones, planningRepository.tasks) { backlog, history, topics, milestones, tasks ->
        PlanningUiState(backlog.toPersistentList(), history.toPersistentList(), topics.toPersistentList(), milestones.toPersistentList(), tasks.toPersistentList())
    }.catch { error -> if (error is CancellationException) throw error else emit(PlanningUiState()) }
    private val goalsState = combine(goals.projects, goals.activities, goals.milestones, goals.progress) { projects, activities, milestones, progress ->
        GoalsUiState(projects.toPersistentList(), activities.toPersistentList(), milestones.toPersistentList(), progress.toPersistentList())
    }.catch { error -> if (error is CancellationException) throw error else emit(GoalsUiState()) }
    val state: StateFlow<TimelineUiState> = combine(content, settings.preferences, panels, exampleData.isLoaded, planning) { data, preferences, panels, loaded, planning ->
        TimelineUiState(data, preferences, panels, loaded, planning)
    }.combine(executionRepository.active) { state, active -> state.copy(execution = active) }
        .combine(patternsRepository.sleep) { state, sleep -> state.copy(sleep = sleep) }
        .combine(goalsState) { state, goals -> state.copy(goals = goals) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState(TimelineContent(initialDate)))

    private suspend fun resolveContent(date: LocalDate, mode: TimelineMode, snapshot: ScheduleSnapshot, config: HealthConfig, periodic: PeriodicBreakConfig = PeriodicBreakConfig()): TimelineContent {
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
                warnings = healthEngine.evaluate(items, config, periodic).map {
                    WarningUi(it.type, it.relatedItemKeys.toPersistentSet(), it.atMinute, RecoveryPlanner().recommendedMinutes(it, config, items))
                }.toPersistentList(),
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
            subjectPresets = PresetFactory.forSubjects(snapshot.subjects).toPersistentList(),
            calendar = snapshot.calendar.filter { it.date in snapshot.from..snapshot.through }.toPersistentList(),
            milestones = snapshot.milestones.sortedWith(compareBy<Milestone> { it.dueDate }.thenBy { it.dueTime ?: java.time.LocalTime.MIN }).toPersistentList(),
            // Due-dated tasks reuse the marker display without touching milestone storage.
            taskMarkers = snapshot.tasks.mapNotNull { task ->
                task.dueDate?.let { due -> Milestone(id = task.id, subjectId = task.subjectId, title = task.title, dueDate = due, dueTime = null, isExam = false, isCompleted = task.completedAtEpochMillis != null) }
            }.sortedWith(compareBy<Milestone> { it.dueDate }).toPersistentList(),
        )
    }

    fun onAction(action: TimelineAction) {
        when (action) {
            is TimelineAction.SelectDate -> {
                savedState["date"] = action.date.coerceIn(ScheduleValidation.firstUiDate, ScheduleValidation.lastUiDate).toEpochDay()
                if (action.openDay) {
                    savedState["mode"] = TimelineMode.DAY.name
                    panels.update { it.copy(showAdd = false, showSettings = false, editingBlock = null,
                        editingMilestone = null, editingSubject = null, pendingDelete = null, confirmDemo = false, showPlanning = false, showTopicEditor = false, completionTarget = null, showTasks = false, entryPrefillTitle = null) }
                }
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
            is TimelineAction.StartExecution -> perform {
                executionRepository.start(action.block.routineBlockId, action.block.occurrenceDate, state.value.preferences.planning, state.value.preferences.automaticHealingEnabled)
            }
            TimelineAction.FinishExecution -> perform {
                executionRepository.finish(state.value.preferences.planning, state.value.preferences.automaticHealingEnabled)
                messages.send(TimelineEffect.Completed)
            }
            TimelineAction.RequestCancelExecution -> panels.update { it.copy(confirmCancelExecution = true) }
            TimelineAction.DismissCancelExecution -> panels.update { it.copy(confirmCancelExecution = false) }
            TimelineAction.CancelExecution -> perform { executionRepository.cancel(); panels.update { it.copy(confirmCancelExecution = false) } }
            is TimelineAction.SetAutomaticHealing -> perform { settings.setAutomaticHealingEnabled(action.enabled) }
            TimelineAction.SyncExecution -> if (!panels.value.isSaving && state.value.execution != null) viewModelScope.launch {
                try {
                    if (executionRepository.synchronize(state.value.preferences.planning, state.value.preferences.automaticHealingEnabled))
                        messages.send(TimelineEffect.Message(R.string.execution_boundary))
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) { messages.send(TimelineEffect.Message(R.string.error_save)) }
            }
            TimelineAction.OpenPlanning -> panels.update { it.copy(showPlanning = true, showAdd = false, showSettings = false) }
            TimelineAction.ClosePlanning -> if (!panels.value.isSaving) panels.update { it.copy(showPlanning = false) }
            TimelineAction.NewTopic -> panels.update { it.copy(showTopicEditor = true) }
            TimelineAction.CloseTopic -> if (!panels.value.isSaving) panels.update { it.copy(showTopicEditor = false) }
            TimelineAction.CloseActual -> if (!panels.value.isSaving) panels.update { it.copy(completionTarget = null) }
            is TimelineAction.RecordActual -> perform {
                executionRepository.record(action.item, action.minutes, state.value.preferences.planning, state.value.preferences.automaticHealingEnabled)
                panels.update { it.copy(completionTarget = null) }
                messages.send(TimelineEffect.Completed)
            }
            is TimelineAction.AutoHeal -> perform {
                val result = planningRepository.autoHeal(action.date, action.actualStartMinutes, state.value.preferences.planning.defaultSlipMinutes, state.value.preferences.planning)
                messages.send(TimelineEffect.Message(R.string.healed_summary, result.bufferUsedMinutes + result.compressionMinutes + result.slackUsedMinutes, result.deferredCount))
            }
            is TimelineAction.AddReserve -> perform {
                val minutes = planningRepository.ensureReserve(action.date, state.value.preferences.planning, action.title)
                messages.send(TimelineEffect.Message(R.string.reserve_added, minutes))
            }
            is TimelineAction.ScheduleBacklog -> perform {
                val result = planningRepository.scheduleBacklog(action.id, action.date, state.value.preferences.planning)
                if (result.placed) savedState["date"] = action.date.toEpochDay()
                messages.send(TimelineEffect.Message(when {
                    result.placed -> R.string.backlog_scheduled
                    result.failure == com.example.mydailyroutine.domain.planning.PlacementFailure.DEADLINE -> R.string.backlog_deadline
                    result.failure == com.example.mydailyroutine.domain.planning.PlacementFailure.DEPENDENCY -> R.string.backlog_dependency
                    else -> R.string.backlog_no_space
                }))
            }
            is TimelineAction.DeleteBacklog -> perform { planningRepository.deleteBacklog(action.id) }
            is TimelineAction.SaveTopic -> perform {
                val result = planningRepository.createTopic(action.topic, state.value.preferences.planning, action.titlePattern)
                panels.update { it.copy(showTopicEditor = false) }
                messages.send(TimelineEffect.Message(R.string.review_plan_summary, result.reviews, result.deferredMinutes))
            }
            is TimelineAction.DeleteTopic -> perform { planningRepository.deleteTopic(action.id) }
            is TimelineAction.PlanMilestone -> perform {
                val result = planningRepository.planMilestone(action.id, action.initialDate, state.value.preferences.planning, action.titlePattern, action.reserveTitle, action.category, action.stages)
                messages.send(TimelineEffect.Message(if (result.alreadyPlanned) R.string.plan_exists else R.string.preparation_summary, result.studyMinutes, result.deferredMinutes))
                if (result.unreservedMinutes > 0) messages.send(TimelineEffect.Message(R.string.reserve_shortfall, result.unreservedMinutes))
            }
            is TimelineAction.SetPlanningConfig -> perform {
                settings.setPlanningConfig(action.config)
                messages.send(TimelineEffect.Message(R.string.planning_saved))
            }
            TimelineAction.OpenAdd -> panels.update { it.copy(showAdd = true, addSession = it.addSession + 1, entryContinuation = null, entryPrefillTitle = null,
                showSettings = false, editingMilestone = null, editingBlock = null, editingSubject = null, pendingDelete = null, confirmDemo = false, showPlanning = false) }
            TimelineAction.OpenSettings -> panels.update { it.copy(showSettings = true, showAdd = false, showPlanning = false, editingBlock = null, editingMilestone = null) }
            TimelineAction.CloseAdd -> if (!panels.value.isSaving) panels.update { it.copy(showAdd = false, editingMilestone = null) }
            TimelineAction.CloseSettings -> if (!panels.value.isSaving) panels.update { it.copy(showSettings = false) }
            TimelineAction.CloseEditor -> if (!panels.value.isSaving) panels.update { it.copy(editingBlock = null) }
            is TimelineAction.Edit -> when (val item = action.item) {
                is ResolvedTimelineItem.Block -> panels.update { it.copy(editingBlock = item) }
                is ResolvedTimelineItem.Milestone -> panels.update { it.copy(editingMilestone = item, showAdd = true, addSession = it.addSession + 1, entryContinuation = null, entryPrefillTitle = null, showSettings = false) }
            }
            is TimelineAction.SaveEntry -> saveEntry(action.draft)
            is TimelineAction.SaveBlockEdit -> perform {
                val series = action.item.seriesKey
                if (action.wholeTemplate && action.allSeriesDays && series != null) patternsRepository.editSeries(series, action.title, action.start, action.end)
                else repository.editBlock(action.item.routineBlockId, action.item.occurrenceDate, action.title, action.start, action.end, action.wholeTemplate)
                panels.update { it.copy(editingBlock = null) }
                messages.send(TimelineEffect.Message(R.string.message_updated))
            }
            is TimelineAction.ToggleComplete -> if (action.item is ResolvedTimelineItem.Block && !action.item.isCompleted &&
                state.value.execution?.let { it.routineBlockId == action.item.routineBlockId && it.occurrenceDate == action.item.occurrenceDate } == true) {
                perform {
                    executionRepository.finish(state.value.preferences.planning, state.value.preferences.automaticHealingEnabled)
                    messages.send(TimelineEffect.Completed)
                }
            } else if (action.item is ResolvedTimelineItem.Block && action.item.category.isDeepWork && !action.item.isCompleted) {
                panels.update { it.copy(completionTarget = action.item) }
            } else perform {
                when (val item = action.item) {
                    is ResolvedTimelineItem.Block -> repository.setCompleted(item.routineBlockId, item.occurrenceDate, !item.isCompleted)
                    is ResolvedTimelineItem.Milestone -> repository.setMilestoneCompleted(item.milestoneId, !item.isCompleted)
                }
                if (!action.item.isCompleted) messages.send(TimelineEffect.Completed)
            }
            is TimelineAction.SetReminder -> perform { repository.setNotificationEnabled(action.routineId, action.enabled) }
            is TimelineAction.Skip -> perform {
                repository.cancelOccurrence(action.item.routineBlockId, action.item.occurrenceDate)
                messages.send(TimelineEffect.Message(R.string.message_skipped))
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
                messages.send(TimelineEffect.Message(R.string.message_deleted))
            } }
            is TimelineAction.EditSubject -> panels.update {
                it.copy(editingSubject = action.subject ?: Subject(name = "", colorHex = RoutineColors.subjectSwatches.first(), defaultDurationMinutes = state.value.preferences.entryDefaults.lessonDurationMinutes))
            }
            TimelineAction.CloseSubjectEditor -> if (!panels.value.isSaving) panels.update { it.copy(editingSubject = null) }
            is TimelineAction.SaveSubject -> perform {
                repository.saveSubject(action.subject)
                panels.update { it.copy(editingSubject = null) }
            }
            is TimelineAction.DeleteSubject -> perform { repository.deleteSubject(action.id) }
            is TimelineAction.SetHaptics -> perform { settings.setHapticsEnabled(action.enabled) }
            is TimelineAction.SetHealthConfig -> perform {
                settings.setHealthConfig(action.config)
                messages.send(TimelineEffect.Message(R.string.message_thresholds_saved))
            }
            is TimelineAction.SetPeriodicBreak -> perform {
                settings.setPeriodicBreak(action.config)
            }
            is TimelineAction.ExportSchedule -> perform {
                val json = backup.exportJson()
                panels.update { it.copy(exportJson = json) }
            }
            is TimelineAction.ImportSchedule -> perform {
                backup.importJson(action.json)
                panels.update { it.copy(exportJson = null) }
                messages.send(TimelineEffect.Message(R.string.message_imported))
            }
            TimelineAction.RequestDemo -> panels.update { it.copy(confirmDemo = true) }
            TimelineAction.DismissDemo -> if (!panels.value.isSaving) panels.update { it.copy(confirmDemo = false) }
            TimelineAction.LoadDemo -> perform {
                val loaded = exampleData.load()
                panels.update { it.copy(confirmDemo = false, showSettings = false) }
                if (loaded) {
                    savedState["date"] = LocalDate.of(2026, 9, 7).toEpochDay()
                    savedState["mode"] = TimelineMode.DAY.name
                }
                messages.send(TimelineEffect.Message(if (loaded) R.string.demo_loaded_message else R.string.demo_already_message))
            }
            is TimelineAction.InsertRecovery -> perform {
                val result = repository.insertRecovery(LocalDate.ofEpochDay(selectedDate.value), action.type, action.anchorKey,
                    state.value.preferences.health, action.recoveryTitle, action.continuationSuffix)
                result.date?.let { savedState["date"] = it.toEpochDay() }
                val text = when (result.status) {
                    RecoveryStatus.INSERTED -> R.string.recovery_inserted
                    RecoveryStatus.FOCUS_SPLIT -> R.string.recovery_split
                    RecoveryStatus.FOCUS_MOVED -> R.string.recovery_moved
                    RecoveryStatus.ALREADY_HANDLED -> R.string.recovery_already
                    RecoveryStatus.NO_SPACE -> R.string.recovery_no_space
                }
                messages.send(TimelineEffect.Message(text, result.minutes.takeIf { it > 0 }))
            }
            is TimelineAction.DeleteSeries -> perform {
                patternsRepository.deleteSeries(action.key)
                panels.update { it.copy(pendingDelete = null) }
                messages.send(TimelineEffect.Message(R.string.message_deleted))
            }
            is TimelineAction.SaveSleep -> perform {
                patternsRepository.saveSleep(action.value, action.sleepTitle, action.morningTitle)
                messages.send(TimelineEffect.Message(R.string.sleep_saved))
            }
            is TimelineAction.SaveEntryDefaults -> perform {
                settings.setEntryDefaults(action.defaults)
                messages.send(TimelineEffect.Message(R.string.entry_defaults_saved))
            }
            is TimelineAction.SetMute -> perform { settings.setMuteDuringSchoolHours(action.muted) }
            is TimelineAction.SetSchoolWindow -> perform {
                settings.setSchoolWindow(action.start, action.end)
                messages.send(TimelineEffect.Message(R.string.message_quiet_saved))
            }
            is TimelineAction.SetTeachingEnd -> perform {
                settings.setTeachingEndDate(action.date)
                messages.send(TimelineEffect.Message(R.string.message_teaching_end_saved))
            }
            TimelineAction.OpenTasks -> panels.update {
                it.copy(showTasks = true, showAdd = false, showSettings = false, showPlanning = false, showTopicEditor = false, editingBlock = null, editingMilestone = null, editingSubject = null, pendingDelete = null)
            }
            TimelineAction.CloseTasks -> if (!panels.value.isSaving) panels.update { it.copy(showTasks = false) }
            is TimelineAction.AddTask -> perform {
                planningRepository.saveTask(Task(title = action.title, dueDate = action.dueDate, subjectId = action.subjectId, createdAtEpochMillis = System.currentTimeMillis()))
                messages.send(TimelineEffect.Message(R.string.tasks_added))
            }
            is TimelineAction.UpdateTask -> perform {
                planningRepository.saveTask(action.task)
                messages.send(TimelineEffect.Message(R.string.tasks_updated))
            }
            is TimelineAction.ToggleTask -> perform {
                val wasOpen = state.value.planning.tasks.any { it.id == action.id && it.completedAtEpochMillis == null }
                planningRepository.toggleTask(action.id)
                if (wasOpen) messages.send(TimelineEffect.Completed)
            }
            is TimelineAction.DeleteTask -> perform {
                planningRepository.deleteTask(action.id)
                messages.send(TimelineEffect.Message(R.string.tasks_deleted))
            }
            TimelineAction.ClearCompletedTasks -> perform { planningRepository.clearCompletedTasks() }
            is TimelineAction.TaskToSchedule -> {
                action.task.dueDate?.takeIf { !it.isBefore(today()) }?.let { savedState["date"] = it.toEpochDay() }
                panels.update {
                    it.copy(showAdd = true, addSession = it.addSession + 1, entryPrefillTitle = action.task.title, entryContinuation = null,
                        showTasks = false, showSettings = false, showPlanning = false, editingBlock = null, editingMilestone = null, editingSubject = null, pendingDelete = null, confirmDemo = false)
                }
            }
            TimelineAction.OpenGoals -> panels.update {
                it.copy(showGoals = true, showSettings = false, showPlanning = false, showAdd = false, editingBlock = null, editingMilestone = null)
            }
            TimelineAction.CloseGoals -> if (!panels.value.isSaving) panels.update { it.copy(showGoals = false) }
            is TimelineAction.SaveGoalsProject -> perform {
                goals.saveProject(action.project)
                messages.send(TimelineEffect.Message(R.string.goals_saved))
            }
            is TimelineAction.DeleteGoalsProject -> perform {
                goals.deleteProject(action.id)
                messages.send(TimelineEffect.Message(R.string.goals_deleted))
            }
            is TimelineAction.SaveGoalActivity -> perform {
                goals.saveActivity(action.activity)
                messages.send(TimelineEffect.Message(R.string.goals_saved))
            }
            is TimelineAction.DeleteGoalActivity -> perform { goals.deleteActivity(action.id) }
            is TimelineAction.SaveGoalMilestone -> perform {
                goals.saveMilestone(action.milestone)
                messages.send(TimelineEffect.Message(R.string.goals_saved))
            }
            is TimelineAction.ToggleGoalMilestone -> perform {
                val wasOpen = state.value.goals.milestones.any { it.id == action.id && !it.isDone }
                goals.toggleMilestone(action.id)
                if (wasOpen) messages.send(TimelineEffect.Completed)
            }
            is TimelineAction.ToggleGoalActivity -> {
                val current = state.value.goals.activities.firstOrNull { it.id == action.id }
                if (current != null) perform {
                    goals.saveActivity(current.copy(isDone = !current.isDone))
                    if (!current.isDone) messages.send(TimelineEffect.Completed)
                }
            }
            is TimelineAction.DeleteGoalMilestone -> perform { goals.deleteMilestone(action.id) }
            is TimelineAction.AddGoalProgress -> perform {
                goals.addProgress(action.entry)
                messages.send(TimelineEffect.Message(R.string.goals_saved))
            }
            is TimelineAction.DeleteGoalProgress -> perform { goals.deleteProgress(action.id) }
            is TimelineAction.GoalActivityToSchedule -> perform {
                goals.setActivityScheduled(action.activity.id, true)
                action.activity.start.takeIf { !it.isBefore(today()) }?.let { savedState["date"] = it.toEpochDay() }
                panels.update {
                    it.copy(showAdd = true, addSession = it.addSession + 1, entryPrefillTitle = action.activity.title, entryContinuation = null,
                        showTasks = false, showSettings = false, showPlanning = false, editingBlock = null, editingMilestone = null, editingSubject = null, pendingDelete = null, confirmDemo = false)
                }
            }
            is TimelineAction.SeedGoalProject -> perform { seedProject(action) }
        }
    }

    /** Pre-fills the official CAS/EE structure; every date stays user-editable afterwards. */
    private suspend fun seedProject(action: TimelineAction.SeedGoalProject) {
        if (goals.projects.first().any { it.kind == action.kind }) {
            messages.send(TimelineEffect.Message(R.string.goals_seed_exists))
            return
        }
        val start = today()
        val end = if (action.kind == "EE") start.plusMonths(17) else start.plusMonths(20)
        val projectId = goals.saveProject(GoalsProject(name = action.projectName, kind = action.kind, start = start, end = end,
            targetHours = if (action.kind == "CAS") 150.0 else null, targetWords = if (action.kind == "EE") 4000 else null))
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end)
        fun at(fraction: Double): LocalDate = start.plusDays((totalDays * fraction).toLong())
        if (action.kind == "EE") {
            val ranges = listOf(0.0 to 0.12, 0.12 to 0.24, 0.24 to 0.53, 0.53 to 0.82, 0.82 to 0.94, 0.94 to 1.0)
            action.activityNames.take(6).zip(ranges).forEach { (name, range) ->
                goals.saveActivity(GoalActivity(projectId = projectId, title = name, category = "STAGE", start = at(range.first), end = at(range.second)))
            }
            val dates = listOf(at(0.24), at(0.94), end.minusDays(14), end.minusDays(7), end)
            action.milestoneNames.take(dates.size).zip(dates).forEach { (name, date) ->
                goals.saveMilestone(GoalMilestone(projectId = projectId, title = name, dueDate = date))
            }
        } else {
            val dates = if (action.kind == "CAS") listOf(start.plusDays(30), at(0.5), end.minusDays(30), end) else listOf(start, end)
            action.milestoneNames.take(dates.size).zip(dates).forEach { (name, date) ->
                goals.saveMilestone(GoalMilestone(projectId = projectId, title = name, dueDate = date))
            }
        }
        panels.update { it.copy(showGoals = true) }
        messages.send(TimelineEffect.Message(R.string.goals_seeded))
    }

    private fun saveEntry(draft: EntryDraft) = perform {
        when (draft.kind) {
            EntryKind.BLOCK -> {
                val start = requireNotNull(draft.start)
                val raw = nominalMinutes(start, requireNotNull(draft.end))
                val fixed = draft.isFixedCommitment || (draft.category == RoutineCategory.SCHOOL || draft.category == RoutineCategory.REST_BUFFER)
                val calibrated = if (draft.calibrateDuration && draft.category.isDeepWork && !fixed && draft.subjectId != null)
                    planningRepository.getCalibratedDuration(raw, draft.subjectId.toString()) else raw
                val minimum = draft.minDurationMinutes ?: if (fixed) raw else if (draft.category == RoutineCategory.EMERGENCY_RESERVE) 0 else minOf(25, raw)
                val duration = maxOf(calibrated, minimum)
                val blueprint = RoutineBlueprint(subjectId = draft.subjectId, title = draft.title, category = draft.category,
                    dayOfWeek = draft.date.dayOfWeek, startTime = start, endTime = start.plusMinutes(duration.toLong()),
                    isNotificationEnabled = draft.notificationsEnabled && draft.category != RoutineCategory.EMERGENCY_RESERVE,
                    validFrom = draft.date, validUntil = if (draft.repeatWeekly) null else draft.date,
                    minDurationMinutes = if (fixed) duration else minimum, elasticity = if (fixed) 0.0 else draft.elasticity,
                    priorityWeight = draft.priorityWeight, isFixedCommitment = fixed, rawDurationMinutes = raw)
                val weekdays = Weekdays.fromMask(draft.repeatDaysMask).ifEmpty { setOf(draft.date.dayOfWeek) }
                patternsRepository.create(RoutinePatternRequest(blueprint,weekdays,draft.repeatWeekly,draft.afterLessonBreakMinutes,draft.breakTitle))
            }
            EntryKind.DEADLINE, EntryKind.EXAM -> repository.saveMilestone(Milestone(
                id = draft.existingMilestoneId, subjectId = draft.subjectId, title = draft.title,
                dueDate = draft.date, dueTime = draft.start, isExam = draft.kind == EntryKind.EXAM,
                isCompleted = draft.isCompleted, estimatedEffortHours = draft.estimatedEffortHours, isTerminalExam = draft.isTerminalExam,
            ))
        }
        if (draft.keepOpen && draft.kind == EntryKind.BLOCK && draft.category == RoutineCategory.SCHOOL) {
            val duration = nominalMinutes(requireNotNull(draft.start),requireNotNull(draft.end))
            val next = RoutinePatternExpander.followingStart(draft.date,draft.start,duration,draft.afterLessonBreakMinutes)
            val shift = java.time.temporal.ChronoUnit.DAYS.between(draft.date,next.toLocalDate()).toInt()
            val selected = draft.repeatDaysMask.takeIf { it != 0 } ?: Weekdays.mask(setOf(draft.date.dayOfWeek))
            savedState["date"] = next.toLocalDate().toEpochDay()
            panels.update { it.copy(showAdd=true,editingMilestone=null,addSession=it.addSession+1,
                entryContinuation=EntryContinuation(next,duration,draft.repeatWeekly,Weekdays.shifted(selected,shift),draft.afterLessonBreakMinutes)) }
        } else {
            val destination = if (draft.kind == EntryKind.BLOCK && draft.repeatWeekly)
                RoutinePatternExpander.firstOccurrence(draft.date, Weekdays.fromMask(draft.repeatDaysMask).ifEmpty { setOf(draft.date.dayOfWeek) }) else draft.date
            savedState["date"] = destination.toEpochDay()
            panels.update { it.copy(showAdd = false, editingMilestone = null, entryContinuation = null) }
        }
        messages.send(TimelineEffect.Message(if (draft.existingMilestoneId == 0L) R.string.message_added else R.string.message_milestone_saved))
    }

    private fun perform(operation: suspend () -> Unit) {
        if (!operationLock.tryLock()) return
        panels.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try { operation() }
            catch (error: CancellationException) { throw error }
            catch (conflict: ScheduleConflict) {
                messages.send(TimelineEffect.Message(when(conflict.reason) {
                    ScheduleConflictReason.ACTIVE_EXECUTION -> R.string.execution_finish_first
                    ScheduleConflictReason.PREVIOUS_STAGE -> R.string.stage_finish_first
                    ScheduleConflictReason.PROTECTED_TIME -> R.string.execution_protected_time
                    ScheduleConflictReason.CLOCK_CHANGED -> R.string.execution_clock_changed
                }))
            }
            catch (_: IllegalArgumentException) { messages.send(TimelineEffect.Message(R.string.error_values)) }
            catch (_: Exception) { messages.send(TimelineEffect.Message(R.string.error_save)) }
            finally {
                panels.update { it.copy(isSaving = false) }
                operationLock.unlock()
            }
        }
    }


}
