package com.example.mydailyroutine.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.domain.model.GoalActivity
import com.example.mydailyroutine.domain.model.GoalMilestone
import com.example.mydailyroutine.domain.model.GoalProgress
import com.example.mydailyroutine.domain.model.GoalsProject
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.domain.model.Task
import com.example.mydailyroutine.features.goals.data.RoomGoalsRepository
import com.example.mydailyroutine.features.planning.data.RoomPlanningRepository
import com.example.mydailyroutine.features.timeline.data.RoomTimelineRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TasksGoalsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: RoutineDatabase
    private lateinit var timeline: RoomTimelineRepository
    private lateinit var planning: RoomPlanningRepository
    private lateinit var goals: RoomGoalsRepository
    private val date = LocalDate.of(2026, 9, 14)

    @Before fun before() {
        db = Room.inMemoryDatabaseBuilder(context, RoutineDatabase::class.java).addCallback(SeedAndIntegrityCallback(context.resources)).build()
        timeline = RoomTimelineRepository(db, {})
        planning = RoomPlanningRepository(db, timeline, {})
        goals = RoomGoalsRepository(db, {})
    }
    @After fun after() { db.close() }

    @Test fun taskFlowRangeToggleAndSubjectDetach(): Unit = runBlocking(Dispatchers.IO) {
        val subjectId = timeline.saveSubject(Subject(name = "Ekonomija", colorHex = 0xFF3B82F6, defaultDurationMinutes = 45))
        val id = planning.saveTask(Task(subjectId = subjectId, title = "Domača naloga", dueDate = date, note = "poglavje 3", createdAtEpochMillis = System.currentTimeMillis()))
        assertTrue(id > 0)
        assertEquals("Domača naloga", db.tasks().get(id)!!.title)
        assertEquals(1, timeline.snapshot(date, date).tasks.size)
        assertTrue(timeline.snapshot(date.minusDays(3), date.minusDays(1)).tasks.isEmpty())
        planning.toggleTask(id)
        assertNotNull(db.tasks().get(id)!!.completedAtEpochMillis)
        planning.toggleTask(id)
        assertNull(db.tasks().get(id)!!.completedAtEpochMillis)
        assertTrue(runCatching { planning.saveTask(Task(title = "   ", subjectId = null, dueDate = null, createdAtEpochMillis = 1)) }.isFailure)
        constraint { db.openHelper.writableDatabase.execSQL("INSERT INTO tasks(id,title,createdAtEpochMillis) VALUES(404,'  ',1000)") }
        timeline.deleteSubject(subjectId)
        assertNull(db.tasks().get(id)!!.subjectId)
        planning.toggleTask(id)
        planning.clearCompletedTasks()
        assertTrue(db.tasks().inRange(date.minusYears(1), date.plusYears(1)).isEmpty())
    }

    @Test fun goalsProjectsActivitiesProgressAndCascade(): Unit = runBlocking(Dispatchers.IO) {
        val start = date
        val end = date.plusMonths(18)
        val projectId = goals.saveProject(GoalsProject(name = "CAS", kind = "CAS", start = start, end = end, targetHours = 150.0))
        val activityId = goals.saveActivity(GoalActivity(projectId = projectId, title = "Tekanje", category = "ACTIVITY", start = start, end = end))
        val milestoneId = goals.saveMilestone(GoalMilestone(projectId = projectId, title = "Srečanje 1", dueDate = start.plusDays(30)))
        goals.addProgress(GoalProgress(projectId = projectId, activityId = activityId, kind = "hour", amount = 2.0, date = start))
        goals.toggleMilestone(milestoneId)
        assertTrue(db.goals().getMilestone(milestoneId)!!.isDone)
        assertEquals(2.0, db.goals().observeProgress().first().sumOf { it.amount }, 0.001)
        assertTrue(runCatching { goals.addProgress(GoalProgress(projectId = projectId, activityId = activityId, kind = "hour", amount = 17.0, date = start)) }.isFailure)
        assertTrue(runCatching { goals.saveActivity(GoalActivity(projectId = projectId, title = "Napaka", category = "UNKNOWN", start = start, end = end)) }.isFailure)
        goals.setActivityScheduled(activityId, true)
        assertTrue(db.goals().observeActivities().first().single().isScheduled)
        goals.deleteProject(projectId)
        assertTrue(db.goals().observeActivities().first().isEmpty())
        assertTrue(db.goals().observeMilestones().first().isEmpty())
        assertTrue(db.goals().observeProgress().first().isEmpty())
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }

    private suspend fun constraint(action: suspend () -> Unit) {
        try { action(); fail("Expected SQLiteConstraintException") }
        catch (_: SQLiteConstraintException) { /* Expected: the trigger rejects the raw write. */ }
    }
}
