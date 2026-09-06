package com.example.mydailyroutine.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: Long,
    val defaultDurationMinutes: Int,
)

@Entity(
    tableName = "routine_blocks",
    foreignKeys = [ForeignKey(
        entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"],
        onDelete = ForeignKey.SET_NULL, onUpdate = ForeignKey.NO_ACTION,
    )],
    indices = [Index("subjectId"), Index(value = ["dayOfWeek", "startTime"])],
)
data class RoutineBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long?,
    val title: String,
    val category: RoutineCategory,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isNotificationEnabled: Boolean,
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
)

@Entity(
    tableName = "event_overrides",
    foreignKeys = [ForeignKey(
        entity = RoutineBlockEntity::class, parentColumns = ["id"], childColumns = ["routineBlockId"],
        onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.NO_ACTION,
    )],
    indices = [Index(value = ["routineBlockId", "overrideDate"], unique = true), Index("overrideDate")],
)
data class EventOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineBlockId: Long,
    val overrideDate: LocalDate,
    val isCancelled: Boolean,
    val customStartTime: LocalTime?,
    val customEndTime: LocalTime?,
    val customTitle: String?,
)

@Entity(
    tableName = "school_calendar",
    indices = [Index(value = ["date", "title"], unique = true), Index(value = ["date", "isWorkFreeDay"])],
)
data class SchoolCalendarEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val isWorkFreeDay: Boolean,
)

@Entity(
    tableName = "milestones",
    foreignKeys = [ForeignKey(
        entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"],
        onDelete = ForeignKey.SET_NULL, onUpdate = ForeignKey.NO_ACTION,
    )],
    indices = [Index("subjectId"), Index(value = ["dueDate", "dueTime"])],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long?,
    val title: String,
    val dueDate: LocalDate,
    val dueTime: LocalTime?,
    val isExam: Boolean,
    val isCompleted: Boolean = false,
)

/** Completion belongs to an occurrence, never to every week of its blueprint. */
@Entity(
    tableName = "routine_completions",
    primaryKeys = ["routineBlockId", "date"],
    foreignKeys = [ForeignKey(
        entity = RoutineBlockEntity::class, parentColumns = ["id"], childColumns = ["routineBlockId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("date")],
)
data class RoutineCompletionEntity(val routineBlockId: Long, val date: LocalDate)

/** Persistent, at-most-once delivery claims survive process death and repeated broadcasts. */
@Entity(
    tableName = "alarm_deliveries",
    primaryKeys = ["routineBlockId", "occurrenceDate", "kind"],
    foreignKeys = [ForeignKey(
        entity = RoutineBlockEntity::class, parentColumns = ["id"], childColumns = ["routineBlockId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("occurrenceDate")],
)
data class AlarmDeliveryEntity(
    val routineBlockId: Long,
    val occurrenceDate: LocalDate,
    val kind: String,
    val deliveredAtEpochMillis: Long,
)
