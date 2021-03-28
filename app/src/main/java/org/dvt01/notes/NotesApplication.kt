package org.dvt01.notes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

class NotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotesRepository.initialize(this)

        val settingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val darkModeIsOn = settingsSharedPreferences.getBoolean(getString(R.string.dark_mode_key), true)

        if (darkModeIsOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}