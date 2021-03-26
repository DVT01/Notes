package org.obebeokeke.notes.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentResultListener
import org.obebeokeke.notes.R
import org.obebeokeke.notes.fragments.NoteFragment
import org.obebeokeke.notes.fragments.NotesListFragment

private const val TAG = "MainActivity"
private const val REQUEST_NOTE = "request_note"

class MainActivity : AppCompatActivity(), FragmentResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.setFragmentResultListener(
            REQUEST_NOTE,
            this,
            this
        )

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)

        if (currentFragment == null) {
            val fragment = NotesListFragment.newInstance(REQUEST_NOTE)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container_view, fragment)
                .commit()
        }
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            REQUEST_NOTE -> {
                val noteName = result.getString(REQUEST_NOTE, "")

                val fragment = NoteFragment.newInstance(noteName)
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container_view, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}