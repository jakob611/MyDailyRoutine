-- Verified v2 entity/trigger structure, retained to test the v2 to v3 migration.
CREATE TABLE subjects ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"name" TEXT NOT NULL,"colorHex" INTEGER NOT NULL,"defaultDurationMinutes" INTEGER NOT NULL);
CREATE TABLE routine_blocks ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"subjectId" INTEGER,"title" TEXT NOT NULL,"category" TEXT NOT NULL,"dayOfWeek" INTEGER NOT NULL,"startTime" INTEGER NOT NULL,"endTime" INTEGER NOT NULL,"isNotificationEnabled" INTEGER NOT NULL,"validFrom" INTEGER,"validUntil" INTEGER,FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE SET NULL);
CREATE TABLE event_overrides ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"routineBlockId" INTEGER NOT NULL,"overrideDate" INTEGER NOT NULL,"isCancelled" INTEGER NOT NULL,"customStartTime" INTEGER,"customEndTime" INTEGER,"customTitle" TEXT,FOREIGN KEY (routineBlockId) REFERENCES routine_blocks(id) ON DELETE CASCADE);
CREATE TABLE school_calendar ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"date" INTEGER NOT NULL,"title" TEXT NOT NULL,"isWorkFreeDay" INTEGER NOT NULL);
CREATE TABLE milestones ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"subjectId" INTEGER,"title" TEXT NOT NULL,"dueDate" INTEGER NOT NULL,"dueTime" INTEGER,"isExam" INTEGER NOT NULL,"isCompleted" INTEGER NOT NULL,FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE SET NULL);
CREATE TABLE routine_completions ("routineBlockId" INTEGER NOT NULL,"date" INTEGER NOT NULL,PRIMARY KEY (routineBlockId,date),FOREIGN KEY (routineBlockId) REFERENCES routine_blocks(id) ON DELETE CASCADE);
CREATE TABLE alarm_deliveries ("routineBlockId" INTEGER NOT NULL,"occurrenceDate" INTEGER NOT NULL,"kind" TEXT NOT NULL,"deliveredAtEpochMillis" INTEGER NOT NULL,PRIMARY KEY (routineBlockId,occurrenceDate,kind),FOREIGN KEY (routineBlockId) REFERENCES routine_blocks(id) ON DELETE CASCADE);
CREATE TABLE demo_imports ("key" TEXT NOT NULL,"importedAtEpochMillis" INTEGER NOT NULL,PRIMARY KEY (key));
CREATE INDEX index_routine_blocks_subjectId ON routine_blocks(subjectId);
CREATE INDEX index_routine_blocks_dayOfWeek_startTime ON routine_blocks(dayOfWeek,startTime);
CREATE UNIQUE INDEX index_event_overrides_routineBlockId_overrideDate ON event_overrides(routineBlockId,overrideDate);
CREATE INDEX index_event_overrides_overrideDate ON event_overrides(overrideDate);
CREATE UNIQUE INDEX index_school_calendar_date_title ON school_calendar(date,title);
CREATE INDEX index_school_calendar_date_isWorkFreeDay ON school_calendar(date,isWorkFreeDay);
CREATE INDEX index_milestones_subjectId ON milestones(subjectId);
CREATE INDEX index_milestones_dueDate_dueTime ON milestones(dueDate,dueTime);
CREATE INDEX index_routine_completions_date ON routine_completions(date);
CREATE INDEX index_alarm_deliveries_occurrenceDate ON alarm_deliveries(occurrenceDate);
CREATE TRIGGER validate_demo_imports_insert BEFORE INSERT ON demo_imports FOR EACH ROW WHEN (
            length(trim(NEW.key)) NOT BETWEEN 1 AND 80 OR NEW.importedAtEpochMillis < 0
        ) BEGIN SELECT RAISE(ABORT, 'Invalid demo_imports values'); END;
CREATE TRIGGER validate_demo_imports_update BEFORE UPDATE ON demo_imports FOR EACH ROW WHEN (
            length(trim(NEW.key)) NOT BETWEEN 1 AND 80 OR NEW.importedAtEpochMillis < 0
        ) BEGIN SELECT RAISE(ABORT, 'Invalid demo_imports values'); END;
CREATE TRIGGER validate_subjects_insert BEFORE INSERT ON subjects FOR EACH ROW WHEN (
            length(trim(NEW.name)) NOT BETWEEN 1 AND 120
            OR NEW.defaultDurationMinutes NOT BETWEEN 1 AND 1439
            OR NEW.colorHex NOT BETWEEN 4278190080 AND 4294967295
        ) BEGIN SELECT RAISE(ABORT, 'Invalid subjects values'); END;
