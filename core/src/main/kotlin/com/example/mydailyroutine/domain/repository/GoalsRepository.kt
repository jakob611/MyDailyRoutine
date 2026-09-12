package com.example.mydailyroutine.domain.repository

import com.example.mydailyroutine.domain.model.GoalActivity
import com.example.mydailyroutine.domain.model.GoalMilestone
import com.example.mydailyroutine.domain.model.GoalProgress
import com.example.mydailyroutine.domain.model.GoalsProject
import kotlinx.coroutines.flow.Flow

/** Long-term CAS/EE plans: projects, activities, milestones and quick progress logs. */
interface GoalsRepository {
    val projects: Flow<List<GoalsProject>>
    val activities: Flow<List<GoalActivity>>
    val milestones: Flow<List<GoalMilestone>>
    val progress: Flow<List<GoalProgress>>
    suspend fun saveProject(project: GoalsProject): Long
    suspend fun deleteProject(id: Long)
    suspend fun saveActivity(activity: GoalActivity): Long
    suspend fun deleteActivity(id: Long)
    suspend fun setActivityScheduled(id: Long, scheduled: Boolean)
    suspend fun saveMilestone(milestone: GoalMilestone): Long
    suspend fun toggleMilestone(id: Long)
    suspend fun deleteMilestone(id: Long)
    suspend fun addProgress(entry: GoalProgress)
    suspend fun deleteProgress(id: Long)
}
