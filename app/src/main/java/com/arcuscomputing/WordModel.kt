package com.arcuscomputing

data class WordModel(
    val word: String = "",
    val definition: String = "",
    val tagCount: Int = 0,
    val synonyms: String = "",
    var type: String = ""
) : Comparable<WordModel> {

    override fun compareTo(other: WordModel): Int = other.tagCount.compareTo(tagCount)
}
