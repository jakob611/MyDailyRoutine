package com.example.mydailyroutine.data.local

import androidx.room.TypeConverter
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.ScheduleValidation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TimeConverters {
    @TypeConverter fun encodeCancellation(value: com.example.mydailyroutine.domain.model.CancellationReason): String = value.name
    @TypeConverter fun decodeCancellation(value: String): com.example.mydailyroutine.domain.model.CancellationReason = com.example.mydailyroutine.domain.model.CancellationReason.valueOf(value)
    @TypeConverter fun encodeDate(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun decodeDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter fun encodeTime(value: LocalTime?): Int? = value?.let {
        ScheduleValidation.minutePrecision(it)
        it.toSecondOfDay() / 60
    }
    @TypeConverter fun decodeTime(value: Int?): LocalTime? = value?.let {
        require(it in 0..1439) { "Invalid stored minute of day: $it" }
        LocalTime.ofSecondOfDay(it * 60L)
    }

    @TypeConverter fun encodeDay(value: DayOfWeek): Int = value.value
    @TypeConverter fun decodeDay(value: Int): DayOfWeek = DayOfWeek.of(value)
    // Store stable names, never ordinal values whose meaning changes when enums are reordered.
    @TypeConverter fun encodeCategory(value: RoutineCategory): String = value.name
    @TypeConverter fun decodeCategory(value: String): RoutineCategory = RoutineCategory.valueOf(value)
}
