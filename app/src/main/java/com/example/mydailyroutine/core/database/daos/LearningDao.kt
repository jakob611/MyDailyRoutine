package com.example.mydailyroutine.core.database.daos

import androidx.room.*
import com.example.mydailyroutine.core.database.entities.*
import java.time.DayOfWeek
import java.time.LocalDate

@Dao
interface LearningDao {
    @Query("SELECT * FROM study_topics WHERE id = :id") suspend fun getTopic(id: Long): StudyTopicEntity?
    @Query("SELECT * FROM historical_velocity ORDER BY timestamp DESC, id DESC LIMIT 10000")
    fun observeHistory(): kotlinx.coroutines.flow.Flow<List<HistoricalVelocityEntity>>
    @Query("SELECT * FROM historical_velocity WHERE subjectId = :subjectId ORDER BY timestamp DESC, id DESC LIMIT 120")
    suspend fun history(subjectId: Long): List<HistoricalVelocityEntity>
    @Query("SELECT * FROM historical_velocity WHERE routineBlockId = :routineId AND occurrenceDate = :date")
    suspend fun sample(routineId: Long, date: LocalDate): HistoricalVelocityEntity?
    @Insert suspend fun insertSample(sample: HistoricalVelocityEntity): Long
    @Update suspend fun updateSample(sample: HistoricalVelocityEntity): Int
    @Query("DELETE FROM historical_velocity WHERE routineBlockId = :routineId AND occurrenceDate = :date") suspend fun deleteSample(routineId: Long, date: LocalDate)
    @Query("SELECT * FROM study_topics ORDER BY finalEpochDay, id") fun observeTopics(): kotlinx.coroutines.flow.Flow<List<StudyTopicEntity>>
    @Insert suspend fun insertTopic(topic: StudyTopicEntity): Long
    @Query("DELETE FROM study_topics WHERE id = :id") suspend fun deleteTopic(id: Long)
    @Query("SELECT * FROM spaced_reviews WHERE scheduledEpochDay BETWEEN :from AND :through ORDER BY scheduledEpochDay, ordinal, id")
    suspend fun reviews(from: Long, through: Long): List<SpacedReviewEntity>
    @Query("SELECT * FROM spaced_reviews WHERE timeBlockId = :routineId") suspend fun reviewForBlock(routineId: Long): SpacedReviewEntity?
    @Insert suspend fun insertReview(review: SpacedReviewEntity): Long
    @Update suspend fun updateReview(review: SpacedReviewEntity): Int
    @Query("UPDATE spaced_reviews SET isCompleted = :completed WHERE timeBlockId = :routineId") suspend fun setReviewCompleted(routineId: Long, completed: Boolean)
    @Query("SELECT * FROM spaced_reviews WHERE id = :id") suspend fun getReview(id: Long): SpacedReviewEntity?
}
