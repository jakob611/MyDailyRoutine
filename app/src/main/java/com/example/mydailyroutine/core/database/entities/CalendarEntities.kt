package com.example.mydailyroutine.core.database.entities

import androidx.room.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.CancellationReason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "school_calendar", indices = [Index(value = ["date", "title"], unique = true), Index(value = ["date", "isWorkFreeDay"])])
data class SchoolCalendarEntryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, val title: String, val isWorkFreeDay: Boolean)

@Entity(tableName = "demo_imports", primaryKeys = ["key"])
data class DemoImportEntity(val key: String, val importedAtEpochMillis: Long)
