package com.example.mydailyroutine.core.database.entities

import androidx.room.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.CancellationReason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

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
