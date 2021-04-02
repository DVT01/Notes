package org.dvt01.notes.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import org.dvt01.notes.R

private const val TAG = "NoteNameDialog"
private const val ARG_DIALOG_TYPE = "dialog_type"

class NoteNameDialog : DialogFragment() {

    private val notesListViewModel: NotesListViewModel by activityViewModels()

    enum class DialogType {
        CREATE, RENAME
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_note_name, null)
        val noteNameEditText: AppCompatEditText = dialogView.findViewById(R.id.note_name)
        val argDialogType = requireArguments().getString(ARG_DIALOG_TYPE) ?: ""

        val dialogTitle: Int
        val positiveButtonText: Int
        val broadcastIntent: Intent
        when (DialogType.valueOf(argDialogType)) {
            DialogType.CREATE -> {
                dialogTitle = R.string.new_note
                positiveButtonText = R.string.create_new_note
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
            .setNegativeButton(R.string.cancel_note, null)
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

                    broadcastIntent.let {
                        it.putExtra(NOTE_NAME, noteName)
                        requireContext().sendBroadcast(it)
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
        fun newInstance(dialogType: DialogType): NoteNameDialog {
            return NoteNameDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIALOG_TYPE, dialogType.name)
                }
            }
        }
    }
}