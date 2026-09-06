package com.example.mydailyroutine.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mydailyroutine.data.seed.DefaultDataSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        RoutineBlockEntity::class,
        EventOverrideEntity::class,
        SchoolCalendarEntryEntity::class,
        MilestoneEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class RoutineDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun routineBlockDao(): RoutineBlockDao
    abstract fun eventOverrideDao(): EventOverrideDao
    abstract fun schoolCalendarDao(): SchoolCalendarDao
    abstract fun milestoneDao(): MilestoneDao

    companion object {
        @Volatile
        private var instance: RoutineDatabase? = null
        private val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getInstance(context: Context): RoutineDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RoutineDatabase::class.java,
                "my_daily_routine.db",
            )
                .setForeignKeyConstraintsEnabled(true)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Room opens lazily. At callback time the singleton has already been
                        // assigned by the builder, so the seeding work does not block first draw.
                        seedScope.launch {
                            val database = instance ?: return@launch
                            DefaultDataSeeder.seedIfNeeded(database)
                        }
                    }
                })
                .build()
                .also { instance = it }
        }
    }
}
