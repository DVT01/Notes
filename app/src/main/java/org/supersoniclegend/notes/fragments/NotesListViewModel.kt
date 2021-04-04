package org.supersoniclegend.notes.fragments

import androidx.lifecycle.ViewModel
import org.supersoniclegend.notes.NotesRepository
import org.supersoniclegend.notes.model.Note

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