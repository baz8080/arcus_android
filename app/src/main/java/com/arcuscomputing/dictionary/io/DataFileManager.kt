package com.arcuscomputing.dictionary.io

import android.content.Context
import androidx.annotation.RawRes
import com.arcuscomputing.dictionarypro.ads.R
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.CRC32

class DataFileManager(context: Context) {

    private val dictionaryDataDir: File = requireNotNull(context.getExternalFilesDir(null)) {
        "Couldn't get a reference to external data directory."
    }

    val indexFile = File(dictionaryDataDir, "index.dat")
    val dataFile = File(dictionaryDataDir, "wdefs_all.dat")

    fun indexFileExists() = indexFile.exists()
    fun dataFileExists() = dataFile.exists()

    fun extractRequiredFiles(context: Context): Boolean {
        val indexOk = copyFile(context.resources.openRawResource(R.raw.index), dictionaryDataDir, "index.dat")
        val defsOk = copyFile(context.resources.openRawResource(R.raw.wdefs_all), dictionaryDataDir, "wdefs_all.dat")
        return indexOk && defsOk
    }

    fun hashesAreOk(): Boolean {
        val indexHash = computeCRC32(indexFile)
        val dataHash = computeCRC32(dataFile)
        return if (indexHash != null && dataHash != null) {
            val ok = indexHash == INDEX_HASH && dataHash == DEFINITIONS_HASH
            if (!ok) Timber.e("Hashes are not as expected")
            ok
        } else {
            Timber.e("Hashes are null")
            false
        }
    }

    private fun computeCRC32(file: File): String? {
        val crc = CRC32()
        return try {
            file.inputStream().use { fis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } > 0) {
                    crc.update(buffer, 0, bytesRead)
                }
            }
            java.lang.Long.toHexString(crc.value)
        } catch (e: Exception) {
            null
        }
    }

    private fun copyFile(inputStream: InputStream, directory: File, fileName: String): Boolean {
        return try {
            val destination = File(directory, fileName)
            inputStream.source().buffer().use { source ->
                destination.sink().buffer().use { sink ->
                    sink.writeAll(source)
                }
            }
            true
        } catch (e: IOException) {
            Timber.e(e, "Error copying file %s", fileName)
            false
        }
    }

    companion object {
        private const val INDEX_HASH = "336996dd"
        private const val DEFINITIONS_HASH = "cb10b5de"
        private const val BUFFER_SIZE = 8192
    }
}
