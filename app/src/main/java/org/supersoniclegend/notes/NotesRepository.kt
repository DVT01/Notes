package org.supersoniclegend.notes

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import org.supersoniclegend.notes.database.NotesDatabase
import org.supersoniclegend.notes.model.Note
import java.lang.IllegalStateException
import java.util.concurrent.Executors

private const val DATABASE_NAME = "notes-database"

class NotesRepository private constructor(context: Context) {

    private val database = Room.databaseBuilder(
        context,
        NotesDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val notesDao = database.notesDao()
    private val executor = Executors.newSingleThreadExecutor()

    fun insertNote(note: Note) {
        executor.execute {
            notesDao.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        executor.execute {
            notesDao.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        executor.execute {
            notesDao.deleteNote(note)
        }
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