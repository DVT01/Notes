package com.digital.construction.notes.fragments

import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digital.construction.notes.R
import com.digital.construction.notes.activities.ACTION_OPEN_ABOUT
import com.digital.construction.notes.activities.ACTION_OPEN_SETTINGS
import com.digital.construction.notes.database.NotesPreferences
import com.digital.construction.notes.model.Note
import com.digital.construction.notes.recyclerview.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

private const val TAG = "NotesListFragment"
private const val ARG_NOTE_ID_REQUEST = "note_id_request_key"

const val ACTION_REPLACE_SELECT_ALL_TEXT = "com.digital.construction.notes.replace_select_all_text"
const val ACTION_OPEN_NOTE = "com.digital.construction.notes.open_note"
const val ACTION_CREATE_NOTE = "com.digital.construction.notes.create_note"
const val ACTION_DELETE_NOTE = "com.digital.construction.notes.delete_note"

const val NOTE_NAME = "note_name"
const val NOTE_NAME_REQUIRED = "Must pass the note's name in the Intent using NOTE_NAME"

const val NOTE_ID = "note_id"

class NotesListFragment : Fragment() {

    private lateinit var notifyEmptyDBTextView: AppCompatTextView
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var ascendingSortMenuItem: MenuItem
    private lateinit var descendingSortMenuItem: MenuItem
    private lateinit var sortByOrder: SortBy
    private lateinit var createNoteFab: FloatingActionButton

    private var notesListAdapter: NotesListAdapter = NotesListAdapter()
    private val importNoteLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { noteDirUri ->
        // Make sure user selected a document to import
        if (noteDirUri == null) {
            showSnackbar(R.string.no_note_selected)
            return@registerForActivityResult
        }

        Timber.i("Starting import (Uri: $noteDirUri)")

        val contentResolver = requireContext().contentResolver

        val fileText: String = StringBuilder().apply {
            contentResolver.openInputStream(noteDirUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().forEach { line ->
                        appendLine(line)
                    }
                }
            }
        }.toString()

        val fileName = contentResolver.query(
            noteDirUri,
            null,
            null,
            null,
            null
        )!!.use { cursor ->
            val fileNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(fileNameIndex).removeSuffix(".txt")
        }

        val importedNote = Note(fileName, fileText)
        notesListViewModel.insertNote(importedNote)

