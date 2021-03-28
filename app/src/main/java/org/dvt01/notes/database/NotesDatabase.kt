package org.dvt01.notes.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.dvt01.notes.model.Note

@Database(entities = [ Note::class ], version = 1)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun notesDao(): NotesDao
}