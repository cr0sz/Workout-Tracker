package com.workouttracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(@PrimaryKey val date: String, val notes: String = "")

@Entity(tableName = "workout_exercises",
    foreignKeys = [ForeignKey(entity = Workout::class, parentColumns = ["date"],
        childColumns = ["workoutDate"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("workoutDate")])
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutDate: String, val exerciseName: String, val orderIndex: Int = 0)

@Entity(tableName = "exercise_sets",
    foreignKeys = [ForeignKey(entity = WorkoutExercise::class, parentColumns = ["id"],
        childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("exerciseId")])
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long, val setNumber: Int, val reps: Int,
    val weight: Float, val isBodyweight: Boolean = false)

@Entity(tableName = "cardio_sessions")
data class CardioSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, val type: String,
    val distanceKm: Float? = null, val durationMinutes: Int? = null,
    val weightKg: Float? = null, val calories: Int? = null, val notes: String = "")

@Entity(tableName = "bodyweight_entries")
data class BodyweightEntry(@PrimaryKey val date: String, val weightKg: Float)

@Entity(tableName = "program_progress")
data class ProgramProgress(
    @PrimaryKey val programId: String,
    val currentWeek: Int = 1, val currentDay: Int = 1,
    val startDate: String = "", val lastSessionDate: String = "")

// ── Workout Templates ─────────────────────────────────────────────────────────

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdDate: String = "",
    val lastUsedDate: String = ""
)

@Entity(tableName = "template_exercises",
    foreignKeys = [ForeignKey(entity = WorkoutTemplate::class, parentColumns = ["id"],
        childColumns = ["templateId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("templateId")])
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseName: String,
    val orderIndex: Int = 0,
    val defaultSets: Int = 3,
    val defaultReps: String = "8-12"
)

// ── Custom Programs ───────────────────────────────────────────────────────────

@Entity(tableName = "custom_programs")
data class CustomProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tagline: String = "",
    val daysPerWeek: Int = 3,
    val durationWeeks: Int = 8,
    val difficulty: String = "Intermediate",
    val createdDate: String = ""
)

@Entity(tableName = "custom_program_days",
    foreignKeys = [ForeignKey(entity = CustomProgram::class, parentColumns = ["id"],
        childColumns = ["programId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("programId")])
data class CustomProgramDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val dayNumber: Int,
    val name: String,
    val focus: String = "",
    val isRestDay: Boolean = false
)

@Entity(tableName = "custom_program_exercises",
    foreignKeys = [ForeignKey(entity = CustomProgramDay::class, parentColumns = ["id"],
        childColumns = ["dayId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("dayId")])
data class CustomProgramExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val exerciseName: String,
    val sets: Int = 3,
    val reps: String = "8-12",
    val notes: String = "",
    val orderIndex: Int = 0
)

// ── Query helpers ─────────────────────────────────────────────────────────────

data class LastSetInfo(val weight: Float, val reps: Int, val isBodyweight: Boolean, val workoutDate: String)
data class ExerciseHistoryEntry(val workoutDate: String, val maxWeight: Float, val totalSets: Int, val totalReps: Int)
data class ExerciseSetEntry(val workoutDate: String, val setNumber: Int, val reps: Int, val weight: Float)

val CARDIO_TYPES = listOf("Walk","Run","Cycle","Swim","Rowing","Elliptical","Stair Climber","Jump Rope","HIIT","Other")

val EXERCISE_CATEGORIES = mapOf(
    "Chest" to listOf(
        "Bench Press", "Incline Bench Press", "Decline Bench Press", "Dumbbell Press", "Incline Dumbbell Press",
        "Chest Press Machine", "Dumbbell Fly", "Incline Dumbbell Fly", "Cable Fly", "Push-up", "Chest Dip",
        "Pec Deck Fly", "Weighted Push-up"
    ),
    "Back" to listOf(
        "Deadlift", "Barbell Row", "Dumbbell Row", "Lat Pulldown", "Pull-up", "Chin-up", "Cable Row",
        "Face Pull", "Shrug", "T-Bar Row", "Seated Cable Row", "Straight Arm Pulldown", "Hyperextension",
        "One Arm Dumbbell Row", "Chest Supported Row"
    ),
    "Shoulders" to listOf(
        "Overhead Press", "Dumbbell Shoulder Press", "Lateral Raise", "Front Raise", "Reverse Fly",
        "Arnold Press", "Upright Row", "Military Press", "Machine Shoulder Press", "Cable Lateral Raise"
    ),
    "Biceps" to listOf(
        "Barbell Bicep Curl", "Dumbbell Bicep Curl", "Hammer Curl", "Concentration Curl", "Preacher Curl",
        "Incline Dumbbell Curl", "Cable Curl", "EZ Bar Curl", "Spider Curl"
    ),
    "Triceps" to listOf(
        "Tricep Extension", "Skull Crusher", "Tricep Pushdown", "Dip", "Close Grip Bench Press",
        "Overhead Dumbbell Extension", "Rope Pushdown", "Kickback", "Bench Dip"
    ),
    "Legs" to listOf(
        "Squat", "Front Squat", "Leg Press", "Romanian Deadlift", "Leg Curl", "Leg Extension",
        "Calf Raise", "Hip Thrust", "Lunge", "Bulgarian Split Squat", "Hack Squat", "Sumo Deadlift",
        "Goblet Squat", "Step-up", "Seated Calf Raise", "Standing Calf Raise", "Glute Bridge"
    ),
    "Core" to listOf(
        "Plank", "Crunch", "Russian Twist", "Leg Raise", "Ab Wheel Rollout", "Mountain Climber",
        "Cable Crunch", "Hanging Leg Raise", "Bicycle Crunch", "V-up", "Bird Dog", "Dead Bug"
    ),
    "Full Body / Other" to listOf(
        "Kettlebell Swing", "Clean and Press", "Burpee", "Farmer's Walk", "Box Jump"
    )
)

val PRESET_EXERCISES = EXERCISE_CATEGORIES.values.flatten().sorted()
