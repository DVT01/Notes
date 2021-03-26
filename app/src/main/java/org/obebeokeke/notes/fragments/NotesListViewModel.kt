package org.obebeokeke.notes.fragments

import androidx.lifecycle.ViewModel
import org.obebeokeke.notes.NotesRepository
import org.obebeokeke.notes.model.Note

class NotesListViewModel : ViewModel() {

    private val notesRepository = NotesRepository.get()
    val notesLiveData = notesRepository.getAllNotes()

    fun insertNote(note: Note) {
        notesRepository.insertNote(note)
    }

    fun deleteNote(note: Note) {
        notesRepository.deleteNote(note)
    }
}