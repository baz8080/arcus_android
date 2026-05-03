package com.arcuscomputing.dictionary.io

import android.content.Context
import android.text.TextUtils
import com.arcuscomputing.WordModel
import com.arcuscomputing.dictionary.DictionaryConstants.ADJECTIVE
import com.arcuscomputing.dictionary.DictionaryConstants.ADJECTIVE_LABEL
import com.arcuscomputing.dictionary.DictionaryConstants.ADVERB
import com.arcuscomputing.dictionary.DictionaryConstants.ADVERB_LABEL
import com.arcuscomputing.dictionary.DictionaryConstants.FIELD_SEPARATOR
import com.arcuscomputing.dictionary.DictionaryConstants.NOUN
import com.arcuscomputing.dictionary.DictionaryConstants.NOUN_LABEL
import com.arcuscomputing.dictionary.DictionaryConstants.OFFSET_INDEX
import com.arcuscomputing.dictionary.DictionaryConstants.QUICK_MAX_READAHEAD
import com.arcuscomputing.dictionary.DictionaryConstants.QUICK_MAX_TO_RETURN
import com.arcuscomputing.dictionary.DictionaryConstants.TAGCOUNT_INDEX
import com.arcuscomputing.dictionary.DictionaryConstants.VERB
import com.arcuscomputing.dictionary.DictionaryConstants.VERB_LABEL
import com.arcuscomputing.dictionary.DictionaryConstants.WORD_INDEX
import timber.log.Timber
import java.io.IOException
import java.util.Random
import java.util.regex.Pattern

class ArcusDictionary(private val dataFileManager: DataFileManager) {

