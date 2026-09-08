package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar as Calendar
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.model.ScheduleValidation
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class CalendarAndValidationTest {
    @Test fun `western winter dates are pinned not inferred from a generic week`() {
        val winter = Calendar.entries().filter { it.title.startsWith("Zimske") }
        assertEquals(5, winter.size)
        assertEquals(LocalDate.of(2027, 2, 22), winter.first().date)
        assertEquals(LocalDate.of(2027, 2, 26), winter.last().date)
        assertTrue(winter.all { it.isWorkFreeDay })
    }

    @Test fun `cross year holiday windows and easter are correct`() {
        val newYear = Calendar.entries().filter { it.title == "Novoletni oddih" }
        assertEquals(9, newYear.size)
        assertEquals(LocalDate.of(2027, 1, 2), newYear.last().date)
        assertEquals(LocalDate.of(2027, 3, 28), Calendar.easterSunday(2027))
        assertTrue(Calendar.entries().any { it.date == LocalDate.of(2027, 3, 29) && it.title == "Velikonočni ponedeljek" })
    }

    @Test fun `bundled calendar is unique and within its declared coverage`() {
        val entries = Calendar.entries()
        assertEquals(entries.size, entries.distinctBy { it.date to it.title }.size)
        assertTrue(entries.all { Calendar.covers(it.date) })
        assertFalse(Calendar.covers(LocalDate.of(2027, 9, 1)))
        assertEquals(0L, Calendar.daysRemaining(LocalDate.of(2028, 1, 1)))
        assertEquals(1L, Calendar.daysRemaining(LocalDate.of(2027, 6, 23)))
    }

    @Test fun `working observances never claim a day off`() {
        val sport = Calendar.entries().single { it.title == "Dan slovenskega športa" }
        assertFalse(sport.isWorkFreeDay)
    }

    @Test fun `strict time parser rejects rollover seconds and junk`() {
        assertEquals(LocalTime.of(7, 45), ScheduleValidation.parseTime("7:45"))
        assertEquals(LocalTime.of(23, 59), ScheduleValidation.parseTime("23:59"))
        listOf("24:00", "12:60", "-1:00", "8:30:45", "noon", "").forEach { assertNull(ScheduleValidation.parseTime(it)) }
    }

    @Test fun `UI date parsing stays within the date pickers supported range`() {
        assertNotNull(ScheduleValidation.parseDate("1900-01-01"))
        assertNotNull(ScheduleValidation.parseDate("2100-12-31"))
        assertNull(ScheduleValidation.parseDate("1899-12-31"))
        assertNull(ScheduleValidation.parseDate("2101-01-01"))
        assertNull(ScheduleValidation.parseDate("2027-02-29"))
    }

    @Test fun `school quiet window is half open and can cross midnight`() {
        val normal = SchedulePreferences()
        assertTrue(normal.isQuietAt(LocalTime.of(7, 45)))
        assertFalse(normal.isQuietAt(LocalTime.of(14, 30)))
        val night = normal.copy(schoolStart = LocalTime.of(22, 0), schoolEnd = LocalTime.of(6, 0))
        assertTrue(night.isQuietAt(LocalTime.of(23, 0)))
        assertTrue(night.isQuietAt(LocalTime.of(5, 59)))
        assertFalse(night.isQuietAt(LocalTime.NOON))
        assertFalse(night.copy(muteDuringSchoolHours = false).isQuietAt(LocalTime.MIDNIGHT))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero duration is rejected instead of interpreted as twenty four hours`() {
        ScheduleValidation.times(LocalTime.NOON, LocalTime.NOON)
    }
}
