package com.digital.construction.notes

import android.app.Application
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

        NotesPreferences.get().applyDarkMode()
    }
}