package org.obebeokeke.notes.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.obebeokeke.notes.model.Note

@Database(entities = [ Note::class ], version = 1)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun notesDao(): NotesDao
}