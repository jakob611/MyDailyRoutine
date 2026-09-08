package com.example.mydailyroutine.core.database.daos

import androidx.room.*
import com.example.mydailyroutine.core.database.entities.*
import java.time.DayOfWeek
import java.time.LocalDate

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
