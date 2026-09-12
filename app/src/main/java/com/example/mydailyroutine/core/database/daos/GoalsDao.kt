package com.example.mydailyroutine.core.database.daos

import androidx.room.*
import com.example.mydailyroutine.core.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalsDao {
    @Query("SELECT * FROM goals_project ORDER BY startDate, id") fun observeProjects(): Flow<List<GoalsProjectEntity>>
    @Query("SELECT * FROM goals_activity ORDER BY projectId, startDate, id") fun observeActivities(): Flow<List<GoalActivityEntity>>
    @Query("SELECT * FROM goals_milestone ORDER BY dueDate, id") fun observeMilestones(): Flow<List<GoalMilestoneEntity>>
    @Query("SELECT * FROM goals_progress ORDER BY logDate, id") fun observeProgress(): Flow<List<GoalProgressEntity>>
    @Query("SELECT * FROM goals_project WHERE id = :id") suspend fun getProject(id: Long): GoalsProjectEntity?
    @Query("SELECT * FROM goals_milestone WHERE id = :id") suspend fun getMilestone(id: Long): GoalMilestoneEntity?
    @Insert suspend fun insertProject(project: GoalsProjectEntity): Long
    @Update suspend fun updateProject(project: GoalsProjectEntity): Int
    @Query("DELETE FROM goals_project WHERE id = :id") suspend fun deleteProject(id: Long)
    @Insert suspend fun insertActivity(activity: GoalActivityEntity): Long
    @Update suspend fun updateActivity(activity: GoalActivityEntity): Int
    @Query("DELETE FROM goals_activity WHERE id = :id") suspend fun deleteActivity(id: Long)
    @Query("UPDATE goals_activity SET isScheduled = :scheduled WHERE id = :id") suspend fun setActivityScheduled(id: Long, scheduled: Boolean)
    @Insert suspend fun insertMilestone(milestone: GoalMilestoneEntity): Long
    @Update suspend fun updateMilestone(milestone: GoalMilestoneEntity): Int
    @Query("UPDATE goals_milestone SET isDone = :done WHERE id = :id") suspend fun setMilestoneDone(id: Long, done: Boolean)
    @Query("DELETE FROM goals_milestone WHERE id = :id") suspend fun deleteMilestone(id: Long)
    @Insert suspend fun insertProgress(entry: GoalProgressEntity): Long
    @Query("DELETE FROM goals_progress WHERE id = :id") suspend fun deleteProgress(id: Long)
}
