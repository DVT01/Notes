package org.obebeokeke.notes.recyclerview

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.RecyclerView
import org.obebeokeke.notes.R
import org.obebeokeke.notes.fragments.ACTION_OPEN_NOTE
import org.obebeokeke.notes.fragments.NOTE_NAME
import org.obebeokeke.notes.fragments.NOTE_TEXT
import org.obebeokeke.notes.model.Note

private const val TAG = "NotesListAdapter"

class NotesListAdapter(
    var notes: List<Note>
) : RecyclerView.Adapter<NotesListAdapter.NoteHolder>() {

    private val selectedItems = MutableLiveData<MutableList<Note>>()
    val selectedItemsLiveData: LiveData<List<Note>> =
        Transformations.map(selectedItems) { newList ->
            newList
        }

    inner class NoteHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener, View.OnLongClickListener {

        private lateinit var note: Note

        private val noteNameTextView: TextView = itemView.findViewById(R.id.note_name)
        private val selectedItemsValue: MutableList<Note>
            get() = selectedItems.value ?: mutableListOf()

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bind(note: Note) {
            this.note = note
            noteNameTextView.text = this.note.name
        }

        override fun onClick(view: View) {
            if (selectedItemsValue.contains(this.note)) {
                deselectNote()
            } else if (selectedItemsValue.isEmpty()) {
                openNote()
            } else if (selectedItemsValue.isNotEmpty() && !selectedItemsValue.contains(this.note)) {
                selectNote()
            }
        }

        override fun onLongClick(view: View): Boolean {
            if (!selectedItemsValue.contains(this.note)) {
                selectNote()
            }
            return true
        }

        private fun selectNote() {
            selectedItems.value = selectedItemsValue.also { it.add(this.note) }
            itemView.isActivated = true
        }

        private fun deselectNote() {
            selectedItems.value = selectedItemsValue.also { it.remove(this.note) }
            itemView.isActivated = false
        }

        private fun openNote() {
            itemView.context?.sendBroadcast(
                Intent().apply {
                    action = ACTION_OPEN_NOTE
                    putExtra(NOTE_NAME, note.name)
                    putExtra(NOTE_TEXT, note.text)
                }
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.list_item_note,
                    parent,
                    false
                )
        return NoteHolder(view)
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val note = notes[position]
        holder.bind(note)
    }

    override fun getItemCount(): Int = notes.size
}