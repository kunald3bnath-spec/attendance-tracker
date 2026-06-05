package com.example.attendancetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create projects table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `projects` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // Insert a default project so existing members can be migrated
        db.execSQL(
            "INSERT INTO projects (name, createdAt) VALUES ('Default Project', ${System.currentTimeMillis()})"
        )
        // Add projectId column to members (all existing members go to project 1)
        db.execSQL("ALTER TABLE members ADD COLUMN projectId INTEGER NOT NULL DEFAULT 1")
        // Create index on projectId
        db.execSQL("CREATE INDEX IF NOT EXISTS index_members_projectId ON members (projectId)")
    }
}

@Database(
    entities = [Project::class, Member::class, AttendanceRecord::class],
    version = 2,
    exportSchema = false,
)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun memberDao(): MemberDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getInstance(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "attendance.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
