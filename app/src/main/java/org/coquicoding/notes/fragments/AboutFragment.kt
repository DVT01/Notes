package org.coquicoding.notes.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import org.coquicoding.notes.BuildConfig
import org.coquicoding.notes.R

private const val GITHUB = "https://github.com/diego-velez/Notes"
private const val AUTHOR_EMAIL = "DVT12@protonmail.com"

class AboutFragment : Fragment() {

    private lateinit var appVersionTextView: AppCompatTextView
    private lateinit var forkOnGitHubLinearLayoutCompat: LinearLayoutCompat
    private lateinit var writeEmailLinearLayoutCompat: LinearLayoutCompat

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_about, container, false).apply {
            appVersionTextView = findViewById(R.id.app_version)
            forkOnGitHubLinearLayoutCompat = findViewById(R.id.fork_on_github)
            writeEmailLinearLayoutCompat = findViewById(R.id.write_me_an_email)

            appVersionTextView.text = getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

            forkOnGitHubLinearLayoutCompat.setOnClickListener {
                openUrl(GITHUB)
            }

            writeEmailLinearLayoutCompat.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$AUTHOR_EMAIL")
                    putExtra(Intent.EXTRA_EMAIL, AUTHOR_EMAIL)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                }

                startActivity(Intent.createChooser(intent, "Email"))
            }
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

    private fun openUrl(url: String) {
        Uri.parse(url).let { website ->
            Intent(Intent.ACTION_VIEW, website).also { intent ->
                startActivity(intent)
            }
        }
    }
}