package com.example.mydailyroutine.domain.repository

import com.example.mydailyroutine.domain.learning.*
import com.example.mydailyroutine.domain.model.Milestone
import com.example.mydailyroutine.domain.model.Task
import com.example.mydailyroutine.domain.planning.*
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface PlanningRepository {
    val backlog: Flow<List<BacklogEntry>>
    val history: Flow<List<HistoricalVelocity>>
    val topics: Flow<List<StudyTopic>>
    val milestones: Flow<List<Milestone>>
    val tasks: Flow<List<Task>>
    suspend fun saveTask(task: Task): Long
    suspend fun toggleTask(id: Long)
    suspend fun deleteTask(id: Long)
    suspend fun clearCompletedTasks()
    suspend fun getCalibratedDuration(rawMinutes: Int, subjectId: String): Int
    suspend fun autoHeal(date: LocalDate, actualStartMinutes: Int, delayMinutes: Int, config: PlanningConfig, excludedOccurrenceKey: String? = null): HealingReport
    suspend fun ensureReserve(date: LocalDate, config: PlanningConfig, title: String): Int
    suspend fun scheduleBacklog(id: Long, date: LocalDate, config: PlanningConfig): PlacementResult
    suspend fun deleteBacklog(id: Long)
    suspend fun createTopic(topic: StudyTopic, config: PlanningConfig, reviewTitlePattern: String): PlanSummary
    suspend fun deleteTopic(id: Long)
    suspend fun planMilestone(id: Long, initialDate: LocalDate, config: PlanningConfig, preparationTitlePattern: String, reserveTitle: String, category: com.example.mydailyroutine.domain.model.RoutineCategory = com.example.mydailyroutine.domain.model.RoutineCategory.FOCUS_ANALYTICAL, stages: List<PreparationStage> = emptyList()): PlanSummary
}
