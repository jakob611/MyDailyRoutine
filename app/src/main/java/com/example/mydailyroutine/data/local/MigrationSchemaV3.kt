package com.example.mydailyroutine.data.local

/** Versioned migration DDL; Room independently validates this structure after migration. */
internal object MigrationSchemaV3 {
    val tables: Map<String, List<String>> = mapOf(
        "study_topics" to listOf(
            """CREATE TABLE IF NOT EXISTS `study_topics` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `subjectId` INTEGER, `initialEpochDay` INTEGER NOT NULL, `finalEpochDay` INTEGER NOT NULL, `reviewCount` INTEGER NOT NULL, `reviewDurationMinutes` INTEGER NOT NULL, `priorityWeight` REAL NOT NULL, `milestoneId` INTEGER, FOREIGN KEY(subjectId) REFERENCES `subjects`(id) ON DELETE SET NULL ON UPDATE NO ACTION, FOREIGN KEY(milestoneId) REFERENCES `milestones`(id) ON DELETE SET NULL ON UPDATE NO ACTION)""",
            """CREATE INDEX IF NOT EXISTS `index_study_topics_subjectId` ON `study_topics`(subjectId)""",
            """CREATE INDEX IF NOT EXISTS `index_study_topics_milestoneId` ON `study_topics`(milestoneId)""",
        ),
        "routine_blocks" to listOf(
            """CREATE TABLE IF NOT EXISTS `routine_blocks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `startMinutes` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `isNotificationEnabled` INTEGER NOT NULL, `validFrom` INTEGER, `validUntil` INTEGER, `minDurationMinutes` INTEGER NOT NULL, `elasticity` REAL NOT NULL, `priorityWeight` REAL NOT NULL, `isFixedCommitment` INTEGER NOT NULL, `completedActualMinutes` INTEGER, `rawDurationMinutes` INTEGER NOT NULL, `topicId` INTEGER, `milestoneId` INTEGER, FOREIGN KEY(subjectId) REFERENCES `subjects`(id) ON DELETE SET NULL ON UPDATE NO ACTION, FOREIGN KEY(topicId) REFERENCES `study_topics`(id) ON DELETE CASCADE ON UPDATE NO ACTION, FOREIGN KEY(milestoneId) REFERENCES `milestones`(id) ON DELETE SET NULL ON UPDATE NO ACTION)""",
            """CREATE INDEX IF NOT EXISTS `index_routine_blocks_subjectId` ON `routine_blocks`(subjectId)""",
            """CREATE INDEX IF NOT EXISTS `index_routine_blocks_topicId` ON `routine_blocks`(topicId)""",
            """CREATE INDEX IF NOT EXISTS `index_routine_blocks_milestoneId` ON `routine_blocks`(milestoneId)""",
            """CREATE INDEX IF NOT EXISTS `index_routine_blocks_dayOfWeek_startMinutes` ON `routine_blocks`(dayOfWeek,startMinutes)""",
        ),
        "event_overrides" to listOf(
            """CREATE TABLE IF NOT EXISTS `event_overrides` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routineBlockId` INTEGER NOT NULL, `overrideDate` INTEGER NOT NULL, `isCancelled` INTEGER NOT NULL, `customStartTime` INTEGER, `customEndTime` INTEGER, `customTitle` TEXT, `dayShift` INTEGER NOT NULL DEFAULT 0, `cancellationReason` TEXT NOT NULL DEFAULT 'MANUAL', FOREIGN KEY(routineBlockId) REFERENCES `routine_blocks`(id) ON DELETE CASCADE ON UPDATE NO ACTION)""",
            """CREATE UNIQUE INDEX IF NOT EXISTS `index_event_overrides_routineBlockId_overrideDate` ON `event_overrides`(routineBlockId,overrideDate)""",
            """CREATE INDEX IF NOT EXISTS `index_event_overrides_overrideDate` ON `event_overrides`(overrideDate)""",
        ),
        "routine_completions" to listOf(
            """CREATE TABLE IF NOT EXISTS `routine_completions` (`routineBlockId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `actualMinutes` INTEGER, PRIMARY KEY(`routineBlockId`,`date`), FOREIGN KEY(routineBlockId) REFERENCES `routine_blocks`(id) ON DELETE CASCADE ON UPDATE NO ACTION)""",
            """CREATE INDEX IF NOT EXISTS `index_routine_completions_date` ON `routine_completions`(date)""",
        ),
        "alarm_deliveries" to listOf(
            """CREATE TABLE IF NOT EXISTS `alarm_deliveries` (`routineBlockId` INTEGER NOT NULL, `occurrenceDate` INTEGER NOT NULL, `kind` TEXT NOT NULL, `deliveredAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`routineBlockId`,`occurrenceDate`,`kind`), FOREIGN KEY(routineBlockId) REFERENCES `routine_blocks`(id) ON DELETE CASCADE ON UPDATE NO ACTION)""",
            """CREATE INDEX IF NOT EXISTS `index_alarm_deliveries_occurrenceDate` ON `alarm_deliveries`(occurrenceDate)""",
        ),
        "historical_velocity" to listOf(
            """CREATE TABLE IF NOT EXISTS `historical_velocity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER, `plannedDurationMinutes` INTEGER NOT NULL, `actualDurationMinutes` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `routineBlockId` INTEGER, `occurrenceDate` INTEGER, FOREIGN KEY(subjectId) REFERENCES `subjects`(id) ON DELETE SET NULL ON UPDATE NO ACTION, FOREIGN KEY(routineBlockId) REFERENCES `routine_blocks`(id) ON DELETE SET NULL ON UPDATE NO ACTION)""",
            """CREATE INDEX IF NOT EXISTS `index_historical_velocity_subjectId_timestamp` ON `historical_velocity`(subjectId,timestamp)""",
            """CREATE UNIQUE INDEX IF NOT EXISTS `index_historical_velocity_routineBlockId_occurrenceDate` ON `historical_velocity`(routineBlockId,occurrenceDate)""",
        ),
        "spaced_reviews" to listOf(
            """CREATE TABLE IF NOT EXISTS `spaced_reviews` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `topicId` INTEGER NOT NULL, `scheduledEpochDay` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `priorityWeight` REAL NOT NULL, `ordinal` INTEGER NOT NULL, `timeBlockId` INTEGER, `isCompleted` INTEGER NOT NULL, FOREIGN KEY(topicId) REFERENCES `study_topics`(id) ON DELETE CASCADE ON UPDATE NO ACTION, FOREIGN KEY(timeBlockId) REFERENCES `routine_blocks`(id) ON DELETE CASCADE ON UPDATE NO ACTION)""",
            """CREATE UNIQUE INDEX IF NOT EXISTS `index_spaced_reviews_topicId_ordinal` ON `spaced_reviews`(topicId,ordinal)""",
            """CREATE INDEX IF NOT EXISTS `index_spaced_reviews_scheduledEpochDay` ON `spaced_reviews`(scheduledEpochDay)""",
            """CREATE UNIQUE INDEX IF NOT EXISTS `index_spaced_reviews_timeBlockId` ON `spaced_reviews`(timeBlockId)""",
        ),
        "backlog_entries" to listOf(
            """CREATE TABLE IF NOT EXISTS `backlog_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `minDurationMinutes` INTEGER NOT NULL, `elasticity` REAL NOT NULL, `priorityWeight` REAL NOT NULL, `subjectId` INTEGER, `sourceRoutineId` INTEGER, `occurrenceDate` INTEGER, `milestoneId` INTEGER, `topicId` INTEGER, `reviewId` INTEGER, `reason` TEXT NOT NULL, FOREIGN KEY(subjectId) REFERENCES `subjects`(id) ON DELETE SET NULL ON UPDATE NO ACTION, FOREIGN KEY(sourceRoutineId) REFERENCES `routine_blocks`(id) ON DELETE CASCADE ON UPDATE NO ACTION, FOREIGN KEY(milestoneId) REFERENCES `milestones`(id) ON DELETE SET NULL ON UPDATE NO ACTION, FOREIGN KEY(topicId) REFERENCES `study_topics`(id) ON DELETE CASCADE ON UPDATE NO ACTION, FOREIGN KEY(reviewId) REFERENCES `spaced_reviews`(id) ON DELETE CASCADE ON UPDATE NO ACTION)""",
            """CREATE INDEX IF NOT EXISTS `index_backlog_entries_subjectId` ON `backlog_entries`(subjectId)""",
            """CREATE UNIQUE INDEX IF NOT EXISTS `index_backlog_entries_sourceRoutineId_occurrenceDate` ON `backlog_entries`(sourceRoutineId,occurrenceDate)""",
            """CREATE INDEX IF NOT EXISTS `index_backlog_entries_milestoneId` ON `backlog_entries`(milestoneId)""",
            """CREATE INDEX IF NOT EXISTS `index_backlog_entries_topicId` ON `backlog_entries`(topicId)""",
            """CREATE UNIQUE INDEX IF NOT EXISTS `index_backlog_entries_reviewId` ON `backlog_entries`(reviewId)""",
        ),
    )
}