    private val pattern = Pattern.compile("^(\\w*?)\\s*?(\\d*?)$")
    private var loaded = false
    private var indexRaf: ReadRandom? = null
    private var definitionsRaf: ReadRandom? = null

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (!loaded) initDatabases(context)
    }

    @Synchronized
    private fun initDatabases(context: Context) {
        if (loaded) return

        val dataFilesExist = when {
            !(dataFileManager.indexFileExists() && dataFileManager.dataFileExists()) ->
                dataFileManager.extractRequiredFiles(context)
            dataFileManager.hashesAreOk() -> true
            else -> dataFileManager.extractRequiredFiles(context)
        }

        if (dataFilesExist) {
            try {
                indexRaf = ReadRandom(dataFileManager.indexFile, "r")
                definitionsRaf = ReadRandom(dataFileManager.dataFile, "r")
            } catch (e: IOException) {
                Timber.e(e, "Unexpected error in initDatabases")
            }
        } else {
            Timber.d("Data file does not exist")
        }

        loaded = dataFilesExist
    }

    @Synchronized
    fun getRandom(): List<WordModel> = getRandom(20, 10)

    @Synchronized
    fun getRandom(maxJump: Int, maxListSize: Int): List<WordModel> {
        if (!checkFiles()) return emptyList()

        val list = mutableListOf<WordModel>()
        try {
            val length = indexRaf!!.length()
            val random = Random(System.currentTimeMillis())
            var i = 0
            while (i < maxJump && list.size < maxListSize) {
                val randomStart = Math.abs(random.nextLong() % length)
                indexRaf!!.seek(randomStart)
                if (indexRaf!!.readLine() != null) {
                    val line = indexRaf!!.readLine() ?: continue
                    if (pattern.matcher(line).matches()) {
                        addResultToList(line, list, definitionsRaf!!)
                    }
                }
                i++
            }
        } catch (e: IOException) {
            Timber.e(e, "Unexpected error in getRandom")
            return emptyList()
        }
        return list
    }

    @Synchronized
    fun getMatches(query: String, pureAlphaSort: Boolean): List<WordModel> {
        if (query.length < 2 || !checkFiles()) return emptyList()

        val list = mutableListOf<WordModel>()
        val q = query.lowercase().trim()

        try {
            val index = indexRaf!!
            var low = 0L
            var high = index.length()

            while (low < high) {
                val mid = (low + high) / 2
                var p = mid
                while (p >= 0) {
                    index.seek(p)
                    if (index.readByte().toInt().toChar() == '\n') break
                    p--
                }
                if (p < 0) index.seek(0)

                val line = index.getNextLine() ?: break
                if (line.substring(0, line.indexOf(FIELD_SEPARATOR)).compareTo(q) < 0) {
                    low = mid + 1
                } else {
                    high = mid
                }
            }

            var p = low
            while (p >= 0) {
                index.seek(p)
                if (index.readByte().toInt().toChar() == '\n') break
                p--
            }
            if (p < 0) index.seek(0)

            while (true) {
                val line = index.getNextLine() ?: break
                if (!line.startsWith(q) || list.size > QUICK_MAX_READAHEAD) break
                addResultToList(line, list, definitionsRaf!!)
            }
        } catch (e: IOException) {
            Timber.e(e, "Unexpected error in getMatches")
        }

        return prepareResults(list, q, pureAlphaSort)
    }

    private fun checkFiles(): Boolean {
        return try {
            if (indexRaf == null) indexRaf = ReadRandom(dataFileManager.indexFile, "r")
            if (definitionsRaf == null) definitionsRaf = ReadRandom(dataFileManager.dataFile, "r")
            true
        } catch (e: IOException) {
            Timber.e(e, "Unexpected error in checkFiles")
            false
        }
    }

    private fun addResultToList(line: String, list: MutableList<WordModel>, defsRandom: ReadRandom) {
        val splitLine = TextUtils.split(line, FIELD_SEPARATOR)
        if (splitLine.size != 2) {
            Timber.e("Unexpected number of tokens after splitting line")
            return
        }
        try {
            defsRandom.seek(splitLine[OFFSET_INDEX].toLong())
            val defAndTagCount = defsRandom.readLine().split(FIELD_SEPARATOR)
            if (defAndTagCount.size != 2) {
                Timber.e("Unexpected number of tokens after splitting defAndTagCount")
                return
            }
            val tagCount = defAndTagCount[TAGCOUNT_INDEX].toInt()
            val word = splitLine[WORD_INDEX]

            var currentDef: String
            while (defsRandom.readLine().also { currentDef = it ?: "" } != null && currentDef != "") {
                val type = currentDef.substring(0, 1)
                val def = currentDef.substring(1)
                val synonyms = getSynonyms(defsRandom)
                val label = when (type) {
                    NOUN -> NOUN_LABEL
                    VERB -> VERB_LABEL
                    ADVERB -> ADVERB_LABEL
                    ADJECTIVE -> ADJECTIVE_LABEL
                    else -> continue
                }
                list.add(WordModel(word, def, tagCount, synonyms, label))
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in addResultToList")
        }
    }

    private fun getSynonyms(definitionsRaf: ReadRandom): String {
        return try {
            val pointer = definitionsRaf.filePointer
            val currentDef = definitionsRaf.readLine() ?: return ""
            if (currentDef.isEmpty()) {
                definitionsRaf.seek(pointer)
                return ""
            }
            val type = currentDef.substring(0, 1)
            if (type == ADJECTIVE || type == VERB || type == NOUN || type == ADVERB) {
                definitionsRaf.seek(pointer)
                ""
            } else {
                val synonyms = currentDef.replace("|", ", ")
                synonyms.dropLast(2)
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

        val trimmed = if (list.size > QUICK_MAX_TO_RETURN) list.subList(0, QUICK_MAX_TO_RETURN).toMutableList() else list

        if (exactIndex == -1) {
            trimmed.add(0, WordModel(query, "No exact results for $query. Long press here for web searches.", -1))
        }

        return trimmed
    }

    @Synchronized
    fun closeFileHandles() {
        try { indexRaf?.close() } catch (e: IOException) { Timber.e(e, "Exception closing indexRaf") }
        try { definitionsRaf?.close() } catch (e: IOException) { Timber.e(e, "Exception closing definitionsRaf") }
        loaded = false
    }
}
