package com.example.mydailyroutine.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.DayOfWeek
import java.time.LocalDate

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<SubjectEntity>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<SubjectEntity>): List<Long>

    @Delete
    suspend fun delete(subject: SubjectEntity)
}

@Dao
interface RoutineBlockDao {
    @Query("SELECT * FROM routine_blocks WHERE dayOfWeek = :dayOfWeek ORDER BY startTime, id")
    fun observeForDay(dayOfWeek: DayOfWeek): kotlinx.coroutines.flow.Flow<List<RoutineBlockEntity>>

    @Query("SELECT * FROM routine_blocks WHERE dayOfWeek = :dayOfWeek ORDER BY startTime, id")
    suspend fun getForDay(dayOfWeek: DayOfWeek): List<RoutineBlockEntity>

    @Query("SELECT * FROM routine_blocks WHERE isNotificationEnabled = 1")
    suspend fun getNotificationEnabled(): List<RoutineBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: RoutineBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocks: List<RoutineBlockEntity>): List<Long>

    @Query("DELETE FROM routine_blocks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface EventOverrideDao {
    @Query("SELECT * FROM event_overrides WHERE overrideDate = :date ORDER BY routineBlockId")
    fun observeForDate(date: LocalDate): kotlinx.coroutines.flow.Flow<List<EventOverrideEntity>>

    @Query("SELECT * FROM event_overrides WHERE overrideDate = :date")
    suspend fun getForDate(date: LocalDate): List<EventOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventOverride: EventOverrideEntity): Long

    @Query("DELETE FROM event_overrides WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SchoolCalendarDao {
    @Query("SELECT * FROM school_calendar_entries WHERE date = :date LIMIT 1")
    fun observeForDate(date: LocalDate): kotlinx.coroutines.flow.Flow<SchoolCalendarEntryEntity?>

    @Query("SELECT * FROM school_calendar_entries WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeInRange(from: LocalDate, to: LocalDate): kotlinx.coroutines.flow.Flow<List<SchoolCalendarEntryEntity>>

    @Query("SELECT * FROM school_calendar_entries WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun getInRange(from: LocalDate, to: LocalDate): List<SchoolCalendarEntryEntity>

    @Query("SELECT COUNT(*) FROM school_calendar_entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<SchoolCalendarEntryEntity>): List<Long>
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE dueDate = :date ORDER BY dueTime, id")
    fun observeForDate(date: LocalDate): kotlinx.coroutines.flow.Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones WHERE dueDate BETWEEN :from AND :to ORDER BY dueDate, dueTime, id")
    fun observeInRange(from: LocalDate, to: LocalDate): kotlinx.coroutines.flow.Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones WHERE dueDate BETWEEN :from AND :to ORDER BY dueDate, dueTime, id")
    suspend fun getInRange(from: LocalDate, to: LocalDate): List<MilestoneEntity>

    @Query("SELECT COUNT(*) FROM milestones")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: MilestoneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<MilestoneEntity>): List<Long>
}
