package com.example.mydailyroutine.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.time.DayOfWeek
import java.time.LocalDate

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE, id")
    suspend fun getAll(): List<SubjectEntity>
    @Insert suspend fun insert(subject: SubjectEntity): Long
    @Update suspend fun update(subject: SubjectEntity): Int
    @Query("DELETE FROM subjects WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface TimeBlockDao {
    @Query("""
        SELECT * FROM routine_blocks
        WHERE dayOfWeek IN (:weekdays)
          AND (validFrom IS NULL OR validFrom <= :through)
          AND (validUntil IS NULL OR validUntil >= :from)
        ORDER BY dayOfWeek, startMinutes, id
    """)
    suspend fun candidates(weekdays: List<DayOfWeek>, from: LocalDate, through: LocalDate): List<TimeBlockEntity>
    @Query("SELECT * FROM routine_blocks WHERE id = :id") suspend fun get(id: Long): TimeBlockEntity?
    @Query("SELECT EXISTS(SELECT 1 FROM routine_blocks WHERE isNotificationEnabled = 1 AND (validUntil IS NULL OR validUntil >= :today))")
    suspend fun hasUpcomingNotificationRoutines(today: LocalDate): Boolean
    @Insert suspend fun insert(routine: TimeBlockEntity): Long
    @Update suspend fun update(routine: TimeBlockEntity): Int
    @Query("SELECT COUNT(*) FROM routine_blocks WHERE milestoneId = :id AND topicId IS NULL") suspend fun forMilestoneCount(id: Long): Int
    @Query("DELETE FROM routine_blocks WHERE id = :id") suspend fun delete(id: Long)
    @Query("UPDATE routine_blocks SET isNotificationEnabled = :enabled WHERE id = :routineId")
    suspend fun setNotificationEnabled(routineId: Long, enabled: Boolean): Int
}

@Dao
interface OverrideDao {
    @Query("SELECT * FROM event_overrides WHERE overrideDate BETWEEN :from AND :through ORDER BY overrideDate, id")
    suspend fun inRange(from: LocalDate, through: LocalDate): List<EventOverrideEntity>
    @Query("SELECT * FROM event_overrides WHERE routineBlockId = :routineId")
    suspend fun forRoutine(routineId: Long): List<EventOverrideEntity>
    @Query("SELECT * FROM event_overrides WHERE routineBlockId = :routineId AND overrideDate = :date")
    suspend fun get(routineId: Long, date: LocalDate): EventOverrideEntity?
    @Insert suspend fun insert(override: EventOverrideEntity): Long
    @Update suspend fun update(override: EventOverrideEntity): Int
    @Query("DELETE FROM event_overrides WHERE routineBlockId = :routineId AND overrideDate = :date")
    suspend fun delete(routineId: Long, date: LocalDate)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM school_calendar WHERE date BETWEEN :from AND :through ORDER BY date, title")
    suspend fun inRange(from: LocalDate, through: LocalDate): List<SchoolCalendarEntryEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(entry: SchoolCalendarEntryEntity): Long
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE dueDate BETWEEN :from AND :through ORDER BY dueDate, dueTime, id")
    suspend fun inRange(from: LocalDate, through: LocalDate): List<MilestoneEntity>
    @Query("SELECT * FROM milestones WHERE id = :id") suspend fun get(id: Long): MilestoneEntity?
    @Query("SELECT * FROM milestones ORDER BY dueDate, id") fun observeAll(): kotlinx.coroutines.flow.Flow<List<MilestoneEntity>>
    @Insert suspend fun insert(milestone: MilestoneEntity): Long
    @Update suspend fun update(milestone: MilestoneEntity): Int
    @Query("UPDATE milestones SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean): Int
    @Query("DELETE FROM milestones WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM routine_completions WHERE date BETWEEN :from AND :through")
    suspend fun inRange(from: LocalDate, through: LocalDate): List<RoutineCompletionEntity>
    @Query("SELECT * FROM routine_completions WHERE routineBlockId = :routineId AND date = :date") suspend fun get(routineId: Long, date: LocalDate): RoutineCompletionEntity?
    @Update suspend fun update(completion: RoutineCompletionEntity): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(completion: RoutineCompletionEntity): Long
    @Query("DELETE FROM routine_completions WHERE routineBlockId = :routineId AND date = :date")
    suspend fun delete(routineId: Long, date: LocalDate)
}

@Dao
interface AlarmDeliveryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun claim(delivery: AlarmDeliveryEntity): Long
    @Query("DELETE FROM alarm_deliveries WHERE occurrenceDate < :before") suspend fun prune(before: LocalDate)
}

@Dao
interface DemoImportDao {
    @Query("SELECT EXISTS(SELECT 1 FROM demo_imports WHERE `key` = :key)")
    fun observeImported(key: String): kotlinx.coroutines.flow.Flow<Boolean>
    @Query("SELECT EXISTS(SELECT 1 FROM demo_imports WHERE `key` = :key)")
    suspend fun isImported(key: String): Boolean
    @Insert suspend fun insert(receipt: DemoImportEntity)
}


@Dao
interface LearningDao {
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

@Dao
interface BacklogDao {
    @Query("SELECT * FROM backlog_entries ORDER BY priorityWeight DESC, id") fun observeAll(): kotlinx.coroutines.flow.Flow<List<BacklogEntryEntity>>
    @Query("SELECT * FROM backlog_entries WHERE id = :id") suspend fun get(id: Long): BacklogEntryEntity?
    @Query("SELECT COUNT(*) FROM backlog_entries WHERE milestoneId = :id AND topicId IS NULL") suspend fun forMilestoneCount(id: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(entry: BacklogEntryEntity): Long
    @Query("DELETE FROM backlog_entries WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM backlog_entries WHERE sourceRoutineId = :routineId AND occurrenceDate = :date") suspend fun deleteOccurrence(routineId: Long, date: LocalDate)
}
