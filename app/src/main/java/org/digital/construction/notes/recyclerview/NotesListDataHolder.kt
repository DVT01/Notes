package org.digital.construction.notes.recyclerview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.digital.construction.notes.model.Note

object NotesListDataHolder {

    var selectAllNotesIsOn = false

    private val selectedItems = MutableLiveData<MutableList<Long>>()

    val selectedItemsLiveData: LiveData<List<Long>> = Transformations.map(selectedItems) { it }

    val selectedItemsValue: MutableList<Long>
        get() = selectedItems.value ?: mutableListOf()

    fun changeLiveDataValue(list: MutableList<Long>) {
        selectedItems.value = list
    }
}