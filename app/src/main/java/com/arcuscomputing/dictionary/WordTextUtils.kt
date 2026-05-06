package com.arcuscomputing.dictionary

internal object WordTextUtils {

    internal val SIMPLE_WORDS = setOf(
        "than", "that", "with", "which",
        "goes", "whose", "what", "where",
        "when", "they", "from", "your", "into"
    )

    internal const val CLEAN_PATTERN = "\\(|\\)|;|\\.|'|`|,|\""

    fun capitalize(str: String?): String {
        if (str.isNullOrEmpty()) return str ?: ""
        return str[0].titlecase() + str.substring(1)
    }

    fun getStoredDefinition(word: WordModel, favouritesMode: Boolean): String {
        if (favouritesMode) return word.definition
        val pos = PartOfSpeech.fromLabel(word.type) ?: return word.definition
        return "${pos.abbreviation} ${word.definition}"
    }

    fun isLinkableWord(cleaned: String): Boolean = cleaned.length > 3 && cleaned !in SIMPLE_WORDS
}
