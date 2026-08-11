package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig

@Database(entities = [TimeEntry::class, UserConfig::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun userConfigDao(): UserConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_entries ADD COLUMN shiftId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN shiftType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN rawCheckIn INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN rawCheckOut INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN normalizedCheckIn INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN normalizedCheckOut INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN workDay REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN otHours REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN lateMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN earlyLeaveMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_entries ADD COLUMN customBreakDeduction INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN customBreakHours REAL DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timesnap_pro_db"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_7_8)
                .fallbackToDestructiveMigration() // safe for production prototyping iteration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
