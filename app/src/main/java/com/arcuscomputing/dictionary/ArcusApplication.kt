package com.arcuscomputing.dictionary

import android.app.Application
import com.arcuscomputing.dictionary.io.ArcusDictionary
import com.arcuscomputing.dictionary.io.DataFileManager
import timber.log.Timber

class ArcusApplication : Application() {

    val dictionary: ArcusDictionary by lazy {
        ArcusDictionary(DataFileManager(this))
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
