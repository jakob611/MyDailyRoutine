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
        require(name.isNotEmpty() && name.length <= 60 && project.kind in setOf("CAS", "EE", "CUSTOM") &&
            !project.end.isBefore(project.start) && ChronoUnit.DAYS.between(project.start, project.end) <= 1095 &&
            (project.targetHours == null || project.targetHours > 0 && project.targetHours <= 1000) &&
            (project.targetWords == null || project.targetWords > 0 && project.targetWords <= 20000))
        val clean = project.copy(name = name)
        if (clean.id == 0L) db.goals().insertProject(clean.entity())
        else { check(db.goals().updateProject(clean.entity()) == 1) { "This project was deleted." }; clean.id }
    }
    override suspend fun deleteProject(id: Long) = transaction { db.goals().deleteProject(id) }

    override suspend fun saveActivity(activity: GoalActivity): Long = transaction {
        val title = activity.title.trim()
        require(title.isNotEmpty() && title.length <= 80 &&
            (activity.category == null || activity.category in setOf("CREATIVITY", "ACTIVITY", "SERVICE", "STAGE")) &&
            !activity.end.isBefore(activity.start) && (activity.note == null || activity.note.length <= 2000))
        val clean = activity.copy(title = title, note = activity.note?.trim()?.takeIf { it.isNotEmpty() })
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
    override suspend fun toggleMilestone(id: Long) = transaction {
        db.goals().getMilestone(id)?.let { db.goals().setMilestoneDone(id, !it.isDone) }
    }
    override suspend fun deleteMilestone(id: Long) = transaction { db.goals().deleteMilestone(id) }

    override suspend fun addProgress(entry: GoalProgress) = transaction {
        require(entry.kind in setOf("hour", "word", "reflection") && entry.amount > 0 &&
            (entry.kind != "hour" || entry.amount <= 16) && (entry.kind != "word" || entry.amount <= 20000) &&
            (entry.kind != "reflection" || !entry.note.isNullOrBlank()) &&
            (entry.note == null || entry.note.length <= 2000))
        db.goals().insertProgress(entry.copy(note = entry.note?.trim()?.takeIf { it.isNotEmpty() }).entity())
    }
    override suspend fun deleteProgress(id: Long) = transaction { db.goals().deleteProgress(id) }
}
