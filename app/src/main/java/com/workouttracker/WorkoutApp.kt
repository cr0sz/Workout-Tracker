package com.workouttracker

import android.app.Application
import com.google.firebase.FirebaseApp

class WorkoutApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initializing FirebaseApp. 
        // App Check (Play Integrity) removed because the app is not on Play Store.
        FirebaseApp.initializeApp(this)
    }
}
