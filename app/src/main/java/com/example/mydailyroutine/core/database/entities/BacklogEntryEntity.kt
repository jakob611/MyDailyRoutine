package com.example.mydailyroutine.core.database.entities

import androidx.room.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.CancellationReason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

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
    val occurrenceDate: LocalDate?, val milestoneId: Long?, val topicId: Long?, val reviewId: Long?, val reason: String,
    @ColumnInfo(defaultValue = "1") val rawDurationMinutes: Int = durationMinutes,
    val stageOrder: Int? = null)

/** Singleton explicit timer, reconstructed from instants instead of keeping a process awake. */
