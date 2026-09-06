package com.example.mydailyroutine.data.local

import android.content.Context
import android.content.res.Resources
import com.example.mydailyroutine.platform.calendarTitle
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar

@Database(
    entities = [SubjectEntity::class, RoutineBlockEntity::class, EventOverrideEntity::class,
        SchoolCalendarEntryEntity::class, MilestoneEntity::class, RoutineCompletionEntity::class,
        AlarmDeliveryEntity::class, DemoImportEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(TimeConverters::class)
abstract class RoutineDatabase : RoomDatabase() {
    abstract fun subjects(): SubjectDao
    abstract fun routines(): RoutineDao
    abstract fun overrides(): OverrideDao
    abstract fun calendar(): CalendarDao
    abstract fun milestones(): MilestoneDao
    abstract fun completions(): CompletionDao
    abstract fun alarmDeliveries(): AlarmDeliveryDao
    abstract fun demoImports(): DemoImportDao

    companion object {
        fun create(context: Context): RoutineDatabase = Room.databaseBuilder(
            context.applicationContext, RoutineDatabase::class.java, "daily-routine.db",
        )
            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .addCallback(SeedAndIntegrityCallback(context.resources))
            // No destructive migration fallback: schema changes must ship an explicit migration.
            .build()
    }
}

class SeedAndIntegrityCallback(private val resources: Resources) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DatabaseIntegrity.install(db)
        // Runs inside Room's creation transaction: the very first read sees the complete calendar.
        // No coroutine callback race, blocking main-thread DAO, fabricated exams, or live download.
        val insert = db.compileStatement(
            "INSERT OR IGNORE INTO school_calendar(date, title, isWorkFreeDay) VALUES (?, ?, ?)",
        )
        insert.use { statement ->
            SlovenianAcademicCalendar.entries().forEach { entry ->
                statement.clearBindings()
                statement.bindLong(1, entry.date.toEpochDay())
                statement.bindString(2, resources.calendarTitle(entry.title))
                statement.bindLong(3, if (entry.isWorkFreeDay) 1L else 0L)
                statement.executeInsert()
            }
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        // Room enables FKs in onConfigure; fail closed if an integration ever disables them.
        db.query("PRAGMA foreign_keys").use { cursor ->
            check(cursor.moveToFirst() && cursor.getInt(0) == 1) { "SQLite foreign keys must be enabled" }
        }
    }
}
