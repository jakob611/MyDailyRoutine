package com.example.mydailyroutine.features.backup.data

import androidx.room.withTransaction
import com.example.mydailyroutine.core.database.RoutineDatabase
import com.example.mydailyroutine.core.database.domain
import com.example.mydailyroutine.core.database.entity
import com.example.mydailyroutine.domain.model.GoalActivity
import com.example.mydailyroutine.domain.model.GoalMilestone
import com.example.mydailyroutine.domain.model.GoalProgress
import com.example.mydailyroutine.domain.model.GoalsProject
import com.example.mydailyroutine.domain.model.Milestone
import com.example.mydailyroutine.domain.model.RoutineBlueprint
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.domain.model.Task
import com.example.mydailyroutine.domain.repository.ScheduleBackupRepository
import com.example.mydailyroutine.domain.repository.TimelineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tolerant JSON backup. Export reads the live snapshot; import rebuilds domain models and inserts
 * them via the validated `.entity()` converters so it stays in sync with the Room schema.
 */
class RoomBackupRepository(
    private val db: RoutineDatabase,
    private val timeline: TimelineRepository,
    private val onChanged: () -> Unit,
) : ScheduleBackupRepository {

    override suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        // Read the DAOs directly: snapshot() caps ranges at two years, a backup must span everything.
        val from = LocalDate.of(2020, 1, 1)
        val through = LocalDate.of(2035, 12, 31)
        val subjectList = db.subjects().getAll().map { it.domain() }
        val subjectNames = subjectList.associateBy({ it.id }, { it.name })
        val root = JSONObject()
        root.put("schemaVersion", 2)

        val subjects = JSONArray()
        subjectList.forEach { s ->
            subjects.put(JSONObject().apply {
                put("name", s.name)
                put("color", "#" + s.colorHex.toString(16).padStart(6, '0'))
                put("defaultDuration", s.defaultDurationMinutes)
            })
        }
        root.put("subjects", subjects)

        val routines = JSONArray()
        db.routines().candidates(DayOfWeek.values().toList(), from, through).map { it.domain() }.forEach { r ->
            routines.put(JSONObject().apply {
                put("title", r.title)
                put("category", r.category.name)
                put("dayOfWeek", r.dayOfWeek.name)
                put("start", r.startTime.toString())
                put("end", r.endTime.toString())
                put("weekly", r.validUntil == null)
                r.validFrom?.let { put("validFrom", it.toString()) }
                r.validUntil?.let { put("validUntil", it.toString()) }
                put("notifications", r.isNotificationEnabled)
                r.subjectId?.let { id -> subjectNames[id]?.let { name -> put("subject", name) } }
            })
        }
        root.put("routines", routines)

        val milestones = JSONArray()
        db.milestones().inRange(from, through).map { it.domain() }.forEach { m ->
            milestones.put(JSONObject().apply {
                put("title", m.title)
                put("date", m.dueDate.toString())
                m.dueTime?.let { put("time", it.toString()) }
                put("isExam", m.isExam)
                m.subjectId?.let { id -> subjectNames[id]?.let { name -> put("subject", name) } }
            })
        }
        root.put("milestones", milestones)

        val taskArray = JSONArray()
        db.tasks().observeAll().first().map { it.domain() }.forEach { t ->
            taskArray.put(JSONObject().apply {
                put("title", t.title)
                t.dueDate?.let { put("due", it.toString()) }
                put("note", t.note.orEmpty())
                t.subjectId?.let { id -> subjectNames[id]?.let { name -> put("subject", name) } }
                put("completed", t.completedAtEpochMillis != null)
            })
        }
        root.put("tasks", taskArray)

        val goalsDao = db.goals()
        val projects = goalsDao.observeProjects().first().map { it.domain() }
        val projectIndex = projects.withIndex().associate { (index, item) -> item.id to index }
        val activities = goalsDao.observeActivities().first().map { it.domain() }
        val activityIndex = activities.withIndex().associate { (index, item) -> item.id to index }
        val projectArray = JSONArray()
        projects.forEach { g ->
            projectArray.put(JSONObject().apply {
                put("name", g.name); put("kind", g.kind)
                put("start", g.start.toString()); put("end", g.end.toString())
                g.targetHours?.let { put("hours", it) }
                g.targetWords?.let { put("words", it) }
            })
        }
        root.put("goalsProjects", projectArray)
        val activityArray = JSONArray()
        activities.forEach { a ->
            activityArray.put(JSONObject().apply {
                put("project", projectIndex[a.projectId] ?: -1)
                put("title", a.title)
                a.category?.let { put("category", it) }
                put("start", a.start.toString()); put("end", a.end.toString())
                put("note", a.note.orEmpty())
                put("casProject", a.isCasProject); put("done", a.isDone); put("scheduled", a.isScheduled)
            })
        }
        root.put("goalActivities", activityArray)
        val goalMilestoneArray = JSONArray()
        goalsDao.observeMilestones().first().map { it.domain() }.forEach { m ->
            goalMilestoneArray.put(JSONObject().apply {
                put("project", projectIndex[m.projectId] ?: -1)
                put("title", m.title); put("date", m.dueDate.toString()); put("done", m.isDone)
            })
        }
        root.put("goalMilestones", goalMilestoneArray)
        val progressArray = JSONArray()
        goalsDao.observeProgress().first().map { it.domain() }.forEach { e ->
            progressArray.put(JSONObject().apply {
                put("project", projectIndex[e.projectId] ?: -1)
                e.activityId?.let { put("activity", activityIndex[it] ?: -1) }
                put("kind", e.kind); put("amount", e.amount); put("date", e.date.toString()); put("note", e.note.orEmpty())
            })
        }
        root.put("goalProgress", progressArray)

        root.toString(2)
    }

    override suspend fun importJson(json: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        db.withTransaction {
            val subjectIds = mutableMapOf<String, Long>()
            root.optJSONArray("subjects")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.getString("name")
                    val color = colorToLong(o.optString("color", "#3B82F6"))
                    val dur = o.optInt("defaultDuration", 45)
                    val id = db.subjects().insert(Subject(0, name, color, dur).entity())
                    subjectIds[name] = id
                }
            }
            root.optJSONArray("routines")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val subjectId = subjectIds[o.optString("subject", "")]
                    val weekly = o.optBoolean("weekly", true)
                    val start = parseTime(o.optString("start", "08:00"))
                    val end = if (o.isNull("end")) start.plusMinutes(45L) else parseTime(o.optString("end"))
                    val bp = RoutineBlueprint(
                        subjectId = subjectId,
                        title = o.optString("title", mapCategory(o.optString("category", "SCHOOL")).name),
                        category = mapCategory(o.optString("category", "SCHOOL")),
                        dayOfWeek = mapDay(o.optString("dayOfWeek", "MONDAY")),
                        startTime = start,
                        endTime = end,
                        isNotificationEnabled = o.optBoolean("notifications", false),
                        validFrom = o.optString("validFrom", "").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.of(2020, 1, 1),
                        validUntil = o.optString("validUntil", "").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    )
                    db.routines().insert(bp.entity())
                }
            }
            root.optJSONArray("milestones")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val subjectId = subjectIds[o.optString("subject", "")]
                    val due = runCatching { LocalDate.parse(o.getString("date")) }.getOrElse { LocalDate.now() }
                    val time = o.optString("time", "").takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    db.milestones().insert(
                        Milestone(0, subjectId, o.getString("title").take(120), due, time, o.optBoolean("isExam", false), false, 0.0, false).entity(),
                    )
                }
            }
            root.optJSONArray("tasks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.tasks().insert(Task(
                        subjectId = subjectIds[o.optString("subject", "")],
                        title = o.getString("title").take(120),
                        dueDate = o.optString("due", "").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        note = o.optString("note", "").takeIf { it.isNotEmpty() }?.take(2000),
                        createdAtEpochMillis = System.currentTimeMillis(),
                        completedAtEpochMillis = if (o.optBoolean("completed", false)) System.currentTimeMillis() else null,
                    ).entity())
                }
            }
            val importedProjects = mutableListOf<Long>()
            root.optJSONArray("goalsProjects")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val start = runCatching { LocalDate.parse(o.getString("start")) }.getOrElse { LocalDate.now() }
                    val end = runCatching { LocalDate.parse(o.getString("end")) }.getOrNull() ?: start.plusDays(1)
                    importedProjects += db.goals().insertProject(GoalsProject(
                        name = o.getString("name").take(60), kind = o.optString("kind", "CUSTOM"),
                        start = start, end = if (end.isBefore(start)) start.plusDays(1) else end,
                        targetHours = o.optString("hours", "").toDoubleOrNull(), targetWords = o.optString("words", "").toIntOrNull(),
                    ).entity())
                }
            }
            val importedActivities = mutableListOf<Long>()
            root.optJSONArray("goalActivities")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val projectId = importedProjects.getOrNull(o.optInt("project", -1)) ?: continue
                    val start = runCatching { LocalDate.parse(o.getString("start")) }.getOrElse { LocalDate.now() }
                    val end = runCatching { LocalDate.parse(o.getString("end")) }.getOrNull() ?: start
                    importedActivities += db.goals().insertActivity(GoalActivity(
                        projectId = projectId, title = o.getString("title").take(80),
                        category = o.optString("category", "").takeIf { it.isNotBlank() }, start = start, end = if (end.isBefore(start)) start else end,
                        note = o.optString("note", "").takeIf { it.isNotEmpty() }?.take(2000),
                        isCasProject = o.optBoolean("casProject", false), isDone = o.optBoolean("done", false), isScheduled = o.optBoolean("scheduled", false),
                    ).entity())
                }
            }
            root.optJSONArray("goalMilestones")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val projectId = importedProjects.getOrNull(o.optInt("project", -1)) ?: continue
                    val due = runCatching { LocalDate.parse(o.getString("date")) }.getOrElse { LocalDate.now() }
                    db.goals().insertMilestone(GoalMilestone(projectId = projectId, title = o.getString("title").take(80), dueDate = due, isDone = o.optBoolean("done", false)).entity())
                }
            }
            root.optJSONArray("goalProgress")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val projectId = importedProjects.getOrNull(o.optInt("project", -1)) ?: continue
                    val activityId = importedActivities.getOrNull(o.optInt("activity", -1))
                    val date = runCatching { LocalDate.parse(o.getString("date")) }.getOrElse { LocalDate.now() }
                    val note = o.optString("note", "").takeIf { it.isNotEmpty() }?.take(2000)
                    val entry = GoalProgress(projectId = projectId, activityId = activityId, kind = o.optString("kind", "hour"),
                        amount = o.optDouble("amount", 1.0), note = if (o.optString("kind", "hour") == "reflection") note ?: "converted" else note, date = date)
                    if (entry.amount > 0) db.goals().insertProgress(entry.entity())
                }
            }
            onChanged()
        }
    }

    private fun colorToLong(hex: String): Long {
        val cleaned = hex.removePrefix("#")
        val full = if (cleaned.length == 3) cleaned.map { "$it$it" }.joinToString("") else cleaned
        return runCatching { full.toLong(16) }.getOrDefault(0x3B82F6)
    }

    private fun mapCategory(name: String): RoutineCategory = try {
        RoutineCategory.valueOf(name.uppercase())
    } catch (_: Exception) {
        when (name.uppercase()) {
            "FOCUS", "STUDY", "WORK" -> RoutineCategory.FOCUS_ANALYTICAL
            "BREAK", "REST" -> RoutineCategory.REST_BUFFER
            "SYNTHESIS", "PROJECT" -> RoutineCategory.FOCUS_SYNTHESIZING
            "LUNCH", "SNACK", "MEAL", "ADMIN" -> RoutineCategory.ADMIN
            else -> RoutineCategory.SCHOOL
        }
    }

    private fun mapDay(name: String): DayOfWeek = try {
        DayOfWeek.valueOf(name.uppercase())
    } catch (_: Exception) {
        DayOfWeek.MONDAY
    }

    private fun parseTime(s: String): LocalTime = runCatching { LocalTime.parse(s) }.getOrDefault(LocalTime.of(8, 0))
}
