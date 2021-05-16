package org.coquicoding.notes.fragments

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.coquicoding.notes.model.Note
import org.coquicoding.notes.model.NotesRepository

private const val TAG = "NotesListViewModel"

class NotesListViewModel : ViewModel() {

    private val notesRepository = NotesRepository.get()
    val notesLiveData = notesRepository.getAllNotes().asLiveData()

    fun insertNote(note: Note) {
        Log.d(TAG, "insertNote: $note")
        viewModelScope.launch {
            notesRepository.insertNote(note)
        }
    }

    fun deleteNote(note: Note) {
        Log.d(TAG, "deleteNote: $note")
        viewModelScope.launch {
            notesRepository.deleteNote(note)
        }
    }
}