CREATE TRIGGER validate_subjects_update BEFORE UPDATE ON subjects FOR EACH ROW WHEN (
            length(trim(NEW.name)) NOT BETWEEN 1 AND 120
            OR NEW.defaultDurationMinutes NOT BETWEEN 1 AND 1439
            OR NEW.colorHex NOT BETWEEN 4278190080 AND 4294967295
        ) BEGIN SELECT RAISE(ABORT, 'Invalid subjects values'); END;
CREATE TRIGGER validate_routine_blocks_insert BEFORE INSERT ON routine_blocks FOR EACH ROW WHEN (
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
        ) BEGIN SELECT RAISE(ABORT, 'Invalid routine_blocks values'); END;
CREATE TRIGGER validate_routine_blocks_update BEFORE UPDATE ON routine_blocks FOR EACH ROW WHEN (
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
        ) BEGIN SELECT RAISE(ABORT, 'Invalid routine_blocks values'); END;
CREATE TRIGGER validate_event_overrides_insert BEFORE INSERT ON event_overrides FOR EACH ROW WHEN (
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
        ) BEGIN SELECT RAISE(ABORT, 'Invalid event_overrides values'); END;
CREATE TRIGGER validate_event_overrides_update BEFORE UPDATE ON event_overrides FOR EACH ROW WHEN (
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
        ) BEGIN SELECT RAISE(ABORT, 'Invalid event_overrides values'); END;
CREATE TRIGGER validate_routine_completions_insert BEFORE INSERT ON routine_completions FOR EACH ROW WHEN (
            EXISTS (SELECT 1 FROM routine_blocks r WHERE r.id = NEW.routineBlockId AND (
                (((NEW.date + 3) % 7 + 7) % 7) + 1 != r.dayOfWeek
                OR (r.validFrom IS NOT NULL AND NEW.date < r.validFrom)
                OR (r.validUntil IS NOT NULL AND NEW.date > r.validUntil)))
        ) BEGIN SELECT RAISE(ABORT, 'Invalid routine_completions values'); END;
CREATE TRIGGER validate_routine_completions_update BEFORE UPDATE ON routine_completions FOR EACH ROW WHEN (
            EXISTS (SELECT 1 FROM routine_blocks r WHERE r.id = NEW.routineBlockId AND (
                (((NEW.date + 3) % 7 + 7) % 7) + 1 != r.dayOfWeek
                OR (r.validFrom IS NOT NULL AND NEW.date < r.validFrom)
                OR (r.validUntil IS NOT NULL AND NEW.date > r.validUntil)))
        ) BEGIN SELECT RAISE(ABORT, 'Invalid routine_completions values'); END;
CREATE TRIGGER validate_alarm_deliveries_insert BEFORE INSERT ON alarm_deliveries FOR EACH ROW WHEN (
            NEW.kind NOT IN ('BLOCK_PREVIEW','RECOVERY_START') OR NEW.deliveredAtEpochMillis < 0
        ) BEGIN SELECT RAISE(ABORT, 'Invalid alarm_deliveries values'); END;
CREATE TRIGGER validate_alarm_deliveries_update BEFORE UPDATE ON alarm_deliveries FOR EACH ROW WHEN (
            NEW.kind NOT IN ('BLOCK_PREVIEW','RECOVERY_START') OR NEW.deliveredAtEpochMillis < 0
        ) BEGIN SELECT RAISE(ABORT, 'Invalid alarm_deliveries values'); END;
CREATE TRIGGER validate_school_calendar_insert BEFORE INSERT ON school_calendar FOR EACH ROW WHEN (
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120 OR NEW.isWorkFreeDay NOT IN (0,1)
        ) BEGIN SELECT RAISE(ABORT, 'Invalid school_calendar values'); END;
CREATE TRIGGER validate_school_calendar_update BEFORE UPDATE ON school_calendar FOR EACH ROW WHEN (
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120 OR NEW.isWorkFreeDay NOT IN (0,1)
        ) BEGIN SELECT RAISE(ABORT, 'Invalid school_calendar values'); END;
CREATE TRIGGER validate_milestones_insert BEFORE INSERT ON milestones FOR EACH ROW WHEN (
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120
            OR (NEW.dueTime IS NOT NULL AND NEW.dueTime NOT BETWEEN 0 AND 1439)
            OR NEW.isExam NOT IN (0,1) OR NEW.isCompleted NOT IN (0,1)
        ) BEGIN SELECT RAISE(ABORT, 'Invalid milestones values'); END;
CREATE TRIGGER validate_milestones_update BEFORE UPDATE ON milestones FOR EACH ROW WHEN (
            length(trim(NEW.title)) NOT BETWEEN 1 AND 120
            OR (NEW.dueTime IS NOT NULL AND NEW.dueTime NOT BETWEEN 0 AND 1439)
            OR NEW.isExam NOT IN (0,1) OR NEW.isCompleted NOT IN (0,1)
        ) BEGIN SELECT RAISE(ABORT, 'Invalid milestones values'); END;
PRAGMA user_version = 2;
