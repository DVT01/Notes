package org.dvt01.notes.recyclerview

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.RecyclerView
import org.dvt01.notes.R
import org.dvt01.notes.fragments.ACTION_OPEN_NOTE
import org.dvt01.notes.fragments.ACTION_REPLACE_SELECT_ALL_TEXT
import org.dvt01.notes.fragments.NOTE_NAME
import org.dvt01.notes.fragments.NOTE_TEXT
import org.dvt01.notes.model.Note

private const val TAG = "NotesListAdapter"
const val ACTION_DESELECT_NOTES = "org.dvt01.notes.deselect_notes"
const val ACTION_SELECT_NOTES = "org.dvt01.notes.select_notes"

class NotesListAdapter(
    var notes: List<Note>
) : RecyclerView.Adapter<NotesListAdapter.NoteHolder>() {

    private lateinit var recyclerView: RecyclerView
    private var selectAllNotesIsOn = false

    private val selectedItems = MutableLiveData<MutableList<Note>>()
    val selectedItemsLiveData: LiveData<List<Note>> =
        Transformations.map(selectedItems) { newList ->
            newList
        }

    private val selectAllNotes: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Receive broadcast to select all notes")

                selectedItems.value = notes.toMutableList()
                selectAllNotes(true)
            }
        }
    }
    private val deselectAllNotes: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Receive broadcast to deselect all notes")

                selectedItems.value = mutableListOf()
                selectAllNotes(false)
            }
        }
    }

    inner class NoteHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener, View.OnLongClickListener {

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
            noteNameTextView.text = this.note.name

            itemView.isActivated = selectedItemsValue.contains(note)
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

            if (selectAllNotesIsOn) {
                selectAllNotesIsOn = false

                itemView.context?.run {
                    sendBroadcast(Intent(ACTION_REPLACE_SELECT_ALL_TEXT))
                }
            }
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
        val view = LayoutInflater.from(parent.context)
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView

        val deselectIntentFilter = IntentFilter(ACTION_DESELECT_NOTES)
        recyclerView.context.registerReceiver(deselectAllNotes, deselectIntentFilter)

        val selectIntentFilter = IntentFilter(ACTION_SELECT_NOTES)
        recyclerView.context.registerReceiver(selectAllNotes, selectIntentFilter)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        recyclerView.context.unregisterReceiver(deselectAllNotes)
        recyclerView.context.unregisterReceiver(selectAllNotes)
    }

    fun selectAllNotes(selectAll: Boolean) {
        selectAllNotesIsOn = selectAll

        for (viewHolderIndex in 0 until recyclerView.childCount) {
            val viewHolder = recyclerView.getChildViewHolder(
                recyclerView.getChildAt(viewHolderIndex)
            )
            viewHolder.itemView.isActivated = selectAll
        }
        notifyDataSetChanged()
    }
}