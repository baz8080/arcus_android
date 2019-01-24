package com.arcuscomputing;

import android.app.Application;

import com.arcuscomputing.dictionary.io.ArcusDictionary;
import com.arcuscomputing.dictionary.io.DataFileManager;

import timber.log.Timber;

/**
 * Created by barry on 12/07/2016.
 */
public class ArcusApplication extends Application {

    private static ArcusDictionary dictionary;

    public static ArcusDictionary getDictionary() {
        return dictionary;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        DataFileManager dataFileManager = new DataFileManager(getApplicationContext());
        dictionary = new ArcusDictionary(dataFileManager);
    }
}
