package com.digital.construction.notes.fragments

import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digital.construction.notes.R
import com.digital.construction.notes.activities.ACTION_OPEN_ABOUT
import com.digital.construction.notes.activities.ACTION_OPEN_SETTINGS
import com.digital.construction.notes.model.Note
import com.digital.construction.notes.recyclerview.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

private const val TAG = "NotesListFragment"
private const val ARG_NOTE_ID_REQUEST = "note_id_request_key"

const val ACTION_REPLACE_SELECT_ALL_TEXT = "com.digital.construction.notes.replace_select_all_text"
const val ACTION_OPEN_NOTE = "com.digital.construction.notes.open_note"
const val ACTION_CREATE_NOTE = "com.digital.construction.notes.create_note"

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

    private var adapter: NotesListAdapter = NotesListAdapter()
    private val importNoteLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { noteDirUri ->
        // Make sure user selected a document to import
        if (noteDirUri == null) {
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

        Note(fileName, fileText).let {
            notesListViewModel.insertNote(it)
        }
    }

    private val itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                Timber.d("Item moved: ${viewHolder.bindingAdapterPosition}")
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                Timber.d("Item swiped: ${viewHolder.bindingAdapterPosition}, direction: $direction")

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        (viewHolder as NotesListHolder).openNote()
                    }
                    ItemTouchHelper.RIGHT -> {
                        adapter.currentList[viewHolder.bindingAdapterPosition].let { note ->
                            notesListViewModel.deleteNote(note)
                        }
                    }
                }
            }
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

                Note(noteName, String()).let { note ->
                    notesListViewModel.insertNote(note).observe(viewLifecycleOwner) { id ->
                        openNote(id)
                    }
                }
            }
        }
    }
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private enum class SortBy(@IdRes val id: Int) {
        ASCENDING(R.id.ascending),
        DESCENDING(R.id.descending);

        companion object {
            fun getSortById(@IdRes id: Int): SortBy? {
                return values().find { it.id == id }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        Timber.tag(TAG)

        sortByOrder = sharedPreferences
            .getString(SORT_MODE_KEY, SortBy.ASCENDING.name)!!
            .let { SortBy.valueOf(it) }

        lifecycle.addObserver(LifecycleObserver())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notes_list, container, false)
            .apply {
                notesRecyclerView = findViewById(R.id.notes_list)
                notifyEmptyDBTextView = findViewById(R.id.empty_list)
                createNoteFab = findViewById(R.id.new_note_fab)
            }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            notesRecyclerView.layoutManager = GridLayoutManager(context, 2)
        } else {
            notesRecyclerView.layoutManager = LinearLayoutManager(context)
        }

        notesRecyclerView.adapter = adapter

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
                .newInstance(NoteNameDialog.DialogType.CREATE, String())
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

        val swipeToOpenOn = sharedPreferences.getBoolean(SWIPE_OPEN_KEY, true)
        val swipeToDeleteOn = sharedPreferences.getBoolean(SWIPE_DELETE_KEY, true)

        itemTouchHelperCallback.setDefaultSwipeDirs(
            when {
                swipeToDeleteOn && swipeToOpenOn -> ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                swipeToOpenOn -> ItemTouchHelper.LEFT
                swipeToDeleteOn -> ItemTouchHelper.RIGHT
                else -> 0
            }
        )

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(notesRecyclerView)
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
                sortByOrder = SortBy.getSortById(item.itemId)!!
                checkSortOrderMenuItem()

                sharedPreferences.edit {
                    putString(SORT_MODE_KEY, sortByOrder.name)
                }

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

    private fun updateUI(notes: List<Note> = adapter.currentList) {
        adapter.submitList(
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
        requireArguments().getString(ARG_NOTE_ID_REQUEST, String()).let { noteIdKey ->
            setFragmentResult(noteIdKey, Bundle().apply {
                putLong(noteIdKey, noteId)
            })
        }
    }

    private fun openSettings() {
        requireContext().sendBroadcast(Intent(ACTION_OPEN_SETTINGS))
    }

    private fun openAbout() {
        requireContext().sendBroadcast(Intent(ACTION_OPEN_ABOUT))
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
                        actionMode.title = "${value.size}/${adapter.currentList.size}"

                        actionMode.menu.findItem(R.id.select_all_notes).apply {
                            title = if (value.size == adapter.currentList.size) {
                                getString(R.string.deselect_all)
                            } else {
                                getString(R.string.select_all)
                            }
                        }
                    }
                }
            }

        // Gets called when all notes were selected and then one note was deselected
        private val replaceSelectAllNotesText: BroadcastReceiver by lazy {
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Timber.i("Received broadcast to replace select all button text")

                    actionMode.menu.findItem(R.id.select_all_notes).apply {
                        title = getString(R.string.select_all)
                    }
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

                    val selectAllNotesItem = mode.menu.findItem(R.id.select_all_notes)

                    if (selectAllNotesItem.title == getString(R.string.deselect_all)) {
                        requireContext().sendBroadcast(Intent(ACTION_DESELECT_NOTES))
                    } else {
                        requireContext().sendBroadcast(Intent(ACTION_SELECT_NOTES))
                        selectAllNotesItem.title = getString(R.string.deselect_all)
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
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            requireContext().run {
                unregisterReceiver(openSelectedNote)
                unregisterReceiver(createNote)
            }
        }
    }
}