package com.digital.construction.notes.recyclerview

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.digital.construction.notes.R
import com.digital.construction.notes.model.Note
import timber.log.Timber

private const val TAG = "NotesListAdapter"

const val ACTION_DESELECT_NOTES = "com.digital.construction.notes.deselect_notes"
const val ACTION_SELECT_NOTES = "com.digital.construction.notes.select_notes"

class NotesListAdapter : ListAdapter<Note, NotesListHolder>(NoteComparator()) {

    private lateinit var recyclerView: RecyclerView

    private val selectAllNotes: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Timber.i("Receive broadcast to select all notes")

                NotesListDataHolder.changeLiveDataValue(currentList.map { it.id }.toMutableList())
                selectAllNotes(true)
            }
        }
    }
    private val deselectAllNotes: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Timber.i("Receive broadcast to deselect all notes")

                NotesListDataHolder.changeLiveDataValue(mutableListOf())
                selectAllNotes(false)
            }
        }
    }

    init {
        Timber.tag(TAG)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesListHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_note, parent, false)

        return NotesListHolder(view)
    }

    override fun onBindViewHolder(holder: NotesListHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView

        recyclerView.context.run {
            registerReceiver(deselectAllNotes, IntentFilter(ACTION_DESELECT_NOTES))
            registerReceiver(selectAllNotes, IntentFilter(ACTION_SELECT_NOTES))
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        recyclerView.context.run {
            unregisterReceiver(deselectAllNotes)
            unregisterReceiver(selectAllNotes)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAllNotes(selectAll: Boolean) {
        NotesListDataHolder.selectAllNotesIsOn = selectAll

        for (viewHolderIndex in 0 until recyclerView.childCount) {
            recyclerView.run {
                getChildViewHolder(getChildAt(viewHolderIndex)).run {
                    itemView.isActivated = selectAll
                }
            }
        }
        notifyDataSetChanged()
    }

    class NoteComparator : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}