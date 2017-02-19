package com.arcuscomputing.dictionary;

import java.util.Arrays;
import java.util.List;

public class DictionaryConstants {


    public static final long SEARCH_SLEEP_TIME = 200;

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
    public static final int QUICK_MIN_SEARCH_LENGTH = 2;

    // FILE RELATED
    public static final int BUFFER_4096 = 4096;

    public static final List<String> SIMPLE_WORDS =
            Arrays.asList("than", "that", "with", "which",
                    "goes", "whose", "what", "where",
                    "when", "they", "from", "your",
                    "into");
    public static final String CLEAN_PATTERN = "\\(|\\)|;|\\.|'|`|,|\"";
    public static final String INITIAL_WORD = "initialWord";

    public static final int VOICE_REQUEST_CODE = 8080;

    @SuppressWarnings("SpellCheckingInspection")
    public static List<String> adultWords =
            Arrays.asList
                    (
                            "nigger", "nigga", "spade", "coon", "jigaboo", "nigra",
                            "cunt", "bitch", "bitchy", "bitchery", "bitchiness", "whore",
                            "whoredom", "whorehouse", "prostitute", "cocotte", "harlot",
                            "bawd", "tart", "prostitution", "harlotry", "brothel", "bordello",
                            "bagnio", "house of ill repute", "house of prostitution", "whoremaster",
                            "whoremonger", "whoreson", "bastard", "by-blow", "love child", "dickhead",
                            "asshole", "cocksucker", "shit", "mother fucker", "motherfucker", "fellatio",
                            "cunnilingus", "fellation", "clitoris", "vagina", "penis", "cunnilinctus",
                            "vulva", "shit", "shite", "shitless", "shitlist", "shitter", "shitting",
                            "shitty", "shitwork", "anus", "nipple", "lingerie", "intimate apparel",
                            "knickers", "faggot", "fagot", "fag", "nance", "pansy", "pussy", "fairy",
                            "queer", "butch", "dike", "dyke", "lesbian", "gay", "sex", "sexy", "sexual",
                            "intercourse", "sexual activity", "arousal", "sexual arousal", "sexual love",
                            "sex act", "dildo", "vibrator", "vaginal", "vibrator", "cock", "anal",
                            "anal sex", "anal intercourse", "buggery", "sodomy", "fuck", "fucker", "fucked",
                            "fuckup", "fuck all", "fuck off", "fucked-up", "fuckhead", "rape", "ravish",
                            "violate", "assault", "dishonor", "raped", "ravaged", "despoiled", "rapist",
                            "murder", "raper", "ravishing", "ravisher", "fanny", "butt", "buttocks", "arse",
                            "bum", "nazi", "ku klux klan", "ku kluxer", "kkk", "klan", "boche", "kraut",
                            "krauthead", "jerry", "hun", "caffer", "caffre", "chink", "chinaman", "coloured",
                            "coloured person", "coolie", "cooley", "dago", "wop", "ginzo", "guinea", "greaseball",
                            "darkey", "darky", "darkie", "greaser", "wetback", "taco", "half-breed", "honky",
                            "whitey", "honkey", "honkie", "hymie", "kike", "sheeny", "yid", "injun", "redskin",
                            "red man", "jap", "nip", "mick", "paddy", "mickey", "oriental", "papist", "popish",
                            "picaninny", "piccaninny", "poof", "white trash", "poove", "popery", "shegetz",
                            "spic", "spick", "spik", "uncle tom", "tom", "wog", "yellow man", "yellow woman"

                    );

}
