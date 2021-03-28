package org.dvt01.notes.fragments

import androidx.lifecycle.ViewModel
import org.dvt01.notes.NotesRepository
import org.dvt01.notes.model.Note

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