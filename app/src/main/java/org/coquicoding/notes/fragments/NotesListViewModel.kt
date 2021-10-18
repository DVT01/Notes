package org.coquicoding.notes.fragments

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.coquicoding.notes.model.Note
import org.coquicoding.notes.model.NotesRepository

private const val TAG = "NotesListViewModel"

class NotesListViewModel : ViewModel() {

    private val notesRepository = NotesRepository.get()
    val notesLiveData = notesRepository.getAllNotes().asLiveData()

    fun insertNote(note: Note): LiveData<Long> {
        Log.i(TAG, "Adding note to DB (id: ${note.id})")

        val result = MutableLiveData<Long>()

        viewModelScope.launch {
            result.value = notesRepository.insertNote(note)
        }

        return result
    }

    fun deleteNote(note: Note) {
        Log.i(TAG, "Deleting note (id: ${note.id})")
        viewModelScope.launch {
            notesRepository.deleteNote(note)
        }
    }
}