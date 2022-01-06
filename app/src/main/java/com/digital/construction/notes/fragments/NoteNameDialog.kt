package com.digital.construction.notes.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.digital.construction.notes.R
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.util.*

private const val TAG = "NoteNameDialog"
private const val ARG_DIALOG_TYPE = "dialog_type"
private const val ARG_NOTE_NAME = "note_name"
private const val MISSING_ARGUMENTS_MSG = "Get an instance of this class using the newInstance function"
private val INVALID_CHARACTERS = arrayOf("/")

class NoteNameDialog : DialogFragment() {

    private val notesListViewModel: NotesListViewModel by activityViewModels()

    enum class DialogType {
        CREATE {
            override val dialogTitle: Int
                get() = R.string.new_note
            override val positiveButtonText: Int
                get() = R.string.create
            override val broadcastIntent: Intent
                get() = Intent(ACTION_CREATE_NOTE)
        },
        RENAME {
            override val dialogTitle: Int
                get() = R.string.rename_note
            override val positiveButtonText: Int
                get() = R.string.rename
            override val broadcastIntent: Intent
                get() = Intent(ACTION_RENAME_NOTE)
        };

        abstract val dialogTitle: Int
        abstract val positiveButtonText: Int
        open val broadcastIntent: Intent? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogTypeStr = requireArguments().getString(ARG_DIALOG_TYPE)
            ?: throw MissingFormatArgumentException(MISSING_ARGUMENTS_MSG)

        val initialNoteName = requireArguments().getString(ARG_NOTE_NAME)
            ?: throw MissingFormatArgumentException(MISSING_ARGUMENTS_MSG)

        val dialogView = layoutInflater.inflate(R.layout.dialog_note_name, null)
        val noteNameEditText: AppCompatEditText = dialogView.findViewById(R.id.note_name)
        val dialogType = DialogType.valueOf(dialogTypeStr)

        noteNameEditText.setText(initialNoteName)
        noteNameEditText.filters = arrayOf(
            object : InputFilter {
                override fun filter(
                    source: CharSequence,
                    start: Int,
                    end: Int,
                    dest: Spanned,
                    dstart: Int,
                    dend: Int
                ): CharSequence {
                    if (INVALID_CHARACTERS.contains(source)) {
                        Snackbar
                            .make(requireView(), R.string.invalid_character, Snackbar.LENGTH_SHORT)
                            .show()

                        return String()
                    }

                    return source
                }
            }
        )

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(dialogType.dialogTitle)
            .setPositiveButton(dialogType.positiveButtonText, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        alertDialog.setOnShowListener {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                val confirmedNoteName = noteNameEditText.text.toString()

                Timber.i("Note name chosen: $confirmedNoteName")

                notesListViewModel.notesLiveData.observe(this) { allNotes ->
                    val noteNames = allNotes.map { it.name }

                    if (noteNames.contains(confirmedNoteName)) {
                        noteNameEditText.apply {
                            setText(String())
                            hint = getString(R.string.note_exists)
                        }
                    } else if (confirmedNoteName.isNotBlank()) {
                        alertDialog.dismiss()

                        dialogType.broadcastIntent?.apply {
                            putExtra(NOTE_NAME, confirmedNoteName)
                            requireContext().sendBroadcast(this)
                        }
                    }
                }
            }

            negativeButton.setOnClickListener {
                alertDialog.cancel()
            }
        }

        return alertDialog
    }

    override fun onDestroy() {
        super.onDestroy()
        notesListViewModel.notesLiveData.removeObservers(this)
    }

    companion object {
        fun newInstance(dialogType: DialogType, noteName: String = String()): NoteNameDialog {
            return NoteNameDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIALOG_TYPE, dialogType.name)
                    putString(ARG_NOTE_NAME, noteName)
                }
            }
        }
    }
}