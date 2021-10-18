package org.coquicoding.notes.recyclerview

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import org.coquicoding.notes.R
import org.coquicoding.notes.fragments.ACTION_OPEN_NOTE
import org.coquicoding.notes.fragments.ACTION_REPLACE_SELECT_ALL_TEXT
import org.coquicoding.notes.fragments.NOTE_ID
import org.coquicoding.notes.model.Note

private const val TAG = "NotesListHolder"

@SuppressLint("ClickableViewAccessibility")
class NotesListHolder(view: View) : RecyclerView.ViewHolder(view) {

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

        itemView.isActivated = NotesListDataHolder.selectedItemsValue.contains(note)
    }

    private fun selectNote() {
        NotesListDataHolder.selectedItemsValue.run {
            add(note)
            NotesListDataHolder.changeLiveDataValue(this)
        }

        itemView.isActivated = true
    }

    private fun deselectNote() {
        NotesListDataHolder.selectedItemsValue.run {
            remove(note)
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

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            Log.d(TAG, "GestureDetector: onSingleTapConfirmed")

            NotesListDataHolder.selectedItemsValue.run {
                if (isEmpty()) { // Not in selection mode
                    openNote()
                } else { // In selection mode
                    when {
                        // Note was selected
                        contains(note) -> deselectNote()
                        // Note was not selected
                        !contains(note) -> selectNote()
                    }
                }
            }
            return true
        }

        override fun onLongPress(event: MotionEvent) {
            Log.d(TAG, "GestureDetector: onLongPress")

            if (!NotesListDataHolder.selectedItemsValue.contains(note)) {
                selectNote()
            }
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            Log.d(TAG, "GestureDetector: onDoubleTap")

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