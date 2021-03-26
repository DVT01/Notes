package org.obebeokeke.notes.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.obebeokeke.notes.R
import org.obebeokeke.notes.model.Note

private const val TAG = "NoteFragment"
private const val ARG_NOTE_NAME = "note_name"

class NoteFragment : Fragment() {

    private lateinit var note: Note
    private lateinit var textField: EditText

    private val noteViewModel: NoteViewModel by lazy {
        ViewModelProvider(this).get(NoteViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val noteName = requireArguments().getString(ARG_NOTE_NAME, "")
        noteViewModel.loadNote(noteName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_note,
            container,
            false
        )

        textField = view.findViewById(R.id.note_text)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel.noteLiveData.observe(viewLifecycleOwner) { note ->
            this.note = note
            updateUI()
        }
    }

    override fun onStart() {
        super.onStart()

        val textFieldWatcher =
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
                }

            }

        textField.addTextChangedListener(textFieldWatcher)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.note_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_note -> {
                noteViewModel.saveNote(note)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDetach() {
        super.onDetach()
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)
    }

    private fun updateUI() {
        (activity as AppCompatActivity).supportActionBar?.title = note.name
        textField.setText(note.text)
    }

    companion object {
        fun newInstance(noteName: String): NoteFragment {
            val args = Bundle().apply {
                putString(ARG_NOTE_NAME, noteName)
            }
            return NoteFragment().apply {
                arguments = args
            }
        }
    }
}