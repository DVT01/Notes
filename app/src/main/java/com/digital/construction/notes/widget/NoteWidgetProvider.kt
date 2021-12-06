package com.digital.construction.notes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.digital.construction.notes.R
import com.digital.construction.notes.activities.MainActivity
import com.digital.construction.notes.database.NotesPreferences
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

    private val scope = CoroutineScope(Job() + Dispatchers.Main.immediate)

    init {
        Timber.tag(TAG)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (widgetId in appWidgetIds) {
            /**
             * preference is null when the widget tries to update before having
             * the widget configuration activity finish thus trying to get a SharedPreference
             * that doesn't exist.
             */
            val preference = NotesPreferences.get().getPreference(widgetId.toString()) ?: continue
            val noteId = preference.value as Long

            scope.launch {
                NotesRepository.get().getNote(noteId).collect { note ->
                    Timber.d("Updating $preference")

                    ListWidgetDataHolder.noteWidgets[widgetId] = note

                    val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java)
                        .apply { putExtra(NOTE_ID, noteId) }
                        .let { intent ->
                            PendingIntent.getActivity(
                                context,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        }

                    val remoteViews = RemoteViews(context.packageName, R.layout.note_widget).apply {
                        setPendingIntentTemplate(R.id.list_view, pendingIntent)

                        val remoteServiceIntent = Intent(context, ListWidgetService::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            putExtra(NOTE_ID, noteId)
                            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                        }

                        setRemoteAdapter(R.id.list_view, remoteServiceIntent)
                    }

                    appWidgetManager.updateAppWidget(widgetId, remoteViews)
                }
            }
        }
    }
}