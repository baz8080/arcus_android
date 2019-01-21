package com.arcuscomputing.dictionary.io;

import android.content.Context;
import android.text.TextUtils;

import com.arcuscomputing.WordModel;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

import static com.arcuscomputing.dictionary.DictionaryConstants.ADJECTIVE;
import static com.arcuscomputing.dictionary.DictionaryConstants.ADJECTIVE_LABEL;
import static com.arcuscomputing.dictionary.DictionaryConstants.ADVERB;
import static com.arcuscomputing.dictionary.DictionaryConstants.ADVERB_LABEL;
import static com.arcuscomputing.dictionary.DictionaryConstants.FIELD_SEPARATOR;
import static com.arcuscomputing.dictionary.DictionaryConstants.NOUN;
import static com.arcuscomputing.dictionary.DictionaryConstants.NOUN_LABEL;
import static com.arcuscomputing.dictionary.DictionaryConstants.OFFSET_INDEX;
import static com.arcuscomputing.dictionary.DictionaryConstants.QUICK_MAX_READAHEAD;
import static com.arcuscomputing.dictionary.DictionaryConstants.QUICK_MAX_TO_RETURN;
import static com.arcuscomputing.dictionary.DictionaryConstants.TAGCOUNT_INDEX;
import static com.arcuscomputing.dictionary.DictionaryConstants.VERB;
import static com.arcuscomputing.dictionary.DictionaryConstants.VERB_LABEL;
import static com.arcuscomputing.dictionary.DictionaryConstants.WORD_INDEX;


public class ArcusDictionary {

    private final Pattern p = Pattern.compile("^(\\w*?)\\s*?(\\d*?)$");
    private final DataFileManager dataFileManager;
    private boolean loaded = false;
    private ReadRandom indexRaf;
    private ReadRandom definitionsRaf;
    private List<WordModel> list;

    public ArcusDictionary(DataFileManager dfm) {
        this.dataFileManager = dfm;
    }

    public synchronized void ensureLoaded(Context context) {
        if (loaded) {
            return;
        }

        initDatabases(context);
    }

    private synchronized void initDatabases(Context context) {

        if (loaded) {
            return;
        }

        boolean dataFilesExist = false;

        if (!(dataFileManager.indexFileExists() && dataFileManager.dataFileExists())) {
            // copy required files if we can write
            if (dataFileManager.extractRequiredFiles(context)) {

                dataFilesExist = true;
            }
        } else {
            // exist already, need to check MD5s

            if (dataFileManager.hashesAreOk()) {
                dataFilesExist = true;
            } else {

                if (dataFileManager.extractRequiredFiles(context)) {
                    dataFilesExist = true;
                }
            }
        }

        if (dataFilesExist) {

            try {
                indexRaf = new ReadRandom(dataFileManager.getIndexFile(), "r");
                definitionsRaf = new ReadRandom(dataFileManager.getDataFile(), "r");
            } catch (IOException e) {
                Timber.e(e, "Unexpected error in initDatabases");
            }
        } else {
            Timber.d("Data file does not exist");
        }

        loaded = dataFilesExist;
    }



    public synchronized List<WordModel> getRandom() {
        return getRandom(20, 10);
    }

