package com.arcuscomputing;

import java.util.ArrayList;
import java.util.List;

public class ArcusDictionaryMockObjects {

    public static List<WordModel> quickResultsFromWord(int numWords, String word) {

        List<WordModel> results = new ArrayList<WordModel>();

        for (int i = 0; i < numWords; i++) {
            results.add(new WordModel(word, "the defintion of " + word, 0));
        }

        return results;
    }
}


