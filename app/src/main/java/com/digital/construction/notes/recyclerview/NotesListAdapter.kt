package com.digital.construction.notes.recyclerview

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.digital.construction.notes.R
import com.digital.construction.notes.fragments.SWIPE_DELETE_KEY
import com.digital.construction.notes.fragments.SWIPE_OPEN_KEY
import com.digital.construction.notes.model.Note
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault
import timber.log.Timber

private const val TAG = "NotesListAdapter"

const val ACTION_DESELECT_NOTES = "com.digital.construction.notes.deselect_notes"
const val ACTION_SELECT_NOTES = "com.digital.construction.notes.select_notes"

class NotesListAdapter : ListAdapter<Note, NotesListHolder>(NoteComparator()),
    SwipeableItemAdapter<NotesListHolder> {

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
        setHasStableIds(true)
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

    override fun getItemId(position: Int): Long {
        return currentList[position].id
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAllNotes(selectAll: Boolean) {
        NotesListDataHolder.selectAllNotesIsOn = selectAll

        for (viewHolderIndex in 0 until recyclerView.childCount) {
            val view = recyclerView.getChildAt(viewHolderIndex)
            val viewHolder = recyclerView.getChildViewHolder(view)
            viewHolder.itemView.isActivated = selectAll
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

    override fun onGetSwipeReactionType(
        holder: NotesListHolder,
        position: Int,
        x: Int,
        y: Int
    ): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(recyclerView.context)

        val swipeToOpenOn = sharedPreferences.getBoolean(SWIPE_OPEN_KEY, true)
        val swipeToDeleteOn = sharedPreferences.getBoolean(SWIPE_DELETE_KEY, true)

        return when {
            swipeToDeleteOn && swipeToOpenOn -> SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
            swipeToOpenOn -> SwipeableItemConstants.REACTION_CAN_SWIPE_LEFT
            swipeToDeleteOn -> SwipeableItemConstants.REACTION_CAN_SWIPE_RIGHT
            else -> SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_ANY
        }
    }

    override fun onSwipeItemStarted(holder: NotesListHolder, position: Int) {}

    override fun onSetSwipeBackground(holder: NotesListHolder, position: Int, type: Int) {}

    override fun onSwipeItem(
        holder: NotesListHolder,
        position: Int,
        result: Int
    ): SwipeResultAction {
        if (result == SwipeableItemConstants.RESULT_SWIPED_LEFT) {
            holder.openNote()
        } else if (result == SwipeableItemConstants.RESULT_SWIPED_RIGHT) {
            holder.deleteNote()
        }

        return SwipeResultActionDefault()
    }
}