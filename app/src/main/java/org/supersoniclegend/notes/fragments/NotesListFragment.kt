package org.supersoniclegend.notes.fragments

import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.supersoniclegend.notes.R
import org.supersoniclegend.notes.activities.ACTION_OPEN_SETTINGS
import org.supersoniclegend.notes.model.Note
import org.supersoniclegend.notes.recyclerview.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

private const val TAG = "NotesListFragment"
private const val ARG_NOTE_NAME_REQUEST = "note_name_request_key"

const val ACTION_REPLACE_SELECT_ALL_TEXT = "org.supersoniclegend.notes.replace_select_all_text"
const val ACTION_OPEN_NOTE = "org.supersoniclegend.notes.open_note"
const val ACTION_CREATE_NOTE = "org.supersoniclegend.notes.create_note"

const val NOTE_NAME = "note_name"
const val NOTE_NAME_REQUIRED = "Must pass the note's name in the Intent using NOTE_NAME"
const val NOTE_TEXT = "note_text"
const val NOTE_TEXT_REQUIRED = "Must pass the note's text in the Intent using NOTE_TEXT"

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
        Log.i(TAG, "Import request for Uri: $noteDirUri")

        // Make sure user selected a document to import
        if (noteDirUri == null) {
            return@registerForActivityResult
        }

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
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                Log.d(TAG, "onMove: ${viewHolder.adapterPosition}")
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                Log.d(TAG, "onSwiped: ${viewHolder.adapterPosition}, direction: $direction")

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        (viewHolder as NotesListHolder).openNote()
                    }
                    ItemTouchHelper.RIGHT -> {
                        adapter.currentList[viewHolder.adapterPosition].let { note ->
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
                Log.i(TAG, "Received broadcast action: ${intent.action}")

                val noteName = intent.getStringExtra(NOTE_NAME)
                    ?: throw MissingFormatArgumentException(NOTE_NAME_REQUIRED)
                val noteText = intent.getStringExtra(NOTE_TEXT)
                    ?: throw MissingFormatArgumentException(NOTE_TEXT_REQUIRED)

                openNote(Note(noteName, noteText))
            }
        }
    }
    private val createNote: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val noteName = intent.getStringExtra(NOTE_NAME)
                    ?: throw MissingFormatArgumentException(NOTE_NAME_REQUIRED)

                Note(noteName, "").let {
                    notesListViewModel.insertNote(it)
                    openNote(it)
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

        sortByOrder = sharedPreferences
            .getString(SORT_MODE_KEY, SortBy.ASCENDING.name)!!
            .let { SortBy.valueOf(it) }
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

        notesListViewModel.notesLiveData.observe(viewLifecycleOwner) { notes ->
            Log.i(TAG, "Received ${notes.size} notes.")

            (requireActivity() as AppCompatActivity).supportActionBar?.apply {
                subtitle =
                    resources.getQuantityString(R.plurals.note_count, notes.size, notes.size)
            }

            updateUI(notes)
        }

        createNoteFab.setOnClickListener {
            NoteNameDialog
                .newInstance(NoteNameDialog.DialogType.CREATE, "")
                .show(childFragmentManager, null)
        }
    }

    override fun onStart() {
        super.onStart()

        val actionMode = NotesListActionMode()
        var actionModeStarted = false

        NotesListDataHolder.selectedItemsLiveData.observe(viewLifecycleOwner) { notesSelected ->
            Log.i(TAG, "Selected ${notesSelected.size} notes")

            if (notesSelected.isNotEmpty() && !actionModeStarted) {
                requireActivity().startActionMode(actionMode)
                actionModeStarted = true

            } else if (notesSelected.isEmpty() && actionModeStarted) {
                actionModeStarted = false
            }

            actionMode.notesSelected = notesSelected
        }

        requireContext().apply {
            registerReceiver(openSelectedNote, IntentFilter(ACTION_OPEN_NOTE))
            registerReceiver(createNote, IntentFilter(ACTION_CREATE_NOTE))
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(notesRecyclerView)
    }

    override fun onStop() {
        super.onStop()

        requireContext().apply {
            unregisterReceiver(openSelectedNote)
            unregisterReceiver(createNote)
        }

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            subtitle = ""
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.note_list_fragment_menu, menu)

        menu.run {
            ascendingSortMenuItem = findItem(R.id.ascending)
            descendingSortMenuItem = findItem(R.id.descending)
        }

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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(notes: List<Note> = adapter.currentList) {
        adapter.submitList(
            when (sortByOrder) {
                SortBy.ASCENDING -> {
                    notes.sortedBy { it.name }
                }
                SortBy.DESCENDING -> {
                    notes.sortedByDescending { it.name }
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

    private fun openNote(note: Note) {
        Log.i(TAG, "Opening note: ${note.name}")

        requireArguments().getString(ARG_NOTE_NAME_REQUEST, "").let { noteNameKey ->
            setFragmentResult(noteNameKey, Bundle().apply {
                putString(noteNameKey, note.name)
            })
        }
    }

    private fun openSettings() {
        requireContext().sendBroadcast(Intent(ACTION_OPEN_SETTINGS))
    }

    private inner class NotesListActionMode : ActionMode.Callback {

        private lateinit var actionMode: ActionMode
        var notesSelected: List<Note> = emptyList()
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
                    Log.i(TAG, "Received action to replace select all button text")

                    actionMode.menu.findItem(R.id.select_all_notes).apply {
                        title = getString(R.string.select_all)
                    }
                }
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.apply {
                inflate(R.menu.note_list_fragment_action_mode, menu)
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode

            IntentFilter(ACTION_REPLACE_SELECT_ALL_TEXT).let {
                requireContext().registerReceiver(replaceSelectAllNotesText, it)
            }

            return true
        }

        override fun onActionItemClicked(
            mode: ActionMode,
            item: MenuItem
        ): Boolean {
            when (item.itemId) {
                R.id.delete_note -> {
                    notesSelected.forEach {
                        notesListViewModel.deleteNote(it)
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
        fun newInstance(noteNameRequest: String): NotesListFragment {
            return NotesListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOTE_NAME_REQUEST, noteNameRequest)
                }
            }
        }
    }
}