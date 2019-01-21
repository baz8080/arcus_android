package com.arcuscomputing;

import android.support.annotation.NonNull;

public class WordModel implements Comparable<WordModel> {

    private String word;
    private String definition;
    private int tagCount;
    private String synonyms;
    private String type;

    public WordModel(String word, String definition, int tagCount, String synonyms, String type) {
        this.word = word;
        this.definition = definition;
        this.tagCount = tagCount;
        this.synonyms = synonyms;
        this.type = type;
    }

    public WordModel(String word, String definition, int tagCount) {
        this(word, definition, tagCount, "", "");
    }

    WordModel() {
        this("", "", 0, "", "");
    }

    String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    String getSynonyms() {
        return synonyms;
    }

    public String getWord() {
        return word;
    }

    void setWord(String word) {
        this.word = word;
    }

    public String getDefinition() {
        return definition;
    }

    void setDefinition(String definition) {
        this.definition = definition;
    }

    int getTagCount() {
        return tagCount;
    }

    @Override
    public int compareTo(@NonNull WordModel another) {
        return Integer.compare(another.tagCount, this.tagCount);
    }
}