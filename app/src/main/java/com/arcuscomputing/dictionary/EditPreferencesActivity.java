package com.arcuscomputing.dictionary;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.arcuscomputing.dictionarypro.ads.R;


public class EditPreferencesActivity extends PreferenceActivity {
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

}
