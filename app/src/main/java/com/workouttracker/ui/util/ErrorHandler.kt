package com.workouttracker.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide error bus. Any ViewModel can post an error here.
 * The root composable listens and shows a Snackbar.
 */
object AppErrorBus {
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun post(message: String) { _error.value = message }
    fun clear() { _error.value = null }
}

/**
 * Wraps a suspending DB/network call with error handling.
 * Posts to AppErrorBus on failure so the user sees a message.
 */
suspend fun <T> safeCall(
    errorMessage: String = "Something went wrong. Please try again.",
    block: suspend () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        val msg = when {
            e.message?.contains("UNIQUE constraint") == true ->
                "This entry already exists."
            e.message?.contains("no such table") == true ->
                "Database error. Please restart the app."
            e.message?.contains("disk") == true || e.message?.contains("space") == true ->
                "Not enough storage space on your device."
            e.message?.contains("network") == true || e.message?.contains("timeout") == true ->
                "Network error. Check your connection and try again."
            else -> errorMessage
        }
        AppErrorBus.post(msg)
        null
    }
}
