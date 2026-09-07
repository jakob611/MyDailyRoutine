package com.example.mydailyroutine.core.database.entities

import androidx.room.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.CancellationReason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "milestones", foreignKeys = [ForeignKey(entity = SubjectEntity::class,
    parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("subjectId"), Index(value = ["dueDate", "dueTime"])])
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long?, val title: String,
    // Preserve the existing indexed SQLite column; its representation has always been epoch day.
    @ColumnInfo(name = "dueDate") val targetDeadlineEpochDay: Long,
    val dueTime: LocalTime?, val isExam: Boolean, val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "0.0") val estimatedEffortHours: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val isTerminalExam: Boolean = false,
)
