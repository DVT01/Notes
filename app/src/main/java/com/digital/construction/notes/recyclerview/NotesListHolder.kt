package com.digital.construction.notes.recyclerview

import android.annotation.SuppressLint
import android.content.Intent
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
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder
import timber.log.Timber

private const val TAG = "NotesListHolder"

@SuppressLint("ClickableViewAccessibility")
class NotesListHolder(view: View) : AbstractSwipeableItemViewHolder(view) {

    private lateinit var note: Note

    private val noteNameTextView: AppCompatTextView = itemView.findViewById(R.id.note_name)

    init {
        GestureDetectorCompat(itemView.context, GestureListener()).run {
            itemView.setOnTouchListener { _, event -> onTouchEvent(event) }
        }
    }

    fun bind(note: Note) {
        this.note = note
        noteNameTextView.text = note.name

        itemView.isActivated = NotesListDataHolder.selectedItemsValue.contains(note.id)
    }

    override fun getSwipeableContainerView(): View {
        return itemView.findViewById(R.id.container)
    }

    private fun selectNote() {
        NotesListDataHolder.selectedItemsValue.run {
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

            itemView.context?.run {
                sendBroadcast(Intent(ACTION_REPLACE_SELECT_ALL_TEXT))
            }
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

        init {
            Timber.tag(TAG)
        }

        override fun onDown(event: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            Timber.d("GestureDetector: onSingleTapConfirmed")

            NotesListDataHolder.selectedItemsValue.run {
                if (isEmpty()) { // Not in selection mode
                    openNote()
                } else { // In selection mode
                    when {
                        // Note was selected
                        contains(note.id) -> deselectNote()
                        // Note was not selected
                        !contains(note.id) -> selectNote()
                    }
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

            // Not in selection mode
            if (NotesListDataHolder.selectedItemsValue.isEmpty()) {
                if (noteNameTextView.maxLines == 3) {
                    noteNameTextView.maxLines = Int.MAX_VALUE
                } else {
                    noteNameTextView.maxLines = 3
                }
            }
            return true
        }
    }
}