package com.example.fitnesstracker

import android.app.Application

class FitnessTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Restore the saved Light/Dark choice before any screen is created.
        ThemeManager.applySavedTheme(this)
    }
}
