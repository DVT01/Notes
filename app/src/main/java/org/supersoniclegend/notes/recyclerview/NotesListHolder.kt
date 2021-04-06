package org.supersoniclegend.notes.recyclerview

import android.content.Intent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import org.supersoniclegend.notes.R
import org.supersoniclegend.notes.fragments.ACTION_OPEN_NOTE
import org.supersoniclegend.notes.fragments.ACTION_REPLACE_SELECT_ALL_TEXT
import org.supersoniclegend.notes.fragments.NOTE_NAME
import org.supersoniclegend.notes.fragments.NOTE_TEXT
import org.supersoniclegend.notes.model.Note

class NotesListHolder(
    view: View,
    private val selectedItems: MutableLiveData<MutableList<Note>>,
    private var selectAllNotesIsOn: Boolean
) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {

    private lateinit var note: Note

    private val noteNameTextView: AppCompatTextView = itemView.findViewById(R.id.note_name)
    private val selectedItemsValue: MutableList<Note>
        get() = selectedItems.value ?: mutableListOf()

    init {
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    fun bind(note: Note) {
        this.note = note
        noteNameTextView.text = note.name

        itemView.isActivated = selectedItemsValue.contains(note)
    }

    override fun onClick(view: View) {
        selectedItemsValue.run {
            // Not in selection mode
            if (isEmpty()) {
                openNote()
                // In selection mode
            } else if (isNotEmpty()) {
                when {
                    // Note was selected
                    contains(note) -> deselectNote()
                    // Note was not selected
                    !contains(note) -> selectNote()
                }
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        // Check if this note was selected
        if (!selectedItemsValue.contains(note)) {
            selectNote()
        }
        return true
    }

    private fun selectNote() {
        selectedItems.value = selectedItemsValue.apply { add(note) }
        itemView.isActivated = true
    }

    private fun deselectNote() {
        selectedItems.value = selectedItemsValue.apply { remove(note) }
        itemView.isActivated = false

        // Turn off all note selection if it was on
        if (selectAllNotesIsOn) {
            selectAllNotesIsOn = false

            itemView.context?.run {
                sendBroadcast(Intent(ACTION_REPLACE_SELECT_ALL_TEXT))
            }
        }
    }

    private fun openNote() {
        itemView.context?.sendBroadcast(
            Intent(ACTION_OPEN_NOTE).apply {
                putExtra(NOTE_NAME, note.name)
                putExtra(NOTE_TEXT, note.text)
            }
        )
    }
}