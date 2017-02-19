package com.arcuscomputing.test;

import com.arcuscomputing.models.WordModel;

import java.util.ArrayList;
import java.util.List;

public class ArcusDictionaryMockObjects {


    public static List<WordModel> quickResults() {

        List<WordModel> results = new ArrayList<WordModel>();

        results.add(new WordModel("inexorable", "impervious to entreaty, and badass in general", 0));
        results.add(new WordModel("morbo", "a kitten hellbent on destruction", 0));
        results.add(new WordModel("explodenate", "something exploding, only ten times more awesome", 0));
        results.add(new WordModel("computer", "The box usually located to the shiney thing", 0));

        return results;
    }

    public static List<WordModel> quickResultsFromWord(int numWords, String word) {

        List<WordModel> results = new ArrayList<WordModel>();

        for (int i = 0; i < numWords; i++) {
            results.add(new WordModel(word, "the defintion of " + word, 0));
        }

        return results;
    }
}


