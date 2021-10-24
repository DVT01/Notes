package com.digital.construction.notes.fragments

import androidx.lifecycle.*
import com.digital.construction.notes.model.Note
import com.digital.construction.notes.model.NotesRepository
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "NotesListViewModel"

class NotesListViewModel : ViewModel() {

    private val notesRepository = NotesRepository.get()
    val notesLiveData = notesRepository.getAllNotes().asLiveData()

    init {
        Timber.tag(TAG)
    }

    fun insertNote(note: Note): LiveData<Long> {
        Timber.i("Adding note to DB (id: ${note.id})")

        val result = MutableLiveData<Long>()

        viewModelScope.launch {
            result.value = notesRepository.insertNote(note)
        }

        return result
    }

    fun deleteNote(note: Note) {
        Timber.i("Deleting note (id: ${note.id})")
        viewModelScope.launch {
            notesRepository.deleteNote(note)
        }
    }

    fun deleteNote(id: Long) {
        Timber.i("Deleting note (id: $id)")
        viewModelScope.launch {
            notesRepository.deleteNote(id)
        }
    }
}