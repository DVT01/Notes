package org.dvt01.notes.fragments

import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.dvt01.notes.R
import org.dvt01.notes.activities.ACTION_OPEN_SETTINGS
import org.dvt01.notes.model.Note
import org.dvt01.notes.recyclerview.ACTION_DESELECT_NOTES
import org.dvt01.notes.recyclerview.ACTION_SELECT_NOTES
import org.dvt01.notes.recyclerview.NotesListAdapter

private const val TAG = "NotesListFragment"
private const val ARG_NOTE_NAME = "requestNote"

const val ACTION_REPLACE_SELECT_ALL_TEXT = "org.dvt01.notes.replace_select_all_text"
const val ACTION_OPEN_NOTE = "org.dvt01.notes.open_note"
const val NOTE_NAME = "org.dvt01.notes.note_name"
const val NOTE_TEXT = "org.dvt01.notes.note_text"

class NotesListFragment : Fragment() {

    private lateinit var emptyNotesTextView: TextView
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var ascendingSortMenuItem: MenuItem
    private lateinit var descendingSortMenuItem: MenuItem
    private lateinit var sortOrder: Sort
    private lateinit var addNoteFab: FloatingActionButton

    private var adapter: NotesListAdapter = NotesListAdapter(emptyList())

    private val notesListViewModel: NotesListViewModel by lazy {
        ViewModelProvider(this).get(NotesListViewModel::class.java)
    }
    private val selectNoteFromAdapter: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received broadcast action: ${intent.action}")

                val noteName = intent.getStringExtra(NOTE_NAME) ?: ""
                val noteText = intent.getStringExtra(NOTE_TEXT) ?: ""

