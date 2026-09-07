package com.example.mydailyroutine.domain.repository

import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.WarningType
import com.example.mydailyroutine.domain.health.RecoveryResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

interface TimelineRepository {
    fun getTimelineForDate(date: LocalDate): Flow<List<ResolvedTimelineItem>>
    fun observeSnapshot(from: LocalDate, through: LocalDate): Flow<ScheduleSnapshot>
    suspend fun snapshot(from: LocalDate, through: LocalDate): ScheduleSnapshot
    suspend fun saveSubject(subject: Subject): Long
    suspend fun deleteSubject(id: Long)
    suspend fun saveRoutine(routine: RoutineBlueprint): Long
    suspend fun deleteRoutine(id: Long)
    suspend fun setNotificationEnabled(routineId: Long, enabled: Boolean)
    suspend fun saveOverride(override: EventOverride)
    suspend fun cancelOccurrence(routineId: Long, date: LocalDate)
    suspend fun restoreOccurrence(routineId: Long, date: LocalDate)
    suspend fun resetOverride(routineId: Long, date: LocalDate)
    suspend fun editBlock(routineId: Long, date: LocalDate, title: String, start: LocalTime, end: LocalTime, wholeTemplate: Boolean)
    suspend fun setCompleted(routineId: Long, date: LocalDate, completed: Boolean, actualMinutes: Int? = null, actualStartedAt: java.time.LocalDateTime? = null)
    suspend fun saveMilestone(milestone: Milestone): Long
    suspend fun setMilestoneCompleted(id: Long, completed: Boolean)
    suspend fun deleteMilestone(id: Long)
    suspend fun insertRecovery(date: LocalDate, type: WarningType, anchorKey: String, config: HealthConfig,
        recoveryTitle: String, continuationSuffix: String): RecoveryResult
}

interface PreferencesRepository {
    val preferences: Flow<SchedulePreferences>
    suspend fun setMuteDuringSchoolHours(muted: Boolean)
    suspend fun setSchoolWindow(start: LocalTime, end: LocalTime)
    suspend fun setTeachingEndDate(date: LocalDate)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setAutomaticHealingEnabled(enabled: Boolean)
    suspend fun setHealthConfig(config: HealthConfig)
    suspend fun setPlanningConfig(config: com.example.mydailyroutine.domain.planning.PlanningConfig)
}

/** Opt-in sample import, separate from application/database initialization. */
interface ExampleDataRepository {
    val isLoaded: Flow<Boolean>
    suspend fun load(): Boolean
}
