package org.coquicoding.notes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import org.coquicoding.notes.BuildConfig
import org.coquicoding.notes.R

class AboutFragment : Fragment() {

    private lateinit var versionTextView: AppCompatTextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_about, container, false).apply {
            versionTextView = findViewById(R.id.version)

            versionTextView.text = BuildConfig.VERSION_NAME
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.about)
        }
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.app_name)
        }
    }
}