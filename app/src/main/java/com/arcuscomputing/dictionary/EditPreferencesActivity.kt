package com.arcuscomputing.dictionary

import android.os.Bundle
import android.preference.PreferenceActivity
import com.arcuscomputing.dictionarypro.ads.R

@Suppress("DEPRECATION")
class EditPreferencesActivity : PreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }
}
