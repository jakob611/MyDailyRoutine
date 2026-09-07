package com.example.mydailyroutine.core.database.daos

import androidx.room.*
import com.example.mydailyroutine.core.database.entities.*
import java.time.DayOfWeek
import java.time.LocalDate

@Dao
interface BacklogDao {
    @Query("SELECT EXISTS(SELECT 1 FROM backlog_entries WHERE milestoneId = :goal AND topicId IS NULL AND stageOrder < :stage)")
    suspend fun hasEarlierStage(goal: Long, stage: Int): Boolean
    @Query("SELECT * FROM backlog_entries ORDER BY priorityWeight DESC, id") fun observeAll(): kotlinx.coroutines.flow.Flow<List<BacklogEntryEntity>>
    @Query("SELECT * FROM backlog_entries WHERE id = :id") suspend fun get(id: Long): BacklogEntryEntity?
    @Query("SELECT COUNT(*) FROM backlog_entries WHERE milestoneId = :id AND topicId IS NULL") suspend fun forMilestoneCount(id: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(entry: BacklogEntryEntity): Long
    @Query("DELETE FROM backlog_entries WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM backlog_entries WHERE sourceRoutineId = :routineId AND occurrenceDate = :date") suspend fun deleteOccurrence(routineId: Long, date: LocalDate)
}
