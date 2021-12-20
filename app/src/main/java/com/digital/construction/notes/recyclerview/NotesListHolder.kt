package com.digital.construction.notes.recyclerview

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.GestureDetectorCompat
import com.digital.construction.notes.R
import com.digital.construction.notes.fragments.ACTION_DELETE_NOTE
import com.digital.construction.notes.fragments.ACTION_OPEN_NOTE
import com.digital.construction.notes.fragments.ACTION_REPLACE_SELECT_ALL_TEXT
import com.digital.construction.notes.fragments.NOTE_ID
import com.digital.construction.notes.model.Note
import com.google.android.material.snackbar.Snackbar
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder
import com.skydoves.expandablelayout.ExpandableLayout
import timber.log.Timber

const val ACTION_ENTERED_SELECTION_MODE = "com.digital.construction.notes.entered_selection_mode"

private const val TAG = "NotesListHolder"

@SuppressLint("ClickableViewAccessibility")
class NotesListHolder(view: View) : AbstractSwipeableItemViewHolder(view) {

    private lateinit var note: Note

    private val expandableLayout: ExpandableLayout = itemView.findViewById(R.id.expandable)

    private val noteNameTextView: AppCompatTextView =
        expandableLayout.parentLayout.findViewById(R.id.note_name)

    private val noteTextTextView: AppCompatTextView =
        expandableLayout.secondLayout.findViewById(R.id.note_text)

    private val enteredSelectionModeReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                expandableLayout.collapse()
            }
        }
    }

    init {
        Timber.tag(TAG)

        GestureDetectorCompat(itemView.context, GestureListener()).run {
            itemView.setOnTouchListener { _, event -> onTouchEvent(event) }
        }

        itemView.context?.registerReceiver(
            enteredSelectionModeReceiver,
            IntentFilter(ACTION_ENTERED_SELECTION_MODE)
        )
    }

    fun bind(note: Note) {
        this.note = note
        noteNameTextView.text = note.name
        noteTextTextView.text = note.text

        // Hide spinner if the note doesn't have any text
        expandableLayout.showSpinner = note.text.isNotEmpty()

        itemView.isActivated = NotesListDataHolder.selectedItemsValue.contains(note.id)
    }

    override fun getSwipeableContainerView(): View {
        return itemView.findViewById(R.id.container)
    }

    private fun selectNote() {
        NotesListDataHolder.selectedItemsValue.run {
            // Send broadcast notifying every receiver that we have entered selection mode
            if (isEmpty()) {
                itemView.context?.sendBroadcast(Intent(ACTION_ENTERED_SELECTION_MODE))
            }

            add(note.id)
            NotesListDataHolder.changeLiveDataValue(this)
        }

        itemView.isActivated = true
    }

    private fun deselectNote() {
        NotesListDataHolder.selectedItemsValue.run {
            remove(note.id)
            NotesListDataHolder.changeLiveDataValue(this)
        }

        itemView.isActivated = false

        // Turn off all note selection if it was on
        if (NotesListDataHolder.selectAllNotesIsOn) {
            NotesListDataHolder.selectAllNotesIsOn = false

            itemView.context?.sendBroadcast(Intent(ACTION_REPLACE_SELECT_ALL_TEXT))
        }
    }

    fun openNote() {
        itemView.context?.sendBroadcast(
            Intent(ACTION_OPEN_NOTE).run {
                putExtra(NOTE_ID, note.id)
            }
        )
    }

    fun deleteNote() {
        itemView.context?.sendBroadcast(
            Intent(ACTION_DELETE_NOTE).run {
                putExtra(NOTE_ID, note.id)
            }
        )
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(event: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            Timber.d("GestureDetector: onSingleTapConfirmed")

            NotesListDataHolder.selectedItemsValue.run {
                when {
                    isEmpty() -> openNote()  // Not in selection mode
                    contains(note.id) -> deselectNote()  // Note was selected
                    !contains(note.id) -> selectNote()  // Note was not selected
                }
            }

            return true
        }

        override fun onLongPress(event: MotionEvent) {
            Timber.d("GestureDetector: onLongPress")

            if (!NotesListDataHolder.selectedItemsValue.contains(note.id)) {
                selectNote()
            }
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            Timber.d("GestureDetector: onDoubleTap")

            if (note.text.isEmpty()) {
                Snackbar
                    .make(itemView, R.string.no_text, Snackbar.LENGTH_SHORT)
                    .show()

                return true
            }

            // Check if we're in selection mode
            if (NotesListDataHolder.selectedItemsValue.isEmpty() && !expandableLayout.isExpanded) {
                expandableLayout.expand()
            } else if (expandableLayout.isExpanded) {
                expandableLayout.collapse()
            }

            return true
        }
    }
}