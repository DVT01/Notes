package org.obebeokeke.notes.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.obebeokeke.notes.R

private const val TAG = "NoteNameDialog"
private const val ARG_NOTE_NAME = "requestKey"

class NoteNameDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            val layoutInflater = requireActivity().layoutInflater
            val view = layoutInflater.inflate(R.layout.dialog_note_name, null)

            val noteNameEditText = view.findViewById<EditText>(R.id.note_name)

            builder
                .setView(view)
                .setTitle(R.string.new_note)
                .setPositiveButton(R.string.create_new_note) { _, _ ->
                    dialog?.dismiss()
                    val noteName = noteNameEditText.text.toString()

                    Log.i(TAG, "Note name: $noteName")

                    val requestKey = requireArguments().getString(ARG_NOTE_NAME, "")
                    val bundle = Bundle().apply { putString(requestKey, noteName) }
                    parentFragmentManager.setFragmentResult(requestKey, bundle)
                }
                .setNegativeButton(R.string.cancel_new_note) { _, _ ->
                    dialog?.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        fun newInstance(requestKey: String): NoteNameDialog {
            val args = Bundle().apply {
                putString(ARG_NOTE_NAME, requestKey)
            }
            return NoteNameDialog().apply {
                arguments = args
            }
        }
    }
}