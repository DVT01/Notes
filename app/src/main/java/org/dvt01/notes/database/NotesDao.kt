package org.dvt01.notes.database

import androidx.lifecycle.LiveData
import androidx.room.*
import org.dvt01.notes.model.Note

@Dao
interface NotesDao {

    @Insert
    fun insertNote(note: Note)

    @Update
    fun updateNote(note: Note)

    @Delete
    fun deleteNote(note: Note)

    @Query("SELECT * FROM note")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM note WHERE name=(:noteName)")
    fun getNote(noteName: String): LiveData<Note>
}