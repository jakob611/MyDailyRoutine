package com.example.mydailyroutine.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `demo_imports` (`key` TEXT NOT NULL, `importedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`key`))")
            DatabaseIntegrity.install(db)
        }
    }
}
