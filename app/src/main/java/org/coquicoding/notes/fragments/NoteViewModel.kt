package org.coquicoding.notes.fragments

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.coquicoding.notes.model.Note
import org.coquicoding.notes.model.NotesRepository

private const val TAG = "NoteViewModel"

class NoteViewModel : ViewModel() {

    private val notesRepository = NotesRepository.get()
    private val noteNameLiveData = MutableLiveData<Long>()

    val noteLiveData: LiveData<Note> =
        Transformations.switchMap(noteNameLiveData) { noteId ->
            Log.i(TAG, "Loading note (id: $noteId)")
            notesRepository.getNote(noteId).asLiveData()
        }

    fun loadNote(noteId: Long) {
        noteNameLiveData.value = noteId
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            Log.i(TAG, "Note saved (id: ${note.id}")
            notesRepository.updateNote(note)
        }
    }
}