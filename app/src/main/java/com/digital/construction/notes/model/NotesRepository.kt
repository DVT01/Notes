package com.digital.construction.notes.model

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import com.digital.construction.notes.database.NotesDatabase

private const val DATABASE_NAME = "notes-database"

class NotesRepository private constructor(context: Context) {

    private val database = Room.databaseBuilder(
        context,
        NotesDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val notesDao = database.notesDao()

    suspend fun insertNote(note: Note): Long {
        return notesDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        notesDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        notesDao.deleteNote(note)
    }

    fun getAllNotes(): Flow<List<Note>> {
        return notesDao.getAllNotes()
    }

    fun getNote(id: Long): Flow<Note> {
        return notesDao.getNote(id)
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