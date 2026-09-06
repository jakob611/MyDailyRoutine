#!/usr/bin/env python3
"""SDK-free SQLite smoke tests. Derive column/FK/index metadata from the actual Room entities,
then execute the app's actual trigger predicates. Room runtime behavior is separately covered
by RoomIntegrityTest on an Android device; this is not a substitute for KSP or instrumentation.
"""
from pathlib import Path
import re
import sqlite3
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
LOCAL = ROOT / "app/src/main/java/com/example/mydailyroutine/data/local"
SOURCE = (LOCAL / "Entities.kt").read_text()
MATCHES = list(re.finditer(r"data class (\w+)\(", SOURCE))
ENTITIES = {}
for index, match in enumerate(MATCHES):
    meta = SOURCE[SOURCE.rfind("@Entity", 0, match.start()):match.start()]
    end = SOURCE.rfind("@Entity", match.end(), MATCHES[index + 1].start()) if index + 1 < len(MATCHES) else len(SOURCE)
    body = SOURCE[match.end():end]
    table = re.search(r'tableName = "(\w+)"', meta).group(1)
    columns = re.findall(r"val (\w+): ([\w?]+)", body)
    ENTITIES[match.group(1)] = (table, meta, body, columns)


def quoted_values(text):
    return re.findall(r'"(\w+)"', text)


def database():
    connection = sqlite3.connect(":memory:", isolation_level=None)
    connection.execute("PRAGMA foreign_keys = ON")
    for table, meta, body, columns in ENTITIES.values():
        definitions = []
        for name, kind in columns:
            sql_kind = "TEXT" if kind.rstrip("?") in ("String", "RoutineCategory") else "INTEGER"
            definition = f'"{name}" {sql_kind}'
            if name == "id" and "@PrimaryKey(autoGenerate = true)" in body:
                definition += " PRIMARY KEY AUTOINCREMENT"
            if not kind.endswith("?"):
                definition += " NOT NULL"
            definitions.append(definition)
        composite = re.search(r"primaryKeys = \[([^]]+)\]", meta)
        if composite:
            definitions.append("PRIMARY KEY (" + ",".join(quoted_values(composite.group(1))) + ")")
        for fk in re.findall(r"ForeignKey\((.*?)\)", meta, re.S):
            parent_class = re.search(r"entity = (\w+)::class", fk).group(1)
            parents = quoted_values(re.search(r"parentColumns = \[([^]]+)\]", fk).group(1))
            children = quoted_values(re.search(r"childColumns = \[([^]]+)\]", fk).group(1))
            delete = re.search(r"onDelete = ForeignKey\.(\w+)", fk).group(1).replace("_", " ")
            definitions.append(f"FOREIGN KEY ({','.join(children)}) REFERENCES {ENTITIES[parent_class][0]}({','.join(parents)}) ON DELETE {delete}")
        connection.execute(f"CREATE TABLE {table} ({','.join(definitions)})")
        for value in re.findall(r"Index\(([^)]+)\)", meta):
            names = quoted_values(value)
            unique = "UNIQUE " if "unique = true" in value else ""
            connection.execute(f"CREATE {unique}INDEX index_{table}_{'_'.join(names)} ON {table}({','.join(names)})")
    predicates = re.findall(r'"(\w+)" to """(.*?)"""\.trimIndent\(\)', (LOCAL / "DatabaseIntegrity.kt").read_text(), re.S)
    assert len(predicates) == len(ENTITIES) == 8, "All entity integrity predicates must be exercised"
    for table, predicate in predicates:
        for operation in ("INSERT", "UPDATE"):
            connection.execute(f"CREATE TRIGGER validate_{table}_{operation.lower()} BEFORE {operation} ON {table} FOR EACH ROW WHEN ({predicate}) BEGIN SELECT RAISE(ABORT, 'Invalid {table} values'); END")
    return connection


