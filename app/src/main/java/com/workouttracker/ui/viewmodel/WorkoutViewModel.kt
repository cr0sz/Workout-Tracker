package com.workouttracker.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.AppDatabase
import com.workouttracker.data.db.ExerciseCount
import com.workouttracker.data.db.VolumeEntry
import com.workouttracker.data.model.*
import com.workouttracker.data.repository.WorkoutRepository
import com.workouttracker.ui.util.AppErrorBus
import com.workouttracker.ui.util.safeCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repo  = WorkoutRepository(AppDatabase.getDatabase(application).workoutDao())
    private val prefs = application.getSharedPreferences("wt_prefs", Context.MODE_PRIVATE)

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(dateFormatter)
    val weekEnd   = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).format(dateFormatter)

    // ── Preferences ───────────────────────────────────────────────────────────
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    fun toggleTheme() {
        val v = !_isDarkTheme.value
        _isDarkTheme.value = v
        prefs.edit().putBoolean("dark_theme", v).apply()
    }

    private val _useLbs = MutableStateFlow(prefs.getBoolean("use_lbs", false))
    val useLbs: StateFlow<Boolean> = _useLbs.asStateFlow()
    fun toggleUnit() {
        val v = !_useLbs.value
        _useLbs.value = v
        prefs.edit().putBoolean("use_lbs", v).apply()
    }

    private val supportedLanguages = setOf("en", "tr", "de", "es", "fr")

    private val _language = MutableStateFlow(run {
        // 1. Explicit user choice saved in prefs — always honour it
        val saved = prefs.getString("language", null)
        if (saved != null) return@run saved

        // 2. Fresh install — detect from the device locale and persist it
        val deviceLang = Locale.getDefault().language
        val detected = if (deviceLang in supportedLanguages) deviceLang else "en"
        prefs.edit().putString("language", detected).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(detected))
        detected
    })
    val language: StateFlow<String> = _language.asStateFlow()

    fun setLanguage(langCode: String) {
        _language.value = langCode
        prefs.edit().putString("language", langCode).apply()

        // Apply the locale change through AppCompat
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)

        // Also update the JVM default locale to ensure immediate consistency in formatting
        Locale.setDefault(Locale(langCode))
    }

    // ── Timer Settings ──
    private val _timerEnabled = MutableStateFlow(prefs.getBoolean("timer_enabled", true))
    val timerEnabled: StateFlow<Boolean> = _timerEnabled.asStateFlow()
    fun setTimerEnabled(enabled: Boolean) {
        _timerEnabled.value = enabled
        prefs.edit().putBoolean("timer_enabled", enabled).apply()
    }

    private val _defaultRestSeconds = MutableStateFlow(prefs.getInt("default_rest_seconds", 90))
    val defaultRestSeconds: StateFlow<Int> = _defaultRestSeconds.asStateFlow()
    fun setDefaultRestSeconds(seconds: Int) {
        _defaultRestSeconds.value = seconds
        prefs.edit().putInt("default_rest_seconds", seconds).apply()
    }

    // ── Timer State ──
    private val _activeTimerSeconds = MutableStateFlow<Int?>(null)
    val activeTimerSeconds: StateFlow<Int?> = _activeTimerSeconds.asStateFlow()
    private var timerJob: Job? = null

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _activeTimerSeconds.value = seconds
        timerJob = viewModelScope.launch {
            while ((_activeTimerSeconds.value ?: 0) > 0) {
                delay(1000)
                _activeTimerSeconds.value = (_activeTimerSeconds.value ?: 0) - 1
            }
            _activeTimerSeconds.value = null
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _activeTimerSeconds.value = null
    }

    // ── Active Workout State ──
    private val _activeWorkoutStartTime = MutableStateFlow<Instant?>(null)
    val activeWorkoutStartTime: StateFlow<Instant?> = _activeWorkoutStartTime.asStateFlow()
    
    private val _workoutDurationFormatted = MutableStateFlow("00:00")
    val workoutDurationFormatted: StateFlow<String> = _workoutDurationFormatted.asStateFlow()
    
    private var durationJob: Job? = null

    fun startWorkout() {
        val startTime = Instant.now()
        _activeWorkoutStartTime.value = startTime
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (true) {
                val duration = Duration.between(startTime, Instant.now())
                val hours = duration.toHours()
                val minutes = duration.toMinutes() % 60
                val seconds = duration.seconds % 60
                _workoutDurationFormatted.value = if (hours > 0) {
                    String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
                }
                delay(1000)
            }
        }
    }

    fun endWorkout() {
        _activeWorkoutStartTime.value = null
        durationJob?.cancel()
        _workoutDurationFormatted.value = "00:00"
    }

    // ── Flows ─────────────────────────────────────────────────────────────────
    val allWorkoutDates  = repo.getAllWorkoutDates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allWorkouts      = repo.getAllWorkouts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allCardioSessions= repo.getAllCardioSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allCardioDates   = repo.getAllCardioDates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val weeklyCardio     = repo.getCardioInRange(weekStart, weekEnd).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val weeklyCardioDistance = repo.getTotalDistanceInRange(weekStart, weekEnd).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val allBodyweightEntries = repo.getAllBodyweightEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allProgramProgress   = repo.getAllProgramProgress().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allTemplates         = repo.getAllTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allCustomPrograms    = repo.getAllCustomPrograms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalWorkouts    = repo.getTotalWorkouts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val topExercises     = repo.getTopExercises().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalVolume      = repo.getTotalVolumeLifted().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val totalSets        = repo.getTotalSets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val personalRecords  = repo.getPersonalRecords().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val weeklyWorkouts   = repo.getWorkoutsCountInRange(weekStart, weekEnd).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val weeklyVolume     = repo.getVolumeInRange(weekStart, weekEnd).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)
    val weeklySets       = repo.getSetsCountInRange(weekStart, weekEnd).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val allUsedExerciseNames = repo.getAllUsedExerciseNames().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val volumeOverTime   = repo.getVolumeOverTime().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val muscleDistribution = repo.getAllExerciseSetCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Per-date flows ────────────────────────────────────────────────────────
    fun getExercisesForDate(date: String) = repo.getExercisesForWorkout(date)
    fun getSetsForExercise(id: Long)      = repo.getSetsForExercise(id)
    suspend fun getSetsSync(id: Long)     = repo.getSetsForExerciseSync(id)
    fun getCardioForDate(date: String)    = repo.getCardioForDate(date)
    fun getExerciseHistory(name: String)    = repo.getExerciseHistory(name)
    fun getExerciseSetHistory(name: String) = repo.getExerciseSetHistory(name)
    suspend fun getExerciseHistorySync(name: String) =
        safeCall { repo.getAllExercisesSync()
            .filter { it.exerciseName == name }
            .let { exercises ->
                exercises.mapNotNull { ex ->
                    val sets = repo.getSetsForExerciseSync(ex.id).filter { !it.isBodyweight }
                    if (sets.isEmpty()) null
                    else com.workouttracker.data.model.ExerciseHistoryEntry(
                        workoutDate = ex.workoutDate,
                        maxWeight   = sets.maxOf { it.weight },
                        totalSets   = sets.size,
                        totalReps   = sets.sumOf { it.reps }
                    )
                }.sortedBy { it.workoutDate }
            }
        } ?: emptyList()
    fun getTemplateExercises(id: Long)    = repo.getTemplateExercises(id)
    fun getCustomDays(id: Long)           = repo.getCustomDays(id)
    fun getCustomExercises(id: Long)      = repo.getCustomExercises(id)
    fun getWorkoutNotesFlow(date: String) = repo.getAllWorkouts()
        .map { it.firstOrNull { w -> w.date == date }?.notes ?: "" }

    // ── Workout actions ───────────────────────────────────────────────────────
    fun updateWorkoutNotes(date: String, notes: String) = viewModelScope.launch {
        safeCall("Couldn't save notes. Please try again.") {
            val e = repo.getWorkoutByDate(date)
            if (e != null) repo.updateWorkout(e.copy(notes = notes))
            else repo.insertWorkout(Workout(date = date, notes = notes))
        }
    }

    fun addExerciseToWorkout(date: String, exerciseName: String) = viewModelScope.launch {
        safeCall("Couldn't add exercise. Please try again.") {
            ensureWorkoutExists(date)
            val count = repo.getExercisesForWorkoutSync(date).size
            repo.insertExercise(WorkoutExercise(
                workoutDate  = date,
                exerciseName = exerciseName,
                orderIndex   = count
            ))
        }
    }

    fun deleteExercise(exercise: WorkoutExercise) = viewModelScope.launch {
        safeCall("Couldn't delete exercise.") {
            repo.deleteAllSetsForExercise(exercise.id)
            repo.deleteExercise(exercise)
        }
    }

    fun addSet(exerciseId: Long, reps: Int, weightKg: Float, isBodyweight: Boolean) = viewModelScope.launch {
        safeCall("Couldn't save set. Please try again.") {
            val count = repo.getSetsForExerciseSync(exerciseId).size
            repo.insertSet(ExerciseSet(
                exerciseId   = exerciseId,
                setNumber    = count + 1,
                reps         = reps,
                weight       = weightKg,
                isBodyweight = isBodyweight
            ))
            if (_timerEnabled.value) {
                startTimer(_defaultRestSeconds.value)
            }
        }
    }

    fun deleteSet(set: ExerciseSet) = viewModelScope.launch {
        safeCall("Couldn't delete set.") { repo.deleteSet(set) }
    }

    // ── Progressive overload ──────────────────────────────────────────────────
    suspend fun getBestLastSet(name: String, before: String) =
        safeCall { repo.getBestLastSet(name, before) }

    // ── Cardio ────────────────────────────────────────────────────────────────
    fun addCardioSession(s: CardioSession) = viewModelScope.launch {
        safeCall("Couldn't save cardio session.") { repo.insertCardio(s) }
    }
    fun deleteCardioSession(s: CardioSession) = viewModelScope.launch {
        safeCall("Couldn't delete cardio session.") { repo.deleteCardio(s) }
    }

    // ── Bodyweight ────────────────────────────────────────────────────────────
    fun upsertBodyweight(date: String, weightKg: Float) = viewModelScope.launch {
        safeCall("Couldn't save weight entry.") {
            repo.insertBodyweight(BodyweightEntry(date, weightKg))
        }
    }
    fun deleteBodyweight(entry: BodyweightEntry) = viewModelScope.launch {
        safeCall("Couldn't delete weight entry.") { repo.deleteBodyweight(entry) }
    }

    // ── Program progress ──────────────────────────────────────────────────────
    fun startOrUpdateProgram(
        programId: String, week: Int, day: Int,
        startDate: String = "", lastDate: String = ""
    ) = viewModelScope.launch {
        safeCall {
            val ex = repo.getProgramProgress(programId)
            repo.upsertProgramProgress(ProgramProgress(
                programId, week, day,
                startDate.ifBlank { ex?.startDate ?: todayDateString() },
                lastDate.ifBlank { todayDateString() }
            ))
        }
    }

    fun stopProgram(id: String) = viewModelScope.launch {
        safeCall { repo.getProgramProgress(id)?.let { repo.deleteProgramProgress(it) } }
    }

    suspend fun getProgramProgress(id: String) = safeCall { repo.getProgramProgress(id) }

    // ── Templates ─────────────────────────────────────────────────────────────
    suspend fun saveWorkoutAsTemplate(date: String, name: String): Long? {
        return safeCall("Couldn't save template. Please try again.") {
            val templateId = repo.insertTemplate(
                WorkoutTemplate(name = name, createdDate = date, lastUsedDate = date)
            )
            val exercises = repo.getExercisesForWorkoutSync(date)
            exercises.forEachIndexed { i, ex ->
                val sets       = repo.getSetsForExerciseSync(ex.id)
                val defaultReps = sets.firstOrNull()?.reps?.toString() ?: "8-12"
                repo.insertTemplateExercise(TemplateExercise(
                    templateId  = templateId,
                    exerciseName= ex.exerciseName,
                    orderIndex  = i,
                    defaultSets = sets.size.coerceAtLeast(1),
                    defaultReps = defaultReps
                ))
            }
            templateId
        }
    }

    fun loadTemplateToWorkout(date: String, templateId: Long) = viewModelScope.launch {
        safeCall("Couldn't load template. Please try again.") {
            ensureWorkoutExists(date)
            val exercises = repo.getTemplateExercisesSync(templateId)
            if (exercises.isEmpty()) {
                AppErrorBus.post("This template has no exercises.")
                return@safeCall
            }
            exercises.forEach { ex -> addExerciseToWorkout(date, ex.exerciseName) }
            repo.getTemplateById(templateId)?.let {
                repo.updateTemplate(it.copy(lastUsedDate = todayDateString()))
            }
        }
    }

    fun deleteTemplate(template: WorkoutTemplate) = viewModelScope.launch {
        safeCall("Couldn't delete template.") {
            repo.deleteAllTemplateExercises(template.id)
            repo.deleteTemplate(template)
        }
    }

    // ── Custom Programs ───────────────────────────────────────────────────────
    suspend fun createCustomProgram(
        name: String, tagline: String, daysPerWeek: Int,
        durationWeeks: Int, difficulty: String
    ): Long? = safeCall("Couldn't create program. Please try again.") {
        repo.insertCustomProgram(CustomProgram(
            name = name, tagline = tagline, daysPerWeek = daysPerWeek,
            durationWeeks = durationWeeks, difficulty = difficulty,
            createdDate = todayDateString()
        ))
    }

    suspend fun addCustomDay(
        programId: Long, dayNumber: Int, name: String,
        focus: String, isRestDay: Boolean
    ): Long? = safeCall("Couldn't add day.") {
        repo.insertCustomDay(CustomProgramDay(
            programId = programId, dayNumber = dayNumber,
            name = name, focus = focus, isRestDay = isRestDay
        ))
    }

    suspend fun addCustomExercise(
        dayId: Long, name: String, sets: Int,
        reps: String, notes: String, order: Int
    ): Long? = safeCall("Couldn't add exercise.") {
        repo.insertCustomExercise(CustomProgramExercise(
            dayId = dayId, exerciseName = name, sets = sets,
            reps = reps, notes = notes, orderIndex = order
        ))
    }

    suspend fun deleteCustomExercise(e: CustomProgramExercise) =
        safeCall { repo.deleteCustomExercise(e) }

    fun deleteCustomProgram(program: CustomProgram) = viewModelScope.launch {
        safeCall("Couldn't delete program.") {
            repo.deleteAllCustomDays(program.id)
            repo.deleteCustomProgram(program)
        }
    }

    fun deleteCustomDay(day: CustomProgramDay) = viewModelScope.launch {
        safeCall("Couldn't delete day.") {
            repo.deleteAllCustomExercises(day.id)
            repo.deleteCustomDay(day)
        }
    }

    fun loadCustomDayToWorkout(date: String, dayId: Long, programId: Long) = viewModelScope.launch {
        safeCall("Couldn't load day. Please try again.") {
            ensureWorkoutExists(date)
            val exercises = repo.getCustomExercisesSync(dayId)
            if (exercises.isEmpty()) {
                AppErrorBus.post("This day has no exercises yet.")
                return@safeCall
            }
            exercises.forEach { ex -> addExerciseToWorkout(date, ex.exerciseName) }
            startOrUpdateProgram("custom_$programId", 1, 1, lastDate = date)
        }
    }

    // ── CSV export ────────────────────────────────────────────────────────────
    suspend fun generateCsv(): String? = safeCall("Couldn't generate export. Please try again.") {
        val sb = StringBuilder()
        sb.appendLine("=== WORKOUTS ===\nDate,Notes")
        repo.getAllWorkoutsSync().forEach { sb.appendLine("${it.date},\"${it.notes}\"") }
        sb.appendLine("\n=== STRENGTH SESSIONS ===\nDate,Exercise,Set,Reps,Weight_kg,Bodyweight")
        val exMap = repo.getAllExercisesSync().associateBy { it.id }
        repo.getAllSetsSync().forEach { s ->
            val ex = exMap[s.exerciseId] ?: return@forEach
            sb.appendLine("${ex.workoutDate},\"${ex.exerciseName}\",${s.setNumber},${s.reps},${s.weight},${s.isBodyweight}")
        }
        sb.appendLine("\n=== CARDIO SESSIONS ===\nDate,Type,Distance_km,Duration_min,Weight_kg,Calories,Notes")
        repo.getAllCardioSync().forEach { c ->
            sb.appendLine("${c.date},${c.type},${c.distanceKm ?: ""},${c.durationMinutes ?: ""},${c.weightKg ?: ""},${c.calories ?: ""},\"${c.notes}\"")
        }
        sb.appendLine("\n=== BODYWEIGHT LOG ===\nDate,Weight_kg")
        repo.getAllBodyweightSync().forEach { sb.appendLine("${it.date},${it.weightKg}") }
        sb.toString()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private suspend fun ensureWorkoutExists(date: String) {
        if (repo.getWorkoutByDate(date) == null) {
            repo.insertWorkout(Workout(date = date))
        }
    }

    fun todayDateString(): String = LocalDate.now().format(dateFormatter)
}
