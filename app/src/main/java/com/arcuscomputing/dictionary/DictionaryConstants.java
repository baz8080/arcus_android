package com.arcuscomputing.dictionary;

import java.util.Arrays;
import java.util.List;

public class DictionaryConstants {


    static final long SEARCH_SLEEP_TIME = 200;

    /////////////////////
    // Dictionary Core //
    /////////////////////

    // TYPES
    public static final String NOUN = "0";
    public static final String VERB = "2";
    public static final String ADJECTIVE = "1";
    public static final String ADVERB = "3";

    // LABELS
    public static final String NOUN_LABEL = "Noun";
    public static final String VERB_LABEL = "Verb";
    public static final String ADJECTIVE_LABEL = "Adjective";
    public static final String ADVERB_LABEL = "Adverb";

    // LABELS (Short)
    public static final String NOUN_LABEL_ABV = "(n.)";
    public static final String VERB_LABEL_ABV = "(v.)";
    public static final String ADJECTIVE_LABEL_ABV = "(adj.)";
    public static final String ADVERB_LABEL_ABV = "(adv.)";

    // INDICIES FOR PARSING
    public static final int TAGCOUNT_INDEX = 1;
    public static final int WORD_INDEX = 0;
    public static final int OFFSET_INDEX = 1;

    // SEPARATOR
    public static final String FIELD_SEPARATOR = "\t";

    // QUICK READAHEAD
    public static final int QUICK_MAX_READAHEAD = 200;
    public static final int QUICK_MAX_TO_RETURN = 40;
    static final int QUICK_MIN_SEARCH_LENGTH = 2;

    // FILE RELATED
    public static final int BUFFER_4096 = 4096;

    public static final List<String> SIMPLE_WORDS =
            Arrays.asList("than", "that", "with", "which",
                    "goes", "whose", "what", "where",
                    "when", "they", "from", "your",
                    "into");
    public static final String CLEAN_PATTERN = "\\(|\\)|;|\\.|'|`|,|\"";
    static final String INITIAL_WORD = "initialWord";

    static final int VOICE_REQUEST_CODE = 8080;
}
