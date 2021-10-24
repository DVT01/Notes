package com.digital.construction.notes.fragments

import androidx.lifecycle.*
import com.digital.construction.notes.model.Note
import com.digital.construction.notes.model.NotesRepository
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "NoteViewModel"

class NoteViewModel : ViewModel() {

    private val notesRepository = NotesRepository.get()
    private val noteNameLiveData = MutableLiveData<Long>()

    init {
        Timber.tag(TAG)
    }

    val noteLiveData: LiveData<Note> =
        Transformations.switchMap(noteNameLiveData) { noteId ->
            Timber.i("Loading note (id: $noteId)")
            notesRepository.getNote(noteId).asLiveData()
        }

    fun loadNote(noteId: Long) {
        noteNameLiveData.value = noteId
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            Timber.i("Note saved (id: ${note.id}")
            notesRepository.updateNote(note)
        }
    }
}