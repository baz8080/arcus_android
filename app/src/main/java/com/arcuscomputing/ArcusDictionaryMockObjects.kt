package com.arcuscomputing

object ArcusDictionaryMockObjects {
    fun quickResultsFromWord(numWords: Int, word: String): List<WordModel> =
        (0 until numWords).map { WordModel(word, "the defintion of $word", 0) }
}
