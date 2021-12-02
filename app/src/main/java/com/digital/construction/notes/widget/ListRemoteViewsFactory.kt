package com.digital.construction.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.digital.construction.notes.R
import com.digital.construction.notes.fragments.NOTE_ID
import com.digital.construction.notes.model.Note
import com.digital.construction.notes.model.NotesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

private const val TAG = "ListRemoteViewsFactory"

class ListRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val noteId = intent.getLongExtra(NOTE_ID, -1)
    private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

    private lateinit var note: Note

    init {
        Timber.tag(TAG)
    }

    override fun onCreate() {
        Timber.d("Creating widget with widgetId=$widgetId and noteId=$noteId")
        Timber.d("Got ${ListWidgetDataHolder.noteWidgets.size} saved widgets")

        getUpToDateNote()
    }

    override fun onDataSetChanged() = getUpToDateNote()

    override fun hasStableIds() = false

    override fun getViewTypeCount() = 1

    override fun getCount() = note.text.lines().size

    override fun getLoadingView() = RemoteViews(context.packageName, R.layout.widget_line)

    override fun getViewAt(position: Int): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_line).apply {
            setOnClickFillInIntent(R.id.line, Intent())
            setTextViewText(R.id.line, note.text.lines()[position])
            setTextViewTextSize(R.id.line, TypedValue.COMPLEX_UNIT_PX, 50F)
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun onDestroy() {
        Timber.d("Deleting widget with widgetId=$widgetId and noteId=$noteId")
        ListWidgetDataHolder.noteWidgets.remove(widgetId)

        // Delete saved widget id and note id entry in the shared preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit {
            remove(widgetId.toString())
        }
    }

    private fun getUpToDateNote() = runBlocking {
        note = ListWidgetDataHolder.noteWidgets.getOrPut(widgetId) {
            /**
             * When the singleton instance of ListWidgetDataHolder is destroyed and restarted,
             * there are no saved widgets and the program crashes.
             *
             * This gets the note from the DB and saves it to the singleton for later use.
             */
            Timber.w("noteId=$noteId was not found for widgetId=$widgetId")
            NotesRepository.get().getNote(noteId).first()
        }
    }
}