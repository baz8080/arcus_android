package com.arcuscomputing.dictionary.io

import com.arcuscomputing.dictionary.PartOfSpeech
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import java.io.File

class ArcusDictionaryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dictionary: ArcusDictionary

    @Before
    fun setUp() = runTest {
        val (indexFile, defsFile) = buildTestFiles(tempFolder.root)
        val dataFileManager = mock<DataFileManager>()
        whenever(dataFileManager.ensureExtracted()).thenReturn(true)
        whenever(dataFileManager.indexFile).thenReturn(indexFile)
        whenever(dataFileManager.dataFile).thenReturn(defsFile)
        dictionary = ArcusDictionary(dataFileManager)
        dictionary.ensureLoaded()
    }

    @Test fun `getMatches returns empty list for single-character query`() = runTest {
        assertTrue(dictionary.getMatches("a", false).isEmpty())
    }

    @Test fun `getMatches returns empty list when not yet loaded`() = runTest {
        val dataFileManager = mock<DataFileManager>()
        val unloadedDictionary = ArcusDictionary(dataFileManager)
        assertTrue(unloadedDictionary.getMatches("dog", false).isEmpty())
    }

    @Test fun `getMatches returns prefix matches`() = runTest {
        val results = dictionary.getMatches("do", false)
        assertTrue(results.any { it.word == "dog" })
    }

    @Test fun `getMatches puts exact match first`() = runTest {
        val results = dictionary.getMatches("dog", false)
        assertTrue(results.isNotEmpty())
        assertEquals("dog", results.first().word)
    }

    @Test fun `getMatches returns correct definition text`() = runTest {
        val dog = dictionary.getMatches("dog", false).first { it.word == "dog" }
        assertEquals("a domestic animal", dog.definition)
    }

    @Test fun `getMatches sets correct part-of-speech label`() = runTest {
        val dog = dictionary.getMatches("dog", false).first { it.word == "dog" }
        assertEquals(PartOfSpeech.NOUN.label, dog.type)
    }

    @Test fun `getMatches populates synonyms from definition file`() = runTest {
        val apple = dictionary.getMatches("apple", false).first { it.word == "apple" }
        assertEquals("wolf, fox", apple.synonyms)
    }

    @Test fun `getMatches returns empty synonyms when none present`() = runTest {
        val dog = dictionary.getMatches("dog", false).first { it.word == "dog" }
        assertEquals("", dog.synonyms)
    }

    @Test fun `getMatches returns empty list for non-matching query`() = runTest {
        assertTrue(dictionary.getMatches("xyz", false).isEmpty())
    }

    @Test fun `getMatches caps results at 40`() = runTest {
        val (indexFile, defsFile) = buildLargeTestFiles(tempFolder.newFolder("large"), 50, "word")
        val dataFileManager = mock<DataFileManager>()
        whenever(dataFileManager.ensureExtracted()).thenReturn(true)
        whenever(dataFileManager.indexFile).thenReturn(indexFile)
        whenever(dataFileManager.dataFile).thenReturn(defsFile)
        val bigDict = ArcusDictionary(dataFileManager)
        bigDict.ensureLoaded()
        assertTrue(bigDict.getMatches("word", false).size <= 40)
    }

    // --- helpers ---

    private fun buildTestFiles(dir: File): Pair<File, File> = writeFiles(
        dir,
        listOf(
            DictEntry("apple", 5, "0", "a round red fruit", "wolf|fox|"),
            DictEntry("dog", 8, "0", "a domestic animal", null),
            DictEntry("elephant", 2, "0", "a large grey mammal", null),
        )
    )

    private fun buildLargeTestFiles(dir: File, count: Int, prefix: String): Pair<File, File> =
        writeFiles(dir, (1..count).map { i ->
            DictEntry("${prefix}%05d".format(i), i, "0", "definition $i", null)
        })

    private data class DictEntry(
        val word: String,
        val tagCount: Int,
        val posCode: String,
        val def: String,
        val synonyms: String?
    )

    private fun writeFiles(dir: File, entries: List<DictEntry>): Pair<File, File> {
        dir.mkdirs()
        val defsOut = ByteArrayOutputStream()
        val offsets = mutableMapOf<String, Long>()

        for (entry in entries) {
            offsets[entry.word] = defsOut.size().toLong()
            defsOut.write("${entry.word}\t${entry.tagCount}\n".iso())
            defsOut.write("${entry.posCode}${entry.def}\n".iso())
            if (entry.synonyms != null) defsOut.write("${entry.synonyms}\n".iso())
            defsOut.write("\n".iso())
        }

        val defsFile = File(dir, "wdefs_all.dat").also { it.writeBytes(defsOut.toByteArray()) }

        val indexContent = entries
            .sortedBy { it.word }
            .joinToString("\n") { "${it.word}\t${offsets[it.word]}" } + "\n"
        val indexFile = File(dir, "index.dat").also { it.writeBytes(indexContent.iso()) }

        return indexFile to defsFile
    }

    private fun String.iso() = toByteArray(Charsets.ISO_8859_1)
}
