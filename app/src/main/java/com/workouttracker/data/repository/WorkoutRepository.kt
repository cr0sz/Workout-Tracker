package com.workouttracker.data.repository

import com.workouttracker.data.db.WorkoutDao
import com.workouttracker.data.model.*

class WorkoutRepository(private val dao: WorkoutDao) {
    // Workouts
    suspend fun insertWorkout(w: Workout) = dao.insertWorkout(w)
    suspend fun updateWorkout(w: Workout) = dao.updateWorkout(w)
    suspend fun getWorkoutByDate(date: String) = dao.getWorkoutByDate(date)
    fun getAllWorkoutDates() = dao.getAllWorkoutDates()
    fun getAllWorkouts() = dao.getAllWorkouts()
    suspend fun getAllWorkoutsSync() = dao.getAllWorkoutsSync()

    // Exercises
    suspend fun insertExercise(e: WorkoutExercise): Long = dao.insertExercise(e)
    suspend fun deleteExercise(e: WorkoutExercise) = dao.deleteExercise(e)
    fun getExercisesForWorkout(date: String) = dao.getExercisesForWorkout(date)
    suspend fun getExercisesForWorkoutSync(date: String) = dao.getExercisesForWorkoutSync(date)
    suspend fun getAllExercisesSync() = dao.getAllExercisesSync()

    // Sets
    suspend fun insertSet(s: ExerciseSet): Long = dao.insertSet(s)
    suspend fun updateSet(s: ExerciseSet) = dao.updateSet(s)
    suspend fun deleteSet(s: ExerciseSet) = dao.deleteSet(s)
    fun getSetsForExercise(id: Long) = dao.getSetsForExercise(id)
    suspend fun getSetsForExerciseSync(id: Long) = dao.getSetsForExerciseSync(id)
    suspend fun deleteAllSetsForExercise(id: Long) = dao.deleteAllSetsForExercise(id)
    suspend fun getAllSetsSync() = dao.getAllSetsSync()

    // Progressive overload
    suspend fun getBestLastSet(name: String, before: String) = dao.getBestLastSet(name, before)

    // Exercise history
    fun getExerciseHistory(name: String)    = dao.getExerciseHistory(name)
    fun getExerciseSetHistory(name: String) = dao.getExerciseSetHistory(name)
    fun getAllUsedExerciseNames() = dao.getAllUsedExerciseNames()

    // Cardio
    suspend fun insertCardio(s: CardioSession): Long = dao.insertCardio(s)
    suspend fun deleteCardio(s: CardioSession) = dao.deleteCardio(s)
    fun getAllCardioSessions() = dao.getAllCardioSessions()
    fun getCardioForDate(date: String) = dao.getCardioForDate(date)
    fun getAllCardioDates() = dao.getAllCardioDates()
    fun getCardioInRange(s: String, e: String) = dao.getCardioInRange(s, e)
    fun getTotalDistanceInRange(s: String, e: String) = dao.getTotalDistanceInRange(s, e)
    suspend fun getAllCardioSync() = dao.getAllCardioSessionsSync()

    // Bodyweight
    suspend fun insertBodyweight(e: BodyweightEntry) = dao.insertBodyweight(e)
    suspend fun deleteBodyweight(e: BodyweightEntry) = dao.deleteBodyweight(e)
    fun getAllBodyweightEntries() = dao.getAllBodyweightEntries()
    suspend fun getAllBodyweightSync() = dao.getAllBodyweightSync()

    // Program progress
    suspend fun upsertProgramProgress(p: ProgramProgress) = dao.upsertProgramProgress(p)
    suspend fun deleteProgramProgress(p: ProgramProgress) = dao.deleteProgramProgress(p)
    suspend fun getProgramProgress(id: String) = dao.getProgramProgress(id)
    fun getAllProgramProgress() = dao.getAllProgramProgress()

    // Templates
    suspend fun insertTemplate(t: WorkoutTemplate): Long = dao.insertTemplate(t)
    suspend fun updateTemplate(t: WorkoutTemplate) = dao.updateTemplate(t)
    suspend fun deleteTemplate(t: WorkoutTemplate) = dao.deleteTemplate(t)
    fun getAllTemplates() = dao.getAllTemplates()
    suspend fun getTemplateById(id: Long) = dao.getTemplateById(id)
    suspend fun insertTemplateExercise(e: TemplateExercise): Long = dao.insertTemplateExercise(e)
    suspend fun deleteTemplateExercise(e: TemplateExercise) = dao.deleteTemplateExercise(e)
    fun getTemplateExercises(id: Long) = dao.getTemplateExercises(id)
    suspend fun getTemplateExercisesSync(id: Long) = dao.getTemplateExercisesSync(id)
    suspend fun deleteAllTemplateExercises(id: Long) = dao.deleteAllTemplateExercises(id)

    // Custom Programs
    suspend fun insertCustomProgram(p: CustomProgram): Long = dao.insertCustomProgram(p)
    suspend fun updateCustomProgram(p: CustomProgram) = dao.updateCustomProgram(p)
    suspend fun deleteCustomProgram(p: CustomProgram) = dao.deleteCustomProgram(p)
    fun getAllCustomPrograms() = dao.getAllCustomPrograms()
    suspend fun insertCustomDay(d: CustomProgramDay): Long = dao.insertCustomDay(d)
    suspend fun updateCustomDay(d: CustomProgramDay) = dao.updateCustomDay(d)
    suspend fun deleteCustomDay(d: CustomProgramDay) = dao.deleteCustomDay(d)
    fun getCustomDays(id: Long) = dao.getCustomDays(id)
    suspend fun getCustomDaysSync(id: Long) = dao.getCustomDaysSync(id)
    suspend fun deleteAllCustomDays(id: Long) = dao.deleteAllCustomDays(id)
    suspend fun insertCustomExercise(e: CustomProgramExercise): Long = dao.insertCustomExercise(e)
    suspend fun deleteCustomExercise(e: CustomProgramExercise) = dao.deleteCustomExercise(e)
    fun getCustomExercises(id: Long) = dao.getCustomExercises(id)
    suspend fun getCustomExercisesSync(id: Long) = dao.getCustomExercisesSync(id)
    suspend fun deleteAllCustomExercises(id: Long) = dao.deleteAllCustomExercises(id)

    // Stats
    fun getTotalWorkouts() = dao.getTotalWorkouts()
    fun getTopExercises() = dao.getTopExercises()
    fun getTotalVolumeLifted() = dao.getTotalVolumeLifted()
    fun getTotalSets() = dao.getTotalSets()
    fun getPersonalRecords() = dao.getPersonalRecords()
    suspend fun getPersonalRecordsSync() = dao.getPersonalRecordsSync()
    fun getWorkoutsCountInRange(s: String, e: String) = dao.getWorkoutsCountInRange(s, e)
    fun getVolumeInRange(s: String, e: String) = dao.getVolumeInRange(s, e)
    fun getSetsCountInRange(s: String, e: String) = dao.getSetsCountInRange(s, e)
    fun getVolumeOverTime() = dao.getVolumeOverTime()
    fun getAllExerciseSetCounts() = dao.getAllExerciseSetCounts()
}
