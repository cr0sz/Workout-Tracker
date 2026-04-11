package com.workouttracker.data.db

import androidx.room.*
import com.workouttracker.data.model.*
import kotlinx.coroutines.flow.Flow

data class ExerciseCount(val exerciseName: String, val count: Int)
data class PersonalRecord(val exerciseName: String, val maxWeight: Float, val date: String)
data class VolumeEntry(val date: String, val volume: Float)

@Dao
interface WorkoutDao {
    // Workouts
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWorkout(w: Workout)
    @Update suspend fun updateWorkout(w: Workout)
    @Delete suspend fun deleteWorkout(w: Workout)
    @Query("SELECT * FROM workouts WHERE date=:date LIMIT 1") suspend fun getWorkoutByDate(date: String): Workout?
    @Query("SELECT date FROM workouts") fun getAllWorkoutDates(): Flow<List<String>>
    @Query("SELECT * FROM workouts ORDER BY date DESC") fun getAllWorkouts(): Flow<List<Workout>>
    @Query("SELECT * FROM workouts ORDER BY date ASC") suspend fun getAllWorkoutsSync(): List<Workout>

    // Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercise(e: WorkoutExercise): Long
    @Delete suspend fun deleteExercise(e: WorkoutExercise)
    @Query("SELECT * FROM workout_exercises WHERE workoutDate=:date ORDER BY orderIndex ASC")
    fun getExercisesForWorkout(date: String): Flow<List<WorkoutExercise>>
    @Query("SELECT * FROM workout_exercises WHERE workoutDate=:date ORDER BY orderIndex ASC")
    suspend fun getExercisesForWorkoutSync(date: String): List<WorkoutExercise>
    @Query("SELECT * FROM workout_exercises ORDER BY workoutDate ASC, orderIndex ASC")
    suspend fun getAllExercisesSync(): List<WorkoutExercise>

    // Sets
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSet(s: ExerciseSet): Long
    @Update suspend fun updateSet(s: ExerciseSet)
    @Delete suspend fun deleteSet(s: ExerciseSet)
    @Query("SELECT * FROM exercise_sets WHERE exerciseId=:id ORDER BY setNumber ASC")
    fun getSetsForExercise(id: Long): Flow<List<ExerciseSet>>
    @Query("SELECT * FROM exercise_sets WHERE exerciseId=:id ORDER BY setNumber ASC")
    suspend fun getSetsForExerciseSync(id: Long): List<ExerciseSet>
    @Query("DELETE FROM exercise_sets WHERE exerciseId=:id") suspend fun deleteAllSetsForExercise(id: Long)
    @Query("SELECT * FROM exercise_sets ORDER BY exerciseId ASC, setNumber ASC")
    suspend fun getAllSetsSync(): List<ExerciseSet>

    // Progressive overload
    @Query("""SELECT es.weight,es.reps,es.isBodyweight,we.workoutDate FROM exercise_sets es
        JOIN workout_exercises we ON es.exerciseId=we.id
        WHERE we.exerciseName=:exerciseName AND we.workoutDate<:beforeDate AND es.isBodyweight=0
        ORDER BY we.workoutDate DESC, es.weight DESC LIMIT 1""")
    suspend fun getBestLastSet(exerciseName: String, beforeDate: String): LastSetInfo?

    // Exercise history (per exercise across all dates)
    @Query("""SELECT we.workoutDate, MAX(es.weight) as maxWeight,
        COUNT(es.id) as totalSets, SUM(es.reps) as totalReps
        FROM exercise_sets es JOIN workout_exercises we ON es.exerciseId=we.id
        WHERE we.exerciseName=:exerciseName AND es.isBodyweight=0
        GROUP BY we.workoutDate ORDER BY we.workoutDate ASC""")
    fun getExerciseHistory(exerciseName: String): Flow<List<ExerciseHistoryEntry>>

    @Query("""SELECT we.workoutDate, es.setNumber, es.reps, es.weight
        FROM exercise_sets es JOIN workout_exercises we ON es.exerciseId=we.id
        WHERE we.exerciseName=:exerciseName AND es.isBodyweight=0
        ORDER BY we.workoutDate ASC, es.setNumber ASC""")
    fun getExerciseSetHistory(exerciseName: String): Flow<List<ExerciseSetEntry>>

