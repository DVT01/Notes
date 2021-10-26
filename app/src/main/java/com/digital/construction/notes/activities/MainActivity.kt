package com.digital.construction.notes.activities

import android.content.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.digital.construction.notes.R
import com.digital.construction.notes.fragments.*
import timber.log.Timber

private const val TAG = "MainActivity"
private const val REQUEST_NOTE_ID = "note_id_request"

const val ACTION_OPEN_SETTINGS = "com.digital.construction.notes.open_settings"
const val ACTION_OPEN_ABOUT = "com.digital.construction.notes.open_about"

class MainActivity : AppCompatActivity(), FragmentResultListener {

    private lateinit var sharedPreferences: SharedPreferences

    private val openSettings: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Timber.i("Received request to open settings")

                openFragmentWithFadeAnim(SettingsFragment())
            }
        }
    }
    private val openAbout: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Timber.i("Received request to open about")

                openFragmentWithFadeAnim(AboutFragment())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.tag(TAG)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        supportFragmentManager.setFragmentResultListener(
            REQUEST_NOTE_ID,
            this,
            this
        )

        lifecycle.addObserver(LifecycleObserver())

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)

        if (currentFragment == null) {
            val notesListFragment = NotesListFragment.newInstance(REQUEST_NOTE_ID)
            supportFragmentManager.commit {
                add(R.id.fragment_container_view, notesListFragment)
            }
        }

        sharedPreferences.getBoolean(INTRODUCTION_SEEN_KEY, false).let { introductionSeen ->
            if (!introductionSeen) {
                startActivity(Intent(this, MainIntroActivity::class.java))
                sharedPreferences.edit {
                    putBoolean(INTRODUCTION_SEEN_KEY, true)
                }
            }
        }
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            REQUEST_NOTE_ID -> {
                val noteId = result.getLong(REQUEST_NOTE_ID)
                Timber.i("Opening note (id: $noteId)")

                val noteFragment = NoteFragment.newInstance(noteId)
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

    private fun openFragmentWithFadeAnim(fragment: Fragment) {
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            replace(R.id.fragment_container_view, fragment)
            addToBackStack(null)
        }
    }

    internal inner class LifecycleObserver : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            registerReceiver(openSettings, IntentFilter(ACTION_OPEN_SETTINGS))
            registerReceiver(openAbout, IntentFilter(ACTION_OPEN_ABOUT))
        }

        override fun onDestroy(owner: LifecycleOwner) {
            unregisterReceiver(openSettings)
            unregisterReceiver(openAbout)
        }
    }
}