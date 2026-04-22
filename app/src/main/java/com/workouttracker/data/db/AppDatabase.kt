package com.workouttracker.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workouttracker.data.model.*

@Database(entities = [
    Workout::class, WorkoutExercise::class, ExerciseSet::class,
    CardioSession::class, BodyweightEntry::class, ProgramProgress::class,
    WorkoutTemplate::class, TemplateExercise::class,
    CustomProgram::class, CustomProgramDay::class, CustomProgramExercise::class,
    UserProfile::class, AiChatMessage::class
], version = 7, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun aiDao(): AiDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // v5 → v6: add routeJson column to cardio_sessions
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cardio_sessions ADD COLUMN routeJson TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // v6 → v7: add name column to cardio_sessions + create AI tables
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cardio_sessions ADD COLUMN name TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_profile` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `age` INTEGER NOT NULL,
                        `experience` TEXT NOT NULL,
                        `primaryGoal` TEXT NOT NULL,
                        `injuries` TEXT NOT NULL,
                        `preferredSplit` TEXT NOT NULL,
                        `trainingDaysPerWeek` INTEGER NOT NULL,
                        `additionalNotes` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "workout_database"
            )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .build().also { INSTANCE = it }
        }
    }
}
