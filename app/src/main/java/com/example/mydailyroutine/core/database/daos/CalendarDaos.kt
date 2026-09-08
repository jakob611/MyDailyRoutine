package com.example.mydailyroutine.core.database.daos

import androidx.room.*
import com.example.mydailyroutine.core.database.entities.*
import java.time.DayOfWeek
import java.time.LocalDate

@Dao
interface CalendarDao {
    @Query("SELECT * FROM school_calendar WHERE date BETWEEN :from AND :through ORDER BY date, title")
    suspend fun inRange(from: LocalDate, through: LocalDate): List<SchoolCalendarEntryEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(entry: SchoolCalendarEntryEntity): Long
}

@Dao
interface DemoImportDao {
    @Query("SELECT EXISTS(SELECT 1 FROM demo_imports WHERE `key` = :key)")
    fun observeImported(key: String): kotlinx.coroutines.flow.Flow<Boolean>
    @Query("SELECT EXISTS(SELECT 1 FROM demo_imports WHERE `key` = :key)")
    suspend fun isImported(key: String): Boolean
    @Insert suspend fun insert(receipt: DemoImportEntity)
}
