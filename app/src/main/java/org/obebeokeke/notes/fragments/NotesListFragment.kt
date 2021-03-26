package org.obebeokeke.notes.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obebeokeke.notes.R
import org.obebeokeke.notes.dialogs.NoteNameDialog
import org.obebeokeke.notes.model.Note
import org.obebeokeke.notes.recyclerview.NotesListAdapter

private const val TAG = "NotesListFragment"
private const val ARG_NOTE_NAME = "requestKey"
private const val REQUEST_NOTE_NAME = "NoteNameDialog"

const val ACTION_OPEN_NOTE = "org.obebeokeke.notes.open_note"
const val NOTE_NAME = "org.obebeokeke.notes.note_name"
const val NOTE_TEXT = "org.obebeokeke.notes.note_text"

class NotesListFragment : Fragment(), FragmentResultListener {

    private lateinit var notesRecyclerView: RecyclerView
    private var adapter: NotesListAdapter = NotesListAdapter(emptyList())

    private val notesListViewModel: NotesListViewModel by lazy {
        ViewModelProvider(this).get(NotesListViewModel::class.java)
    }
    private val broadcastReceiver: BroadcastReceiver by lazy {
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
        childFragmentManager.setFragmentResultListener(REQUEST_NOTE_NAME, viewLifecycleOwner, this)
    }

    override fun onStart() {
        super.onStart()
        adapter.selectedItemsLiveData.observe(viewLifecycleOwner) { notesSelected ->
            Log.i(TAG, "Selected: $notesSelected")

            val actionMode = NotesListActionMode(notesSelected)

            requireActivity().startActionMode(actionMode)

            if (notesSelected.isEmpty()) {
                actionMode.getActionMode().finish()
            }
        }

        val intentFilter = IntentFilter(ACTION_OPEN_NOTE)
        requireContext().registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.note_list_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_note -> {
                with(NoteNameDialog.newInstance(REQUEST_NOTE_NAME)) {
                    show(this@NotesListFragment.childFragmentManager, null)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            REQUEST_NOTE_NAME -> {
                val noteName = result.getString(requestKey, "")
                Log.i(TAG, "Received request to create note with name: $noteName")
                val note = Note(noteName, "")
                notesListViewModel.insertNote(note)
                openNote(note)
            }
        }
    }

    private fun updateUI(notes: List<Note>) {
        adapter.notes = notes
        notesRecyclerView.adapter = adapter
    }

    private fun openNote(note: Note) {
        val requestKey = requireArguments().getString(ARG_NOTE_NAME, "")
        val resultBundle = Bundle().apply {
            putString(requestKey, note.name)
        }
        setFragmentResult(requestKey, resultBundle)
    }

    private inner class NotesListActionMode(private val notesSelected: List<Note>) : ActionMode.Callback {

        private lateinit var actionMode: ActionMode

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
        }

        fun getActionMode(): ActionMode {
            return actionMode
        }
    }

    companion object {
        fun newInstance(requestKey: String): NotesListFragment {
            val args = Bundle().apply {
                putString(ARG_NOTE_NAME, requestKey)
            }
            return NotesListFragment().apply {
                arguments = args
            }
        }
    }
}