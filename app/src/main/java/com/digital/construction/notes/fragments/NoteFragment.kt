package com.digital.construction.notes.fragments

import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.digital.construction.notes.R
import com.digital.construction.notes.database.NotesPreferences
import com.digital.construction.notes.model.Note
import com.digital.construction.notes.widget.NoteWidgetDataHolder
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*

private const val TAG = "NoteFragment"
private const val ARG_NOTE_ID = "note_id"
private const val PROVIDER_AUTHORITY = "com.digital.construction.notes.fileprovider"

const val ACTION_RENAME_NOTE = "com.digital.construction.notes.rename_note"

class NoteFragment : Fragment() {

    private lateinit var noteTextEditText: AppCompatEditText
    private lateinit var savedNoteText: String
    private lateinit var note: Note

    private val noteTextIsSaved: Boolean
        get() = note.text == savedNoteText

    private val noteTextIsNotSaved: Boolean
        get() = !noteTextIsSaved

    private val exportNoteLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { noteDirUri ->
        Timber.i("Starting export (id: ${note.id})")

        if (noteDirUri == null) {
            showSnackbar(getString(R.string.export_failed, getString(R.string.no_file_created)))
            return@registerForActivityResult
        }

        requireContext().contentResolver.run {
            openFileDescriptor(noteDirUri, "w")?.use { parcelFileDescriptor ->
                FileOutputStream(parcelFileDescriptor.fileDescriptor).use { fileOutputStream ->
                    fileOutputStream.write(note.text.toByteArray())
                }
            }
        }

        showSnackbar(R.string.export_successful)
    }

    private val noteViewModel: NoteViewModel by lazy {
        ViewModelProvider(this).get(NoteViewModel::class.java)
    }
    private val renameNote: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val noteName = intent.getStringExtra(NOTE_NAME)
                    ?: throw MissingFormatArgumentException(NOTE_NAME_REQUIRED)

                note.name = noteName

                noteViewModel.run {
                    saveNote(note)
                    loadNote(note.id)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        Timber.tag(TAG)

        requireArguments().getLong(ARG_NOTE_ID).let { noteId ->
            noteViewModel.loadNote(noteId)
        }

        lifecycle.addObserver(LifecycleObserver())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note, container, false)

        val fontSizePercentage = NotesPreferences.get().fontSizePercentage.value
            .toFloat()
            .div(100)

        noteTextEditText = view.findViewById(R.id.note_text)
        noteTextEditText.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            noteTextEditText.textSize.times(fontSizePercentage)
        )

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel.noteLiveData.observe(viewLifecycleOwner) { note ->
            if (note == null) return@observe

            Timber.i("Loaded note (id: ${note.id})")

            this.note = note
            savedNoteText = note.text

            updateUI()
        }
    }

    override fun onStart() {
        super.onStart()

        val noteTextWatcher =
            object : TextWatcher {
                override fun beforeTextChanged(
                    sequence: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    sequence: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    note.text = sequence.toString()
                }

                override fun afterTextChanged(sequence: Editable) {
                    (activity as AppCompatActivity).supportActionBar?.apply {
                        title = if (noteTextIsSaved) {
                            note.name
                        } else {
                            "*${note.name}"
                        }
                    }
                }
            }

        noteTextEditText.addTextChangedListener(noteTextWatcher)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.note_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.export_note -> {
                exportNoteLauncher.launch(note.fileName)
            }
            R.id.share_note -> {
                shareNote()
            }
            R.id.rename_note -> {
                NoteNameDialog
                    .newInstance(NoteNameDialog.DialogType.RENAME, note.name)
                    .show(childFragmentManager, null)
            }
            android.R.id.home -> {
                requireActivity().onBackPressed()
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onStop() {
        super.onStop()
        saveNote()
    }

    override fun onDestroy() {
        super.onDestroy()

        requireContext().deleteFile(note.fileName)
    }

    private fun updateUI() {
        (activity as AppCompatActivity).supportActionBar?.title = note.name

        noteTextEditText.setText(note.text)
    }

    private fun shareNote() {
        // Create note text file and write note text
        val noteFile = File(requireContext().filesDir, note.fileName).apply {
            outputStream().use { fileOutputStream ->
                fileOutputStream.write(note.text.toByteArray())
            }
        }

        val sendNoteIntent = Intent(Intent.ACTION_SEND).apply {
            val uri = FileProvider.getUriForFile(requireContext(), PROVIDER_AUTHORITY, noteFile)

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, uri)
            type = requireContext().contentResolver.getType(uri)
        }

        val noteChooserIntent = Intent.createChooser(sendNoteIntent, getString(R.string.note_chooser_text))

        startActivity(noteChooserIntent)
    }

    private fun saveNote() {
        /**
         * Update all widgets that display this [note]
         */
        fun updateWidgets() {
            val appWidgetManager = AppWidgetManager.getInstance(requireContext())
            NoteWidgetDataHolder.updateWidgets(appWidgetManager, note)
        }

        if (noteTextIsNotSaved) {
            noteViewModel.saveNote(note)
            updateWidgets()
            showSnackbar(R.string.note_saved)
        }
    }

    private fun showSnackbar(text: String) {
        Snackbar
            .make(requireView(), text, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun showSnackbar(@StringRes text: Int) {
        showSnackbar(getString(text))
    }

    companion object {
        fun newInstance(noteId: Long): NoteFragment {
            return NoteFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_NOTE_ID, noteId)
                }
            }
        }
    }

    internal inner class LifecycleObserver : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            requireContext().registerReceiver(renameNote, IntentFilter(ACTION_RENAME_NOTE))

            // Enable back arrow in left toolbar
            (activity as AppCompatActivity).supportActionBar?.run {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                setHomeAsUpIndicator(R.drawable.ic_outline_arrow_back_24)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            requireContext().unregisterReceiver(renameNote)

            // Disable back arrow in left toolbar and change title
            (activity as AppCompatActivity).supportActionBar?.apply {
                title = getString(R.string.app_name)
                setDisplayHomeAsUpEnabled(false)
                setDisplayShowHomeEnabled(false)
            }
        }
    }
}