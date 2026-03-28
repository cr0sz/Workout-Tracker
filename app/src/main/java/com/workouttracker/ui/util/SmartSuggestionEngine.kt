package com.workouttracker.ui.util

import com.workouttracker.data.model.ExerciseHistoryEntry
import com.workouttracker.data.model.LastSetInfo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ── Weight Suggestion ─────────────────────────────────────────────────────────

enum class SuggestionType {
    INCREASE,   // You're ready to go heavier
    SAME,       // Stick with this weight
    DELOAD,     // Been a while, take it slightly lighter
    FIRST_TIME  // No history yet
}

data class WeightSuggestion(
    val type: SuggestionType,
    val suggestedWeight: Float?,  // null for FIRST_TIME
    val suggestedReps: Int?,
    val reason: String,
    val emoji: String
)

/**
 * Analyses last session data and full history to suggest the best weight
 * for the next set of a given exercise.
 *
 * Rules:
 * 1. No history                → FIRST_TIME — no suggestion, just a tip
 * 2. Last session > 14 days    → DELOAD to 90% of last weight
 * 3. Last set hit target reps  → INCREASE by 2.5kg (small) or 5kg (compound)
 * 4. Last set failed reps      → SAME weight, fix form/reps first
 * 5. Consistent progress       → INCREASE with encouragement
 */
fun buildWeightSuggestion(
    lastSet: LastSetInfo?,
    history: List<ExerciseHistoryEntry>,
    exerciseName: String,
    useLbs: Boolean
): WeightSuggestion {

    // No history at all
    if (lastSet == null || lastSet.isBodyweight) {
        return WeightSuggestion(
            type            = SuggestionType.FIRST_TIME,
            suggestedWeight = null,
            suggestedReps   = null,
            reason          = "First time logging this exercise — start light and focus on form",
            emoji           = "💡"
        )
    }

    val daysSinceLastSession = run {
        val lastDate = runCatching { LocalDate.parse(lastSet.workoutDate) }.getOrNull()
        val today    = LocalDate.now()
        if (lastDate != null) ChronoUnit.DAYS.between(lastDate, today).toInt() else 0
    }

    val lastWeight = lastSet.weight
    val lastReps   = lastSet.reps

    // Deload after long break
    if (daysSinceLastSession > 14) {
        val deloadWeight = roundToNearest(lastWeight * 0.9f, useLbs)
        return WeightSuggestion(
            type            = SuggestionType.DELOAD,
            suggestedWeight = deloadWeight,
            suggestedReps   = lastReps,
            reason          = "It's been $daysSinceLastSession days — start at 90% to ease back in",
            emoji           = "🔄"
        )
    }

    // Determine if this is a compound (bigger jumps) or isolation (smaller jumps)
    val isCompound = COMPOUND_EXERCISES.any { exerciseName.contains(it, ignoreCase = true) }
    val increment  = if (useLbs) {
        if (isCompound) 5f else 2.5f
    } else {
        if (isCompound) 2.5f else 1.25f
    }

    // Check if last set hit the "top" of the rep range
    // If they did 8+ reps, they're ready to go up
    val hitTargetReps = lastReps >= 8

    // Check for consistent recent progress in history
    val recentHistory = history.takeLast(3)
    val isProgressing = recentHistory.size >= 2 &&
            recentHistory.last().maxWeight > recentHistory.first().maxWeight

    return if (hitTargetReps) {
        val newWeight = roundToNearest(lastWeight + increment, useLbs)
        WeightSuggestion(
            type            = SuggestionType.INCREASE,
            suggestedWeight = newWeight,
            suggestedReps   = 6,
            reason          = if (isProgressing)
                "You're on a roll! ${lastWeight.formatWeight(useLbs)} × $lastReps last time → try ${newWeight.formatWeight(useLbs)}"
            else
                "Great reps last time — add ${increment.formatWeight(useLbs)} and see how it feels",
            emoji           = "⬆️"
        )
    } else {
        WeightSuggestion(
            type            = SuggestionType.SAME,
            suggestedWeight = lastWeight,
            suggestedReps   = lastReps + 1,
            reason          = "Stick with ${lastWeight.formatWeight(useLbs)} and aim for ${lastReps + 1} reps this time",
            emoji           = "🎯"
        )
    }
}

// ── Workout Suggestion (shown on calendar) ────────────────────────────────────

data class WorkoutSuggestion(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val type: WorkoutSuggestionType
)

enum class WorkoutSuggestionType {
    STREAK, REST_REMINDER, MUSCLE_GROUP, GENERAL
}

fun buildWorkoutSuggestion(
    workoutDates: List<String>,
    today: LocalDate = LocalDate.now()
): WorkoutSuggestion? {
    if (workoutDates.isEmpty()) return null

    val parsedDates = workoutDates.mapNotNull {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }.sortedDescending()

    // Calculate current streak
    var streak = 0
    var checkDate = today
    for (date in parsedDates) {
        if (date == checkDate || date == checkDate.minusDays(1)) {
            streak++
            checkDate = date
        } else break
    }

    // Days since last workout
    val lastWorkout = parsedDates.firstOrNull()
    val daysSinceLast = lastWorkout?.let { ChronoUnit.DAYS.between(it, today).toInt() } ?: 99

    return when {
        // On a streak — motivate
        streak >= 3 -> WorkoutSuggestion(
            title    = "🔥 $streak day streak!",
            subtitle = "You're on fire — keep showing up",
            emoji    = "🔥",
            type     = WorkoutSuggestionType.STREAK
        )
        // Trained yesterday — gentle check in
        daysSinceLast == 1 -> WorkoutSuggestion(
            title    = "Ready for day ${streak + 1}?",
            subtitle = "You trained yesterday — how are you feeling today?",
            emoji    = "💪",
            type     = WorkoutSuggestionType.GENERAL
        )
        // 2 days rest
        daysSinceLast == 2 -> WorkoutSuggestion(
            title    = "2 days rest — muscles are recovered",
            subtitle = "Great time to get back in the gym",
            emoji    = "⚡",
            type     = WorkoutSuggestionType.GENERAL
        )
        // Long break
        daysSinceLast >= 7 -> WorkoutSuggestion(
            title    = "It's been $daysSinceLast days",
            subtitle = "Start with something light — your body will thank you",
            emoji    = "🌱",
            type     = WorkoutSuggestionType.REST_REMINDER
        )
        // Default — trained recently
        else -> WorkoutSuggestion(
            title    = "Last workout: $daysSinceLast day${if (daysSinceLast != 1) "s" else ""} ago",
            subtitle = "Tap today to log a new session",
            emoji    = "📅",
            type     = WorkoutSuggestionType.GENERAL
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val COMPOUND_EXERCISES = listOf(
    "Squat", "Deadlift", "Bench Press", "Overhead Press",
    "Barbell Row", "Romanian Deadlift", "Leg Press",
    "Hip Thrust", "Pull-up", "Chin-up"
)

private fun roundToNearest(weight: Float, useLbs: Boolean): Float {
    val increment = if (useLbs) 2.5f else 1.25f
    return Math.round(weight / increment) * increment
}

fun Float.formatWeight(useLbs: Boolean): String {
    val v = if (useLbs) this * WeightUnit.KG_TO_LBS else this
    return if (v == Math.floor(v.toDouble()).toFloat()) "${v.toInt()} ${WeightUnit.unitLabel(useLbs)}"
    else "${String.format("%.2f", v)} ${WeightUnit.unitLabel(useLbs)}"
}
