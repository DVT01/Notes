package org.supersoniclegend.notes.fragments

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.supersoniclegend.notes.model.Note
import org.supersoniclegend.notes.model.NotesRepository

class NoteViewModel : ViewModel() {

    private val notesRepository = NotesRepository.get()
    private val noteNameLiveData = MutableLiveData<String>()

    val noteLiveData: LiveData<Note> =
        Transformations.switchMap(noteNameLiveData) { noteName ->
            notesRepository.getNote(noteName).asLiveData()
        }

    fun loadNote(noteName: String) {
        noteNameLiveData.value = noteName
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            notesRepository.updateNote(note)
        }
    }
}