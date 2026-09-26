package com.example.mydailyroutine.core.database

import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.core.database.daos.*

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar

@Database(
    entities = [SubjectEntity::class, TimeBlockEntity::class, EventOverrideEntity::class,
        SchoolCalendarEntryEntity::class, MilestoneEntity::class, RoutineCompletionEntity::class,
        AlarmDeliveryEntity::class, DemoImportEntity::class, HistoricalVelocityEntity::class,
        StudyTopicEntity::class, SpacedReviewEntity::class, BacklogEntryEntity::class, ActiveExecutionEntity::class,
        TaskEntity::class, GoalsProjectEntity::class, GoalActivityEntity::class, GoalMilestoneEntity::class, GoalProgressEntity::class],
    version = 9,
    exportSchema = true,
)
@TypeConverters(TimeConverters::class)
abstract class RoutineDatabase : RoomDatabase() {
    abstract fun subjects(): SubjectDao
    abstract fun routines(): TimeBlockDao
    abstract fun overrides(): OverrideDao
    abstract fun calendar(): CalendarDao
    abstract fun milestones(): MilestoneDao
    abstract fun completions(): CompletionDao
    abstract fun alarmDeliveries(): AlarmDeliveryDao
    abstract fun demoImports(): DemoImportDao
    abstract fun learning(): LearningDao
    abstract fun backlog(): BacklogDao
    abstract fun execution(): ExecutionDao
    abstract fun tasks(): TaskDao
    abstract fun goals(): GoalsDao

    companion object {
        fun create(context: Context): RoutineDatabase = Room.databaseBuilder(
            context.applicationContext, RoutineDatabase::class.java, "daily-routine.db",
        )
            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6, DatabaseMigrations.MIGRATION_6_7, DatabaseMigrations.MIGRATION_7_8, DatabaseMigrations.MIGRATION_8_9)
            .addCallback(SeedAndIntegrityCallback())
            // No destructive migration fallback: schema changes must ship an explicit migration.
            .build()
    }
}

/**
 * Seeds the bundled calendar and installs the integrity triggers.
 *
 * The calendar is seeded in **dataset keys**, not in translated titles. It used to be seeded in
 * whatever language the app happened to speak on first launch, and nothing ever rewrote those rows:
 * a reader who started in Slovenian and switched to English kept reading "Jesenske počitnice" under
 * an English interface. The key is stable, the screen translates it on the way out
 * (`Resources.calendarTitle`), and the rows stay correct in both languages for the life of the
 * install. The Slovenian titles are the keys, so a database seeded in Slovenian — every database
 * that predates the English translation — already holds exactly these rows and needs no migration.
 */
class SeedAndIntegrityCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DatabaseIntegrity.install(db)
        DatabaseMigrations.installExecutionIntegrity(db)
        DatabaseMigrations.installActualTimingIntegrity(db)
        DatabaseMigrations.installPatternIntegrity(db)
        // Runs inside Room's creation transaction: the very first read sees the complete calendar.
        // No coroutine callback race, blocking main-thread DAO, fabricated exams, or live download.
        val insert = db.compileStatement(
            "INSERT OR IGNORE INTO school_calendar(date, title, isWorkFreeDay) VALUES (?, ?, ?)",
        )
        insert.use { statement ->
            SlovenianAcademicCalendar.entries().forEach { entry ->
                statement.clearBindings()
                statement.bindLong(1, entry.date.toEpochDay())
                statement.bindString(2, entry.title)
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
