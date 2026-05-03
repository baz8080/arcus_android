package com.arcuscomputing

import android.app.Application
import com.arcuscomputing.dictionary.io.ArcusDictionary
import com.arcuscomputing.dictionary.io.DataFileManager
import timber.log.Timber

class ArcusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        val dataFileManager = DataFileManager(applicationContext)
        dictionary = ArcusDictionary(dataFileManager)
    }

    companion object {
        lateinit var dictionary: ArcusDictionary
            private set
    }
}
