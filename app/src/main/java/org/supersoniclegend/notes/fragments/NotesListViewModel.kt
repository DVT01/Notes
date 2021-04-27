package org.supersoniclegend.notes.fragments

import androidx.lifecycle.ViewModel
import org.supersoniclegend.notes.model.Note
import org.supersoniclegend.notes.model.NotesRepository

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