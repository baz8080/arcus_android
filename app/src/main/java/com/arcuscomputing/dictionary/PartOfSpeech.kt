package com.arcuscomputing.dictionary

enum class PartOfSpeech(val code: String, val label: String, val abbreviation: String) {
    NOUN("0", "Noun", "(n.)"),
    ADJECTIVE("1", "Adjective", "(adj.)"),
    VERB("2", "Verb", "(v.)"),
    ADVERB("3", "Adverb", "(adv.)");

    companion object {
        fun fromCode(code: String): PartOfSpeech? = entries.firstOrNull { it.code == code }
        fun fromLabel(label: String): PartOfSpeech? = entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
        fun fromAbbreviation(abbr: String): PartOfSpeech? = entries.firstOrNull { abbr.startsWith(it.abbreviation) }
    }
}
