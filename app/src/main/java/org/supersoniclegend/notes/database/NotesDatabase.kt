package org.supersoniclegend.notes.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.supersoniclegend.notes.model.Note

@Database(entities = [ Note::class ], version = 1)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun notesDao(): NotesDao
}