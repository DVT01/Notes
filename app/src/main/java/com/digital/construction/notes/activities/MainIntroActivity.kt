package com.digital.construction.notes.activities

import android.os.Bundle
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import com.digital.construction.notes.R

class MainIntroActivity : IntroActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isButtonCtaVisible = true
        isButtonNextVisible = false
        isButtonBackVisible = false

        buttonCtaTintMode = BUTTON_CTA_TINT_MODE_TEXT
        buttonCtaLabel = getString(R.string.get_started)

        addSlide(
            SimpleSlide.Builder()
                .title(R.string.app_name)
                .description(R.string.app_description)
                .image(R.mipmap.ic_launcher_round)
                .background(R.color.colorPrimaryDark)
                .backgroundDark(R.color.colorPrimaryDark)
                .build()
        )

        addSlide(
            SimpleSlide.Builder()
                .title(R.string.swipe_feature)
                .background(R.color.colorPrimaryDark)
                .backgroundDark(R.color.colorPrimaryDark)
                .build()
        )

        addSlide(
            SimpleSlide.Builder()
                .title(R.string.double_tap_feature)
                .background(R.color.colorPrimaryDark)
                .backgroundDark(R.color.colorPrimaryDark)
                .build()
        )

        addSlide(
            SimpleSlide.Builder()
                .title(R.string.import_and_export)
                .description(R.string.import_and_export_description)
                .background(R.color.colorPrimaryDark)
                .backgroundDark(R.color.colorPrimaryDark)
                .build()
        )
    }
}