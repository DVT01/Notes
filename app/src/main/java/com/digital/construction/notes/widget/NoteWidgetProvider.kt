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
import com.digital.construction.notes.fragments.NOTE_ID
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
            for (widgetId in appWidgetIds) {
                val noteId = sharedPreferences.getLong(widgetId.toString(), -1)

                /**
                 * noteId is -1 when the widget tries to update before having
                 * the widget configuration activity finish
                 */
                if (noteId == -1L) continue

                NotesRepository.get().getNote(noteId).collect { note ->
                    Timber.d("Updating widget (id=$widgetId)")

                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(NOTE_ID, noteId)
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val remoteViews =
                        RemoteViews(context.packageName, R.layout.note_widget).apply {
                            setOnClickPendingIntent(R.id.widget, pendingIntent)
                            setTextViewText(R.id.note_text, note.text)
                        }

                    appWidgetManager.updateAppWidget(widgetId, remoteViews)
                }
            }
        }
    }
}