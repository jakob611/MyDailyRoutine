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
interface RoutineDao {
    @Query("""
        SELECT * FROM routine_blocks
        WHERE dayOfWeek IN (:weekdays)
          AND (validFrom IS NULL OR validFrom <= :through)
          AND (validUntil IS NULL OR validUntil >= :from)
        ORDER BY dayOfWeek, startTime, id
    """)
    suspend fun candidates(weekdays: List<DayOfWeek>, from: LocalDate, through: LocalDate): List<RoutineBlockEntity>
    @Query("SELECT * FROM routine_blocks WHERE id = :id") suspend fun get(id: Long): RoutineBlockEntity?
    @Query("SELECT EXISTS(SELECT 1 FROM routine_blocks WHERE isNotificationEnabled = 1 AND (validUntil IS NULL OR validUntil >= :today))")
    suspend fun hasUpcomingNotificationRoutines(today: LocalDate): Boolean
    @Insert suspend fun insert(routine: RoutineBlockEntity): Long
    @Update suspend fun update(routine: RoutineBlockEntity): Int
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
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(completion: RoutineCompletionEntity): Long
    @Query("DELETE FROM routine_completions WHERE routineBlockId = :routineId AND date = :date")
    suspend fun delete(routineId: Long, date: LocalDate)
}

@Dao
interface AlarmDeliveryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun claim(delivery: AlarmDeliveryEntity): Long
    @Query("DELETE FROM alarm_deliveries WHERE occurrenceDate < :before") suspend fun prune(before: LocalDate)
}
