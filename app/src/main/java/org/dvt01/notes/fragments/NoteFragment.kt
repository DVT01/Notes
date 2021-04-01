package org.dvt01.notes.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import org.dvt01.notes.R
import org.dvt01.notes.model.Note
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "NoteFragment"
private const val ARG_NOTE_NAME = "note_name"
private const val PROVIDER_AUTHORITY = "org.dvt01.notes.fileprovider"

class NoteFragment : Fragment() {

    private lateinit var note: Note
    private lateinit var textField: EditText

    private val exportNoteLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { noteDirUri ->
        Log.i(TAG, "Export request for ${note.name}")

        try {
            requireContext().contentResolver.openFileDescriptor(noteDirUri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fileStream ->
                    fileStream.write(note.text.toByteArray())
                }
            }
        } catch (error: Exception) {
            when (error) {
                is NullPointerException, is FileNotFoundException, is IOException -> {
                    Log.e(TAG, "Failed to export note: ${note.name}", error)
                }
                else -> throw error
            }
        }
    }

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

        val settingsSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val fontSizePercentage =
            settingsSharedPreferences.getString(FONT_SIZE_KEY, "100")!!.toFloat().div(100)
        textField.textSize = textField.textSize * fontSizePercentage

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
            R.id.export_note -> {
                exportNoteLauncher.launch(note.fileName)
                true
            }
            R.id.share_note -> {
                shareNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)
        requireContext().deleteFile(note.fileName)
    }

    private fun updateUI() {
        (activity as AppCompatActivity).supportActionBar?.title = note.name
        textField.setText(note.text)
    }

    private fun shareNote() {
        val noteFile = File(requireContext().filesDir, note.fileName)

        noteFile.outputStream().use { fileOutputStream ->
            fileOutputStream.write(note.text.toByteArray())
        }

        val noteUri = FileProvider.getUriForFile(
            requireContext(),
            PROVIDER_AUTHORITY,
            noteFile
        )

        val contentResolver = requireContext().contentResolver

        val sendNoteIntent = Intent(Intent.ACTION_SEND).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, noteUri)
            type = contentResolver.getType(noteUri)
        }

        val noteChooserIntent = Intent.createChooser(
            sendNoteIntent,
            getString(R.string.send_note)
        )

        startActivity(noteChooserIntent)
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