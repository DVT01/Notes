package com.digital.construction.notes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import com.digital.construction.notes.R
import com.digital.construction.notes.activities.MainActivity
import com.digital.construction.notes.model.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "NoteWidgetProvider"

class NoteWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    init {
        Timber.tag(TAG)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        scope.launch {
            appWidgetIds.forEach { widgetId ->
                sharedPreferences.getLong(widgetId.toString(), 1).let { noteId ->
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    NotesRepository.get().getNote(noteId).collect { note ->
                        // note is null when widget tries to update before finishing the configuration
                        if (note == null) return@collect

                        Timber.d("Widget id ($widgetId); Note id ($noteId)")

                        val views = RemoteViews(context.packageName, R.layout.note_widget).apply {
                            setOnClickPendingIntent(R.id.widget, pendingIntent)
                            setTextViewText(R.id.note_text, note.text)

                            Intent().apply {

                            }
                        }

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }
            }
        }
    }
}