class SQLiteIntegritySmokeTest(unittest.TestCase):
    def setUp(self):
        self.db = database()
        self.db.execute("INSERT INTO subjects VALUES (1, 'Math HL', ?, 45)", (0xFF4499CC,))
        self.db.execute("INSERT INTO routine_blocks VALUES (1, 1, 'School', 'SCHOOL', 1, 480, 540, 1, NULL, NULL)")
        # 2026-09-07, a Monday, expressed as epoch day just like TimeConverters.
        import datetime
        self.day = (datetime.date(2026, 9, 7) - datetime.date(1970, 1, 1)).days

    def tearDown(self):
        self.db.close()

    def fails(self, sql, values=()):
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute(sql, values)

    def test_foreign_keys_are_enabled_and_orphans_rejected(self):
        self.assertEqual(1, self.db.execute("PRAGMA foreign_keys").fetchone()[0])
        self.fails("UPDATE routine_blocks SET subjectId = 999 WHERE id = 1")
        self.fails("INSERT INTO routine_completions VALUES(999, ?)", (self.day,))
        self.fails("INSERT INTO milestones VALUES(1, 999, 'EE', ?, NULL, 0, 0)", (self.day,))

    def test_title_time_enum_and_boolean_checks(self):
        for sql in (
            "UPDATE routine_blocks SET endTime = startTime", "UPDATE routine_blocks SET startTime = -1",
            "UPDATE routine_blocks SET endTime = 1440", "UPDATE routine_blocks SET title = '  '",
            "UPDATE routine_blocks SET category = 'MISSING'", "UPDATE routine_blocks SET dayOfWeek = 8",
            "UPDATE routine_blocks SET isNotificationEnabled = 3", "UPDATE routine_blocks SET validFrom = 10, validUntil = 5",
        ):
            self.fails(sql)

    def test_subject_constraints(self):
        self.fails("UPDATE subjects SET defaultDurationMinutes = 0")
        self.fails("UPDATE subjects SET colorHex = 16777215")
        self.fails("UPDATE subjects SET name = ''")

    def test_unique_override_and_partial_time_validation(self):
        self.db.execute("INSERT INTO event_overrides VALUES(1, 1, ?, 0, 510, NULL, 'Revision')", (self.day,))
        self.fails("INSERT INTO event_overrides VALUES(2, 1, ?, 1, NULL, NULL, NULL)", (self.day,))
        self.fails("UPDATE event_overrides SET customStartTime = 540")
        self.fails("UPDATE event_overrides SET overrideDate = ?", (self.day + 1,))
        self.fails("UPDATE routine_blocks SET endTime = 510")

    def test_completion_must_belong_to_occurrence(self):
        self.fails("INSERT INTO routine_completions VALUES(1, ?)", (self.day + 1,))
        self.db.execute("INSERT INTO routine_completions VALUES(1, ?)", (self.day,))
        self.fails("UPDATE routine_blocks SET dayOfWeek = 2")
        self.fails("UPDATE routine_blocks SET validUntil = ?", (self.day - 1,))

    def test_delete_semantics(self):
        self.db.execute("INSERT INTO event_overrides VALUES(1, 1, ?, 0, NULL, NULL, NULL)", (self.day,))
        self.db.execute("INSERT INTO routine_completions VALUES(1, ?)", (self.day,))
        self.db.execute("INSERT INTO alarm_deliveries VALUES(1, ?, 'BLOCK_PREVIEW', 1000)", (self.day,))
        self.db.execute("INSERT INTO milestones VALUES(1, 1, 'IA', ?, NULL, 0, 0)", (self.day,))
        self.db.execute("DELETE FROM subjects WHERE id = 1")
        self.assertIsNone(self.db.execute("SELECT subjectId FROM routine_blocks").fetchone()[0])
        self.assertIsNone(self.db.execute("SELECT subjectId FROM milestones").fetchone()[0])
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM event_overrides").fetchone()[0])
        self.db.execute("DELETE FROM routine_blocks WHERE id = 1")
        for table in ("event_overrides", "routine_completions", "alarm_deliveries"):
            self.assertEqual(0, self.db.execute(f"SELECT count(*) FROM {table}").fetchone()[0])
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM milestones").fetchone()[0])

    def test_delivery_claim_is_unique(self):
        for _ in range(2):
            self.db.execute("INSERT OR IGNORE INTO alarm_deliveries VALUES(1, ?, 'BLOCK_PREVIEW', 1000)", (self.day,))
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM alarm_deliveries").fetchone()[0])
        self.fails("UPDATE alarm_deliveries SET kind = 'INVALID'")

    def test_query_indexes_exist(self):
        query = "EXPLAIN QUERY PLAN SELECT * FROM milestones WHERE dueDate BETWEEN 1 AND 2 ORDER BY dueDate, dueTime"
        self.assertIn("index_milestones_dueDate_dueTime", str(self.db.execute(query).fetchall()))
        query = "EXPLAIN QUERY PLAN SELECT * FROM routine_blocks WHERE dayOfWeek IN (1,2) ORDER BY dayOfWeek, startTime"
        self.assertIn("index_routine_blocks_dayOfWeek_startTime", str(self.db.execute(query).fetchall()))

    def test_calendar_unique_and_work_free_boolean(self):
        self.db.execute("INSERT INTO school_calendar VALUES(1, ?, 'Holiday', 1)", (self.day,))
        self.fails("INSERT INTO school_calendar VALUES(2, ?, 'Holiday', 1)", (self.day,))
        self.fails("UPDATE school_calendar SET isWorkFreeDay = 2")

    def test_negative_epoch_weekdays(self):
        import datetime
        day = (datetime.date(1960, 1, 4) - datetime.date(1970, 1, 1)).days
        self.db.execute("INSERT INTO event_overrides VALUES(1, 1, ?, 0, NULL, NULL, NULL)", (day,))
        self.fails("UPDATE event_overrides SET overrideDate = ?", (day - 1,))

    def test_xml_and_no_network_permission(self):
        for file in (ROOT / "app/src/main").rglob("*.xml"):
            ET.parse(file)
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        android = "{http://schemas.android.com/apk/res/android}"
        tools = "{http://schemas.android.com/tools}"
        for permission in manifest.findall("uses-permission"):
            if permission.get(android + "name") in ("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"):
                self.assertEqual("remove", permission.get(tools + "node"))
        self.assertEqual("false", manifest.find("application").get(android + "allowBackup"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
