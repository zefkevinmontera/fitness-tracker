package com.example.fitnesstracker

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * Saves the user's Light/Dark choice in SharedPreferences and applies it with
 * AppCompatDelegate. Until the user taps the toggle, the app follows the system setting.
 */
object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_NIGHT_MODE = "night_mode"

    /** Call once at app start (see FitnessTrackerApp). */
    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(savedMode(context))
    }

    /** Flip between Light and Dark, save the choice, and apply it (the activity recreates itself). */
    fun toggle(activityContext: Context) {
        val newMode =
            if (isDark(activityContext)) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES

        activityContext.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_NIGHT_MODE, newMode)
            .apply()

        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    private fun savedMode(context: Context): Int =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /** Uses the ACTIVITY's configuration so it reflects the theme currently on screen. */
    private fun isDark(activityContext: Context): Boolean {
        val nightMask =
            activityContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMask == Configuration.UI_MODE_NIGHT_YES
    }
}
