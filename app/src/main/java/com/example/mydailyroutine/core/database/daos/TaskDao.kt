package com.example.mydailyroutine.core.database.daos

import androidx.room.*
import com.example.mydailyroutine.core.database.entities.*
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate IS NULL, dueDate, id") fun observeAll(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :from AND :through ORDER BY dueDate, id")
    suspend fun inRange(from: LocalDate, through: LocalDate): List<TaskEntity>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun get(id: Long): TaskEntity?
    @Insert suspend fun insert(task: TaskEntity): Long
    @Update suspend fun update(task: TaskEntity): Int
    @Query("UPDATE tasks SET completedAtEpochMillis = :stamp WHERE id = :id") suspend fun setCompleted(id: Long, stamp: Long?)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM tasks WHERE completedAtEpochMillis IS NOT NULL") suspend fun clearCompleted()
}