    @Query("""SELECT DISTINCT we.exerciseName FROM workout_exercises we ORDER BY we.exerciseName ASC""")
    fun getAllUsedExerciseNames(): Flow<List<String>>

    // Cardio
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCardio(s: CardioSession): Long
    @Update suspend fun updateCardio(s: CardioSession)
    @Delete suspend fun deleteCardio(s: CardioSession)
    @Query("SELECT * FROM cardio_sessions ORDER BY date DESC, id DESC") fun getAllCardioSessions(): Flow<List<CardioSession>>
    @Query("SELECT * FROM cardio_sessions WHERE date=:date ORDER BY id ASC") fun getCardioForDate(date: String): Flow<List<CardioSession>>
    @Query("SELECT date FROM cardio_sessions") fun getAllCardioDates(): Flow<List<String>>
    @Query("SELECT * FROM cardio_sessions WHERE date>=:s AND date<=:e ORDER BY date DESC") fun getCardioInRange(s: String, e: String): Flow<List<CardioSession>>
    @Query("SELECT SUM(distanceKm) FROM cardio_sessions WHERE date>=:s AND date<=:e") fun getTotalDistanceInRange(s: String, e: String): Flow<Float?>
    @Query("SELECT * FROM cardio_sessions ORDER BY date ASC") suspend fun getAllCardioSessionsSync(): List<CardioSession>

    // Bodyweight
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBodyweight(e: BodyweightEntry)
    @Delete suspend fun deleteBodyweight(e: BodyweightEntry)
    @Query("SELECT * FROM bodyweight_entries ORDER BY date DESC") fun getAllBodyweightEntries(): Flow<List<BodyweightEntry>>
    @Query("SELECT * FROM bodyweight_entries ORDER BY date ASC") suspend fun getAllBodyweightSync(): List<BodyweightEntry>

    // Program progress
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProgramProgress(p: ProgramProgress)
    @Delete suspend fun deleteProgramProgress(p: ProgramProgress)
    @Query("SELECT * FROM program_progress WHERE programId=:id LIMIT 1") suspend fun getProgramProgress(id: String): ProgramProgress?
    @Query("SELECT * FROM program_progress") fun getAllProgramProgress(): Flow<List<ProgramProgress>>

    // Workout Templates
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTemplate(t: WorkoutTemplate): Long
    @Update suspend fun updateTemplate(t: WorkoutTemplate)
    @Delete suspend fun deleteTemplate(t: WorkoutTemplate)
    @Query("SELECT * FROM workout_templates ORDER BY lastUsedDate DESC, createdDate DESC")
    fun getAllTemplates(): Flow<List<WorkoutTemplate>>
    @Query("SELECT * FROM workout_templates WHERE id=:id LIMIT 1") suspend fun getTemplateById(id: Long): WorkoutTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTemplateExercise(e: TemplateExercise): Long
    @Delete suspend fun deleteTemplateExercise(e: TemplateExercise)
    @Query("SELECT * FROM template_exercises WHERE templateId=:id ORDER BY orderIndex ASC")
    fun getTemplateExercises(id: Long): Flow<List<TemplateExercise>>
    @Query("SELECT * FROM template_exercises WHERE templateId=:id ORDER BY orderIndex ASC")
    suspend fun getTemplateExercisesSync(id: Long): List<TemplateExercise>
    @Query("DELETE FROM template_exercises WHERE templateId=:id") suspend fun deleteAllTemplateExercises(id: Long)

