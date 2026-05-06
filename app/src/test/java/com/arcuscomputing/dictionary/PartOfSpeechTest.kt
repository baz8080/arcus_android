package com.arcuscomputing.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PartOfSpeechTest {

    @Test fun `fromCode returns correct enum for each valid code`() {
        assertEquals(PartOfSpeech.NOUN, PartOfSpeech.fromCode("0"))
        assertEquals(PartOfSpeech.ADJECTIVE, PartOfSpeech.fromCode("1"))
        assertEquals(PartOfSpeech.VERB, PartOfSpeech.fromCode("2"))
        assertEquals(PartOfSpeech.ADVERB, PartOfSpeech.fromCode("3"))
    }

    @Test fun `fromCode returns null for unknown code`() {
        assertNull(PartOfSpeech.fromCode("4"))
        assertNull(PartOfSpeech.fromCode(""))
        assertNull(PartOfSpeech.fromCode("noun"))
    }

    @Test fun `fromLabel matches label exactly`() {
        assertEquals(PartOfSpeech.NOUN, PartOfSpeech.fromLabel("Noun"))
        assertEquals(PartOfSpeech.ADJECTIVE, PartOfSpeech.fromLabel("Adjective"))
        assertEquals(PartOfSpeech.VERB, PartOfSpeech.fromLabel("Verb"))
        assertEquals(PartOfSpeech.ADVERB, PartOfSpeech.fromLabel("Adverb"))
    }

    @Test fun `fromLabel is case-insensitive`() {
        assertEquals(PartOfSpeech.NOUN, PartOfSpeech.fromLabel("noun"))
        assertEquals(PartOfSpeech.NOUN, PartOfSpeech.fromLabel("NOUN"))
        assertEquals(PartOfSpeech.VERB, PartOfSpeech.fromLabel("vErB"))
    }

    @Test fun `fromLabel returns null for unknown label`() {
        assertNull(PartOfSpeech.fromLabel("Pronoun"))
        assertNull(PartOfSpeech.fromLabel(""))
    }

    @Test fun `fromAbbreviation matches exact abbreviation`() {
        assertEquals(PartOfSpeech.NOUN, PartOfSpeech.fromAbbreviation("(n.)"))
        assertEquals(PartOfSpeech.ADJECTIVE, PartOfSpeech.fromAbbreviation("(adj.)"))
        assertEquals(PartOfSpeech.VERB, PartOfSpeech.fromAbbreviation("(v.)"))
        assertEquals(PartOfSpeech.ADVERB, PartOfSpeech.fromAbbreviation("(adv.)"))
    }

    @Test fun `fromAbbreviation matches when abbreviation is a prefix of input`() {
        assertEquals(PartOfSpeech.NOUN, PartOfSpeech.fromAbbreviation("(n.) something extra"))
        assertEquals(PartOfSpeech.VERB, PartOfSpeech.fromAbbreviation("(v.) to run fast"))
    }

    @Test fun `fromAbbreviation returns null for unknown abbreviation`() {
        assertNull(PartOfSpeech.fromAbbreviation("(pron.)"))
        assertNull(PartOfSpeech.fromAbbreviation(""))
        assertNull(PartOfSpeech.fromAbbreviation("n."))
    }
}
