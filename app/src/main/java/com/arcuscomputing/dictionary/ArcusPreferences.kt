package com.arcuscomputing.dictionary

import android.content.Context
import android.preference.PreferenceManager

@Suppress("DEPRECATION")
class ArcusPreferences(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val isPureAlpha: Boolean get() = preferences.getBoolean(ALPHA_SORT_KEY, false)
    val isMoneyWarningShown: Boolean get() = preferences.getBoolean(MONEY_WARNING_KEY, false)
    val useUSEnglish: Boolean get() = preferences.getBoolean(LANG_KEY, false)
    val useAutoCorrect: Boolean get() = preferences.getBoolean(AUTOCORRECT_KEY, false)
    val isInternetDisabled: Boolean get() = preferences.getBoolean(WEB_KEY, false)

    fun setMoneyWarningShown() {
        preferences.edit().putBoolean(MONEY_WARNING_KEY, true).apply()
    }

    companion object {
        private const val ALPHA_SORT_KEY = "alphasort"
        private const val MONEY_WARNING_KEY = "moneyWarning"
        private const val LANG_KEY = "lang"
        private const val AUTOCORRECT_KEY = "autocorrect"
        private const val WEB_KEY = "web"
    }
}
