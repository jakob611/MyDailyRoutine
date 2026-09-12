package com.example.mydailyroutine.core.database

import androidx.sqlite.db.SupportSQLiteDatabase

/** Cross-column invariants complement SQLite's strict, enabled FK enforcement. */
object DatabaseIntegrity {
    private val predicates = mapOf(
        "subjects" to """
            length(trim(NEW.name)) NOT BETWEEN 1 AND 120 OR NEW.defaultDurationMinutes NOT BETWEEN 1 AND 1439
            OR NEW.colorHex NOT BETWEEN 4278190080 AND 4294967295
        """.trimIndent(),
        "routine_blocks" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120 OR NEW.dayOfWeek NOT BETWEEN 1 AND 7
            OR NEW.startMinutes NOT BETWEEN 0 AND 1439 OR NEW.durationMinutes NOT BETWEEN 1 AND 1439
            OR NEW.rawDurationMinutes NOT BETWEEN 1 AND 1439 OR NEW.minDurationMinutes NOT BETWEEN 0 AND NEW.durationMinutes
            OR (NEW.category NOT IN ('REST_BUFFER','EMERGENCY_RESERVE') AND NEW.minDurationMinutes < 1)
            OR NEW.category NOT IN ('SCHOOL','FOCUS_ANALYTICAL','FOCUS_SYNTHESIZING','ADMIN','REST_BUFFER','EMERGENCY_RESERVE')
            OR NEW.elasticity < 0 OR NEW.elasticity > 1000000 OR NEW.priorityWeight <= 0 OR NEW.priorityWeight > 1000000
            OR NEW.isFixedCommitment NOT IN (0,1) OR NEW.isNotificationEnabled NOT IN (0,1)
            OR (NEW.category = 'SCHOOL' AND (NEW.isFixedCommitment != 1 OR NEW.elasticity != 0 OR NEW.minDurationMinutes != NEW.durationMinutes))
            OR (NEW.completedActualMinutes IS NOT NULL AND NEW.completedActualMinutes NOT BETWEEN 1 AND 10080)
            OR (NEW.validFrom IS NOT NULL AND NEW.validUntil IS NOT NULL AND NEW.validUntil < NEW.validFrom)
            OR EXISTS (SELECT 1 FROM event_overrides e WHERE e.routineBlockId = NEW.id AND (
                (((e.overrideDate + 3) % 7 + 7) % 7) + 1 != NEW.dayOfWeek
                OR (NEW.validFrom IS NOT NULL AND e.overrideDate < NEW.validFrom)
                OR (NEW.validUntil IS NOT NULL AND e.overrideDate > NEW.validUntil)
                OR COALESCE(e.customStartTime, NEW.startMinutes) = COALESCE(e.customEndTime, (NEW.startMinutes + NEW.durationMinutes) % 1440)))
            OR EXISTS (SELECT 1 FROM routine_completions c WHERE c.routineBlockId = NEW.id AND (
                (((c.date + 3) % 7 + 7) % 7) + 1 != NEW.dayOfWeek
                OR (NEW.validFrom IS NOT NULL AND c.date < NEW.validFrom)
                OR (NEW.validUntil IS NOT NULL AND c.date > NEW.validUntil)))
        """.trimIndent(),
        "event_overrides" to """
            NEW.isCancelled NOT IN (0,1) OR NEW.dayShift NOT BETWEEN 0 AND 7
            OR NEW.cancellationReason NOT IN ('MANUAL','AUTO_HEAL','BACKLOG','BUFFER_CONSUMED')
            OR (NEW.customTitle IS NOT NULL AND length(trim(NEW.customTitle)) NOT BETWEEN 1 AND 120)
            OR (NEW.customStartTime IS NOT NULL AND NEW.customStartTime NOT BETWEEN 0 AND 1439)
            OR (NEW.customEndTime IS NOT NULL AND NEW.customEndTime NOT BETWEEN 0 AND 1439)
            OR COALESCE(NEW.customStartTime, (SELECT startMinutes FROM routine_blocks WHERE id = NEW.routineBlockId))
               = COALESCE(NEW.customEndTime, (SELECT (startMinutes + durationMinutes) % 1440 FROM routine_blocks WHERE id = NEW.routineBlockId))
            OR EXISTS (SELECT 1 FROM routine_blocks r WHERE r.id = NEW.routineBlockId AND (
                (((NEW.overrideDate + 3) % 7 + 7) % 7) + 1 != r.dayOfWeek
                OR (r.validFrom IS NOT NULL AND NEW.overrideDate < r.validFrom)
                OR (r.validUntil IS NOT NULL AND NEW.overrideDate > r.validUntil)))
        """.trimIndent(),
        "routine_completions" to """
            (NEW.actualMinutes IS NOT NULL AND NEW.actualMinutes NOT BETWEEN 1 AND 10080)
            OR EXISTS (SELECT 1 FROM routine_blocks r WHERE r.id = NEW.routineBlockId AND (
                (((NEW.date + 3) % 7 + 7) % 7) + 1 != r.dayOfWeek
                OR (r.validFrom IS NOT NULL AND NEW.date < r.validFrom)
                OR (r.validUntil IS NOT NULL AND NEW.date > r.validUntil)))
        """.trimIndent(),
        "alarm_deliveries" to """
            NEW.kind NOT IN ('BLOCK_PREVIEW','RECOVERY_START') OR NEW.deliveredAtEpochMillis < 0
        """.trimIndent(),
        "school_calendar" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120 OR NEW.isWorkFreeDay NOT IN (0,1)
        """.trimIndent(),
        "milestones" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120 OR NEW.isExam NOT IN (0,1) OR NEW.isCompleted NOT IN (0,1)
            OR (NEW.dueTime IS NOT NULL AND NEW.dueTime NOT BETWEEN 0 AND 1439)
            OR NEW.estimatedEffortHours < 0 OR NEW.estimatedEffortHours > 1000 OR NEW.isTerminalExam NOT IN (0,1)
            OR (NEW.isTerminalExam = 1 AND NEW.isExam = 0)
        """.trimIndent(),
        "demo_imports" to """
            length(trim(NEW.key)) NOT BETWEEN 1 AND 80 OR NEW.importedAtEpochMillis < 0
        """.trimIndent(),
        "historical_velocity" to """
            NEW.plannedDurationMinutes NOT BETWEEN 1 AND 1439 OR NEW.actualDurationMinutes NOT BETWEEN 1 AND 10080 OR NEW.timestamp < 0
        """.trimIndent(),
        "study_topics" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120 OR NEW.finalEpochDay < NEW.initialEpochDay
            OR NEW.finalEpochDay - NEW.initialEpochDay > 366 OR NEW.reviewCount NOT BETWEEN 1 AND 52
            OR NEW.reviewDurationMinutes NOT BETWEEN 1 AND 120 OR NEW.priorityWeight <= 0 OR NEW.priorityWeight > 1000000
        """.trimIndent(),
        "spaced_reviews" to """
            NEW.durationMinutes NOT BETWEEN 1 AND 120 OR NEW.ordinal NOT BETWEEN 1 AND 52
            OR NEW.priorityWeight <= 0 OR NEW.priorityWeight > 1000000 OR NEW.isCompleted NOT IN (0,1)
        """.trimIndent(),
        "backlog_entries" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120 OR NEW.durationMinutes NOT BETWEEN 1 AND 1439
            OR NEW.minDurationMinutes NOT BETWEEN 0 AND NEW.durationMinutes OR NEW.elasticity < 0 OR NEW.elasticity > 1000000
            OR NEW.priorityWeight <= 0 OR NEW.priorityWeight > 1000000
            OR NEW.category NOT IN ('SCHOOL','FOCUS_ANALYTICAL','FOCUS_SYNTHESIZING','ADMIN','REST_BUFFER','EMERGENCY_RESERVE')
            OR NEW.rawDurationMinutes NOT BETWEEN 1 AND 1439
            OR NEW.reason NOT IN ('SLIPPAGE','CAPACITY','REVIEW_CAPACITY')
        """.trimIndent(),
        "tasks" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120 OR NEW.createdAtEpochMillis < 0
            OR (NEW.note IS NOT NULL AND length(NEW.note) > 2000)
            OR (NEW.completedAtEpochMillis IS NOT NULL AND NEW.completedAtEpochMillis < NEW.createdAtEpochMillis)
        """.trimIndent(),
        "goals_project" to """
            length(trim(NEW.name)) NOT BETWEEN 1 AND 60 OR NEW.kind NOT IN ('CAS','EE','CUSTOM')
            OR NEW.endDate < NEW.startDate OR NEW.endDate - NEW.startDate > 1095
            OR (NEW.targetHours IS NOT NULL AND (NEW.targetHours <= 0 OR NEW.targetHours > 1000))
            OR (NEW.targetWords IS NOT NULL AND (NEW.targetWords <= 0 OR NEW.targetWords > 20000))
        """.trimIndent(),
        "goals_activity" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 80 OR NEW.endDate < NEW.startDate
            OR (NEW.category IS NOT NULL AND NEW.category NOT IN ('CREATIVITY','ACTIVITY','SERVICE','STAGE'))
            OR (NEW.note IS NOT NULL AND length(NEW.note) > 2000)
            OR NEW.isCasProject NOT IN (0,1) OR NEW.isDone NOT IN (0,1) OR NEW.isScheduled NOT IN (0,1)
        """.trimIndent(),
        "goals_milestone" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 80 OR NEW.isDone NOT IN (0,1)
        """.trimIndent(),
        "goals_progress" to """
            NEW.kind NOT IN ('hour','word','reflection') OR NEW.amount <= 0 OR NEW.amount > 1000000
            OR (NEW.kind = 'hour' AND NEW.amount > 16)
            OR (NEW.kind = 'reflection' AND (NEW.note IS NULL OR length(trim(NEW.note)) = 0))
            OR (NEW.note IS NOT NULL AND length(NEW.note) > 2000)
        """.trimIndent(),
    )
    fun install(db: SupportSQLiteDatabase, validateRawBacklog: Boolean = true) {
        // Stepwise migrations call this at every upgrade stage; tables created by later migrations
        // (tasks at v8, goals_* at v9) must be skipped until they exist — CREATE TRIGGER compiles its
        // body immediately and would abort with "no such table". The final migration and onCreate re-run
        // install() when everything is present, so no trigger is ever missed in a fully migrated database.
        val existing = HashSet<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) existing.add(cursor.getString(0))
        }
        predicates.forEach { (table, fullPredicate) ->
            if (table !in existing) return@forEach
            val predicate = if (table == "backlog_entries" && !validateRawBacklog)
                fullPredicate.replace("OR NEW.rawDurationMinutes NOT BETWEEN 1 AND 1439", "") else fullPredicate
            listOf("INSERT", "UPDATE").forEach { operation ->
                db.execSQL("CREATE TRIGGER IF NOT EXISTS validate_${table}_${operation.lowercase()} BEFORE $operation ON $table FOR EACH ROW WHEN ($predicate) BEGIN SELECT RAISE(ABORT, 'Invalid $table values'); END")
            }
        }
    }
}
