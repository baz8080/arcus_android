package com.arcuscomputing.dictionary.io

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.arcuscomputing.dictionarypro.ads.R
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File
import java.io.IOException

class DataFileManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)

    val indexFile = File(appContext.filesDir, "index.dat")
    val dataFile = File(appContext.filesDir, "wdefs_all.dat")

    fun ensureExtracted(): Boolean {
        if (prefs.getInt(VERSION_KEY, 0) == DATA_VERSION
            && indexFile.exists()
            && dataFile.exists()
        ) return true

        val ok = copyResource(R.raw.index, indexFile) && copyResource(R.raw.wdefs_all, dataFile)
        if (ok) prefs.edit { putInt(VERSION_KEY, DATA_VERSION) }
        return ok
    }

    private fun copyResource(resId: Int, destination: File): Boolean = try {
        appContext.resources.openRawResource(resId).source().buffer().use { source ->
            destination.sink().buffer().use { sink -> sink.writeAll(source) }
        }
        true
    } catch (e: IOException) {
        Timber.e(e, "Error copying %s", destination.name)
        false
    }

    companion object {
        // Bump when the bundled data files change so they get re-extracted.
        private const val DATA_VERSION = 1
        private const val VERSION_KEY = "data_version"
    }
}
