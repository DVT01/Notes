package org.obebeokeke.notes.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.obebeokeke.notes.NotesRepository
import org.obebeokeke.notes.model.Note

class NoteViewModel : ViewModel() {

    private val notesRepository = NotesRepository.get()
    private val noteNameLiveData = MutableLiveData<String>()

    var noteLiveData: LiveData<Note> =
        Transformations.switchMap(noteNameLiveData) { noteName ->
            notesRepository.getNote(noteName)
        }

    fun loadNote(noteName: String) {
        noteNameLiveData.value = noteName
    }

    fun saveNote(note: Note) {
        notesRepository.updateNote(note)
    }
}