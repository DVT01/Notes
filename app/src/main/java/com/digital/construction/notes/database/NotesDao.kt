package com.digital.construction.notes.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.digital.construction.notes.model.Note

@Dao
interface NotesDao {

    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM note")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE id=(:id)")
    fun getNote(id: Long): Flow<Note>
}