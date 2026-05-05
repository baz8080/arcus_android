package com.arcuscomputing.dictionary.io

import com.arcuscomputing.dictionary.PartOfSpeech
import com.arcuscomputing.dictionary.WordModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

private const val FIELD_SEPARATOR = "\t"
private const val QUICK_MAX_READAHEAD = 200
private const val QUICK_MAX_TO_RETURN = 40

class ArcusDictionary(private val dataFileManager: DataFileManager) {

    // Single-threaded dispatcher serialises all access to the mapped buffers
    // below, since each MappedByteBuffer carries mutable position state.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    private var loaded = false
    private var index: MappedFile? = null
    private var definitions: MappedFile? = null

    suspend fun ensureLoaded() = withContext(dispatcher) {
        if (loaded) return@withContext
        if (!dataFileManager.ensureExtracted()) {
            Timber.e("Failed to extract dictionary data files")
            return@withContext
        }
        try {
            index = MappedFile(dataFileManager.indexFile)
            definitions = MappedFile(dataFileManager.dataFile)
            loaded = true
        } catch (e: IOException) {
            Timber.e(e, "Unexpected error opening dictionary files")
        }
    }

    suspend fun getMatches(query: String, pureAlphaSort: Boolean): List<WordModel> = withContext(dispatcher) {
        if (query.length < 2 || !loaded) return@withContext emptyList()

        val list = mutableListOf<WordModel>()
        val q = query.lowercase().trim()

        try {
            val idx = checkNotNull(index)
            val defs = checkNotNull(definitions)
            var low = 0L
            var high = idx.length

            while (low < high) {
                val mid = (low + high) / 2
                var p = mid
                while (p >= 0) {
                    idx.seek(p)
                    if (idx.readByte().toInt().toChar() == '\n') break
                    p--
                }
                if (p < 0) idx.seek(0)

                val line = idx.readLine() ?: break
                if (line.substring(0, line.indexOf(FIELD_SEPARATOR)).compareTo(q) < 0) {
                    low = mid + 1
                } else {
                    high = mid
                }
            }

            var p = low
            while (p >= 0) {
                idx.seek(p)
                if (idx.readByte().toInt().toChar() == '\n') break
                p--
            }
            if (p < 0) idx.seek(0)

            while (true) {
                val line = idx.readLine() ?: break
                if (!line.startsWith(q) || list.size > QUICK_MAX_READAHEAD) break
                addResultToList(line, list, defs)
            }
        } catch (e: IOException) {
            Timber.e(e, "Unexpected error in getMatches")
        }

        prepareResults(list, q, pureAlphaSort)
    }

    private fun addResultToList(line: String, list: MutableList<WordModel>, defs: MappedFile) {
        val splitLine = line.split(FIELD_SEPARATOR)
        if (splitLine.size != 2) {
            Timber.e("Unexpected number of tokens after splitting line")
            return
        }
        try {
            defs.seek(splitLine[1].toLong())
            val defAndTagCount = defs.readLine()?.split(FIELD_SEPARATOR) ?: return
            if (defAndTagCount.size != 2) {
                Timber.e("Unexpected number of tokens after splitting defAndTagCount")
                return
            }
            val tagCount = defAndTagCount[1].toInt()
            val word = splitLine[0]

            var currentDef: String
            while (defs.readLine().also { currentDef = it ?: "" } != null && currentDef != "") {
                val pos = PartOfSpeech.fromCode(currentDef.substring(0, 1)) ?: continue
                val def = currentDef.substring(1)
                val synonyms = getSynonyms(defs)
                list.add(WordModel(word, def, tagCount, synonyms, pos.label))
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in addResultToList")
        }
    }

    private fun getSynonyms(defs: MappedFile): String {
        return try {
            val pointer = defs.position
            val currentDef = defs.readLine() ?: return ""
            if (currentDef.isEmpty()) {
                defs.seek(pointer)
                return ""
            }
            if (PartOfSpeech.fromCode(currentDef.substring(0, 1)) != null) {
                defs.seek(pointer)
                ""
            } else {
                currentDef.replace("|", ", ").dropLast(2)
            }
        } catch (e: IOException) {
            Timber.e(e, "Error getting synonyms")
            ""
        }
    }

    private fun prepareResults(list: MutableList<WordModel>, query: String, pureAlphaSort: Boolean): List<WordModel> {
        if (!pureAlphaSort) list.sort()

        val exactIndex = list.indexOfFirst { it.word == query }
        if (exactIndex > 0) list.add(0, list.removeAt(exactIndex))

        return if (list.size > QUICK_MAX_TO_RETURN) list.subList(0, QUICK_MAX_TO_RETURN) else list
    }
}
