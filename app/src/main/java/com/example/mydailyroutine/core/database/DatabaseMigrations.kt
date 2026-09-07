package com.example.mydailyroutine.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `demo_imports` (`key` TEXT NOT NULL, `importedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`key`))")
        }
    }
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // SQLite on API 24 cannot rename/drop columns. Back up children BEFORE dropping the
            // parent; never turn foreign_keys off inside Room's migration transaction.
            val copied = listOf("routine_blocks", "event_overrides", "routine_completions", "alarm_deliveries")
            copied.forEach { db.execSQL("CREATE TEMP TABLE _v2_$it AS SELECT * FROM $it") }
            val triggers = mutableListOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'validate_%'").use { cursor ->
                while (cursor.moveToNext()) triggers += cursor.getString(0)
            }
            triggers.forEach { db.execSQL("DROP TRIGGER `$it`") }
            listOf("event_overrides", "routine_completions", "alarm_deliveries", "routine_blocks").forEach { db.execSQL("DROP TABLE $it") }
            db.execSQL("ALTER TABLE milestones ADD COLUMN estimatedEffortHours REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE milestones ADD COLUMN isTerminalExam INTEGER NOT NULL DEFAULT 0")
            MigrationSchemaV3.tables.values.flatten().forEach(db::execSQL)
            db.execSQL("""
                INSERT INTO routine_blocks(id,subjectId,title,category,dayOfWeek,startMinutes,durationMinutes,isNotificationEnabled,
                    validFrom,validUntil,minDurationMinutes,elasticity,priorityWeight,isFixedCommitment,completedActualMinutes,rawDurationMinutes,topicId,milestoneId)
                SELECT id,subjectId,title,CASE category WHEN 'FOCUS_STUDY' THEN 'FOCUS_ANALYTICAL' WHEN 'PROJECT' THEN 'FOCUS_SYNTHESIZING'
                    WHEN 'PERSONAL' THEN 'ADMIN' WHEN 'REST_BREAK' THEN 'REST_BUFFER' ELSE 'SCHOOL' END,
                    dayOfWeek,startTime,(endTime-startTime+1440)%1440,isNotificationEnabled,validFrom,validUntil,
                    CASE WHEN category IN ('SCHOOL','PERSONAL','REST_BREAK') THEN (endTime-startTime+1440)%1440 ELSE MIN(25,(endTime-startTime+1440)%1440) END,
                    CASE WHEN category IN ('SCHOOL','PERSONAL') THEN 0.0 ELSE 1.0 END,
                    CASE WHEN category='FOCUS_STUDY' THEN 3.0 WHEN category='PROJECT' THEN 2.0 ELSE 1.0 END,
                    CASE WHEN category IN ('SCHOOL','PERSONAL') THEN 1 ELSE 0 END,NULL,(endTime-startTime+1440)%1440,NULL,NULL
                FROM _v2_routine_blocks
            """.trimIndent())
            db.execSQL("INSERT INTO event_overrides(id,routineBlockId,overrideDate,isCancelled,customStartTime,customEndTime,customTitle,dayShift,cancellationReason) SELECT id,routineBlockId,overrideDate,isCancelled,customStartTime,customEndTime,customTitle,0,'MANUAL' FROM _v2_event_overrides")
            db.execSQL("INSERT INTO routine_completions(routineBlockId,date,actualMinutes) SELECT routineBlockId,date,NULL FROM _v2_routine_completions")
            db.execSQL("INSERT INTO alarm_deliveries SELECT * FROM _v2_alarm_deliveries")
            copied.forEach { db.execSQL("DROP TABLE _v2_$it") }
            DatabaseIntegrity.install(db, validateRawBacklog = false)
            db.query("PRAGMA foreign_key_check").use { check(it.count == 0) { "Migration would violate foreign keys" } }
        }
    }
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TRIGGER IF EXISTS validate_backlog_entries_insert")
            db.execSQL("DROP TRIGGER IF EXISTS validate_backlog_entries_update")
            db.execSQL("ALTER TABLE backlog_entries ADD COLUMN rawDurationMinutes INTEGER NOT NULL DEFAULT 1")
            db.execSQL("UPDATE backlog_entries SET rawDurationMinutes = COALESCE((SELECT rawDurationMinutes FROM routine_blocks WHERE id = sourceRoutineId), durationMinutes)")
            DatabaseIntegrity.install(db)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE routine_completions ADD COLUMN actualStartEpochMinute INTEGER")
            db.execSQL("ALTER TABLE routine_blocks ADD COLUMN stageOrder INTEGER")
            db.execSQL("ALTER TABLE backlog_entries ADD COLUMN stageOrder INTEGER")
            db.execSQL("CREATE TABLE IF NOT EXISTS active_execution (id INTEGER NOT NULL, routineBlockId INTEGER NOT NULL, occurrenceDate INTEGER NOT NULL, startedAtEpochMillis INTEGER NOT NULL, expectedEndEpochMillis INTEGER NOT NULL, lastHealedEndEpochMillis INTEGER NOT NULL, stoppedAtEpochMillis INTEGER, PRIMARY KEY(id), FOREIGN KEY(routineBlockId) REFERENCES routine_blocks(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_execution_routineBlockId ON active_execution(routineBlockId)")
            installExecutionIntegrity(db)
        }
    }
    fun installExecutionIntegrity(db: SupportSQLiteDatabase) {
        for (operation in listOf("INSERT", "UPDATE")) db.execSQL("CREATE TRIGGER IF NOT EXISTS validate_execution_${operation.lowercase()} BEFORE $operation ON active_execution WHEN NEW.id != 1 OR NEW.startedAtEpochMillis < 0 OR NEW.expectedEndEpochMillis < 0 OR NEW.lastHealedEndEpochMillis < 0 OR (NEW.stoppedAtEpochMillis IS NOT NULL AND NEW.stoppedAtEpochMillis < NEW.startedAtEpochMillis) BEGIN SELECT RAISE(ABORT, 'Invalid execution'); END")
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE routine_completions ADD COLUMN actualStartedAtEpochMillis INTEGER")
            db.execSQL("ALTER TABLE routine_completions ADD COLUMN actualEndedAtEpochMillis INTEGER")
            db.execSQL("ALTER TABLE routine_completions ADD COLUMN actualZoneId TEXT")
            installActualTimingIntegrity(db)
        }
    }

    fun installActualTimingIntegrity(db: SupportSQLiteDatabase) {
        for (operation in listOf("INSERT", "UPDATE")) db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS validate_completion_timing_${operation.lowercase()} BEFORE $operation ON routine_completions
            WHEN (NEW.actualStartedAtEpochMillis IS NULL) != (NEW.actualEndedAtEpochMillis IS NULL)
                OR (NEW.actualStartedAtEpochMillis IS NOT NULL AND (NEW.actualEndedAtEpochMillis <= NEW.actualStartedAtEpochMillis
                    OR NEW.actualZoneId IS NULL OR length(trim(NEW.actualZoneId)) = 0 OR NEW.actualMinutes IS NULL))
            BEGIN SELECT RAISE(ABORT, 'Invalid measured interval'); END
        """.trimIndent())
    }

}
