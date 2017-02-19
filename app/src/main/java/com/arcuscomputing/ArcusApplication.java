package com.arcuscomputing;

import android.app.Application;

import com.arcuscomputing.dictionary.io.ArcusDictionary;
import com.arcuscomputing.dictionary.io.DataFileManager;
import com.arcuscomputing.dictionarypro.parent.R;
import com.google.android.gms.ads.MobileAds;

import timber.log.Timber;

/**
 * Created by barry on 12/07/2016.
 */
public class ArcusApplication extends Application {

    private static ArcusApplication instance;

    private static DataFileManager dataFileManager;

    private static ArcusDictionary dictionary;

    public static ArcusApplication getInstance() {
        return instance;
    }

    public static ArcusDictionary getDictionary() {
        return dictionary;
    }

    public static DataFileManager getDataFileManager() {
        return dataFileManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        instance = this;

        dataFileManager = new DataFileManager(getApplicationContext());
        dictionary = new ArcusDictionary(dataFileManager);

        String adUnitId = getString(R.string.ad_unit_id);

        if (adUnitId != null && adUnitId.trim().length() > 0) {
            MobileAds.initialize(getApplicationContext(), adUnitId);
        }
    }
}
