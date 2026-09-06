package com.example.mydailyroutine.domain.presets

import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.Subject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Display names come from Android resources; IDs and behavior are locale-independent. */
enum class PresetKind { DEEP_WORK, POMODORO, WALK, IB_REVISION, EXAM, SUBJECT_LESSON, SUBJECT_STUDY, SUBJECT_TEST }

data class QuickAddPreset(
    val key: String,
    val kind: PresetKind,
    val durationMinutes: Int,
    val category: RoutineCategory,
    val subjectId: Long? = null,
    val subjectName: String? = null,
    val colorHex: Long? = null,
) {
    val isExam: Boolean get() = kind == PresetKind.EXAM || kind == PresetKind.SUBJECT_TEST
    fun window(date: LocalDate, start: LocalTime): PresetWindow = PresetWindow(
        date.atTime(start), date.atTime(start).plusMinutes(durationMinutes.toLong()),
    )
}
data class PresetWindow(val start: LocalDateTime, val end: LocalDateTime)

/** Subjects are the source of truth: edit/delete a subject and all three presets update/disappear. */
object PresetFactory {
    fun forSubject(subject: Subject): List<QuickAddPreset> = listOf(
        PresetKind.SUBJECT_LESSON to RoutineCategory.SCHOOL,
        PresetKind.SUBJECT_STUDY to RoutineCategory.FOCUS_STUDY,
        PresetKind.SUBJECT_TEST to RoutineCategory.SCHOOL,
    ).map { (kind, category) ->
        QuickAddPreset("subject:${subject.id}:${kind.name}", kind, subject.defaultDurationMinutes, category,
            subject.id, subject.name, subject.colorHex)
    }
    fun forSubjects(subjects: List<Subject>): List<QuickAddPreset> = subjects.distinctBy { it.id }.flatMap(::forSubject)
    fun standard(): List<QuickAddPreset> = listOf(
        QuickAddPreset("deep-work", PresetKind.DEEP_WORK, 90, RoutineCategory.FOCUS_STUDY),
        QuickAddPreset("pomodoro", PresetKind.POMODORO, 45, RoutineCategory.FOCUS_STUDY),
        QuickAddPreset("walk", PresetKind.WALK, 15, RoutineCategory.REST_BREAK),
        QuickAddPreset("ib-revision", PresetKind.IB_REVISION, 90, RoutineCategory.FOCUS_STUDY),
        QuickAddPreset("exam", PresetKind.EXAM, 45, RoutineCategory.SCHOOL),
    )
}
