package org.dvt01.notes.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.dvt01.notes.R
import org.dvt01.notes.activities.ACTION_OPEN_SETTINGS
import org.dvt01.notes.model.Note
import org.dvt01.notes.recyclerview.ACTION_DESELECT_NOTES
import org.dvt01.notes.recyclerview.NotesListAdapter

private const val TAG = "NotesListFragment"
private const val ARG_NOTE_NAME = "requestNote"

const val ACTION_OPEN_NOTE = "org.dvt01.notes.open_note"
const val NOTE_NAME = "org.dvt01.notes.note_name"
const val NOTE_TEXT = "org.dvt01.notes.note_text"

class NotesListFragment : Fragment() {

    private lateinit var notesRecyclerView: RecyclerView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notesListViewModel.notesLiveData.observe(viewLifecycleOwner) { notes ->
            Log.i(TAG, "Received ${notes.size} notes.")
            updateUI(notes)
        }
    }

    override fun onStart() {
        super.onStart()

        val actionMode = NotesListActionMode()
        var actionModeStarted = false

        adapter.selectedItemsLiveData.observe(viewLifecycleOwner) { notesSelected ->
            Log.i(TAG, "Selected: $notesSelected")

            actionMode.notesSelected = notesSelected

            if (notesSelected.isNotEmpty() && !actionModeStarted) {
                requireActivity().startActionMode(actionMode)
                actionModeStarted = true

            } else if (notesSelected.isEmpty() && actionModeStarted) {
                actionMode.getActionMode().finish()
                actionModeStarted = false
            }
        }

        val intentFilter = IntentFilter(ACTION_OPEN_NOTE)
        requireContext().registerReceiver(selectNoteFromAdapter, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(selectNoteFromAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.note_list_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_note -> {
                showNoteCreationDialog()
                true
            }
            R.id.open_settings -> {
                openSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(notes: List<Note>) {
        adapter.notes = notes
        notesRecyclerView.adapter = adapter
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

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.note_list_fragment_action_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
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
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            for (i in 0 until notesRecyclerView.childCount) {
                val a = notesRecyclerView.findViewHolderForAdapterPosition(i)
                a?.let {
                    it.itemView.isActivated = false
                }
            }

            requireContext().sendBroadcast(
                Intent().apply {
                    action = ACTION_DESELECT_NOTES
                }
            )
        }

        fun getActionMode(): ActionMode {
            return actionMode
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