                openNote(
                    Note(noteName, noteText)
                )
            }
        }
    }
    private val settingsSharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    enum class Sort(@IdRes val id: Int) {
        Ascending(R.id.ascending),
        Descending(R.id.descending);

        companion object {
            fun getEnumById(@IdRes id: Int): Sort? {
                values().forEach {
                    if (it.id == id) {
                        return it
                    }
                }
                return null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        sortOrder = Sort.valueOf(
            settingsSharedPreferences.getString(
                SORT_MODE_KEY,
                Sort.Ascending.name
            ).toString()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_notes_list,
            container,
            false
        )

        notesRecyclerView = view.findViewById(R.id.notes_list)
        notesRecyclerView.layoutManager = LinearLayoutManager(context)
        notesRecyclerView.adapter = adapter

        emptyNotesTextView = view.findViewById(R.id.empty_list)
        addNoteFab = view.findViewById(R.id.new_note_fab)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notesListViewModel.notesLiveData.observe(viewLifecycleOwner) { notes ->
            Log.i(TAG, "Received ${notes.size} notes.")

            val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
            actionBar?.subtitle =
                resources.getQuantityString(R.plurals.note_count, notes.size, notes.size)

            updateUI(notes)
        }

        addNoteFab.setOnClickListener {
            showNoteCreationDialog()
        }
    }

    override fun onStart() {
        super.onStart()

        val actionMode = NotesListActionMode()
        var actionModeStarted = false

        adapter.selectedItemsLiveData.observe(viewLifecycleOwner) { notesSelected ->
            val notesNames =
                notesSelected.mapIndexed { index, note -> "Note ${index + 1}: ${note.name}" }
            Log.i(TAG, "Selected: $notesNames")

            if (notesSelected.isNotEmpty() && !actionModeStarted) {
                requireActivity().startActionMode(actionMode)
                actionModeStarted = true

            } else if (notesSelected.isEmpty() && actionModeStarted) {
                actionModeStarted = false
            }

            actionMode.notesSelected = notesSelected
        }

        val intentFilter = IntentFilter(ACTION_OPEN_NOTE)
        requireContext().registerReceiver(selectNoteFromAdapter, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(selectNoteFromAdapter)

        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar?.subtitle = ""
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.note_list_fragment_menu, menu)

        ascendingSortMenuItem = menu.findItem(R.id.ascending)
        descendingSortMenuItem = menu.findItem(R.id.descending)

        checkSortOrder()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.open_settings -> {
                openSettings()
                true
            }
            in Sort.values().map { it.id } -> {
                sortOrder = Sort.getEnumById(item.itemId)!!
                checkSortOrder()

                settingsSharedPreferences.edit {
                    putString(SORT_MODE_KEY, sortOrder.name)
                }
                updateUI()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(notes: List<Note> = adapter.notes) {
        adapter.notes = when (sortOrder) {
            Sort.Ascending -> {
                notes.sortedBy { it.name }
            }
            Sort.Descending -> {
                notes.sortedByDescending { it.name }
            }
        }

        notesRecyclerView.adapter = adapter

        if (notes.isEmpty()) {
            emptyNotesTextView.visibility = View.VISIBLE
            notesRecyclerView.visibility = View.GONE
        } else {
            emptyNotesTextView.visibility = View.GONE
            notesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun checkSortOrder() {
        if (sortOrder.id == ascendingSortMenuItem.itemId) {
            ascendingSortMenuItem.isChecked = true
        } else {
            descendingSortMenuItem.isChecked = true
        }
    }

    private fun showNoteCreationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_note_name, null)
        val noteNameEditText = dialogView.findViewById<EditText>(R.id.note_name)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(R.string.new_note)
            .setPositiveButton(R.string.create_new_note, null)
            .setNegativeButton(R.string.cancel_new_note, null)
            .create()

        alertDialog.setOnShowListener {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                val noteName = noteNameEditText.text.toString()

                Log.i(TAG, "Note name: $noteName")

                val notes = notesListViewModel.notesLiveData.value?.map { it.name } ?: emptyList()
                if (notes.contains(noteName)) {
                    noteNameEditText.setText("")
                    noteNameEditText.hint = getString(R.string.note_already_exists)
                } else if (noteName.isNotBlank()) {
                    alertDialog.dismiss()

                    val note = Note(noteName, "")
                    notesListViewModel.insertNote(note)
                    openNote(note)
                }
            }

            negativeButton.setOnClickListener {
                alertDialog.cancel()
            }
        }

        alertDialog.show()
    }

    private fun openNote(note: Note) {
        val requestNoteKey = requireArguments().getString(ARG_NOTE_NAME, "")
        val resultNoteBundle = Bundle().apply {
            putString(requestNoteKey, note.name)
        }
        setFragmentResult(requestNoteKey, resultNoteBundle)
    }

    private fun openSettings() {
        val openSettingsIntent = Intent(ACTION_OPEN_SETTINGS)
        requireContext().sendBroadcast(openSettingsIntent)
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
                        actionMode.title = "${value.size}/${adapter.notes.size}"
                    }
                }
            }

        private val replaceSelectAllNotesText: BroadcastReceiver by lazy {
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.i(TAG, "Received action to replace select all button text")

                    val selectNotes = actionMode.menu.findItem(R.id.select_all_notes)
                    selectNotes.title = getString(R.string.select_all)
                }
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.note_list_fragment_action_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode

            val replaceSelectAllNotesTextFilter = IntentFilter(ACTION_REPLACE_SELECT_ALL_TEXT)
            requireContext().registerReceiver(
                replaceSelectAllNotesText,
                replaceSelectAllNotesTextFilter
            )

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

                    val selectNotes = mode.menu.findItem(R.id.select_all_notes)

                    if (selectNotes.title == getString(R.string.deselect_all)) {
                        requireContext().sendBroadcast(Intent(ACTION_DESELECT_NOTES))
                    } else {
                        requireContext().sendBroadcast(Intent(ACTION_SELECT_NOTES))
                        selectNotes.title = getString(R.string.deselect_all)
                    }
                }
            }

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            with(requireContext()) {
                sendBroadcast(Intent(ACTION_DESELECT_NOTES))
                unregisterReceiver(replaceSelectAllNotesText)
            }
        }
    }

    companion object {
        fun newInstance(openNoteRequest: String): NotesListFragment {
            val args = Bundle().apply {
                putString(ARG_NOTE_NAME, openNoteRequest)
            }
            return NotesListFragment().apply {
                arguments = args
            }
        }
    }
}