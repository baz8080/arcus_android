package com.arcuscomputing.dictionary;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ArcusPreferences {

    private final static String ALPHA_SORT_KEY = "alphasort";

    private final static String MONEY_WARNING_KEY = "moneyWarning";

    private final static String LANG_KEY = "lang";

    private final static String WEB_KEY = "web";

    private final SharedPreferences preferences;

    public ArcusPreferences(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isPureAlpha() {
        return preferences.getBoolean(ALPHA_SORT_KEY, false);
    }

    public boolean isMoneyWarningShown() {
        return preferences.getBoolean(MONEY_WARNING_KEY, false);
    }

    public void setMoneyWarningShown() {
        preferences.edit().putBoolean(MONEY_WARNING_KEY, true).apply();
    }

    public boolean useUSEnglish() {
        return preferences.getBoolean(LANG_KEY, false);
    }

    public boolean useAutoCorrect() {
        return preferences.getBoolean(LANG_KEY, false);
    }

    public boolean isInternetDisabled() {
        return preferences.getBoolean(WEB_KEY, false);
    }
}
