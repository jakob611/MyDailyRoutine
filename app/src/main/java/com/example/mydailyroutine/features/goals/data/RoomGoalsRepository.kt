package com.example.mydailyroutine.features.goals.data

import androidx.room.withTransaction
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.repository.GoalsRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Long-term plan storage; every write funnels through one transaction so the flows stay consistent. */
class RoomGoalsRepository(private val db: RoutineDatabase, private val onChanged: () -> Unit) : GoalsRepository {
    override val projects = db.goals().observeProjects().map { it.map { row -> row.domain() } }
    override val activities = db.goals().observeActivities().map { it.map { row -> row.domain() } }
    override val milestones = db.goals().observeMilestones().map { it.map { row -> row.domain() } }
    override val progress = db.goals().observeProgress().map { it.map { row -> row.domain() } }

    private suspend fun <T> transaction(operation: suspend () -> T): T = withContext(Dispatchers.IO) {
        db.withTransaction { operation().also { onChanged() } }
    }

    override suspend fun saveProject(project: GoalsProject): Long = transaction {
        val name = project.name.trim()
        val start = project.start
        val end = project.end
        val hours = project.targetHours
        val words = project.targetWords
        require(name.isNotEmpty() && name.length <= 60 && project.kind in setOf("CAS", "EE", "CUSTOM") &&
            !end.isBefore(start) && ChronoUnit.DAYS.between(start, end) <= 1095 &&
            (hours == null || hours > 0 && hours <= 1000) &&
            (words == null || words > 0 && words <= 20000))
        val clean = project.copy(name = name)
        if (clean.id == 0L) db.goals().insertProject(clean.entity())
        else { check(db.goals().updateProject(clean.entity()) == 1) { "This project was deleted." }; clean.id }
    }
    override suspend fun deleteProject(id: Long) = transaction { db.goals().deleteProject(id) }

    override suspend fun saveActivity(activity: GoalActivity): Long = transaction {
        val title = activity.title.trim()
        val category = activity.category
        val start = activity.start
        val end = activity.end
        val note = activity.note
        require(title.isNotEmpty() && title.length <= 80 &&
            (category == null || category in setOf("CREATIVITY", "ACTIVITY", "SERVICE", "STAGE")) &&
            !end.isBefore(start) && (note == null || note.length <= 2000))
        val clean = activity.copy(title = title, note = note?.trim()?.takeIf { it.isNotEmpty() })
        if (clean.id == 0L) db.goals().insertActivity(clean.entity())
        else { check(db.goals().updateActivity(clean.entity()) == 1) { "This activity was deleted." }; clean.id }
    }
    override suspend fun deleteActivity(id: Long) = transaction { db.goals().deleteActivity(id) }
    override suspend fun setActivityScheduled(id: Long, scheduled: Boolean) = transaction { db.goals().setActivityScheduled(id, scheduled) }

    override suspend fun saveMilestone(milestone: GoalMilestone): Long = transaction {
        val title = milestone.title.trim()
        require(title.isNotEmpty() && title.length <= 80)
        val clean = milestone.copy(title = title)
        if (clean.id == 0L) db.goals().insertMilestone(clean.entity())
        else { check(db.goals().updateMilestone(clean.entity()) == 1) { "This milestone was deleted." }; clean.id }
    }
    override suspend fun toggleMilestone(id: Long) {
        transaction { db.goals().getMilestone(id)?.let { db.goals().setMilestoneDone(id, !it.isDone) } }
    }
    override suspend fun deleteMilestone(id: Long) = transaction { db.goals().deleteMilestone(id) }

    override suspend fun addProgress(entry: GoalProgress) = transaction {
        val kind = entry.kind
        val amount = entry.amount
        val note = entry.note
        require(kind in setOf("hour", "word", "reflection") && amount > 0 &&
            (kind != "hour" || amount <= 16) && (kind != "word" || amount <= 20000) &&
            (kind != "reflection" || !note.isNullOrBlank()) &&
            (note == null || note.length <= 2000))
        db.goals().insertProgress(entry.copy(note = note?.trim()?.takeIf { it.isNotEmpty() }).entity())
    }
    override suspend fun deleteProgress(id: Long) = transaction { db.goals().deleteProgress(id) }
}
