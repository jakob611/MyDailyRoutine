package com.example.mydailyroutine.data.seed

import android.content.Context
import androidx.annotation.StringRes
import androidx.room.withTransaction
import com.example.mydailyroutine.R
import com.example.mydailyroutine.data.local.*
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.repository.ExampleDataRepository
import com.example.mydailyroutine.domain.repository.PreferencesRepository
import com.example.mydailyroutine.ui.theme.RoutineColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Weekly layout and milestone examples adapted from agent2 d439b635. Explicit Settings opt-in only.
 * The receipt and all rows commit together. No guessed IDs, replacement, or startup/demo alarms.
 */
class DemoDataSeeder(
    private val context: Context,
    private val db: RoutineDatabase,
    private val preferences: PreferencesRepository,
    private val onChanged: () -> Unit,
) : ExampleDataRepository {
    override val isLoaded = db.demoImports().observeImported(IMPORT_KEY).catch { error ->
        if (error is kotlinx.coroutines.CancellationException) throw error
        emit(false)
    }

    override suspend fun load(): Boolean = withContext(Dispatchers.IO) {
        val teachingEnd = preferences.preferences.first().teachingEndDate
            .coerceIn(SlovenianAcademicCalendar.start, SlovenianAcademicCalendar.end)
        db.withTransaction {
            if (db.demoImports().isImported(IMPORT_KEY)) return@withTransaction false
            suspend fun subject(@StringRes name: Int, colorIndex: Int, duration: Int): Long = db.subjects().insert(
                SubjectEntity(name = context.getString(R.string.demo_prefix, context.getString(name)),
                    colorHex = RoutineColors.subjectSwatches[colorIndex], defaultDurationMinutes = duration),
            )
            val mathId = subject(R.string.demo_math, 0, 90)
            val physicsId = subject(R.string.demo_physics, 1, 90)
            val englishId = subject(R.string.demo_english, 3, 60)
            val historyId = subject(R.string.demo_history, 2, 60)
            val germanId = subject(R.string.demo_german, 4, 60)
            val casId = subject(R.string.demo_tok, 5, 60)
            fun t(h: Int, m: Int = 0): LocalTime = LocalTime.of(h, m)
            fun block(subjectId: Long?, @StringRes title: Int, category: RoutineCategory,
                day: DayOfWeek, start: LocalTime, end: LocalTime) = RoutineBlockEntity(
                subjectId = subjectId, title = context.getString(R.string.demo_prefix, context.getString(title)),
                category = category, dayOfWeek = day, startTime = start, endTime = end,
                isNotificationEnabled = false, validFrom = SlovenianAcademicCalendar.start,
                validUntil = if (category == RoutineCategory.SCHOOL) teachingEnd else SlovenianAcademicCalendar.end,
            )
            fun milestone(subjectId: Long?, @StringRes title: Int, dueDate: LocalDate,
                dueTime: LocalTime? = null, isExam: Boolean = false) = MilestoneEntity(
                subjectId = subjectId, title = context.getString(R.string.demo_milestone_prefix, context.getString(title)),
                dueDate = dueDate, dueTime = dueTime, isExam = isExam,
            )
            val blocks = buildList {
                // ---------- MONDAY ----------
                add(block(null, R.string.demo_wake, RoutineCategory.PERSONAL, DayOfWeek.MONDAY, t(6, 45), t(7, 30)))
                add(block(mathId, R.string.demo_math, RoutineCategory.SCHOOL, DayOfWeek.MONDAY, t(7, 45), t(9, 15)))
                add(block(englishId, R.string.demo_english, RoutineCategory.SCHOOL, DayOfWeek.MONDAY, t(9, 20), t(10, 50)))
                add(block(null, R.string.demo_morning_break, RoutineCategory.REST_BREAK, DayOfWeek.MONDAY, t(10, 50), t(11, 20)))
                add(block(physicsId, R.string.demo_physics, RoutineCategory.SCHOOL, DayOfWeek.MONDAY, t(11, 20), t(12, 50)))
                add(block(null, R.string.demo_lunch, RoutineCategory.REST_BREAK, DayOfWeek.MONDAY, t(12, 50), t(13, 40)))
                add(block(historyId, R.string.demo_history, RoutineCategory.SCHOOL, DayOfWeek.MONDAY, t(13, 40), t(14, 25)))
                add(block(mathId, R.string.demo_math_problem_sets, RoutineCategory.FOCUS_STUDY, DayOfWeek.MONDAY, t(15, 15), t(16, 45)))
                add(block(null, R.string.demo_walk_reset, RoutineCategory.REST_BREAK, DayOfWeek.MONDAY, t(16, 45), t(17, 15)))
                add(block(physicsId, R.string.demo_physics_analysis, RoutineCategory.FOCUS_STUDY, DayOfWeek.MONDAY, t(17, 15), t(18, 45)))
                add(block(null, R.string.demo_gym, RoutineCategory.PERSONAL, DayOfWeek.MONDAY, t(19, 30), t(20, 30)))

                // ---------- TUESDAY ----------
                add(block(null, R.string.demo_wake, RoutineCategory.PERSONAL, DayOfWeek.TUESDAY, t(6, 45), t(7, 30)))
                add(block(physicsId, R.string.demo_physics, RoutineCategory.SCHOOL, DayOfWeek.TUESDAY, t(7, 45), t(9, 15)))
                add(block(germanId, R.string.demo_german, RoutineCategory.SCHOOL, DayOfWeek.TUESDAY, t(9, 20), t(10, 50)))
                add(block(null, R.string.demo_morning_break, RoutineCategory.REST_BREAK, DayOfWeek.TUESDAY, t(10, 50), t(11, 20)))
                add(block(mathId, R.string.demo_math, RoutineCategory.SCHOOL, DayOfWeek.TUESDAY, t(11, 20), t(12, 50)))
                add(block(null, R.string.demo_lunch, RoutineCategory.REST_BREAK, DayOfWeek.TUESDAY, t(12, 50), t(13, 40)))
                add(block(casId, R.string.demo_tok_lesson, RoutineCategory.SCHOOL, DayOfWeek.TUESDAY, t(13, 40), t(14, 25)))
                add(block(englishId, R.string.demo_english_draft, RoutineCategory.FOCUS_STUDY, DayOfWeek.TUESDAY, t(14, 40), t(16, 10)))
                add(block(null, R.string.demo_walk_snack, RoutineCategory.REST_BREAK, DayOfWeek.TUESDAY, t(16, 10), t(16, 40)))
                add(block(historyId, R.string.demo_history_reading, RoutineCategory.FOCUS_STUDY, DayOfWeek.TUESDAY, t(16, 40), t(18, 0)))
                add(block(null, R.string.demo_free_time, RoutineCategory.PERSONAL, DayOfWeek.TUESDAY, t(20, 0), t(22, 0)))

                // ---------- WEDNESDAY ----------
                add(block(null, R.string.demo_wake, RoutineCategory.PERSONAL, DayOfWeek.WEDNESDAY, t(6, 45), t(7, 30)))
                add(block(englishId, R.string.demo_english, RoutineCategory.SCHOOL, DayOfWeek.WEDNESDAY, t(7, 45), t(9, 15)))
                add(block(historyId, R.string.demo_history, RoutineCategory.SCHOOL, DayOfWeek.WEDNESDAY, t(9, 20), t(10, 50)))
                add(block(null, R.string.demo_morning_break, RoutineCategory.REST_BREAK, DayOfWeek.WEDNESDAY, t(10, 50), t(11, 20)))
                add(block(physicsId, R.string.demo_physics, RoutineCategory.SCHOOL, DayOfWeek.WEDNESDAY, t(11, 20), t(12, 50)))
                add(block(null, R.string.demo_lunch, RoutineCategory.REST_BREAK, DayOfWeek.WEDNESDAY, t(12, 50), t(13, 40)))
                add(block(germanId, R.string.demo_german, RoutineCategory.SCHOOL, DayOfWeek.WEDNESDAY, t(13, 40), t(14, 25)))
                add(block(mathId, R.string.demo_math_revision, RoutineCategory.FOCUS_STUDY, DayOfWeek.WEDNESDAY, t(15, 0), t(16, 30)))
                add(block(null, R.string.demo_cas_sport, RoutineCategory.PERSONAL, DayOfWeek.WEDNESDAY, t(17, 0), t(18, 30)))
                add(block(germanId, R.string.demo_german_vocabulary, RoutineCategory.FOCUS_STUDY, DayOfWeek.WEDNESDAY, t(19, 0), t(20, 0)))

                // ---------- THURSDAY ----------
                add(block(null, R.string.demo_wake, RoutineCategory.PERSONAL, DayOfWeek.THURSDAY, t(6, 45), t(7, 30)))
                add(block(mathId, R.string.demo_math, RoutineCategory.SCHOOL, DayOfWeek.THURSDAY, t(7, 45), t(9, 15)))
                add(block(physicsId, R.string.demo_physics, RoutineCategory.SCHOOL, DayOfWeek.THURSDAY, t(9, 20), t(10, 50)))
                add(block(null, R.string.demo_morning_break, RoutineCategory.REST_BREAK, DayOfWeek.THURSDAY, t(10, 50), t(11, 20)))
                add(block(englishId, R.string.demo_english, RoutineCategory.SCHOOL, DayOfWeek.THURSDAY, t(11, 20), t(12, 50)))
                add(block(null, R.string.demo_lunch, RoutineCategory.REST_BREAK, DayOfWeek.THURSDAY, t(12, 50), t(13, 40)))
                add(block(casId, R.string.demo_cas_project, RoutineCategory.PROJECT, DayOfWeek.THURSDAY, t(13, 40), t(15, 10)))
                add(block(physicsId, R.string.demo_physics_focus, RoutineCategory.FOCUS_STUDY, DayOfWeek.THURSDAY, t(15, 30), t(17, 0)))
                add(block(null, R.string.demo_dinner_rest, RoutineCategory.REST_BREAK, DayOfWeek.THURSDAY, t(17, 0), t(18, 30)))
                add(block(historyId, R.string.demo_history_draft, RoutineCategory.FOCUS_STUDY, DayOfWeek.THURSDAY, t(18, 30), t(20, 0)))

                // ---------- FRIDAY ----------
                add(block(null, R.string.demo_wake, RoutineCategory.PERSONAL, DayOfWeek.FRIDAY, t(6, 45), t(7, 30)))
                add(block(germanId, R.string.demo_german, RoutineCategory.SCHOOL, DayOfWeek.FRIDAY, t(7, 45), t(9, 15)))
                add(block(historyId, R.string.demo_history, RoutineCategory.SCHOOL, DayOfWeek.FRIDAY, t(9, 20), t(10, 50)))
                add(block(null, R.string.demo_morning_break, RoutineCategory.REST_BREAK, DayOfWeek.FRIDAY, t(10, 50), t(11, 20)))
                add(block(mathId, R.string.demo_math, RoutineCategory.SCHOOL, DayOfWeek.FRIDAY, t(11, 20), t(12, 50)))
                add(block(null, R.string.demo_lunch, RoutineCategory.REST_BREAK, DayOfWeek.FRIDAY, t(12, 50), t(13, 40)))
                add(block(physicsId, R.string.demo_physics_lab, RoutineCategory.SCHOOL, DayOfWeek.FRIDAY, t(13, 40), t(15, 10)))
                add(block(null, R.string.demo_wind_down, RoutineCategory.REST_BREAK, DayOfWeek.FRIDAY, t(15, 10), t(17, 0)))
                add(block(null, R.string.demo_friends, RoutineCategory.PERSONAL, DayOfWeek.FRIDAY, t(18, 0), t(23, 0)))

                // ---------- SATURDAY ----------
                add(block(null, R.string.demo_sleep_breakfast, RoutineCategory.PERSONAL, DayOfWeek.SATURDAY, t(8, 30), t(10, 0)))
                add(block(mathId, R.string.demo_math_weekly_review, RoutineCategory.FOCUS_STUDY, DayOfWeek.SATURDAY, t(10, 15), t(11, 45)))
                add(block(null, R.string.demo_walk_outside, RoutineCategory.REST_BREAK, DayOfWeek.SATURDAY, t(11, 45), t(12, 30)))
                add(block(englishId, R.string.demo_ee_project, RoutineCategory.PROJECT, DayOfWeek.SATURDAY, t(13, 0), t(15, 0)))
                add(block(null, R.string.demo_sport, RoutineCategory.PERSONAL, DayOfWeek.SATURDAY, t(16, 0), t(17, 30)))
                add(block(null, R.string.demo_social, RoutineCategory.PERSONAL, DayOfWeek.SATURDAY, t(19, 0), t(23, 30)))

                // ---------- SUNDAY ----------
                add(block(null, R.string.demo_sleep_in, RoutineCategory.PERSONAL, DayOfWeek.SUNDAY, t(9, 0), t(10, 30)))
                add(block(physicsId, R.string.demo_physics_notes, RoutineCategory.FOCUS_STUDY, DayOfWeek.SUNDAY, t(11, 0), t(12, 30)))
                add(block(null, R.string.demo_lunch_family, RoutineCategory.REST_BREAK, DayOfWeek.SUNDAY, t(12, 30), t(14, 0)))
                add(block(casId, R.string.demo_week_planning, RoutineCategory.PROJECT, DayOfWeek.SUNDAY, t(14, 30), t(15, 30)))
                add(block(null, R.string.demo_recharge, RoutineCategory.REST_BREAK, DayOfWeek.SUNDAY, t(15, 30), t(21, 0)))
                add(block(null, R.string.demo_sleep, RoutineCategory.PERSONAL, DayOfWeek.SUNDAY, t(22, 30), t(6, 45)))
            }
            val milestones = listOf(
                milestone(subjectId = mathId, title = R.string.demo_math_ia, dueDate = LocalDate.of(2026, 10, 16), dueTime = t(15, 0)),
                milestone(subjectId = physicsId, title = R.string.demo_physics_data, dueDate = LocalDate.of(2026, 11, 13), dueTime = t(15, 0)),
                milestone(subjectId = englishId, title = R.string.demo_ee_draft, dueDate = LocalDate.of(2026, 12, 4), dueTime = t(16, 0)),
                milestone(subjectId = historyId, title = R.string.demo_history_ia, dueDate = LocalDate.of(2027, 1, 22), dueTime = t(15, 0)),
                milestone(subjectId = mathId, title = R.string.demo_math_mock, dueDate = LocalDate.of(2027, 2, 3), isExam = true),
                milestone(subjectId = physicsId, title = R.string.demo_physics_mock, dueDate = LocalDate.of(2027, 2, 4), isExam = true),
                milestone(subjectId = englishId, title = R.string.demo_english_mock, dueDate = LocalDate.of(2027, 2, 5), isExam = true),
                milestone(subjectId = casId, title = R.string.demo_tok_essay, dueDate = LocalDate.of(2027, 3, 5), dueTime = t(15, 0)),
                milestone(subjectId = germanId, title = R.string.demo_german_oral, dueDate = LocalDate.of(2027, 3, 18), dueTime = t(9, 0), isExam = true),
                milestone(subjectId = physicsId, title = R.string.demo_physics_final_ia, dueDate = LocalDate.of(2027, 3, 19), dueTime = t(15, 0)),
                milestone(subjectId = mathId, title = R.string.demo_math_exam, dueDate = LocalDate.of(2027, 5, 5), dueTime = t(9, 0), isExam = true),
                milestone(subjectId = physicsId, title = R.string.demo_physics_exam, dueDate = LocalDate.of(2027, 5, 6), dueTime = t(9, 0), isExam = true),
                milestone(subjectId = englishId, title = R.string.demo_english_exam, dueDate = LocalDate.of(2027, 5, 10), dueTime = t(9, 0), isExam = true),
                milestone(subjectId = historyId, title = R.string.demo_history_exam, dueDate = LocalDate.of(2027, 5, 12), dueTime = t(9, 0), isExam = true),
                milestone(subjectId = germanId, title = R.string.demo_german_exam, dueDate = LocalDate.of(2027, 5, 13), dueTime = t(9, 0), isExam = true)
            )
            blocks.forEach { db.routines().insert(it) }
            milestones.forEach { db.milestones().insert(it) }
            db.demoImports().insert(DemoImportEntity(IMPORT_KEY, Instant.now().toEpochMilli()))
            onChanged() // The queued refresh transaction waits for this transaction to commit.
            true
        }
    }

    companion object { const val IMPORT_KEY = "ib-west-2026-27-v1" }
}
