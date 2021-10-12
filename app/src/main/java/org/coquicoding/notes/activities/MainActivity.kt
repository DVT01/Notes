package org.coquicoding.notes.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import org.coquicoding.notes.R
import org.coquicoding.notes.fragments.AboutFragment
import org.coquicoding.notes.fragments.NoteFragment
import org.coquicoding.notes.fragments.NotesListFragment
import org.coquicoding.notes.fragments.SettingsFragment

private const val TAG = "MainActivity"
private const val REQUEST_NOTE_NAME = "note_name_request"

const val ACTION_OPEN_SETTINGS = "org.coquicoding.notes.open_settings"
const val ACTION_OPEN_ABOUT = "org.coquicoding.notes.open_about"

class MainActivity : AppCompatActivity(), FragmentResultListener {

    private val openSettings: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received request to open settings")

                SettingsFragment().let { settings ->
                    supportFragmentManager.commit {
                        setCustomAnimations(
                            R.anim.fade_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.fade_out
                        )
                        replace(R.id.fragment_container_view, settings)
                        addToBackStack(null)
                    }
                }
            }
        }
    }
    private val openAbout: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received request to open about page")

                AboutFragment().let { about ->
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container_view, about)
                        addToBackStack(null)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.setFragmentResultListener(
            REQUEST_NOTE_NAME,
            this,
            this
        )

        registerReceiver(openSettings, IntentFilter(ACTION_OPEN_SETTINGS))
        registerReceiver(openAbout, IntentFilter(ACTION_OPEN_ABOUT))

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)

        if (currentFragment == null) {
            val notesListFragment = NotesListFragment.newInstance(REQUEST_NOTE_NAME)
            supportFragmentManager.commit {
                add(R.id.fragment_container_view, notesListFragment)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(openSettings)
        unregisterReceiver(openAbout)
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            REQUEST_NOTE_NAME -> {
                val noteName = result.getString(REQUEST_NOTE_NAME, "")
                Log.i(TAG, "Request to open note: $noteName")

                val noteFragment = NoteFragment.newInstance(noteName)
                supportFragmentManager.commit {
                    setCustomAnimations(
                        R.anim.slide_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out
                    )
                    replace(R.id.fragment_container_view, noteFragment)
                    addToBackStack(null)
                }
            }
        }
    }
}