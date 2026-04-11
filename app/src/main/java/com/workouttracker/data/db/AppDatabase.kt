package com.workouttracker.data.db

import android.content.Context
import androidx.room.*
import com.workouttracker.data.model.*

@Database(entities = [
    Workout::class, WorkoutExercise::class, ExerciseSet::class,
    CardioSession::class, BodyweightEntry::class, ProgramProgress::class,
    WorkoutTemplate::class, TemplateExercise::class,
    CustomProgram::class, CustomProgramDay::class, CustomProgramExercise::class,
    UserProfile::class, AiChatMessage::class
], version = 5, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun aiDao(): AiDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // ── Add new migrations here whenever version bumps ─────────────────────
        // Example for a future v5→v6 bump:
        //
        // val MIGRATION_5_6 = object : Migration(5, 6) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE WorkoutExercise ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        //     }
        // }

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "workout_database"
            )
                // .addMigrations(MIGRATION_5_6)  ← add migrations here when bumping version
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }
}
