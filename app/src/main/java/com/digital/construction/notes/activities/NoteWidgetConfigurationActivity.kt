package com.digital.construction.notes.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.lifecycle.asLiveData
import com.digital.construction.notes.R
import com.digital.construction.notes.database.NotesPreferences
import com.digital.construction.notes.model.Note
import com.digital.construction.notes.model.NotesRepository
import com.digital.construction.notes.widget.NoteWidgetProvider
import timber.log.Timber

private const val TAG = "NoteWidgetConfigurationActivity"

class NoteWidgetConfigurationActivity : AppCompatActivity() {

    private lateinit var notesListSpinner: AppCompatSpinner
    private lateinit var noteTextTextView: TextView
    private lateinit var acceptButton: Button

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_widget_configuration)
        setResult(Activity.RESULT_CANCELED)

        Timber.tag(TAG)

        notesListSpinner = findViewById(R.id.notes_list)
        noteTextTextView = findViewById(R.id.note_text)
        acceptButton = findViewById(R.id.accept)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    override fun onStart() {
        super.onStart()

        notesListSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    pos: Int,
                    id: Long
                ) {
                    noteTextTextView.text = (parent.getItemAtPosition(pos) as Note).text
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        val noteArrayAdapter =
            object : ArrayAdapter<Note>(this, android.R.layout.simple_spinner_item) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return initView(position, convertView, parent, false)
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    return initView(position, convertView, parent, true)
                }

                private fun initView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup,
                    dropDownView: Boolean
                ): View {
                    return when (convertView) {
                        null -> {
                            val viewType =
                                if (dropDownView)
                                    android.R.layout.simple_spinner_dropdown_item
                                else
                                    android.R.layout.simple_spinner_item

                            layoutInflater.inflate(
                                viewType,
                                parent,
                                false
                            ).apply {
                                findViewById<TextView>(android.R.id.text1).apply {
                                    text = getItem(position)!!.name
                                }
                            }
                        }
                        else -> convertView
                    }
                }
            }

        NotesRepository.get().getAllNotes().asLiveData().observe(this) { allNotes ->
            noteArrayAdapter.addAll(
                // Make sure to remove all notes without anything written
                allNotes.filter { it.text.isNotEmpty() }
            )
        }

        notesListSpinner.adapter = noteArrayAdapter

        acceptButton.setOnClickListener {
            val note = try {
                notesListSpinner.selectedItem as Note
            } catch (error: NullPointerException) {
                Timber.e(error, "There are no notes with text to choose from")
                finish()
                return@setOnClickListener
            }

            val preference = NotesPreferences.get().createPreference(appWidgetId.toString(), -1L, note.id)
            Timber.d("Creating $preference")

            val noteWidgetProviderIntent = Intent(this, NoteWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

                val appWidgetIds = AppWidgetManager
                    .getInstance(application)
                    .getAppWidgetIds(
                        ComponentName(
                            application,
                            NoteWidgetProvider::class.java
                        )
                    )

                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }

            sendBroadcast(noteWidgetProviderIntent)
            setResult(Activity.RESULT_OK, noteWidgetProviderIntent)
            finish()
        }
    }
}