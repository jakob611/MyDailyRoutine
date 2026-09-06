package com.example.mydailyroutine.data.local

import androidx.room.TypeConverter
import com.example.mydailyroutine.domain.RoutineCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AppTypeConverters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek?): String? = value?.name

    @TypeConverter
    fun toDayOfWeek(value: String?): DayOfWeek? = value?.let(DayOfWeek::valueOf)

    @TypeConverter
    fun fromRoutineCategory(value: RoutineCategory?): String? = value?.name

    @TypeConverter
    fun toRoutineCategory(value: String?): RoutineCategory? = value?.let(RoutineCategory::valueOf)
}
