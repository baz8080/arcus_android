package com.arcuscomputing.models;

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

    public WordModel() {
        this("", "", 0, "", "");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public int getTagCount() {
        return tagCount;
    }

    @Override
    public int compareTo(@NonNull WordModel another) {
        if (this.tagCount < another.tagCount) {
            return 1;
        } else if (this.tagCount == another.tagCount) {
            return 0;
        } else {
            return -1;
        }
    }
}