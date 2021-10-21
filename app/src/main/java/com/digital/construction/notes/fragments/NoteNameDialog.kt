package com.digital.construction.notes.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.digital.construction.notes.R
import java.util.*

private const val TAG = "NoteNameDialog"
private const val ARG_DIALOG_TYPE = "dialog_type"
private const val ARG_NOTE_NAME = "note_name"

private const val MISSING_ARGUMENTS_MSG =
    "Get an instance of this class using the newInstance function"

class NoteNameDialog : DialogFragment() {

    private val notesListViewModel: NotesListViewModel by activityViewModels()

    enum class DialogType {
        CREATE, RENAME
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogType = requireArguments().getString(ARG_DIALOG_TYPE)
            ?: throw MissingFormatArgumentException(MISSING_ARGUMENTS_MSG)

        val initialNoteName = requireArguments().getString(ARG_NOTE_NAME)
            ?: throw MissingFormatArgumentException(MISSING_ARGUMENTS_MSG)

        val dialogView = layoutInflater.inflate(R.layout.dialog_note_name, null)
        val noteNameEditText = dialogView.findViewById<AppCompatEditText>(R.id.note_name).apply {
            setText(initialNoteName)
        }

        @StringRes val dialogTitle: Int
        @StringRes val positiveButtonText: Int
        val broadcastIntent: Intent

        when (DialogType.valueOf(dialogType)) {
            DialogType.CREATE -> {
                dialogTitle = R.string.new_note
                positiveButtonText = R.string.create
                broadcastIntent = Intent(ACTION_CREATE_NOTE)
            }
            DialogType.RENAME -> {
                dialogTitle = R.string.rename_note
                positiveButtonText = R.string.rename
                broadcastIntent = Intent(ACTION_RENAME_NOTE)
            }
        }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(dialogTitle)
            .setPositiveButton(positiveButtonText, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        alertDialog.setOnShowListener {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                val confirmedNoteName = noteNameEditText.text.toString()

                Log.i(TAG, "Note name chosen: $confirmedNoteName")

                val notes = notesListViewModel.notesLiveData.value?.map { it.name } ?: emptyList()

                if (notes.contains(confirmedNoteName)) {
                    noteNameEditText.apply {
                        setText(String())
                        hint = getString(R.string.note_exists)
                    }
                } else if (confirmedNoteName.isNotBlank()) {
                    alertDialog.dismiss()

                    broadcastIntent.apply {
                        putExtra(NOTE_NAME, confirmedNoteName)
                        requireContext().sendBroadcast(this)
                    }
                }
            }

            negativeButton.setOnClickListener {
                alertDialog.cancel()
            }
        }

        return alertDialog
    }

    companion object {
        fun newInstance(dialogType: DialogType, noteName: String): NoteNameDialog {
            return NoteNameDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIALOG_TYPE, dialogType.name)
                    putString(ARG_NOTE_NAME, noteName)
                }
            }
        }
    }
}