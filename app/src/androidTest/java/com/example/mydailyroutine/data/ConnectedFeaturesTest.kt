package com.example.mydailyroutine.data

import com.example.mydailyroutine.app.presentation.RoutineViewModel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.core.database.daos.*
import com.example.mydailyroutine.features.timeline.data.RoomTimelineRepository
import com.example.mydailyroutine.features.examples.data.DemoDataSeeder
import com.example.mydailyroutine.domain.health.*
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.presets.*
import com.example.mydailyroutine.domain.repository.PreferencesRepository
import com.example.mydailyroutine.core.presentation.*
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectedFeaturesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: RoutineDatabase
    private lateinit var repo: RoomTimelineRepository
    private val date = LocalDate.of(2026, 9, 7)
    private val prefs = MemoryPreferences()
    @Before fun before() {
        db = Room.inMemoryDatabaseBuilder(context, RoutineDatabase::class.java).addCallback(SeedAndIntegrityCallback(context.resources)).build()
        repo = RoomTimelineRepository(db, {})
    }
    @After fun after() { db.close() }

    @Test fun demoIsExplicitAtomicIdempotentAndSilent(): Unit = runBlocking {
        val demo = DemoDataSeeder(context, db, prefs, {}, com.example.mydailyroutine.core.designsystem.theme.RoutineColors.subjectSwatches)
        assertTrue(repo.snapshot(date, date).routines.isEmpty())
        assertFalse(demo.isLoaded.first())
        assertTrue(demo.load())
        assertFalse(demo.load())
        val snapshot = repo.snapshot(date, date.plusMonths(11))
        assertEquals(6, snapshot.subjects.size)
        assertEquals(63, snapshot.routines.size)
        assertEquals(15, snapshot.milestones.size)
        assertTrue(snapshot.routines.none { it.isNotificationEnabled })
        assertTrue(snapshot.milestones.all { it.title.contains(context.getString(R.string.demo_prefix, "").trim()) })
        assertTrue(demo.isLoaded.first())
    }

    @Test fun persistedSubjectsDriveReusablePresetsAndRealExamEntries(): Unit = runBlocking {
        val id = repo.saveSubject(Subject(name = "Matematika", colorHex = 0xFF3B82F6, defaultDurationMinutes = 45))
        suspend fun presets() = PresetFactory.forSubjects(repo.observeSnapshot(date, date).first().subjects)
        assertEquals(3, presets().size)
        val exam = presets().single { it.kind == PresetKind.SUBJECT_TEST }
        repo.saveMilestone(Milestone(subjectId = exam.subjectId, title = context.getString(R.string.preset_test, exam.subjectName), dueDate = date, dueTime = LocalTime.NOON, isExam = true))
        assertTrue(repo.snapshot(date, date).milestones.single().isExam)
        repo.saveSubject(Subject(id, "Matematika HL", 0xFF8B5CF6, 90))
        assertTrue(presets().all { it.subjectName == "Matematika HL" && it.durationMinutes == 90 })
        repo.deleteSubject(id)
        assertTrue(presets().isEmpty())
        assertNull(repo.snapshot(date, date).milestones.single().subjectId)
    }

    @Test fun maximumLengthSubjectStillCreatesValidLocalizedEntries(): Unit = runBlocking {
        val id = repo.saveSubject(Subject(name = "M".repeat(120), colorHex = 0xFF3B82F6, defaultDurationMinutes = 45))
        val presets = PresetFactory.forSubject(repo.snapshot(date, date).subjects.single())
        for (preset in presets) {
            val title = preset.title(context)
            assertTrue(title.length <= 120)
            assertTrue(title.endsWith(context.getString(preset.kind.labelRes(), "").trim()))
            if (preset.isExam) repo.saveMilestone(Milestone(subjectId = id, title = title, dueDate = date, dueTime = null, isExam = true))
            else repo.saveRoutine(RoutineBlueprint(subjectId = id, title = title, category = preset.category, dayOfWeek = date.dayOfWeek,
                startTime = LocalTime.of(8, 0), endTime = LocalTime.of(8, 45), isNotificationEnabled = false, validFrom = date, validUntil = date))
        }
        assertEquals(3, repo.getTimelineForDate(date).first().size)
    }

    @Test fun warningActionPersistsOneOffRecoveryAndPreservesNextWeek(): Unit = runBlocking {
        repo.saveRoutine(RoutineBlueprint(subjectId = null, title = "Učenje", category = RoutineCategory.FOCUS_ANALYTICAL,
            dayOfWeek = date.dayOfWeek, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(10, 0), isNotificationEnabled = false))
        val original = repo.getTimelineForDate(date).first().single()
        val result = repo.insertRecovery(date, WarningType.CONCENTRATION_LIMIT, original.key, HealthConfig(), context.getString(R.string.auto_recovery_title), context.getString(R.string.continuation_suffix))
        assertEquals(RecoveryStatus.FOCUS_SPLIT, result.status)
        val today = repo.getTimelineForDate(date).first()
        assertEquals(3, today.size)
        assertEquals(120, ScheduleMetrics.forDay(today).focusMinutes)
        assertEquals(15, ScheduleMetrics.forDay(today).recoveryMinutes)
        val nextWeek = repo.getTimelineForDate(date.plusWeeks(1)).first().filterIsInstance<ResolvedTimelineItem.Block>()
        assertEquals(1, nextWeek.size)
        assertEquals(120, nextWeek.single().durationMinutes)
        val duplicate = repo.insertRecovery(date, WarningType.CONCENTRATION_LIMIT, original.key, HealthConfig(), context.getString(R.string.auto_recovery_title), context.getString(R.string.continuation_suffix))
        assertEquals(RecoveryStatus.ALREADY_HANDLED, duplicate.status)
    }

    @Test fun thresholdPreferenceChangeRecomputesViewModelWithoutDatabaseEdit(): Unit = runBlocking {
        repo.saveRoutine(RoutineBlueprint(subjectId = null, title = "Fokus", category = RoutineCategory.FOCUS_ANALYTICAL, dayOfWeek = date.dayOfWeek,
            startTime = LocalTime.of(8, 0), endTime = LocalTime.of(9, 15), isNotificationEnabled = false))
        val model = withContext(Dispatchers.Main) { RoutineViewModel(repo, prefs, SavedStateHandle(mapOf("date" to date.toEpochDay())), DemoDataSeeder(context, db, prefs, {}, com.example.mydailyroutine.core.designsystem.theme.RoutineColors.subjectSwatches), com.example.mydailyroutine.features.planning.data.RoomPlanningRepository(db, repo, {}),
                com.example.mydailyroutine.features.execution.data.RoomExecutionRepository(db, repo, com.example.mydailyroutine.features.planning.data.RoomPlanningRepository(db, repo, {}), {})) }
        val collector = launch { model.state.collect() }
        try {
            val initial = withTimeout(10000) { model.state.first { !it.content.isLoading } }
            assertTrue(initial.content.days.getValue(date).warnings.none { it.type == WarningType.CONCENTRATION_LIMIT })
            prefs.setHealthConfig(HealthConfig(focusLimitMinutes = 60))
            val changed = withTimeout(10000) { model.state.first { it.preferences.health.focusLimitMinutes == 60 && it.content.days[date]?.warnings?.any { warning -> warning.type == WarningType.CONCENTRATION_LIMIT } == true } }
            assertEquals(60, changed.preferences.health.focusLimitMinutes)
        } finally {
            collector.cancelAndJoin()
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test fun versionOneMigratesWithoutLosingUserRows(): Unit = runBlocking(Dispatchers.IO) {
        val name = "migration-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { legacy ->
                // Triggers contain semicolons; split only at SQL statement terminators, not inside BEGIN/END.
                val sql = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("schema-v1.sql").bufferedReader().use { it.readText() }
                val statements = Regex("(?ms)CREATE TRIGGER.*?END;|CREATE TABLE.*?;|CREATE (?:UNIQUE )?INDEX.*?;|PRAGMA.*?;").findAll(sql)
                statements.forEach { legacy.execSQL(it.value.trim().removeSuffix(";")) }
                legacy.execSQL("INSERT INTO subjects VALUES (1, 'Matematika', 4282090230, 45)")
            }
            val migrated = Room.databaseBuilder(context, RoutineDatabase::class.java, name).addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6)
                .addCallback(SeedAndIntegrityCallback(context.resources)).build()
            try {
                assertEquals("Matematika", migrated.subjects().getAll().single().name)
                assertFalse(migrated.demoImports().isImported(DemoDataSeeder.IMPORT_KEY))
                migrated.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }

    private class MemoryPreferences : PreferencesRepository {
        override val preferences = MutableStateFlow(SchedulePreferences())
        override suspend fun setMuteDuringSchoolHours(muted: Boolean) { preferences.update { it.copy(muteDuringSchoolHours = muted) } }
        override suspend fun setSchoolWindow(start: LocalTime, end: LocalTime) { preferences.update { it.copy(schoolStart = start, schoolEnd = end) } }
        override suspend fun setTeachingEndDate(date: LocalDate) { preferences.update { it.copy(teachingEndDate = date) } }
        override suspend fun setAutomaticHealingEnabled(enabled: Boolean) { preferences.update { it.copy(automaticHealingEnabled = enabled) } }
        override suspend fun setHapticsEnabled(enabled: Boolean) { preferences.update { it.copy(hapticsEnabled = enabled) } }
        override suspend fun setPlanningConfig(config: com.example.mydailyroutine.domain.planning.PlanningConfig) { preferences.update { it.copy(planning = config) } }
        override suspend fun setHealthConfig(config: HealthConfig) { preferences.update { it.copy(health = config) } }
    }
}