    synchronized List<WordModel> getRandom(int maxJump, int maxListSize) {

        if (!checkFiles()) {
            return Collections.emptyList();
        }

        list = new LinkedList<>();

        try {
            long length = indexRaf.length();
            long randomStart;
            Random random = new Random(System.currentTimeMillis());

            for (int i = 0; i < maxJump && list.size() < maxListSize; i++) {
                randomStart = Math.abs(random.nextLong() % length);
                indexRaf.seek(randomStart);
                String line;

                if (indexRaf.readLine() != null && ((line = indexRaf.readLine()) != null)) {
                    Matcher m = p.matcher(line);

                    if (m.matches()) {
                        addResultToList(line, list, this.definitionsRaf);
                    }
                }
            }
        } catch (IOException e) {
            Timber.e(e, "Unexpected error in getRandom");
            return Collections.emptyList();
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<WordModel> getMatches(String query, boolean pureAlphaSort) {

        if (query.length() < 2 || !checkFiles()) {
            return Collections.emptyList();
        }

        list = new LinkedList<>();
        query = query.toLowerCase().trim();

        String line;


        try {
            long low = 0;
            long high = indexRaf.length();

            long p;
            while (low < high) {
                long mid = (low + high) / 2;
                p = mid;
                while (p >= 0) {
                    indexRaf.seek(p);

                    char c = (char) indexRaf.readByte();

                    if (c == '\n') {
                        break;
                    }
                    p--;
                }

                if (p < 0) {
                    indexRaf.seek(0);
                }

                line = indexRaf.getNextLine();

                //noinspection ConstantConditions
                if (line.substring(0, line.indexOf(FIELD_SEPARATOR)).compareTo(query) < 0) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }

            p = low;
            while (p >= 0) {
                indexRaf.seek(p);
                if (((char) indexRaf.readByte()) == '\n') {
                    break;
                }
                p--;
            }

            if (p < 0) {
                indexRaf.seek(0);
            }

            while (true) {

                line = indexRaf.getNextLine();

                if (line == null || !line.startsWith(query) || list.size() > QUICK_MAX_READAHEAD) {
                    break;
                }

                addResultToList(line, list, definitionsRaf);
            }
        } catch (IOException e) {
            Timber.e(e, "Unexpected error in getMatches");
        }


        list = prepareResults(list, query, pureAlphaSort);

        return list == null ? Collections.<WordModel>emptyList() : list;
    }

    private boolean checkFiles() {

        try {
            if (indexRaf == null) {
                indexRaf = new ReadRandom(dataFileManager.getIndexFile(), "r");
            }

            if (definitionsRaf == null) {
                definitionsRaf = new ReadRandom(dataFileManager.getDataFile(), "r");
            }

        } catch (IOException e) {
            Timber.e(e, "Unexpected error in checkFiles");
            return false;
        }

        return true;
    }

    private void addResultToList(String line, List<WordModel> list, ReadRandom defsRandom) {
        String[] splitLine = TextUtils.split(line, FIELD_SEPARATOR);

        if (splitLine.length == 2) {

            try {
                defsRandom.seek(Long.valueOf(splitLine[OFFSET_INDEX]));

                String[] defAndTagCount = defsRandom.readLine().split(FIELD_SEPARATOR);

                if (defAndTagCount.length == 2) {

                    StringBuilder definitions = new StringBuilder(75);
                    String currentDef;
                    String synonyms;

                    while (!((currentDef = defsRandom.readLine()).equals(""))) {

                        // Get the type, first char in line
                        String type = currentDef.substring(0, 1);

                        // Reset string builder and synonyms
                        definitions.delete(0, definitions.length());

                        switch (type) {
                            case NOUN:
                                definitions.append(currentDef.substring(1));
                                synonyms = getSynonyms(defsRandom);
                                list.add(new WordModel(splitLine[WORD_INDEX], definitions.toString(), Integer.parseInt(defAndTagCount[TAGCOUNT_INDEX]), synonyms, NOUN_LABEL));
                                break;
                            case VERB:
                                definitions.append(currentDef.substring(1));
                                synonyms = getSynonyms(defsRandom);
                                list.add(new WordModel(splitLine[WORD_INDEX], definitions.toString(), Integer.parseInt(defAndTagCount[TAGCOUNT_INDEX]), synonyms, VERB_LABEL));
                                break;
                            case ADVERB:
                                definitions.append(currentDef.substring(1));
                                synonyms = getSynonyms(defsRandom);
                                list.add(new WordModel(splitLine[WORD_INDEX], definitions.toString(), Integer.parseInt(defAndTagCount[TAGCOUNT_INDEX]), synonyms, ADVERB_LABEL));
                                break;
                            case ADJECTIVE:
                                definitions.append(currentDef.substring(1));
                                synonyms = getSynonyms(defsRandom);
                                list.add(new WordModel(splitLine[WORD_INDEX], definitions.toString(), Integer.parseInt(defAndTagCount[TAGCOUNT_INDEX]), synonyms, ADJECTIVE_LABEL));
                                break;
                        }

                    } // end while

                } else {
                    Timber.e("Unexpected number of tokens after splitting defAndTagCount");
                }

            } catch (Exception e) {
                Timber.e(e, "Unexpected error in addResultToList");
            }

        } else {
            Timber.e("Unexpected number of tokens after splitting line");
        }
    }

    private String getSynonyms(ReadRandom definitionsRaf) {

        long pointer;
        String synonyms = "";

        try {
            pointer = definitionsRaf.getFilePointer();
            String currentDef = definitionsRaf.readLine();

            if (!currentDef.equals("")) {

                String type = currentDef.substring(0, 1);

                if (type.equals(ADJECTIVE) || type.equals(VERB) || type.equals(NOUN) || type.equals(ADVERB)) {
                    // We have gone onto another definition, reset the pointer
                    definitionsRaf.seek(pointer);
                } else {
                    // We have synonyms
                    synonyms = currentDef.replaceAll("\\|", ", ");
                    synonyms = synonyms.substring(0, synonyms.length() - 2);
                }
            } else {
                definitionsRaf.seek(pointer);
            }

        } catch (IOException e) {
            Timber.e(e, "Error getting synonyms");
        }

        return synonyms;
    }

    private List<WordModel> prepareResults(List<WordModel> list, String query, boolean pureAlphaSort) {

        // Sort the list, then promote any exact query match to the front, despite tag count.
        if (!pureAlphaSort) {
            Collections.sort(list);
        }

        // only interested in doing this if the word is not exactly matched
        boolean exactMatchFound = false;

        for (int i = 0; i < list.size(); i++) {

            if ((list.get(i).getWord()).equals(query)) {

                exactMatchFound = true;

                if (i > 0) {
                    // remove and promote to the start of the list
                    list.add(0, list.remove(i));
                }
            }
        }

        if (list.size() > QUICK_MAX_TO_RETURN) {
            list = list.subList(0, QUICK_MAX_TO_RETURN);
        }

        // Add a fake word for web lookups
        if (!exactMatchFound) {
            list.add(0, new WordModel(query, "No exact results for " + query + ". Long press here for web searches.", -1));
        }

        return list;
    }

    public synchronized void closeFileHandles() {

        if (indexRaf != null) {
            try {
                indexRaf.close();
            } catch (IOException e) {
                Timber.e(e, "Exception closing indexRaf");
            }
        }

        if (definitionsRaf != null) {
            try {
                definitionsRaf.close();
            } catch (IOException e) {
                Timber.e(e, "Exception closing definitionsRaf");
            }
        }

        loaded = false;
    }

}
