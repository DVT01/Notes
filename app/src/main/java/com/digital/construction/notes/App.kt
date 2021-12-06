package com.digital.construction.notes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.digital.construction.notes.database.NotesPreferences
import com.digital.construction.notes.model.NotesRepository
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        NotesRepository.initialize(this)
        NotesPreferences.initialize(this)

        val darkModeIsOn = NotesPreferences.get().darkModeIsOn.value

        if (darkModeIsOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}