package com.arcuscomputing.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordTextUtilsTest {

    // --- capitalize ---

    @Test fun `capitalize uppercases first character`() {
        assertEquals("Hello", WordTextUtils.capitalize("hello"))
    }

    @Test fun `capitalize preserves already-capitalised string`() {
        assertEquals("Hello", WordTextUtils.capitalize("Hello"))
    }

    @Test fun `capitalize returns empty string for empty input`() {
        assertEquals("", WordTextUtils.capitalize(""))
    }

    @Test fun `capitalize returns empty string for null input`() {
        assertEquals("", WordTextUtils.capitalize(null))
    }

    @Test fun `capitalize handles single character`() {
        assertEquals("A", WordTextUtils.capitalize("a"))
    }

    @Test fun `capitalize leaves rest of string unchanged`() {
        assertEquals("hELLO", WordTextUtils.capitalize("hELLO"))
    }

    // --- getStoredDefinition ---

    @Test fun `getStoredDefinition in favourites mode returns definition unchanged`() {
        val word = WordModel(word = "cat", definition = "(n.) a feline", type = "Noun")
        assertEquals("(n.) a feline", WordTextUtils.getStoredDefinition(word, favouritesMode = true))
    }

    @Test fun `getStoredDefinition outside favourites mode prepends part-of-speech abbreviation`() {
        val word = WordModel(word = "cat", definition = "a feline", type = "Noun")
        assertEquals("(n.) a feline", WordTextUtils.getStoredDefinition(word, favouritesMode = false))
    }

    @Test fun `getStoredDefinition with unknown type returns definition unchanged`() {
        val word = WordModel(word = "cat", definition = "a feline", type = "Pronoun")
        assertEquals("a feline", WordTextUtils.getStoredDefinition(word, favouritesMode = false))
    }

    @Test fun `getStoredDefinition uses correct abbreviation for each part of speech`() {
        assertEquals("(adj.) fast", WordTextUtils.getStoredDefinition(
            WordModel(word = "fast", definition = "fast", type = "Adjective"), favouritesMode = false))
        assertEquals("(v.) run", WordTextUtils.getStoredDefinition(
            WordModel(word = "run", definition = "run", type = "Verb"), favouritesMode = false))
        assertEquals("(adv.) quickly", WordTextUtils.getStoredDefinition(
            WordModel(word = "quickly", definition = "quickly", type = "Adverb"), favouritesMode = false))
    }

    // --- isLinkableWord ---

    @Test fun `isLinkableWord returns false for words three characters or shorter`() {
        assertFalse(WordTextUtils.isLinkableWord("the"))
        assertFalse(WordTextUtils.isLinkableWord("cat"))
        assertFalse(WordTextUtils.isLinkableWord("a"))
        assertFalse(WordTextUtils.isLinkableWord(""))
    }

    @Test fun `isLinkableWord returns false for words in the simple words exclusion list`() {
        assertFalse(WordTextUtils.isLinkableWord("that"))
        assertFalse(WordTextUtils.isLinkableWord("with"))
        assertFalse(WordTextUtils.isLinkableWord("from"))
        assertFalse(WordTextUtils.isLinkableWord("they"))
    }

    @Test fun `isLinkableWord returns true for meaningful words longer than three characters`() {
        assertTrue(WordTextUtils.isLinkableWord("house"))
        assertTrue(WordTextUtils.isLinkableWord("canine"))
        assertTrue(WordTextUtils.isLinkableWord("rapid"))
        assertTrue(WordTextUtils.isLinkableWord("bark"))
    }

    @Test fun `isLinkableWord boundary: exactly four characters that are not a simple word`() {
        assertTrue(WordTextUtils.isLinkableWord("bark"))
    }
}
