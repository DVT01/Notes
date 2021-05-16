package org.coquicoding.notes.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import org.coquicoding.notes.R
import java.util.*

private const val TAG = "NoteNameDialog"
private const val ARG_DIALOG_TYPE = "dialog_type"
private const val ARG_NOTE_NAME = "note_name"

class NoteNameDialog : DialogFragment() {

    private val notesListViewModel: NotesListViewModel by activityViewModels()

    enum class DialogType {
        CREATE, RENAME
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogType = requireArguments().getString(ARG_DIALOG_TYPE)
            ?: throw MissingFormatArgumentException("Get an instance using newInstance function")
        val noteName = requireArguments().getString(ARG_NOTE_NAME)
            ?: throw MissingFormatArgumentException("Get an instance using newInstance function")

        val dialogView = layoutInflater.inflate(R.layout.dialog_note_name, null)
        val noteNameEditText = dialogView.findViewById<AppCompatEditText>(R.id.note_name).apply {
            setText(noteName)
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
                val noteName = noteNameEditText.text.toString()

                Log.i(TAG, "Note name: $noteName")

                val notes = notesListViewModel.notesLiveData.value?.map { it.name } ?: emptyList()

                if (notes.contains(noteName)) {
                    noteNameEditText.apply {
                        setText("")
                        hint = getString(R.string.note_exists)
                    }
                } else if (noteName.isNotBlank()) {
                    alertDialog.dismiss()

                    broadcastIntent.apply {
                        putExtra(NOTE_NAME, noteName)
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