package com.example.mydailyroutine.core.database.entities

import androidx.room.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.CancellationReason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

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
    val stageOrder: Int? = null,
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

@Entity(tableName = "routine_completions", primaryKeys = ["routineBlockId", "date"],
    foreignKeys = [ForeignKey(entity = TimeBlockEntity::class, parentColumns = ["id"], childColumns = ["routineBlockId"], onDelete = ForeignKey.CASCADE)], indices = [Index("date")])
data class RoutineCompletionEntity(val routineBlockId: Long, val date: LocalDate, val actualMinutes: Int? = null,
    val actualStartEpochMinute: Long? = null, val actualStartedAtEpochMillis: Long? = null,
    val actualEndedAtEpochMillis: Long? = null, val actualZoneId: String? = null)

@Entity(tableName = "alarm_deliveries", primaryKeys = ["routineBlockId", "occurrenceDate", "kind"],
    foreignKeys = [ForeignKey(entity = TimeBlockEntity::class, parentColumns = ["id"], childColumns = ["routineBlockId"], onDelete = ForeignKey.CASCADE)], indices = [Index("occurrenceDate")])
data class AlarmDeliveryEntity(val routineBlockId: Long, val occurrenceDate: LocalDate, val kind: String, val deliveredAtEpochMillis: Long)

@Entity(tableName = "active_execution", foreignKeys = [ForeignKey(entity = TimeBlockEntity::class,
    parentColumns = ["id"], childColumns = ["routineBlockId"], onDelete = ForeignKey.CASCADE)], indices = [Index("routineBlockId")])
data class ActiveExecutionEntity(@PrimaryKey val id: Int = 1, val routineBlockId: Long, val occurrenceDate: LocalDate,
    val startedAtEpochMillis: Long, val expectedEndEpochMillis: Long, val lastHealedEndEpochMillis: Long,
    val stoppedAtEpochMillis: Long? = null)
