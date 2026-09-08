package com.example.mydailyroutine.core.database.entities

import androidx.room.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.CancellationReason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "subjects")
data class SubjectEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
    val colorHex: Long, val defaultDurationMinutes: Int)

/** Weekly/one-off blueprint. Per-occurrence changes and actuals remain in their own indexed tables. */