        showSnackbar(R.string.import_successful)
    }

    private val notesListViewModel: NotesListViewModel by lazy {
        ViewModelProvider(this).get(NotesListViewModel::class.java)
    }
    private val openSelectedNote: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val noteId = intent.getLongExtra(NOTE_ID, 0)
                openNote(noteId)
            }
        }
    }
    private val createNote: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val noteName = intent.getStringExtra(NOTE_NAME)
                    ?: throw MissingFormatArgumentException(NOTE_NAME_REQUIRED)

                val newNote = Note(noteName, String())
                val newNoteIdLiveData = notesListViewModel.insertNote(newNote)
                newNoteIdLiveData.observe(viewLifecycleOwner) { noteId ->
                    openNote(noteId)
                }
            }
        }
    }
    private val deleteNote: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val noteId = intent.getLongExtra(NOTE_ID, 0)
                notesListViewModel.deleteNote(noteId)
            }
        }
    }

    enum class SortBy(@IdRes val id: Int) {
        ASCENDING(R.id.ascending),
        DESCENDING(R.id.descending);

        companion object {
            fun getSortFromId(@IdRes id: Int): SortBy? {
                return values().find { it.id == id }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        Timber.tag(TAG)

        val savedSortBy = NotesPreferences.get().savedSortBy.value
        sortByOrder = SortBy.valueOf(savedSortBy)

        lifecycle.addObserver(LifecycleObserver())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notes_list, container, false)

        notesRecyclerView = view.findViewById(R.id.notes_list)
        notifyEmptyDBTextView = view.findViewById(R.id.empty_list)
        createNoteFab = view.findViewById(R.id.new_note_fab)

        notesRecyclerView.run {
            layoutManager =
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    GridLayoutManager(context, 2)
                } else {
                    LinearLayoutManager(context)
                }

            val swipeManager = RecyclerViewSwipeManager()
            adapter = swipeManager.createWrappedAdapter(notesListAdapter)
            swipeManager.attachRecyclerView(this)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notesListViewModel.notesLiveData.observe(viewLifecycleOwner) { allNotes ->
            Timber.i("Received ${allNotes.size} notes.")

            (requireActivity() as AppCompatActivity).supportActionBar?.subtitle =
                resources.getQuantityString(
                    R.plurals.note_count,
                    allNotes.size,
                    allNotes.size
                )

            updateUI(allNotes)
        }

        createNoteFab.setOnClickListener {
            NoteNameDialog
                .newInstance(NoteNameDialog.DialogType.CREATE)
                .show(childFragmentManager, null)
        }
    }

    override fun onStart() {
        super.onStart()

        val actionMode = NotesListActionMode()
        var actionModeStarted = false

        NotesListDataHolder.selectedItemsLiveData.observe(viewLifecycleOwner) { notesSelected ->
            Timber.i("Selected ${notesSelected.size} notes")

            if (notesSelected.isNotEmpty() && !actionModeStarted) {
                requireActivity().startActionMode(actionMode)
                actionModeStarted = true

            } else if (notesSelected.isEmpty() && actionModeStarted) {
                actionModeStarted = false
            }

            actionMode.notesSelected = notesSelected
        }
    }

    override fun onStop() {
        super.onStop()

        (requireActivity() as AppCompatActivity).supportActionBar?.subtitle = String()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.note_list_fragment_menu, menu)

        ascendingSortMenuItem = menu.findItem(R.id.ascending)
        descendingSortMenuItem = menu.findItem(R.id.descending)

        checkSortOrderMenuItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.open_settings -> {
                openSettings()
                true
            }
            R.id.import_note -> {
                importNoteLauncher.launch(arrayOf("text/plain"))
                true
            }
            in SortBy.values().map { it.id } -> {
                sortByOrder = SortBy.getSortFromId(item.itemId)!!
                checkSortOrderMenuItem()

                NotesPreferences.get().savedSortBy.value = sortByOrder.name

                updateUI()
                true
            }
            R.id.about -> {
                openAbout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(notes: List<Note> = notesListAdapter.currentList) {
        notesListAdapter.submitList(
            when (sortByOrder) {
                SortBy.ASCENDING -> {
                    notes.sortedBy { note ->
                        note.name
                    }
                }
                SortBy.DESCENDING -> {
                    notes.sortedByDescending { note ->
                        note.name
                    }
                }
            }
        )

        if (notes.isEmpty()) {
            notifyEmptyDBTextView.visibility = View.VISIBLE
            notesRecyclerView.visibility = View.GONE
        } else {
            notifyEmptyDBTextView.visibility = View.GONE
            notesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun checkSortOrderMenuItem() {
        if (sortByOrder.id == ascendingSortMenuItem.itemId) {
            ascendingSortMenuItem.isChecked = true
        } else {
            descendingSortMenuItem.isChecked = true
        }
    }

    private fun openNote(noteId: Long) {
        val noteIdKey = requireArguments().getString(ARG_NOTE_ID_REQUEST, String())
        val chosenNoteId = Bundle().apply { putLong(noteIdKey, noteId) }
        setFragmentResult(noteIdKey, chosenNoteId)
    }

    private fun openSettings() {
        requireContext().sendBroadcast(Intent(ACTION_OPEN_SETTINGS))
    }

    private fun openAbout() {
        requireContext().sendBroadcast(Intent(ACTION_OPEN_ABOUT))
    }

    private fun showSnackbar(@StringRes text: Int) {
        Snackbar
            .make(requireView(), text, Snackbar.LENGTH_SHORT)
            .show()
    }

    private inner class NotesListActionMode : ActionMode.Callback {

        private lateinit var actionMode: ActionMode
        var notesSelected: List<Long> = emptyList()
            set(value) {
                field = value

                if (::actionMode.isInitialized) {
                    if (value.isEmpty()) {
                        actionMode.finish()
                    } else {
                        val selectNotesMenuItem = actionMode.menu.findItem(R.id.select_all_notes)

                        actionMode.title = "${value.size}/${notesListAdapter.currentList.size}"
                        selectNotesMenuItem.title =
                            if (value.size == notesListAdapter.currentList.size) {
                                getString(R.string.deselect_all)
                            } else {
                                getString(R.string.select_all)
                            }
                    }
                }
            }

        // Gets called when all notes were selected and then one note was deselected
        private val replaceSelectAllNotesText: BroadcastReceiver by lazy {
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Timber.i("Received broadcast to replace select all button text")

                    val selectNotesMenuItem = actionMode.menu.findItem(R.id.select_all_notes)
                    selectNotesMenuItem.title = getString(R.string.select_all)
                }
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.note_list_fragment_action_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode

            requireContext().registerReceiver(
                replaceSelectAllNotesText,
                IntentFilter(ACTION_REPLACE_SELECT_ALL_TEXT)
            )

            return true
        }

        override fun onActionItemClicked(
            mode: ActionMode,
            item: MenuItem
        ): Boolean {
            when (item.itemId) {
                R.id.delete_note -> {
                    notesSelected.forEach { noteId ->
                        notesListViewModel.deleteNote(noteId)
                    }

                    mode.finish()
                }
                R.id.select_all_notes -> {

                    val selectNotesMenuItem = mode.menu.findItem(R.id.select_all_notes)

                    if (selectNotesMenuItem.title == getString(R.string.deselect_all)) {
                        requireContext().sendBroadcast(Intent(ACTION_DESELECT_NOTES))
                    } else {
                        requireContext().sendBroadcast(Intent(ACTION_SELECT_NOTES))
                        selectNotesMenuItem.title = getString(R.string.deselect_all)
                    }
                }
            }

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            requireContext().run {
                sendBroadcast(Intent(ACTION_DESELECT_NOTES))
                unregisterReceiver(replaceSelectAllNotesText)
            }
        }
    }

    companion object {
        fun newInstance(noteIdRequest: String): NotesListFragment {
            return NotesListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOTE_ID_REQUEST, noteIdRequest)
                }
            }
        }
    }

    internal inner class LifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            requireContext().run {
                registerReceiver(openSelectedNote, IntentFilter(ACTION_OPEN_NOTE))
                registerReceiver(createNote, IntentFilter(ACTION_CREATE_NOTE))
                registerReceiver(deleteNote, IntentFilter(ACTION_DELETE_NOTE))
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            requireContext().run {
                unregisterReceiver(openSelectedNote)
                unregisterReceiver(createNote)
                unregisterReceiver(deleteNote)
            }
        }
    }
}