package com.digital.construction.notes.widget

import android.appwidget.AppWidgetManager
import com.digital.construction.notes.R
import com.digital.construction.notes.database.NotesPreferences
import com.digital.construction.notes.model.Note
import com.digital.construction.notes.model.NotesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

private const val TAG = "NoteWidgetDataHolder"

object NoteWidgetDataHolder {
    /**
     * This stores all of the widgets, and their respective [Note], using the [NoteWidget] class.
     */
    private val noteWidgets = mutableListOf<NoteWidget>()

    init {
        Timber.tag(TAG)

        initializeNoteWidgets()
    }

    data class NoteWidget(val widgetId: Int, val note: Note) {
        fun delete() {
            noteWidgets.remove(this)
        }
    }

    /**
     * Creates a [NoteWidget] and adds it to the [Set].
     *
     * @param widgetId The id of the widget
     * @param note The note that corresponds to the widget
     * @return The created [NoteWidget]
     */
    fun addWidget(widgetId: Int, note: Note): NoteWidget {
        val noteWidget = NoteWidget(widgetId, note)
        noteWidgets.add(noteWidget)
        return noteWidget
    }

    /**
     * Returns the [NoteWidget] associated with the widget id
     *
     * @throws NullPointerException If there wasn't a [NoteWidget] saved with that id
     * @param widgetId The id of the widget you want
     * @return [NoteWidget]
     */
    fun getWidget(widgetId: Int): NoteWidget {
        val result = noteWidgets.find { it.widgetId == widgetId }
        Timber.d("Got $result for widgetId=$widgetId")
        Timber.d("noteWidgets=$noteWidgets")

        return result!!
    }

    /**
     * @param note The note that the returned widgets display.
     * @return An [Array] of the ids of widgets that display the [note].
     */
    fun updateWidgets(appWidgetManager: AppWidgetManager, note: Note) {
        val widgetIds = NotesPreferences.get().widgetNotes
            .filter { it.value == note.id }
            .map { it.key.toInt() }
            .toIntArray()

        noteWidgets
            .filter { it.note.id == note.id }
            .map {
                it.note.apply {
                    name = note.name
                    text = note.text
                }
            }

        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view)
    }

    /**
     * Gets all the widgets from [NotesPreferences.widgetNotes] and populates [noteWidgets]
     */
    private fun initializeNoteWidgets() {
        for (preference in NotesPreferences.get().widgetNotes) {
            runBlocking {
                val widgetId = preference.key.toInt()
                val noteId = preference.value as Long

                val note = NotesRepository.get().getNote(noteId).first()
                val noteWidget = NoteWidget(widgetId, note)

                noteWidgets.add(noteWidget)
            }
        }
    }
}