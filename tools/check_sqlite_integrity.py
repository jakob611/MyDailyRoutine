#!/usr/bin/env python3
"""SDK-free checks against current entity DDL + actual validation predicates and migration SQL."""
from pathlib import Path
import re, sqlite3, unittest
import xml.etree.ElementTree as ET
from schema_metadata import ROOT, ddl
LOCAL=ROOT/'app/src/main/java/com/example/mydailyroutine/core/database'

def install(connection):
    predicates=re.findall(r'"(\w+)" to """(.*?)"""\.trimIndent\(\)',(LOCAL/'DatabaseIntegrity.kt').read_text(),re.S)
    assert len(predicates)==12 and len(ddl())==13
    for table,predicate in predicates:
        for op in ('INSERT','UPDATE'):
            connection.execute(f"CREATE TRIGGER IF NOT EXISTS validate_{table}_{op.lower()} BEFORE {op} ON {table} FOR EACH ROW WHEN ({predicate}) BEGIN SELECT RAISE(ABORT,'Invalid {table} values'); END")

def install_measured_timing(db):
    source=(LOCAL/'DatabaseMigrations.kt').read_text()
    columns={row[1] for row in db.execute('PRAGMA table_info(routine_completions)')}
    if 'actualStartedAtEpochMillis' not in columns: return
    sql=next(text for text in re.findall(r'"""(.*?)"""',source,re.S) if 'validate_completion_timing_' in text)
    for operation in ('INSERT','UPDATE'):
        db.execute(sql.replace('${operation.lowercase()}',operation.lower()).replace('$operation',operation))

def database():
    db=sqlite3.connect(':memory:',isolation_level=None);db.execute('PRAGMA foreign_keys=ON')
    for statements in ddl().values():
        for statement in statements:db.execute(statement)
    install(db)
    install_measured_timing(db)
    source=(LOCAL/'DatabaseMigrations.kt').read_text()
    pattern=next(text for text in re.findall(r'"""(.*?)"""',source,re.S) if 'validate_pattern_' in text)
    for operation in ('INSERT','UPDATE'):
        db.execute(pattern.replace('${operation.lowercase()}',operation.lower()).replace('$operation',operation))
    return db

OVERRIDE='INSERT INTO event_overrides(id,routineBlockId,overrideDate,isCancelled,customStartTime,customEndTime,customTitle)'
COMPLETION='INSERT INTO routine_completions(routineBlockId,date)'
MILESTONE='INSERT INTO milestones(id,subjectId,title,dueDate,dueTime,isExam,isCompleted)'

