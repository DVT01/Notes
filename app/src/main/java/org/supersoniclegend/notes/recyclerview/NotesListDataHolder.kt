package org.supersoniclegend.notes.recyclerview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.supersoniclegend.notes.model.Note

object NotesListDataHolder {

    var selectAllNotesIsOn = false

    private val selectedItems = MutableLiveData<MutableList<Note>>()

    val selectedItemsLiveData: LiveData<List<Note>> = Transformations.map(selectedItems) { it }

    val selectedItemsValue: MutableList<Note>
        get() = selectedItems.value ?: mutableListOf()

    fun changeLiveDataValue(list: MutableList<Note>) {
        selectedItems.value = list
    }
}