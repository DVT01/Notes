package com.digital.construction.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.digital.construction.notes.R
import com.digital.construction.notes.database.NotesPreferences
import com.digital.construction.notes.fragments.NOTE_ID
import com.digital.construction.notes.model.Note
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
        getUpToDateNote()
    }

    override fun onDataSetChanged() {
        Timber.d("Updating widgetId=$widgetId")
        getUpToDateNote()
    }

    override fun hasStableIds() = false

    override fun getViewTypeCount() = 1

    override fun getCount() = note.text.lines().size

    override fun getLoadingView() = RemoteViews(context.packageName, R.layout.widget_line)

    override fun getViewAt(position: Int): RemoteViews {
        Timber.d("getViewAt: widgetId=$widgetId")
        return RemoteViews(context.packageName, R.layout.widget_line).apply {
            setOnClickFillInIntent(R.id.line, Intent())
            setTextViewText(R.id.line, note.text.lines()[position])
            setTextViewTextSize(R.id.line, TypedValue.COMPLEX_UNIT_PX, 50F)
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun onDestroy() {
        val preference = NotesPreferences.get().getPreference(widgetId.toString())
        Timber.d("Deleting $preference")

        preference?.delete()
        NoteWidgetDataHolder.getWidget(widgetId).delete()
    }

    private fun getUpToDateNote() = runBlocking {
        note = NoteWidgetDataHolder.getWidget(widgetId).note
    }
}