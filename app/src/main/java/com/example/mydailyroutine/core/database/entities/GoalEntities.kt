package com.example.mydailyroutine.core.database.entities

import androidx.room.*
import java.time.LocalDate

@Entity(tableName = "goals_project")
data class GoalsProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val kind: String,
    val startDate: LocalDate, val endDate: LocalDate,
    val targetHours: Double?, val targetWords: Int?,
)

@Entity(tableName = "goals_activity", foreignKeys = [ForeignKey(entity = GoalsProjectEntity::class,
    parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")])
data class GoalActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long, val title: String, val category: String?,
    val startDate: LocalDate, val endDate: LocalDate, val note: String?,
    val isCasProject: Boolean, val isDone: Boolean, val isScheduled: Boolean,
)

@Entity(tableName = "goals_milestone", foreignKeys = [ForeignKey(entity = GoalsProjectEntity::class,
    parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")])
data class GoalMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long, val title: String, val dueDate: LocalDate, val isDone: Boolean,
)

@Entity(tableName = "goals_progress", foreignKeys = [ForeignKey(entity = GoalsProjectEntity::class,
    parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = GoalActivityEntity::class,
    parentColumns = ["id"], childColumns = ["activityId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("activityId")])
data class GoalProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long, val activityId: Long?, val kind: String,
    val amount: Double, val note: String?, val logDate: LocalDate,
)
