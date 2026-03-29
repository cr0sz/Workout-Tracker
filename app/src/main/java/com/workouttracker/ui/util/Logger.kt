package com.workouttracker.ui.util

import android.util.Log
import com.workouttracker.BuildConfig

/**
 * Custom Logger that only outputs in debug builds.
 * In release builds, these calls are effectively empty and
 * ProGuard will strip the underlying android.util.Log calls as well.
 */
object Logger {
    private const val TAG = "WorkoutTracker"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun i(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(TAG, message, throwable)
    }

    fun v(message: String) {
        if (BuildConfig.DEBUG) Log.v(TAG, message)
    }

    fun w(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }
}
