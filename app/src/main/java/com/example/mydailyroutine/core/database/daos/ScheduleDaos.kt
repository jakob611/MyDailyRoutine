package com.example.mydailyroutine.core.database.daos

import androidx.room.*
import com.example.mydailyroutine.core.database.entities.*
import java.time.DayOfWeek
import java.time.LocalDate

@Dao
interface TimeBlockDao {
    @Query("SELECT * FROM routine_blocks WHERE milestoneId = :goal AND topicId IS NULL AND stageOrder IS NOT NULL ORDER BY stageOrder, validFrom, startMinutes")
    suspend fun goalStages(goal: Long): List<TimeBlockEntity>
    @Query("""SELECT EXISTS(SELECT 1 FROM routine_blocks b WHERE b.milestoneId = :goal AND b.topicId IS NULL
        AND b.stageOrder < :stage AND b.category IN ('FOCUS_ANALYTICAL','FOCUS_SYNTHESIZING')
        AND NOT EXISTS(SELECT 1 FROM routine_completions c WHERE c.routineBlockId = b.id AND c.date = b.validFrom)
        AND NOT EXISTS(SELECT 1 FROM event_overrides e WHERE e.routineBlockId = b.id AND e.overrideDate = b.validFrom AND e.isCancelled = 1))""")
    suspend fun hasUnfinishedEarlierStage(goal: Long, stage: Int): Boolean
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
interface ExecutionDao {
    @Query("SELECT * FROM active_execution WHERE id = 1") fun observe(): kotlinx.coroutines.flow.Flow<ActiveExecutionEntity?>
    @Query("SELECT * FROM active_execution WHERE id = 1") suspend fun get(): ActiveExecutionEntity?
    @Insert suspend fun insert(value: ActiveExecutionEntity)
    @Update suspend fun update(value: ActiveExecutionEntity): Int
    @Query("DELETE FROM active_execution WHERE id = 1") suspend fun clear()
}
