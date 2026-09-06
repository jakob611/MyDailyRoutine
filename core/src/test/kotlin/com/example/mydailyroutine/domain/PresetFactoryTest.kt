package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.presets.*
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class PresetFactoryTest {
    private val subject = Subject(42, "Matematika", 0xFF3B82F6, 45)
    @Test fun `saved subject automatically yields lesson study and test`() {
        val presets = PresetFactory.forSubject(subject)
        assertEquals(listOf(PresetKind.SUBJECT_LESSON, PresetKind.SUBJECT_STUDY, PresetKind.SUBJECT_TEST), presets.map { it.kind })
        assertTrue(presets.all { it.subjectId == 42L && it.subjectName == "Matematika" && it.colorHex == subject.colorHex && it.durationMinutes == 45 })
        assertEquals(RoutineCategory.SCHOOL, presets[0].category)
        assertEquals(RoutineCategory.FOCUS_STUDY, presets[1].category)
        assertTrue(presets[2].isExam)
    }
    @Test fun `IDs are unique across subjects and purposes`() {
        val presets = PresetFactory.forSubjects(listOf(subject, subject.copy(id = 43)))
        assertEquals(6, presets.size)
        assertEquals(6, presets.map { it.key }.toSet().size)
    }
    @Test fun `renaming recoloring and changing duration updates presets without duplicating`() {
        val before = PresetFactory.forSubject(subject)
        val after = PresetFactory.forSubject(subject.copy(name = "Matematika HL", colorHex = 0xFF8B5CF6, defaultDurationMinutes = 90))
        assertEquals(before.map { it.key }, after.map { it.key })
        assertTrue(after.all { it.subjectName == "Matematika HL" && it.colorHex == 0xFF8B5CF6L && it.durationMinutes == 90 })
    }
    @Test fun `deleting the source removes its presets with no orphan rows`() {
        assertTrue(PresetFactory.forSubjects(emptyList()).isEmpty())
        val remaining = PresetFactory.forSubjects(listOf(subject.copy(id = 2)))
        assertFalse(remaining.any { it.subjectId == subject.id })
    }
    @Test fun `duplicate source rows cannot duplicate presets`() { assertEquals(3, PresetFactory.forSubjects(listOf(subject, subject)).size) }
    @Test fun `preset duration spans midnight accurately`() {
        val window = PresetFactory.forSubject(subject)[1].window(monday, LocalTime.of(23, 45))
        assertEquals(monday.plusDays(1).atTime(0, 30), window.end)
    }
    @Test fun `standard presets retain requested durations and exam semantics`() {
        val presets = PresetFactory.standard()
        assertEquals(90, presets.single { it.kind == PresetKind.DEEP_WORK }.durationMinutes)
        assertEquals(45, presets.single { it.kind == PresetKind.POMODORO }.durationMinutes)
        assertEquals(15, presets.single { it.kind == PresetKind.WALK }.durationMinutes)
        assertEquals(RoutineCategory.REST_BREAK, presets.single { it.kind == PresetKind.WALK }.category)
        assertTrue(presets.single { it.kind == PresetKind.EXAM }.isExam)
    }
}
