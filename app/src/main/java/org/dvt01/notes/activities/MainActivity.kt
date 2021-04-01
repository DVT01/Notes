package org.dvt01.notes.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import org.dvt01.notes.R
import org.dvt01.notes.fragments.NoteFragment
import org.dvt01.notes.fragments.NotesListFragment
import org.dvt01.notes.fragments.SettingsFragment

private const val TAG = "MainActivity"
private const val REQUEST_NOTE = "request_note"

const val ACTION_OPEN_SETTINGS = "org.dvt01.notes.open_settings"

class MainActivity : AppCompatActivity(), FragmentResultListener {

    private val openSettings: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received request to open settings")

                val settingsFragment = SettingsFragment()
                supportFragmentManager.commit {
                    replace(R.id.fragment_container_view, settingsFragment)
                    addToBackStack(null)
                }
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
            supportFragmentManager.commit {
                add(R.id.fragment_container_view, fragment)
            }
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
                supportFragmentManager.commit {
                    replace(R.id.fragment_container_view, fragment)
                    addToBackStack(null)
                }
            }
        }
    }
}