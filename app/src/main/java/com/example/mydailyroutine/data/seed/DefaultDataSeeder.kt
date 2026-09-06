package com.example.mydailyroutine.data.seed

import com.example.mydailyroutine.data.local.MilestoneEntity
import com.example.mydailyroutine.data.local.RoutineDatabase
import com.example.mydailyroutine.data.local.RoutineBlockEntity
import com.example.mydailyroutine.data.local.SchoolCalendarEntryEntity
import com.example.mydailyroutine.data.local.SubjectEntity
import com.example.mydailyroutine.domain.RoutineCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Installs a useful first-run plan and the Slovenian western-region school calendar.
 * All values are local and deterministic; no network or account is needed.
 */
object DefaultDataSeeder {
    private val seedMutex = Mutex()

    suspend fun seedIfNeeded(database: RoutineDatabase) = seedMutex.withLock {
        seedIfNeededLocked(database)
    }

    private suspend fun seedIfNeededLocked(database: RoutineDatabase) {
        val subjectDao = database.subjectDao()
        val routineDao = database.routineBlockDao()
        val calendarDao = database.schoolCalendarDao()
        val milestoneDao = database.milestoneDao()

        val subjects = if (subjectDao.count() == 0) {
            subjectDao.insertAll(
                listOf(
                    SubjectEntity(name = "Mathematics HL", colorHex = 0xFF6750A4, defaultDurationMinutes = 60),
                    SubjectEntity(name = "Physics HL", colorHex = 0xFF006A6A, defaultDurationMinutes = 60),
                    SubjectEntity(name = "English A", colorHex = 0xFF9C4146, defaultDurationMinutes = 45),
                ),
            )
            subjectDao.getAll()
        } else {
            subjectDao.getAll()
        }

        if (routineDao.getForDay(DayOfWeek.MONDAY).isEmpty() && routineDao.getForDay(DayOfWeek.TUESDAY).isEmpty()) {
            val math = subjects.firstOrNull { it.name.startsWith("Mathematics") }?.id
            val physics = subjects.firstOrNull { it.name.startsWith("Physics") }?.id
            val english = subjects.firstOrNull { it.name.startsWith("English") }?.id
            val schoolDays = DayOfWeek.entries.take(5)
            routineDao.insertAll(
                schoolDays.map { day ->
                    RoutineBlockEntity(
                        subjectId = null,
                        title = "School day",
                        category = RoutineCategory.SCHOOL,
                        dayOfWeek = day,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(14, 0),
                        isNotificationEnabled = true,
                    )
                } + listOf(
                    RoutineBlockEntity(subjectId = math, title = "Mathematics deep work", category = RoutineCategory.FOCUS_STUDY, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(16, 0), endTime = LocalTime.of(17, 30), isNotificationEnabled = true),
                    RoutineBlockEntity(subjectId = physics, title = "Physics problem set", category = RoutineCategory.FOCUS_STUDY, dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(16, 0), endTime = LocalTime.of(17, 30), isNotificationEnabled = true),
                    RoutineBlockEntity(subjectId = english, title = "English reading", category = RoutineCategory.FOCUS_STUDY, dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(16, 0), endTime = LocalTime.of(16, 45), isNotificationEnabled = true),
                    RoutineBlockEntity(subjectId = null, title = "Walk and reset", category = RoutineCategory.REST_BREAK, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(17, 30), endTime = LocalTime.of(17, 45), isNotificationEnabled = true),
                    RoutineBlockEntity(subjectId = null, title = "Personal time", category = RoutineCategory.PERSONAL, dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(16, 0), endTime = LocalTime.of(18, 0), isNotificationEnabled = false),
                ),
            )
        }

        val currentSchoolYear = SchoolYearBounds.forDate(LocalDate.now())
        if (calendarDao.getInRange(currentSchoolYear.start, currentSchoolYear.end).isEmpty()) {
            calendarDao.insertAll(SlovenianAcademicCalendar.forSchoolYear(LocalDate.now()))
        }

        if (milestoneDao.count() == 0 && subjects.isNotEmpty()) {
            val subject = subjects.first()
            milestoneDao.insertAll(
                listOf(
                    MilestoneEntity(
                        subjectId = subject.id,
                        title = "Plan your first IA outline",
                        dueDate = LocalDate.now().plusDays(14),
                        dueTime = LocalTime.of(18, 0),
                        isExam = false,
                    ),
                    MilestoneEntity(
                        subjectId = null,
                        title = "Mock exam review",
                        dueDate = LocalDate.now().plusDays(28),
                        dueTime = null,
                        isExam = true,
                    ),
                ),
            )
        }
    }
}

/** Official school-free date families used by the western region (Zahodna regija). */
object SlovenianAcademicCalendar {
    fun forSchoolYear(anyDateInCycle: LocalDate): List<SchoolCalendarEntryEntity> {
        val bounds = SchoolYearBounds.forDate(anyDateInCycle)
        val entries = linkedMapOf<LocalDate, String>()

        fun addRange(start: LocalDate, end: LocalDate, title: String) {
            var date = start
            while (!date.isAfter(end)) {
                entries[date] = title
                date = date.plusDays(1)
            }
        }

        addRange(LocalDate.of(bounds.start.year, 10, 26), LocalDate.of(bounds.start.year, 10, 30), "Jesenske počitnice")
        addRange(LocalDate.of(bounds.start.year, 12, 25), LocalDate.of(bounds.end.year, 1, 2), "Novoletne počitnice")
        addRange(LocalDate.of(bounds.end.year, 2, 15), LocalDate.of(bounds.end.year, 2, 19), "Zimske počitnice · Zahodna regija")
        addRange(LocalDate.of(bounds.end.year, 4, 27), LocalDate.of(bounds.end.year, 5, 2), "Prvomajske počitnice")

        // National holidays which matter to a school timetable.
        listOf(
            LocalDate.of(bounds.end.year, 2, 8) to "Prešernov dan",
            easterSunday(bounds.end.year).plusDays(1) to "Velikonočni ponedeljek",
            LocalDate.of(bounds.end.year, 4, 27) to "Dan upora proti okupatorju",
            LocalDate.of(bounds.end.year, 5, 1) to "Praznik dela",
            LocalDate.of(bounds.end.year, 5, 2) to "Praznik dela",
            LocalDate.of(bounds.end.year, 6, 25) to "Dan državnosti",
        ).forEach { (date, title) -> entries[date] = title }

        return entries.map { (date, title) ->
            SchoolCalendarEntryEntity(date = date, title = title, isWorkFreeDay = true)
        }
    }

    private fun easterSunday(year: Int): LocalDate {
        // Gregorian computus, valid for all modern school years.
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
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}

data class SchoolYearBounds(val start: LocalDate, val end: LocalDate) {
    companion object {
        fun forDate(date: LocalDate): SchoolYearBounds {
            val startYear = if (date.monthValue >= 9) date.year else date.year - 1
            return SchoolYearBounds(
                start = LocalDate.of(startYear, 9, 1),
                end = LocalDate.of(startYear + 1, 6, 24),
            )
        }
    }
}
