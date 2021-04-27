package org.supersoniclegend.notes.database

import androidx.lifecycle.LiveData
import androidx.room.*
import org.supersoniclegend.notes.model.Note

@Dao
interface NotesDao {

    @Insert
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM note")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM note WHERE name=(:noteName)")
    fun getNote(noteName: String): LiveData<Note>
}