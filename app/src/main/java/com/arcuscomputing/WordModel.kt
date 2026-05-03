package com.arcuscomputing

data class WordModel(
    var word: String = "",
    var definition: String = "",
    var tagCount: Int = 0,
    var synonyms: String = "",
    var type: String = ""
) : Comparable<WordModel> {

    constructor(word: String, definition: String, tagCount: Int) : this(word, definition, tagCount, "", "")

    override fun compareTo(other: WordModel): Int = other.tagCount.compareTo(tagCount)
}
