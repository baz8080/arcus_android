package com.arcuscomputing.dictionary

import android.content.Context
import androidx.preference.PreferenceManager

class ArcusPreferences(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val isPureAlpha: Boolean get() = preferences.getBoolean(ALPHA_SORT_KEY, false)
    val useUSEnglish: Boolean get() = preferences.getBoolean(LANG_KEY, false)

    companion object {
        private const val ALPHA_SORT_KEY = "alphasort"
        private const val LANG_KEY = "lang"
    }
}
