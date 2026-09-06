package com.example.mydailyroutine.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room has no CHECK annotation. Equivalent BEFORE triggers also protect raw SQL / future imports.
 * Foreign keys and unique indexes are defined on the entities and enforced by SQLite itself.
 */
object DatabaseIntegrity {
    private val predicates = mapOf(
        "demo_imports" to """
            length(trim(NEW.key)) NOT BETWEEN 1 AND 80 OR NEW.importedAtEpochMillis < 0
        """.trimIndent(),
        "subjects" to """
            length(trim(NEW.name)) NOT BETWEEN 1 AND 120
            OR NEW.defaultDurationMinutes NOT BETWEEN 1 AND 1439
            OR NEW.colorHex NOT BETWEEN 4278190080 AND 4294967295
        """.trimIndent(),
        "routine_blocks" to """
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120
            OR NEW.dayOfWeek NOT BETWEEN 1 AND 7
            OR NEW.startTime NOT BETWEEN 0 AND 1439 OR NEW.endTime NOT BETWEEN 0 AND 1439
            OR NEW.startTime = NEW.endTime
            OR NEW.category NOT IN ('SCHOOL','FOCUS_STUDY','REST_BREAK','PROJECT','PERSONAL')
            OR NEW.isNotificationEnabled NOT IN (0,1)
            OR (NEW.validFrom IS NOT NULL AND NEW.validUntil IS NOT NULL AND NEW.validUntil < NEW.validFrom)
            OR EXISTS (SELECT 1 FROM event_overrides e WHERE e.routineBlockId = NEW.id AND (
                (((e.overrideDate + 3) % 7 + 7) % 7) + 1 != NEW.dayOfWeek
                OR (NEW.validFrom IS NOT NULL AND e.overrideDate < NEW.validFrom)
                OR (NEW.validUntil IS NOT NULL AND e.overrideDate > NEW.validUntil)
                OR COALESCE(e.customStartTime, NEW.startTime) = COALESCE(e.customEndTime, NEW.endTime)))
            OR EXISTS (SELECT 1 FROM routine_completions c WHERE c.routineBlockId = NEW.id AND (
                (((c.date + 3) % 7 + 7) % 7) + 1 != NEW.dayOfWeek
                OR (NEW.validFrom IS NOT NULL AND c.date < NEW.validFrom)
                OR (NEW.validUntil IS NOT NULL AND c.date > NEW.validUntil)))
        """.trimIndent(),
        "event_overrides" to """
            NEW.isCancelled NOT IN (0,1)
            OR (NEW.customTitle IS NOT NULL AND length(trim(NEW.customTitle)) NOT BETWEEN 1 AND 120)
            OR (NEW.customStartTime IS NOT NULL AND NEW.customStartTime NOT BETWEEN 0 AND 1439)
            OR (NEW.customEndTime IS NOT NULL AND NEW.customEndTime NOT BETWEEN 0 AND 1439)
            OR COALESCE(NEW.customStartTime, (SELECT startTime FROM routine_blocks WHERE id = NEW.routineBlockId))
             = COALESCE(NEW.customEndTime, (SELECT endTime FROM routine_blocks WHERE id = NEW.routineBlockId))
            OR EXISTS (SELECT 1 FROM routine_blocks r WHERE r.id = NEW.routineBlockId AND (
                (((NEW.overrideDate + 3) % 7 + 7) % 7) + 1 != r.dayOfWeek
                OR (r.validFrom IS NOT NULL AND NEW.overrideDate < r.validFrom)
                OR (r.validUntil IS NOT NULL AND NEW.overrideDate > r.validUntil)))
        """.trimIndent(),
        "routine_completions" to """
            EXISTS (SELECT 1 FROM routine_blocks r WHERE r.id = NEW.routineBlockId AND (
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
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120
            OR (NEW.dueTime IS NOT NULL AND NEW.dueTime NOT BETWEEN 0 AND 1439)
            OR NEW.isExam NOT IN (0,1) OR NEW.isCompleted NOT IN (0,1)
        """.trimIndent(),
    )

    fun install(db: SupportSQLiteDatabase) {
        for ((table, predicate) in predicates) {
            for (operation in listOf("INSERT", "UPDATE")) {
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS validate_${table}_${operation.lowercase()}
                    BEFORE $operation ON $table FOR EACH ROW WHEN ($predicate)
                    BEGIN SELECT RAISE(ABORT, 'Invalid $table values'); END
                """.trimIndent())
            }
        }
    }
}
