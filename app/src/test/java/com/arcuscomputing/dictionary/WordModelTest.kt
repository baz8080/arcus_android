package com.arcuscomputing.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordModelTest {

    @Test fun `higher tagCount sorts before lower tagCount`() {
        val rare = WordModel(word = "rare", tagCount = 1)
        val common = WordModel(word = "common", tagCount = 100)
        assertTrue(common < rare)
    }

    @Test fun `equal tagCount is considered equal`() {
        val a = WordModel(word = "alpha", tagCount = 5)
        val b = WordModel(word = "beta", tagCount = 5)
        assertEquals(0, a.compareTo(b))
    }

    @Test fun `sorted list puts highest tagCount first`() {
        val words = listOf(
            WordModel(word = "rare", tagCount = 1),
            WordModel(word = "common", tagCount = 100),
            WordModel(word = "medium", tagCount = 50),
        )
        val sorted = words.sorted()
        assertEquals(listOf("common", "medium", "rare"), sorted.map { it.word })
    }

    @Test fun `zero tagCount sorts last`() {
        val words = listOf(
            WordModel(word = "zero", tagCount = 0),
            WordModel(word = "one", tagCount = 1),
        )
        assertEquals("one", words.sorted().first().word)
    }
}
