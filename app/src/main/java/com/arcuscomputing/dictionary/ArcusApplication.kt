package com.arcuscomputing.dictionary

import android.app.Application
import com.arcuscomputing.dictionary.io.ArcusDictionary
import com.arcuscomputing.dictionary.io.DataFileManager
import com.google.android.material.color.DynamicColors
import timber.log.Timber

class ArcusApplication : Application() {

    val dictionary: ArcusDictionary by lazy {
        ArcusDictionary(DataFileManager(this))
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        Timber.plant(Timber.DebugTree())
    }
}
