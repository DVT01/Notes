package org.coquicoding.notes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import org.coquicoding.notes.fragments.DARK_MODE_KEY
import org.coquicoding.notes.model.NotesRepository

class App : Application() {

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