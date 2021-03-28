package org.obebeokeke.notes.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentResultListener
import org.obebeokeke.notes.R
import org.obebeokeke.notes.fragments.NoteFragment
import org.obebeokeke.notes.fragments.NotesListFragment
import org.obebeokeke.notes.fragments.SettingsFragment

private const val TAG = "MainActivity"
private const val REQUEST_NOTE = "request_note"

const val ACTION_OPEN_SETTINGS = "org.obebeokeke.notes.open_settings"

class MainActivity : AppCompatActivity(), FragmentResultListener {

    private val openSettings: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received request to open settings")

                val settingsFragment = SettingsFragment()
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container_view, settingsFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.setFragmentResultListener(
            REQUEST_NOTE,
            this,
            this
        )

        val settingsIntentFilter = IntentFilter(ACTION_OPEN_SETTINGS)
        registerReceiver(openSettings, settingsIntentFilter)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)

        if (currentFragment == null) {
            val fragment = NotesListFragment.newInstance(REQUEST_NOTE)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container_view, fragment)
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(openSettings)
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