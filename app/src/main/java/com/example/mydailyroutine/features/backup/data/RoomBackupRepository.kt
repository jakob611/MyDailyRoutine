package com.example.mydailyroutine.features.backup.data

import androidx.room.withTransaction
import com.example.mydailyroutine.core.database.RoutineDatabase
import com.example.mydailyroutine.domain.model.Milestone
import com.example.mydailyroutine.domain.model.RoutineBlueprint
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.Subject
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
        val snapshot = timeline.observeSnapshot(LocalDate.of(2020, 1, 1), LocalDate.of(2035, 12, 31)).first()
        val root = JSONObject()
        root.put("schemaVersion", 1)

        val subjects = JSONArray()
        snapshot.subjects.forEach { s ->
            subjects.put(JSONObject().apply {
                put("name", s.name)
                put("color", "#" + s.colorHex.toString(16).padStart(6, '0'))
                put("defaultDuration", s.defaultDurationMinutes)
            })
        }
        root.put("subjects", subjects)

        val routines = JSONArray()
        snapshot.routines.forEach { r ->
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
            })
        }
        root.put("routines", routines)

        val milestones = JSONArray()
        snapshot.milestones.forEach { m ->
            milestones.put(JSONObject().apply {
                put("title", m.title)
                put("date", m.dueDate.toString())
                m.dueTime?.let { put("time", it.toString()) }
                put("isExam", m.isExam)
            })
        }
        root.put("milestones", milestones)

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
                    val end = if (o.isNull("end")) start.plusMinutes(45) else parseTime(o.optString("end"))
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
                    val due = runCatching { LocalDate.parse(o.getString("date")) }.getOrElse(0) { LocalDate.now() }
                    val time = o.optString("time", "").takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    db.milestones().insert(
                        Milestone(0, subjectId, o.getString("title"), due, time, o.optBoolean("isExam", false), false, 0.0, false).entity(),
                    )
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
