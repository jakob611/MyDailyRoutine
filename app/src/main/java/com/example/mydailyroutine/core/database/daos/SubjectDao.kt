package com.example.mydailyroutine.core.database.daos

import androidx.room.*
import com.example.mydailyroutine.core.database.entities.*
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
