package com.example.mydailyroutine.domain.repository

/**
 * Round-trip serialization of the user's schedule.
 *
 * The JSON schema is intentionally tolerant so it can be produced by an external tool or an AI
 * from a photo of a school timetable: subjects/routines/milestones use friendly names and
 * "HH:mm" / "YYYY-MM-DD" strings. Importing never mutates the running app's behaviour beyond
 * inserting the parsed data, so the schedule remains fully editable afterwards.
 */
interface ScheduleBackupRepository {
    suspend fun exportJson(): String
    suspend fun importJson(json: String)
}
