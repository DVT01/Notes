package com.digital.construction.notes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.digital.construction.notes.fragments.DARK_MODE_KEY
import com.digital.construction.notes.model.NotesRepository
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
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