class SQLiteIntegritySmokeTest(unittest.TestCase):
    def setUp(self):
        self.db=database()
        self.db.execute("INSERT INTO subjects VALUES(1,'Matematika',?,45)",(0xFF3B82F6,))
        self.db.execute("INSERT INTO routine_blocks VALUES(1,1,'Pouk','SCHOOL',1,480,60,1,NULL,NULL,60,0.0,5.0,1,NULL,60,NULL,NULL,NULL,NULL,NULL,'USER',1)")
        import datetime
        self.day=(datetime.date(2026,9,7)-datetime.date(1970,1,1)).days
    def tearDown(self):self.db.close()
    def fails(self,sql,args=()):
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute(sql,args)
    def test_foreign_keys_and_orphans(self):
        self.assertEqual(1,self.db.execute('PRAGMA foreign_keys').fetchone()[0])
        self.fails('UPDATE routine_blocks SET subjectId=999')
        self.fails(COMPLETION+' VALUES(999,?)',(self.day,))
        self.fails(MILESTONE+" VALUES(1,999,'EE',?,NULL,0,0)",(self.day,))
    def test_minutes_weights_fixed_and_categories(self):
        for sql in ['UPDATE routine_blocks SET durationMinutes=0','UPDATE routine_blocks SET startMinutes=-1',
                    'UPDATE routine_blocks SET minDurationMinutes=61','UPDATE routine_blocks SET elasticity=-1',
                    'UPDATE routine_blocks SET priorityWeight=0','UPDATE routine_blocks SET isFixedCommitment=0',
                    "UPDATE routine_blocks SET category='MISSING'",'UPDATE routine_blocks SET dayOfWeek=8',
                    'UPDATE routine_blocks SET isNotificationEnabled=3','UPDATE routine_blocks SET validFrom=10,validUntil=5']:
            self.fails(sql)
    def test_subject_constraints(self):
        self.fails('UPDATE subjects SET defaultDurationMinutes=0');self.fails('UPDATE subjects SET colorHex=1');self.fails("UPDATE subjects SET name=' '")
    def test_unique_override_and_partial_times(self):
        self.db.execute(OVERRIDE+" VALUES(1,1,?,0,510,NULL,'Učenje')",(self.day,))
        self.fails(OVERRIDE+' VALUES(2,1,?,1,NULL,NULL,NULL)',(self.day,))
        self.fails('UPDATE event_overrides SET customStartTime=540')
        self.fails('UPDATE event_overrides SET dayShift=8')
        self.fails('UPDATE routine_blocks SET durationMinutes=30,minDurationMinutes=30')
    def test_completion_belongs_to_occurrence(self):
        self.fails(COMPLETION+' VALUES(1,?)',(self.day+1,))
        self.db.execute(COMPLETION+' VALUES(1,?)',(self.day,))
        self.fails('UPDATE routine_blocks SET dayOfWeek=2')
        self.fails('UPDATE routine_completions SET actualMinutes=0')
    def test_detach_subject_and_cascade_routine(self):
        self.db.execute(OVERRIDE+' VALUES(1,1,?,0,NULL,NULL,NULL)',(self.day,))
        self.db.execute(COMPLETION+' VALUES(1,?)',(self.day,))
        self.db.execute("INSERT INTO alarm_deliveries VALUES(1,?,'BLOCK_PREVIEW',1000)",(self.day,))
        self.db.execute(MILESTONE+" VALUES(1,1,'IA',?,NULL,0,0)",(self.day,))
        self.db.execute('DELETE FROM subjects WHERE id=1')
        self.assertIsNone(self.db.execute('SELECT subjectId FROM routine_blocks').fetchone()[0])
        self.assertIsNone(self.db.execute('SELECT subjectId FROM milestones').fetchone()[0])
        self.db.execute('DELETE FROM routine_blocks WHERE id=1')
        for table in ('event_overrides','routine_completions','alarm_deliveries'):self.assertEqual(0,self.db.execute(f'SELECT count(*) FROM {table}').fetchone()[0])
        self.assertEqual(1,self.db.execute('SELECT count(*) FROM milestones').fetchone()[0])
    def test_delivery_unique(self):
        for _ in range(2):self.db.execute("INSERT OR IGNORE INTO alarm_deliveries VALUES(1,?,'BLOCK_PREVIEW',1000)",(self.day,))
        self.assertEqual(1,self.db.execute('SELECT count(*) FROM alarm_deliveries').fetchone()[0])
    def test_indexes(self):
        self.assertIn('index_milestones_dueDate_dueTime',str(self.db.execute('EXPLAIN QUERY PLAN SELECT * FROM milestones WHERE dueDate BETWEEN 1 AND 2 ORDER BY dueDate,dueTime').fetchall()))
        self.assertIn('index_routine_blocks_dayOfWeek_startMinutes',str(self.db.execute('EXPLAIN QUERY PLAN SELECT * FROM routine_blocks WHERE dayOfWeek IN (1,2) ORDER BY dayOfWeek,startMinutes').fetchall()))
    def test_calendar_unique(self):
        self.db.execute("INSERT INTO school_calendar VALUES(1,?,'Počitnice',1)",(self.day,))
        self.fails("INSERT INTO school_calendar VALUES(2,?,'Počitnice',1)",(self.day,))
    def test_history_validity_and_detachment(self):
        self.fails("INSERT INTO historical_velocity VALUES(1,1,0,30,1000,1,?)",(self.day,))
        self.db.execute("INSERT INTO historical_velocity VALUES(1,1,60,90,1000,1,?)",(self.day,))
        self.fails("INSERT INTO historical_velocity VALUES(2,1,60,90,1000,1,?)",(self.day,))
        self.db.execute('DELETE FROM routine_blocks WHERE id=1')
        self.assertIsNone(self.db.execute('SELECT routineBlockId FROM historical_velocity').fetchone()[0])
    def test_topics_reviews_and_backlog_fks(self):
        self.db.execute("INSERT INTO study_topics VALUES(1,'Optika',1,1,100,4,15,3.0,NULL)")
        self.db.execute('INSERT INTO spaced_reviews VALUES(1,1,20,15,3.0,1,NULL,0)')
        self.fails('INSERT INTO spaced_reviews VALUES(2,1,20,15,3.0,1,NULL,0)')
        self.db.execute("INSERT INTO backlog_entries VALUES(1,'Optika','FOCUS_ANALYTICAL',15,15,1.0,3.0,1,NULL,NULL,NULL,1,1,'REVIEW_CAPACITY',15,NULL)")
        self.db.execute('DELETE FROM study_topics WHERE id=1')
        self.assertEqual(0,self.db.execute('SELECT count(*) FROM backlog_entries').fetchone()[0])
    def test_migration_preserves_existing_rows_without_disabling_fks(self):
        db=sqlite3.connect(':memory:',isolation_level=None);db.execute('PRAGMA foreign_keys=ON')
        db.executescript((ROOT/'app/src/androidTest/assets/schema-v2.sql').read_text())
        db.execute("INSERT INTO subjects VALUES(1,'Matematika',4282090230,45)")
        db.execute("INSERT INTO routine_blocks VALUES(1,1,'Spanec','PERSONAL',1,1380,60,0,NULL,NULL)")
        db.execute('INSERT INTO event_overrides VALUES(1,1,?,0,NULL,NULL,NULL)',(self.day,))
        db.execute('INSERT INTO routine_completions VALUES(1,?)',(self.day,))
        db.execute("INSERT INTO alarm_deliveries VALUES(1,?,'BLOCK_PREVIEW',1000)",(self.day,))
        tables=['routine_blocks','event_overrides','routine_completions','alarm_deliveries']
        for table in tables:db.execute(f'CREATE TEMP TABLE _v2_{table} AS SELECT * FROM {table}')
        for (name,) in db.execute("SELECT name FROM sqlite_master WHERE type='trigger'").fetchall():db.execute(f'DROP TRIGGER `{name}`')
        for table in tables[1:]+tables[:1]:db.execute(f'DROP TABLE {table}')
        db.execute('ALTER TABLE milestones ADD COLUMN estimatedEffortHours REAL NOT NULL DEFAULT 0.0')
        db.execute('ALTER TABLE milestones ADD COLUMN isTerminalExam INTEGER NOT NULL DEFAULT 0')
        for sql in re.findall(r'"""(.*?)"""',(LOCAL/'MigrationSchemaV3.kt').read_text(),re.S):db.execute(sql)
        migration=(LOCAL/'DatabaseMigrations.kt').read_text()
        sql=re.search(r'db.execSQL\("""(.*?)"""',migration,re.S).group(1);db.execute(sql)
        for table,columns,select in [
            ('event_overrides','id,routineBlockId,overrideDate,isCancelled,customStartTime,customEndTime,customTitle,dayShift,cancellationReason',"id,routineBlockId,overrideDate,isCancelled,customStartTime,customEndTime,customTitle,0,'MANUAL'"),
            ('routine_completions','routineBlockId,date,actualMinutes','routineBlockId,date,NULL')]:
            db.execute(f'INSERT INTO {table}({columns}) SELECT {select} FROM _v2_{table}')
        db.execute('INSERT INTO alarm_deliveries SELECT * FROM _v2_alarm_deliveries')
        db.execute('ALTER TABLE backlog_entries ADD COLUMN rawDurationMinutes INTEGER NOT NULL DEFAULT 1')
        db.execute('UPDATE backlog_entries SET rawDurationMinutes=COALESCE((SELECT rawDurationMinutes FROM routine_blocks WHERE id=sourceRoutineId),durationMinutes)')
        install(db)
        self.assertEqual(('ADMIN',1380,120,1),db.execute('SELECT category,startMinutes,durationMinutes,isFixedCommitment FROM routine_blocks').fetchone())
        self.assertEqual(1,db.execute('SELECT count(*) FROM event_overrides').fetchone()[0])
        self.assertEqual([],db.execute('PRAGMA foreign_key_check').fetchall());db.close()
    def test_execution_is_singleton_and_cascades(self):
        self.db.execute("INSERT INTO active_execution VALUES(1,1,?,1000,2000,2000,NULL)",(self.day,))
        self.fails("INSERT INTO active_execution VALUES(1,1,?,1000,2000,2000,NULL)",(self.day,))
        self.db.execute('DELETE FROM routine_blocks WHERE id=1')
        self.assertEqual(0,self.db.execute('SELECT count(*) FROM active_execution').fetchone()[0])
    def test_measured_timing_requires_a_complete_positive_interval(self):
        self.db.execute(COMPLETION+' VALUES(1,?)',(self.day,))
        self.fails('UPDATE routine_completions SET actualStartedAtEpochMillis=1000')
        self.fails("UPDATE routine_completions SET actualMinutes=30,actualStartedAtEpochMillis=2000,actualEndedAtEpochMillis=1000,actualZoneId='UTC'")
        self.db.execute("UPDATE routine_completions SET actualMinutes=30,actualStartedAtEpochMillis=1000,actualEndedAtEpochMillis=1801000,actualZoneId='UTC'")
        self.assertEqual(1801000,self.db.execute('SELECT actualEndedAtEpochMillis FROM routine_completions').fetchone()[0])
    def test_companion_fk_origin_and_series_constraints(self):
        self.db.execute("UPDATE routine_blocks SET seriesKey='group' WHERE id=1")
        self.db.execute("INSERT INTO routine_blocks(id,subjectId,title,category,dayOfWeek,startMinutes,durationMinutes,isNotificationEnabled,minDurationMinutes,elasticity,priorityWeight,isFixedCommitment,rawDurationMinutes,seriesKey,parentRoutineId,origin,isEnabled) VALUES(2,NULL,'Odmor','REST_BUFFER',1,525,5,0,5,0.0,1.0,1,5,'group',1,'LESSON_BREAK',1)")
        self.fails("UPDATE routine_blocks SET parentRoutineId=2 WHERE id=2")
        self.fails("UPDATE routine_blocks SET category='ADMIN' WHERE id=1")
        self.fails("UPDATE routine_blocks SET dayOfWeek=2 WHERE id=1")
        self.db.execute('DELETE FROM routine_blocks WHERE id=1')
        self.assertEqual(0,self.db.execute('SELECT count(*) FROM routine_blocks').fetchone()[0])
    def test_v6_to_v7_additions_preserve_existing_rows_with_fks_enabled(self):
        db=sqlite3.connect(':memory:',isolation_level=None);db.execute('PRAGMA foreign_keys=ON')
        db.executescript((ROOT/'app/src/androidTest/assets/schema-v6.sql').read_text())
        db.execute("INSERT INTO routine_blocks(id,subjectId,title,category,dayOfWeek,startMinutes,durationMinutes,isNotificationEnabled,minDurationMinutes,elasticity,priorityWeight,isFixedCommitment,rawDurationMinutes) VALUES(1,NULL,'Pouk','SCHOOL',1,480,45,0,45,0.0,3.0,1,45)")
        source=(LOCAL/'DatabaseMigrations.kt').read_text();part=source.split('val MIGRATION_6_7',1)[1].split('fun installPatternIntegrity',1)[0]
        for sql in re.findall(r'db.execSQL\("([^\"]+)"\)',part):db.execute(sql)
        self.assertEqual((45,None,None,'USER',1),db.execute('SELECT durationMinutes,seriesKey,parentRoutineId,origin,isEnabled FROM routine_blocks').fetchone())
        self.assertEqual([],db.execute('PRAGMA foreign_key_check').fetchall());db.close()
    def test_privacy_resources(self):
        for file in (ROOT/'app/src/main').rglob('*.xml'):ET.parse(file)
        manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
        android='{http://schemas.android.com/apk/res/android}';tools='{http://schemas.android.com/tools}'
        for permission in manifest.findall('uses-permission'):
            if permission.get(android+'name') in ('android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE'):self.assertEqual('remove',permission.get(tools+'node'))
        self.assertEqual('false',manifest.find('application').get(android+'allowBackup'))

if __name__=='__main__':unittest.main(verbosity=2)
