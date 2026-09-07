package com.example.mydailyroutine.data.local

import androidx.room.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.CancellationReason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "subjects")
data class SubjectEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
    val colorHex: Long, val defaultDurationMinutes: Int)

/** Weekly/one-off blueprint. Per-occurrence changes and actuals remain in their own indexed tables. */
@Entity(tableName = "routine_blocks", foreignKeys = [
    ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = StudyTopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = MilestoneEntity::class, parentColumns = ["id"], childColumns = ["milestoneId"], onDelete = ForeignKey.SET_NULL),
], indices = [Index("subjectId"), Index("topicId"), Index("milestoneId"), Index(value = ["dayOfWeek", "startMinutes"])])
data class TimeBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long?, val title: String, val category: RoutineCategory, val dayOfWeek: DayOfWeek,
    val startMinutes: Int, val durationMinutes: Int, val isNotificationEnabled: Boolean,
    val validFrom: LocalDate? = null, val validUntil: LocalDate? = null,
    val minDurationMinutes: Int = if (category == RoutineCategory.SCHOOL) durationMinutes else if (category == RoutineCategory.EMERGENCY_RESERVE) 0 else minOf(25, durationMinutes),
    val elasticity: Double = if (category == RoutineCategory.SCHOOL) 0.0 else 1.0,
    val priorityWeight: Double = if (category.isDeepWork) 3.0 else 1.0,
    val isFixedCommitment: Boolean = category == RoutineCategory.SCHOOL,
    val completedActualMinutes: Int? = null,
    val rawDurationMinutes: Int = durationMinutes,
    val topicId: Long? = null, val milestoneId: Long? = null,
)

@Entity(tableName = "event_overrides", foreignKeys = [ForeignKey(entity = TimeBlockEntity::class,
    parentColumns = ["id"], childColumns = ["routineBlockId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["routineBlockId", "overrideDate"], unique = true), Index("overrideDate")])
data class EventOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineBlockId: Long, val overrideDate: LocalDate, val isCancelled: Boolean,
    val customStartTime: LocalTime?, val customEndTime: LocalTime?, val customTitle: String?,
    @ColumnInfo(defaultValue = "0") val dayShift: Int = 0,
    @ColumnInfo(defaultValue = "'MANUAL'") val cancellationReason: CancellationReason = CancellationReason.MANUAL,
)

@Entity(tableName = "school_calendar", indices = [Index(value = ["date", "title"], unique = true), Index(value = ["date", "isWorkFreeDay"])])
data class SchoolCalendarEntryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, val title: String, val isWorkFreeDay: Boolean)

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

@Entity(tableName = "routine_completions", primaryKeys = ["routineBlockId", "date"],
    foreignKeys = [ForeignKey(entity = TimeBlockEntity::class, parentColumns = ["id"], childColumns = ["routineBlockId"], onDelete = ForeignKey.CASCADE)], indices = [Index("date")])
data class RoutineCompletionEntity(val routineBlockId: Long, val date: LocalDate, val actualMinutes: Int? = null)

@Entity(tableName = "alarm_deliveries", primaryKeys = ["routineBlockId", "occurrenceDate", "kind"],
    foreignKeys = [ForeignKey(entity = TimeBlockEntity::class, parentColumns = ["id"], childColumns = ["routineBlockId"], onDelete = ForeignKey.CASCADE)], indices = [Index("occurrenceDate")])
data class AlarmDeliveryEntity(val routineBlockId: Long, val occurrenceDate: LocalDate, val kind: String, val deliveredAtEpochMillis: Long)

@Entity(tableName = "demo_imports", primaryKeys = ["key"])
data class DemoImportEntity(val key: String, val importedAtEpochMillis: Long)

@Entity(tableName = "historical_velocity", foreignKeys = [
    ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = TimeBlockEntity::class, parentColumns = ["id"], childColumns = ["routineBlockId"], onDelete = ForeignKey.SET_NULL),
], indices = [Index(value = ["subjectId", "timestamp"]), Index(value = ["routineBlockId", "occurrenceDate"], unique = true)])
data class HistoricalVelocityEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long?, val plannedDurationMinutes: Int, val actualDurationMinutes: Int, val timestamp: Long,
    val routineBlockId: Long?, val occurrenceDate: LocalDate?)

@Entity(tableName = "study_topics", foreignKeys = [
    ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = MilestoneEntity::class, parentColumns = ["id"], childColumns = ["milestoneId"], onDelete = ForeignKey.SET_NULL),
], indices = [Index("subjectId"), Index("milestoneId")])
data class StudyTopicEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String,
    val subjectId: Long?, val initialEpochDay: Long, val finalEpochDay: Long, val reviewCount: Int,
    val reviewDurationMinutes: Int, val priorityWeight: Double, val milestoneId: Long?)

@Entity(tableName = "spaced_reviews", foreignKeys = [
    ForeignKey(entity = StudyTopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = TimeBlockEntity::class, parentColumns = ["id"], childColumns = ["timeBlockId"], onDelete = ForeignKey.CASCADE),
], indices = [Index(value = ["topicId", "ordinal"], unique = true), Index("scheduledEpochDay"), Index(value = ["timeBlockId"], unique = true)])
data class SpacedReviewEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val topicId: Long,
    val scheduledEpochDay: Long, val durationMinutes: Int, val priorityWeight: Double, val ordinal: Int,
    val timeBlockId: Long? = null, val isCompleted: Boolean = false)

@Entity(tableName = "backlog_entries", foreignKeys = [
    ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = TimeBlockEntity::class, parentColumns = ["id"], childColumns = ["sourceRoutineId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = MilestoneEntity::class, parentColumns = ["id"], childColumns = ["milestoneId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = StudyTopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = SpacedReviewEntity::class, parentColumns = ["id"], childColumns = ["reviewId"], onDelete = ForeignKey.CASCADE),
], indices = [Index("subjectId"), Index(value = ["sourceRoutineId", "occurrenceDate"], unique = true),
    Index("milestoneId"), Index("topicId"), Index(value = ["reviewId"], unique = true)])
data class BacklogEntryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String,
    val category: RoutineCategory, val durationMinutes: Int, val minDurationMinutes: Int,
    val elasticity: Double, val priorityWeight: Double, val subjectId: Long?, val sourceRoutineId: Long?,
    val occurrenceDate: LocalDate?, val milestoneId: Long?, val topicId: Long?, val reviewId: Long?, val reason: String)
