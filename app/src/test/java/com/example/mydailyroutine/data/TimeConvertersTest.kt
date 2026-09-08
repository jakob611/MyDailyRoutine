package com.example.mydailyroutine.data

import com.example.mydailyroutine.core.database.TimeConverters
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class TimeConvertersTest {
    private val converters = TimeConverters()
    @Test fun `epoch dates round trip before and after 1970`() {
        listOf(LocalDate.of(1960, 1, 1), LocalDate.of(2027, 1, 2), LocalDate.of(2028, 2, 29)).forEach {
            assertEquals(it, converters.decodeDate(converters.encodeDate(it)))
        }
        assertNull(converters.decodeDate(null))
        assertNull(converters.encodeDate(null))
    }
    @Test fun `time uses sortable whole minutes and preserves null overrides`() {
        assertEquals(1439, converters.encodeTime(LocalTime.of(23, 59)))
        assertEquals(LocalTime.MIDNIGHT, converters.decodeTime(0))
        assertEquals(LocalTime.of(7, 45), converters.decodeTime(converters.encodeTime(LocalTime.of(7, 45))))
        assertNull(converters.encodeTime(null))
        assertNull(converters.decodeTime(null))
    }
    @Test fun `enum storage is stable`() {
        DayOfWeek.values().forEach { assertEquals(it, converters.decodeDay(converters.encodeDay(it))) }
        RoutineCategory.entries.forEach { assertEquals(it, converters.decodeCategory(converters.encodeCategory(it))) }
    }
    @Test(expected = IllegalArgumentException::class)
    fun `seconds are rejected rather than silently truncated`() { converters.encodeTime(LocalTime.of(9, 0, 1)) }
    @Test(expected = IllegalArgumentException::class)
    fun `invalid persisted minutes are rejected`() { converters.decodeTime(1440) }
}
