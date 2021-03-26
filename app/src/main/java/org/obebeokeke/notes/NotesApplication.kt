package org.obebeokeke.notes

import android.app.Application

class NotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotesRepository.initialize(this)
    }
}