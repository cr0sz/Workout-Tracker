package com.workouttracker.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.BuildConfig
import com.workouttracker.data.api.ClaudeApiClient
import com.workouttracker.data.db.AppDatabase
import com.workouttracker.data.model.AiChatMessage
import com.workouttracker.data.model.UserProfile
import com.workouttracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AiCoachViewModel(application: Application) : AndroidViewModel(application) {

    private val db          = AppDatabase.getDatabase(application)
    private val aiDao       = db.aiDao()
    private val workoutRepo = WorkoutRepository(db.workoutDao())
    private val prefs       = application.getSharedPreferences("wt_prefs", Context.MODE_PRIVATE)

    // ── Public state ──────────────────────────────────────────────────────────

    val messages: StateFlow<List<AiChatMessage>> = aiDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userProfile: StateFlow<UserProfile?> = aiDao.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Profile ───────────────────────────────────────────────────────────────

    fun saveProfile(profile: UserProfile) = viewModelScope.launch {
        aiDao.upsertProfile(profile)
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    fun clearError() { _error.value = null }

    fun clearConversation() = viewModelScope.launch {
        aiDao.clearAllMessages()
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        viewModelScope.launch {
            aiDao.insertMessage(AiChatMessage(role = "user", content = userText.trim()))
            _isLoading.value = true
            _error.value = null

            runCatching {
                val context      = buildContext()
                val langCode     = prefs.getString("language", "en") ?: "en"
                val langName     = mapOf(
                    "en" to "English", "tr" to "Turkish", "de" to "German",
                    "es" to "Spanish", "fr" to "French"
                )[langCode] ?: "English"
                val langPrefix   = "CRITICAL INSTRUCTION: You MUST respond exclusively in $langName ($langCode). Every single word of your reply must be in $langName. Do not use any other language.\n\n"
                val systemPrompt = langPrefix + COACHING_SYSTEM_PROMPT + "\n\n---\n\n## Current User Data\n\n$context"

                val history = aiDao.getAllMessagesSync()
                    .takeLast(20)
                    .map { it.role to it.content }

                ClaudeApiClient.sendMessage(
                    workerUrl    = BuildConfig.COACH_WORKER_URL,
                    appSecret    = BuildConfig.COACH_APP_SECRET,
                    systemPrompt = systemPrompt,
                    messages     = history
                ).getOrThrow()
            }.onSuccess { reply ->
                aiDao.insertMessage(AiChatMessage(role = "assistant", content = reply))
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to get a response"
            }

            _isLoading.value = false
        }
    }

    // ── Context builder ───────────────────────────────────────────────────────

    private suspend fun buildContext(): String {
        val profile      = aiDao.getUserProfile()
        val workouts     = workoutRepo.getAllWorkoutsSync()
        val allExercises = workoutRepo.getAllExercisesSync()
        val allSets      = workoutRepo.getAllSetsSync()
        val bodyweight   = workoutRepo.getAllBodyweightSync().takeLast(14)
        val cardio       = workoutRepo.getAllCardioSync().takeLast(20)
        val prs          = workoutRepo.getPersonalRecordsSync()
        val useLbs       = prefs.getBoolean("use_lbs", false)
        val unit         = if (useLbs) "lbs" else "kg"
        val kgToLbs      = 2.20462f

        val exercisesByDate = allExercises.groupBy { it.workoutDate }
        val setsByExercise  = allSets.groupBy { it.exerciseId }

        val sb = StringBuilder()

        if (profile != null) {
            sb.appendLine("### User Profile")
            if (profile.name.isNotBlank())            sb.appendLine("Name: ${profile.name}")
            if (profile.age > 0)                      sb.appendLine("Age: ${profile.age}")
            sb.appendLine("Experience level: ${profile.experience}")
            if (profile.primaryGoal.isNotBlank())     sb.appendLine("Primary goal: ${profile.primaryGoal}")
            sb.appendLine("Training days/week: ${profile.trainingDaysPerWeek}")
            if (profile.preferredSplit.isNotBlank())  sb.appendLine("Preferred split: ${profile.preferredSplit}")
            if (profile.injuries.isNotBlank())        sb.appendLine("Injuries/limitations: ${profile.injuries}")
            if (profile.additionalNotes.isNotBlank()) sb.appendLine("Extra notes: ${profile.additionalNotes}")
            sb.appendLine()
        }

        if (prs.isNotEmpty()) {
            sb.appendLine("### All-Time Personal Records")
            prs.take(25).forEach { pr ->
                val w = if (useLbs) pr.maxWeight * kgToLbs else pr.maxWeight
                sb.appendLine("${pr.exerciseName}: ${String.format("%.1f", w)}$unit (${pr.date})")
            }
            sb.appendLine()
        }

        val recentWorkouts = workouts.takeLast(20)
        if (recentWorkouts.isNotEmpty()) {
            sb.appendLine("### Recent Workouts (newest first)")
            recentWorkouts.reversed().forEach { workout ->
                val exercises = exercisesByDate[workout.date] ?: emptyList()
                val weighted  = exercises.filter { ex ->
                    (setsByExercise[ex.id] ?: emptyList()).any { !it.isBodyweight }
                }
                if (weighted.isEmpty()) return@forEach
                sb.appendLine("**${workout.date}**${if (workout.notes.isNotBlank()) " — ${workout.notes}" else ""}")
                weighted.forEach { ex ->
                    val sets    = (setsByExercise[ex.id] ?: emptyList()).filter { !it.isBodyweight }
                    val setsStr = sets.joinToString(", ") {
                        val w = if (useLbs) it.weight * kgToLbs else it.weight
                        "${it.reps}×${String.format("%.1f", w)}$unit"
                    }
                    sb.appendLine("  ${ex.exerciseName}: $setsStr")
                }
            }
            sb.appendLine()
        }

        if (bodyweight.isNotEmpty()) {
            sb.appendLine("### Bodyweight Log (recent)")
            bodyweight.reversed().forEach { bw ->
                val w = if (useLbs) bw.weightKg * kgToLbs else bw.weightKg
                sb.appendLine("${bw.date}: ${String.format("%.1f", w)}$unit")
            }
            sb.appendLine()
        }

        if (cardio.isNotEmpty()) {
            sb.appendLine("### Recent Cardio Sessions")
            cardio.reversed().take(12).forEach { c ->
                val parts = mutableListOf<String>(c.type)
                c.distanceKm?.let      { parts.add("${String.format("%.1f", it)}km") }
                c.durationMinutes?.let { parts.add("${it}min") }
                c.calories?.let        { parts.add("${it}cal") }
                sb.appendLine("${c.date}: ${parts.joinToString(", ")}")
            }
        }

        return sb.toString()
    }
}

// ── Science-backed system prompt ──────────────────────────────────────────────

private val COACHING_SYSTEM_PROMPT = """
You are an expert AI fitness and strength coach with deep knowledge grounded in exercise science research. You have real-time access to this user's complete workout history, personal records, bodyweight trend, and profile (provided below). Use that data actively — you are not a generic chatbot, you are THEIR coach.

## Core Scientific Framework

### Progressive Overload
The single most important principle. Every session should be marginally harder than the last.
- Methods: add load, add reps, add sets, reduce rest, improve range of motion, increase frequency
- Double progression: reach the top of a rep range across ALL sets before adding weight
- If a lift hasn't progressed in 2+ weeks: diagnose and intervene immediately
- Track it — if you're not measuring it, you're not managing it

### Volume Landmarks (Israetel / Renaissance Periodization)
MEV (Minimum Effective Volume) → MAV (Maximum Adaptive Volume) → MRV (Maximum Recoverable Volume)
Weekly direct sets per muscle group:
- Chest: MEV 8 / MAV 12–20 / MRV 22+
- Back: MEV 10 / MAV 14–22 / MRV 25+
- Shoulders: MEV 8 / MAV 16–22 / MRV 26+
- Biceps: MEV 8 / MAV 14–20 / MRV 26+
- Triceps: MEV 6 / MAV 10–14 / MRV 18+
- Quads: MEV 8 / MAV 12–18 / MRV 20+
- Hamstrings: MEV 6 / MAV 10–16 / MRV 20+
- Glutes: MEV 0 / MAV 4–12 / MRV 16+
- Calves: MEV 8 / MAV 12–16 / MRV 20+
Most natural lifters are chronically under their MEV. Check their actual set counts.

### RPE / RIR
- RPE 10 / RIR 0: Absolute failure — avoid on most exercises
- RPE 9 / RIR 1: One rep in reserve — appropriate for top sets
- RPE 8 / RIR 2: Good for volume work
- RPE 7 / RIR 3: Technique work, warm-ups, deloads
- Training to failure every set → kills recovery, elevates injury risk
- Most sets should be RPE 7–9

### Periodization
- Linear (beginner): Add weight/reps every session. Works 6–18 months.
- Undulating/DUP (intermediate): Rotate rep ranges across sessions (5×5 / 4×8 / 3×12 same week).
- Block (advanced): Accumulation → Intensification → Realization. 3–6 week blocks.
- Deload: Drop volume 40–50% every 4–8 weeks, or when performance drops/joints ache. Non-negotiable.

### Hypertrophy
- Rep ranges 5–30 all build muscle if taken close to failure. Sweet spot: 8–20 reps.
- 10–20 hard sets per muscle per week optimal for most.
- Minimum 2×/week frequency per muscle. 3× often better for intermediate/advanced.
- Full ROM > partial ROM. Compound movements first.

### Strength Development
- 1–5 reps at 85–100% 1RM for neural adaptations.
- Specificity: train the exact lift you want to improve.
- Peaking: drop volume 40% last 2–3 weeks, maintain intensity.

### Recovery
- Sleep 7–9h: #1 recovery tool. GH peaks in deep sleep. Cortisol spikes on <6h.
- 48–72h between sessions for the same muscle group.
- Protein: 1.6–2.2g/kg/day (0.72–1g/lb). Non-negotiable.
- Lean bulk: 200–400 kcal/day surplus. Cut: max 0.5–1% bodyweight/week.
- Creatine 3–5g/day: most evidence-backed supplement for strength and muscle.

### Cardio
- Zone 2 (can hold conversation): 2–3×/week 30–45min, minimal hypertrophy interference.
- HIIT: can interfere with leg hypertrophy. Max 2×/week.
- Separate cardio and lifting by 6+ hours when possible.

### Injury Prevention
- 2–3 progressive warm-up sets before working sets always.
- Pain ≠ progress. Joint pain = stop immediately.
- Anterior knee pain: control descent, strengthen VMO.
- Lower back: brace core, neutral spine.
- Shoulder: strengthen rotator cuff, reduce internal rotation dominance.

### Common Mistakes
- Ego lifting (too heavy, form breaks)
- Stagnation (same weight/reps for months) — most common
- Insufficient volume for stated goals
- Skipping deloads until forced
- Neglecting posterior chain
- Insufficient protein

## How to Coach
1. Always read their data before responding — reference their actual numbers
2. Be specific: "Add 2.5kg to your bench next session" not "try more weight"
3. Reference their data: "Your squat has been stuck at 80kg for 3 weeks — here's why"
4. Explain the why behind every recommendation
5. Be direct — if they're making a mistake, say so
6. Celebrate real progress
7. Ask clarifying questions when data is insufficient
8. Adapt to their level: beginner ≠ advanced advice
9. Short answers for simple questions, detailed for complex ones

## Style
- Direct, confident, evidence-based
- Like a knowledgeable training partner, not a textbook
- No generic platitudes — give actionable, specific advice
""".trimIndent()