    // Custom Programs
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCustomProgram(p: CustomProgram): Long
    @Update suspend fun updateCustomProgram(p: CustomProgram)
    @Delete suspend fun deleteCustomProgram(p: CustomProgram)
    @Query("SELECT * FROM custom_programs ORDER BY createdDate DESC") fun getAllCustomPrograms(): Flow<List<CustomProgram>>
    @Query("SELECT * FROM custom_programs WHERE id=:id LIMIT 1") suspend fun getCustomProgramById(id: Long): CustomProgram?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCustomDay(d: CustomProgramDay): Long
    @Update suspend fun updateCustomDay(d: CustomProgramDay)
    @Delete suspend fun deleteCustomDay(d: CustomProgramDay)
    @Query("SELECT * FROM custom_program_days WHERE programId=:id ORDER BY dayNumber ASC")
    fun getCustomDays(id: Long): Flow<List<CustomProgramDay>>
    @Query("SELECT * FROM custom_program_days WHERE programId=:id ORDER BY dayNumber ASC")
    suspend fun getCustomDaysSync(id: Long): List<CustomProgramDay>
    @Query("DELETE FROM custom_program_days WHERE programId=:id") suspend fun deleteAllCustomDays(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCustomExercise(e: CustomProgramExercise): Long
    @Delete suspend fun deleteCustomExercise(e: CustomProgramExercise)
    @Query("SELECT * FROM custom_program_exercises WHERE dayId=:id ORDER BY orderIndex ASC")
    fun getCustomExercises(id: Long): Flow<List<CustomProgramExercise>>
    @Query("SELECT * FROM custom_program_exercises WHERE dayId=:id ORDER BY orderIndex ASC")
    suspend fun getCustomExercisesSync(id: Long): List<CustomProgramExercise>
    @Query("DELETE FROM custom_program_exercises WHERE dayId=:id") suspend fun deleteAllCustomExercises(id: Long)

    // Stats
    @Query("SELECT COUNT(*) FROM workouts") fun getTotalWorkouts(): Flow<Int>
    @Query("SELECT exerciseName,COUNT(*) as count FROM workout_exercises GROUP BY exerciseName ORDER BY count DESC LIMIT 5")
    fun getTopExercises(): Flow<List<ExerciseCount>>
    @Query("SELECT SUM(weight*reps) FROM exercise_sets WHERE isBodyweight=0") fun getTotalVolumeLifted(): Flow<Float?>
    @Query("SELECT COUNT(*) FROM exercise_sets") fun getTotalSets(): Flow<Int>
    @Query("""SELECT exerciseName, weight as maxWeight, workoutDate as date FROM (
        SELECT we.exerciseName, es.weight, we.workoutDate
        FROM exercise_sets es
        JOIN workout_exercises we ON es.exerciseId=we.id
        WHERE es.isBodyweight=0 AND es.weight>0
        ORDER BY es.weight DESC, we.workoutDate DESC
    ) GROUP BY exerciseName ORDER BY maxWeight DESC""")
    fun getPersonalRecords(): Flow<List<PersonalRecord>>

    @Query("""SELECT exerciseName, weight as maxWeight, workoutDate as date FROM (
        SELECT we.exerciseName, es.weight, we.workoutDate
        FROM exercise_sets es
        JOIN workout_exercises we ON es.exerciseId=we.id
        WHERE es.isBodyweight=0 AND es.weight>0
        ORDER BY es.weight DESC, we.workoutDate DESC
    ) GROUP BY exerciseName ORDER BY maxWeight DESC""")
    suspend fun getPersonalRecordsSync(): List<PersonalRecord>
    @Query("SELECT COUNT(*) FROM workouts WHERE date>=:s AND date<=:e") fun getWorkoutsCountInRange(s: String, e: String): Flow<Int>
    @Query("""SELECT COALESCE(SUM(es.weight*es.reps),0) FROM exercise_sets es
        JOIN workout_exercises we ON es.exerciseId=we.id
        WHERE we.workoutDate>=:s AND we.workoutDate<=:e AND es.isBodyweight=0""")
    fun getVolumeInRange(s: String, e: String): Flow<Float>
    @Query("""SELECT COUNT(*) FROM exercise_sets es JOIN workout_exercises we ON es.exerciseId=we.id
        WHERE we.workoutDate>=:s AND we.workoutDate<=:e""")
    fun getSetsCountInRange(s: String, e: String): Flow<Int>

    @Query("""SELECT we.workoutDate as date, SUM(es.weight * es.reps) as volume
        FROM exercise_sets es
        JOIN workout_exercises we ON es.exerciseId = we.id
        WHERE es.isBodyweight = 0
        GROUP BY we.workoutDate
        ORDER BY we.workoutDate ASC""")
    fun getVolumeOverTime(): Flow<List<VolumeEntry>>

    @Query("""SELECT we.exerciseName, COUNT(es.id) as count
        FROM exercise_sets es
        JOIN workout_exercises we ON es.exerciseId = we.id
        GROUP BY we.exerciseName""")
    fun getAllExerciseSetCounts(): Flow<List<ExerciseCount>>
}
