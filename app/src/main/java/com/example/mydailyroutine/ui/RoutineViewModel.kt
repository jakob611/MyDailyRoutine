package com.example.mydailyroutine.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mydailyroutine.data.TimelineRepository
import com.example.mydailyroutine.notifications.ScheduleAlarmScheduler
import com.example.mydailyroutine.data.local.MilestoneEntity
import com.example.mydailyroutine.data.local.RoutineBlockEntity
import com.example.mydailyroutine.data.local.SubjectEntity
import com.example.mydailyroutine.domain.HealthWarning
import com.example.mydailyroutine.domain.ResolvedTimelineItem
import com.example.mydailyroutine.domain.RoutineCategory
import com.example.mydailyroutine.domain.ScheduleHealthEngine
import com.example.mydailyroutine.domain.YearOverview
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoutineViewModel(
    private val repository: TimelineRepository,
    private val alarmScheduler: ScheduleAlarmScheduler? = null,
) : ViewModel() {
    private val engine = ScheduleHealthEngine()
    private val mutableSelectedDate = MutableStateFlow(LocalDate.now())
    private val mutableViewMode = MutableStateFlow(ViewMode.DAY)
    private val mutableCompleted = MutableStateFlow<Set<String>>(emptySet())

    val selectedDate: StateFlow<LocalDate> = mutableSelectedDate.asStateFlow()
    val viewMode: StateFlow<ViewMode> = mutableViewMode.asStateFlow()
    val completedIds: StateFlow<Set<String>> = mutableCompleted.asStateFlow()

    val timeline: StateFlow<List<ResolvedTimelineItem>> = mutableSelectedDate
        .flatMapLatest(repository::getTimelineForDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val warnings: StateFlow<List<HealthWarning>> = timeline
        .map(engine::evaluate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects: StateFlow<List<SubjectEntity>> = repository.observeSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthMarkers = mutableSelectedDate
        .flatMapLatest(repository::observeMonthMarkers)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.example.mydailyroutine.domain.MonthMarkers(emptySet(), emptySet(), emptySet()))

    val yearOverview: StateFlow<YearOverview> = mutableSelectedDate
        .flatMapLatest(repository::observeYearOverview)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            YearOverview(LocalDate.now(), LocalDate.now(), 0, 0, emptyList()),
        )

    val week: StateFlow<List<DaySummary>> = mutableSelectedDate
        .map { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
        .distinctUntilChanged()
        .flatMapLatest { monday ->
            flow {
                emit(withContext(Dispatchers.IO) {
                    (0L..6L).map { offset ->
                        val date = monday.plusDays(offset)
                        DaySummary(date, repository.getTimelineForDate(date).firstValue())
                    }
                })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) { repository.initialize() }
    }

    fun setViewMode(mode: ViewMode) {
        mutableViewMode.value = mode
    }

    fun moveDate(days: Long) {
        mutableSelectedDate.value = mutableSelectedDate.value.plusDays(days)
    }

    fun selectDate(date: LocalDate) {
        mutableSelectedDate.value = date
    }

    fun toggleCompleted(item: ResolvedTimelineItem) {
        mutableCompleted.value = mutableCompleted.value.toMutableSet().apply {
            if (!add(item.stableId)) remove(item.stableId)
        }
    }

    fun addSubjectBlock(subject: SubjectEntity) {
        addRoutine(
            title = subject.name,
            category = RoutineCategory.FOCUS_STUDY,
            subjectId = subject.id,
            duration = subject.defaultDurationMinutes,
            notification = true,
        )
    }

    fun addPreset(preset: QuickPreset) {
        when (preset) {
            QuickPreset.DEEP_WORK -> addRoutine("90 min Deep Work", RoutineCategory.FOCUS_STUDY, null, 90, true)
            QuickPreset.POMODORO -> addRoutine("45 min Pomodoro", RoutineCategory.FOCUS_STUDY, null, 45, true)
            QuickPreset.WALK -> addRoutine("15 min Walk", RoutineCategory.REST_BREAK, null, 15, true)
            QuickPreset.IB_REVISION -> addRoutine("IB Revision", RoutineCategory.FOCUS_STUDY, null, 60, true)
            QuickPreset.EXAM -> viewModelScope.launch(Dispatchers.IO) {
                repository.addMilestone(
                    MilestoneEntity(
                        title = "Exam",
                        dueDate = mutableSelectedDate.value,
                        dueTime = suggestedStartTime(),
                        isExam = true,
                    ),
                )
            }
        }
    }

    private fun addRoutine(title: String, category: RoutineCategory, subjectId: Long?, duration: Int, notification: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val start = suggestedStartTime()
            val safeDuration = duration.coerceIn(5, 240)
            val end = start.plusMinutes(safeDuration.toLong()).coerceAtMost(LocalTime.of(23, 59))
            if (end.isAfter(start)) {
                repository.addRoutineBlock(
                    RoutineBlockEntity(
                        subjectId = subjectId,
                        title = title,
                        category = category,
                        dayOfWeek = mutableSelectedDate.value.dayOfWeek,
                        startTime = start,
                        endTime = end,
                        isNotificationEnabled = notification,
                    ),
                )
                alarmScheduler?.scheduleUpcoming(daysAhead = 7)
            }
        }
    }

    private fun suggestedStartTime(): LocalTime {
        val now = if (mutableSelectedDate.value == LocalDate.now()) LocalTime.now() else LocalTime.of(16, 0)
        val roundedMinute = ((now.minute + 14) / 15) * 15
        return if (roundedMinute >= 60) {
            if (now.hour >= 23) LocalTime.of(23, 45) else now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
        } else {
            now.withMinute(roundedMinute).withSecond(0).withNano(0)
        }
    }

    enum class QuickPreset(val label: String) {
        DEEP_WORK("90 min Deep Work"),
        POMODORO("45 min Pomodoro"),
        WALK("15 min Walk"),
        IB_REVISION("IB Revision"),
        EXAM("Exam"),
    }
}

@Immutable
data class DaySummary(val date: LocalDate, val items: List<ResolvedTimelineItem>)

enum class ViewMode(val label: String) {
    DAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
}

private suspend fun <T> Flow<T>.firstValue(): T = first()

class RoutineViewModelFactory(
    private val repository: TimelineRepository,
    private val alarmScheduler: ScheduleAlarmScheduler? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RoutineViewModel::class.java))
        return RoutineViewModel(repository, alarmScheduler) as T
    }
}
