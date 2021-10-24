package com.digital.construction.notes.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import com.digital.construction.notes.R

const val DARK_MODE_KEY = "dark_mode"
const val SORT_MODE_KEY = "sort_mode"
const val FONT_SIZE_KEY = "font_size"
const val SAVE_NOTE_AUTOMATICALLY_KEY = "back_behaviour"
const val INTRODUCTION_SEEN_KEY = "introduction_seen"
const val SWIPE_DELETE_KEY = "swipe_delete"
const val SWIPE_OPEN_KEY = "swipe_open"

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onStart() {
        super.onStart()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            DARK_MODE_KEY -> {
                val darkModeIsOn = sharedPreferences.getBoolean(key, false)

                if (darkModeIsOn) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }
}