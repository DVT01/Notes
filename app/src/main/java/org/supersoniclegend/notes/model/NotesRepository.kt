package org.supersoniclegend.notes.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import org.supersoniclegend.notes.database.NotesDatabase

private const val DATABASE_NAME = "notes-database"

class NotesRepository private constructor(context: Context) {

    private val database = Room.databaseBuilder(
        context,
        NotesDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val notesDao = database.notesDao()

    suspend fun insertNote(note: Note) {
        notesDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        notesDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        notesDao.deleteNote(note)
    }

    fun getAllNotes(): LiveData<List<Note>> {
        return notesDao.getAllNotes()
    }

    fun getNote(noteName: String): LiveData<Note> {
        return notesDao.getNote(noteName)
    }

    companion object {

        private var INSTANCE: NotesRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = NotesRepository(context)
            }
        }

        fun get(): NotesRepository {
            return INSTANCE
                ?: throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}