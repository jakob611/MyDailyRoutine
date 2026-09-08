package com.example.mydailyroutine.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.core.database.daos.*
import com.example.mydailyroutine.features.timeline.data.RoomTimelineRepository
import com.example.mydailyroutine.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomIntegrityTest {
    private lateinit var db: RoutineDatabase
    private lateinit var repository: RoomTimelineRepository
    private val date = LocalDate.of(2026, 9, 7)
    private val template = RoutineBlueprint(subjectId = null, title = "Study", category = RoutineCategory.FOCUS_ANALYTICAL,
        dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(9, 0), isNotificationEnabled = true)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RoutineDatabase::class.java)
            .addCallback(SeedAndIntegrityCallback(ApplicationProvider.getApplicationContext<android.content.Context>().resources)).build()
        repository = RoomTimelineRepository(db, {})
    }
    @After fun tearDown() { db.close() }

    @Test fun calendarIsCompleteOnFirstRead(): Unit = runBlocking {
        val entries = db.calendar().inRange(LocalDate.of(2027, 2, 22), LocalDate.of(2027, 2, 26))
        assertEquals(5, entries.count { it.title.startsWith("Zimske") })
        assertTrue(entries.all { it.isWorkFreeDay })
        db.openHelper.readableDatabase.query("PRAGMA foreign_keys").use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
        }
    }

    @Test fun subjectDeletionDetachesButRoutineDeletionCascades(): Unit = runBlocking {
        val subjectId = repository.saveSubject(Subject(name = "Math HL", colorHex = 0xFF4499CC, defaultDurationMinutes = 45))
        val routineId = repository.saveRoutine(template.copy(subjectId = subjectId))
        repository.saveOverride(EventOverride(routineBlockId = routineId, overrideDate = date, customTitle = "Revision"))
        repository.setCompleted(routineId, date, true)
        repository.saveMilestone(Milestone(subjectId = subjectId, title = "IA", dueDate = date, dueTime = null, isExam = false))
        db.alarmDeliveries().claim(AlarmDeliveryEntity(routineId, date, "BLOCK_PREVIEW", 1000))
        repository.deleteSubject(subjectId)
        val snapshot = repository.snapshot(date, date)
        assertNull(snapshot.routines.single().subjectId)
        assertNull(snapshot.milestones.single().subjectId)
        assertEquals(1, snapshot.overrides.size)
        repository.deleteRoutine(routineId)
        assertTrue(db.overrides().inRange(date, date).isEmpty())
        assertTrue(db.completions().inRange(date, date).isEmpty())
        assertEquals(1, db.milestones().inRange(date, date).size)
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM alarm_deliveries").use {
            assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
        }
    }

    @Test fun foreignKeysRejectOrphans(): Unit = runBlocking {
        constraint { repository.saveRoutine(template.copy(subjectId = 9999)) }
        constraint { db.overrides().insert(EventOverrideEntity(routineBlockId = 9999, overrideDate = date, isCancelled = false,
            customStartTime = null, customEndTime = null, customTitle = null)) }
        constraint { db.completions().insert(RoutineCompletionEntity(9999, date)) }
        constraint { repository.saveMilestone(Milestone(subjectId = 9999, title = "Orphan", dueDate = date, dueTime = null, isExam = false)) }
    }

    @Test fun oneOverridePerDateAndRepositoryUpdatesWithoutReplace(): Unit = runBlocking {
        val id = repository.saveRoutine(template)
        repository.saveOverride(EventOverride(routineBlockId = id, overrideDate = date, customTitle = "First"))
        val storedId = db.overrides().get(id, date)!!.id
        repository.saveOverride(EventOverride(routineBlockId = id, overrideDate = date, customTitle = "Second"))
        assertEquals(storedId, db.overrides().get(id, date)!!.id)
        assertEquals("Second", db.overrides().get(id, date)!!.customTitle)
        constraint { db.overrides().insert(EventOverrideEntity(routineBlockId = id, overrideDate = date, isCancelled = false,
            customStartTime = null, customEndTime = null, customTitle = "Duplicate")) }
    }

    @Test fun sqliteTriggersProtectEvenRawWrites(): Unit = runBlocking {
        val id = repository.saveRoutine(template)
        constraint { db.openHelper.writableDatabase.execSQL("UPDATE routine_blocks SET durationMinutes = 0 WHERE id = ?", arrayOf(id)) }
        constraint { db.openHelper.writableDatabase.execSQL("UPDATE routine_blocks SET category = 'INVALID' WHERE id = ?", arrayOf(id)) }
        constraint { db.openHelper.writableDatabase.execSQL("UPDATE routine_blocks SET title = '  ' WHERE id = ?", arrayOf(id)) }
        constraint { db.completions().insert(RoutineCompletionEntity(id, date.plusDays(1))) }
        assertEquals("Study", db.routines().get(id)!!.title)
    }

    @Test fun alarmClaimsAreIdempotent(): Unit = runBlocking {
        val id = repository.saveRoutine(template)
        val claim = AlarmDeliveryEntity(id, date, "BLOCK_PREVIEW", 1000)
        assertNotEquals(-1L, db.alarmDeliveries().claim(claim))
        assertEquals(-1L, db.alarmDeliveries().claim(claim))
    }

    @Test fun reactiveResolutionSeesAtomicTransactionsAndSubjectChanges(): Unit = runBlocking {
        val emissions = Channel<List<ResolvedTimelineItem>>(Channel.UNLIMITED)
        val collector = launch(Dispatchers.Default) { repository.getTimelineForDate(date).collect { emissions.send(it) } }
        suspend fun next(predicate: (List<ResolvedTimelineItem>) -> Boolean): List<ResolvedTimelineItem> = withTimeout(5000) {
            var value = emissions.receive()
            while (!predicate(value)) value = emissions.receive()
            value
        }
        try {
            next { it.isEmpty() }
            val subjectId = repository.saveSubject(Subject(name = "Math", colorHex = 0xFF4499CC, defaultDurationMinutes = 45))
            val id = repository.saveRoutine(template.copy(subjectId = subjectId))
            next { it.singleOrNull()?.title == "Study" }
            repository.saveSubject(Subject(subjectId, "Math HL", 0xFF4499CC, 45))
            assertEquals("Math HL", next { it.singleOrNull()?.subject?.name == "Math HL" }.single().subject?.name)
            db.withTransaction {
                val original = db.routines().get(id)!!
                db.routines().update(original.copy(title = "Intermediate title"))
                db.overrides().insert(EventOverrideEntity(routineBlockId = id, overrideDate = date, isCancelled = true,
                    customStartTime = null, customEndTime = null, customTitle = null))
            }
            next { values ->
                assertFalse(values.any { it.title == "Intermediate title" })
                values.isEmpty()
            }
        } finally { collector.cancelAndJoin(); emissions.close() }
    }

    private suspend fun constraint(action: suspend () -> Unit) {
        try { action(); fail("Expected SQLiteConstraintException") }
        catch (_: SQLiteConstraintException) { /* Expected: the entire statement is rolled back. */ }
    }
}
