package com.example.mydailyroutine.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mydailyroutine.domain.RoutineCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: Long,
    val defaultDurationMinutes: Int = 45,
)

@Entity(
    tableName = "routine_blocks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["dayOfWeek", "startTime"]),
        Index(value = ["subjectId"]),
    ],
)
data class RoutineBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val title: String,
    val category: RoutineCategory,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isNotificationEnabled: Boolean = false,
)

@Entity(
    tableName = "event_overrides",
    foreignKeys = [
        ForeignKey(
            entity = RoutineBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineBlockId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["routineBlockId", "overrideDate"], unique = true),
        Index(value = ["overrideDate"]),
    ],
)
data class EventOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineBlockId: Long,
    val overrideDate: LocalDate,
    val isCancelled: Boolean = false,
    val customStartTime: LocalTime? = null,
    val customEndTime: LocalTime? = null,
    val customTitle: String? = null,
)

@Entity(
    tableName = "school_calendar_entries",
    indices = [Index(value = ["date"], unique = true)],
)
data class SchoolCalendarEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val isWorkFreeDay: Boolean,
)

@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["dueDate", "dueTime"]),
        Index(value = ["subjectId"]),
    ],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val title: String,
    val dueDate: LocalDate,
    val dueTime: LocalTime? = null,
    val isExam: Boolean = false,
)
