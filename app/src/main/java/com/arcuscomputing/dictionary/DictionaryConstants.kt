package com.arcuscomputing.dictionary

object DictionaryConstants {

    const val SEARCH_SLEEP_TIME = 200L

    const val NOUN = "0"
    const val VERB = "2"
    const val ADJECTIVE = "1"
    const val ADVERB = "3"

    const val NOUN_LABEL = "Noun"
    const val VERB_LABEL = "Verb"
    const val ADJECTIVE_LABEL = "Adjective"
    const val ADVERB_LABEL = "Adverb"

    const val NOUN_LABEL_ABV = "(n.)"
    const val VERB_LABEL_ABV = "(v.)"
    const val ADJECTIVE_LABEL_ABV = "(adj.)"
    const val ADVERB_LABEL_ABV = "(adv.)"

    const val TAGCOUNT_INDEX = 1
    const val WORD_INDEX = 0
    const val OFFSET_INDEX = 1

    const val FIELD_SEPARATOR = "\t"

    const val QUICK_MAX_READAHEAD = 200
    const val QUICK_MAX_TO_RETURN = 40
    const val QUICK_MIN_SEARCH_LENGTH = 2

    const val BUFFER_4096 = 4096

    val SIMPLE_WORDS = listOf(
        "than", "that", "with", "which",
        "goes", "whose", "what", "where",
        "when", "they", "from", "your", "into"
    )

    const val CLEAN_PATTERN = "\\(|\\)|;|\\.|'|`|,|\""
    const val INITIAL_WORD = "initialWord"
    const val VOICE_REQUEST_CODE = 8080
}
