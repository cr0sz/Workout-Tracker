package com.workouttracker.data.db

import android.content.Context
import androidx.room.*
import com.workouttracker.data.model.*

@Database(entities = [
    Workout::class, WorkoutExercise::class, ExerciseSet::class,
    CardioSession::class, BodyweightEntry::class, ProgramProgress::class,
    WorkoutTemplate::class, TemplateExercise::class,
    CustomProgram::class, CustomProgramDay::class, CustomProgramExercise::class
], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "workout_database")
                .fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
