package org.dvt01.notes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import org.dvt01.notes.fragments.DARK_MODE_KEY

class NotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotesRepository.initialize(this)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val darkModeIsOn = sharedPreferences.getBoolean(DARK_MODE_KEY, false)

        if (darkModeIsOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}