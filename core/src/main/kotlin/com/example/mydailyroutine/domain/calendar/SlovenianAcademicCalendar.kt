package com.example.mydailyroutine.domain.calendar

import com.example.mydailyroutine.domain.model.CalendarEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Bundled, versioned 2026/27 Western-region SECONDARY-school calendar, verified 2026-09-06.
 * See docs/CALENDAR.md for the Ministry sources and the final-year distinction.
 * Deliberately not extrapolated: the regional winter rotation changes between school cycles.
 */
object SlovenianAcademicCalendar {
    const val CYCLE_LABEL = "2026/27 · Zahodna regija"
    val start: LocalDate = LocalDate.of(2026, 9, 1)
    val end: LocalDate = LocalDate.of(2027, 8, 31)
    val teachingEnd: LocalDate = LocalDate.of(2027, 6, 24)
    val finalYearTeachingEnd: LocalDate = LocalDate.of(2027, 5, 21)

    fun covers(date: LocalDate): Boolean = date in start..end
    fun daysRemaining(today: LocalDate, target: LocalDate = teachingEnd): Long =
        ChronoUnit.DAYS.between(today, target).coerceAtLeast(0)

    fun entries(): List<CalendarEntry> = buildList {
        fun day(date: String, title: String, free: Boolean = true) {
            add(CalendarEntry(date = LocalDate.parse(date), title = title, isWorkFreeDay = free))
        }
        fun range(first: String, last: String, title: String) {
            var date = LocalDate.parse(first)
            val until = LocalDate.parse(last)
            while (date <= until) {
                add(CalendarEntry(date = date, title = title, isWorkFreeDay = true))
                date = date.plusDays(1)
            }
        }
        day("2026-09-01", "Začetek pouka", false)
        range("2026-10-26", "2026-10-30", "Jesenske počitnice")
        // Public holidays + vacation days together form these continuous no-school windows.
        range("2026-12-25", "2027-01-02", "Novoletni oddih")
        range("2027-02-22", "2027-02-26", "Zimske počitnice · Zahod")
        day("2027-04-26", "Pouka prost dan")
        range("2027-04-27", "2027-05-02", "Prvomajski oddih")
        day("2027-05-21", "Konec pouka · zaključni letniki", false)
        day("2027-06-24", "Konec pouka · ostali letniki", false)
        range("2027-06-28", "2027-08-31", "Poletne počitnice")

        day("2026-10-31", "Dan reformacije")
        day("2026-11-01", "Dan spomina na mrtve")
        day("2026-12-25", "Božič")
        day("2026-12-26", "Dan samostojnosti in enotnosti")
        day("2027-01-01", "Novo leto")
        day("2027-01-02", "Novo leto")
        day("2027-02-08", "Prešernov dan")
        val easter = easterSunday(2027)
        add(CalendarEntry(date = easter, title = "Velikonočna nedelja", isWorkFreeDay = true))
        add(CalendarEntry(date = easter.plusDays(1), title = "Velikonočni ponedeljek", isWorkFreeDay = true))
        add(CalendarEntry(date = easter.plusDays(49), title = "Binkošti", isWorkFreeDay = true))
        day("2027-04-27", "Dan upora proti okupatorju")
        day("2027-05-01", "Praznik dela")
        day("2027-05-02", "Praznik dela")
        day("2027-06-25", "Dan državnosti")
        day("2027-08-15", "Marijino vnebovzetje")

        // These national observances are NOT days off. Do not mute school for them.
        day("2026-09-15", "Priključitev Primorske k matični domovini", false)
        day("2026-09-23", "Dan slovenskega športa", false)
        day("2026-10-25", "Dan suverenosti", false)
        day("2026-11-10", "Dan znanosti", false)
        day("2026-11-23", "Dan Rudolfa Maistra", false)
        day("2027-06-08", "Dan Primoža Trubarja", false)
        day("2027-08-17", "Združitev prekmurskih Slovencev z matičnim narodom", false)
    }.distinctBy { it.date to it.title }.sortedWith(compareBy<CalendarEntry> { it.date }.thenBy { it.title })

    /** Gregorian Meeus/Jones/Butcher computus; no network, locale, or timezone dependence. */
    fun easterSunday(year: Int): LocalDate {
        require(year in 1583..4099)
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }
}
