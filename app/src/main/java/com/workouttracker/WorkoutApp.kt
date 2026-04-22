package com.workouttracker

import android.app.Application
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration

class WorkoutApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initializing FirebaseApp.
        // App Check (Play Integrity) removed because the app is not on Play Store.
        FirebaseApp.initializeApp(this)

        // OSMDroid — must be initialized before any MapView is created
        Configuration.getInstance().apply {
            load(this@WorkoutApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
    }
}
