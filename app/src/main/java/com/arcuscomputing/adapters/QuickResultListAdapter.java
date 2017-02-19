package com.arcuscomputing.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.arcuscomputing.dictionary.ArcusSearchActivity;

import com.arcuscomputing.dictionarypro.parent.R;
import com.arcuscomputing.models.WordModel;

import java.util.List;

import static com.arcuscomputing.dictionary.DictionaryConstants.ADJECTIVE_LABEL;
import static com.arcuscomputing.dictionary.DictionaryConstants.ADJECTIVE_LABEL_ABV;
import static com.arcuscomputing.dictionary.DictionaryConstants.ADVERB_LABEL;
import static com.arcuscomputing.dictionary.DictionaryConstants.ADVERB_LABEL_ABV;
import static com.arcuscomputing.dictionary.DictionaryConstants.CLEAN_PATTERN;
import static com.arcuscomputing.dictionary.DictionaryConstants.NOUN_LABEL;
import static com.arcuscomputing.dictionary.DictionaryConstants.NOUN_LABEL_ABV;
import static com.arcuscomputing.dictionary.DictionaryConstants.SIMPLE_WORDS;
import static com.arcuscomputing.dictionary.DictionaryConstants.VERB_LABEL;
import static com.arcuscomputing.dictionary.DictionaryConstants.VERB_LABEL_ABV;

public class QuickResultListAdapter implements ListAdapter {

    private List<WordModel> results;
    private LayoutInflater mInflater;
    private ArcusSearchActivity activity;
    public QuickResultListAdapter(List<WordModel> results, ArcusSearchActivity parentActivity) {
        mInflater = (LayoutInflater) parentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.activity = parentActivity;
        this.results = results;
    }

    private static String capitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuilder(strLen)
                .append(Character.toTitleCase(str.charAt(0)))
                .append(str.substring(1))
                .toString();
    }

    public List<WordModel> getResults() {
        return results;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int arg0) {
        return false;
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public Object getItem(int position) {
        return results.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return IGNORE_ITEM_VIEW_TYPE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final View view = (convertView != null) ? convertView : createView(parent);

        final WordModel word = results.get(position);

        /** Word */
        TextView tv = (TextView) view.findViewById(R.id.definition_tv_headline);
        tv.setText(capitalize(word.getWord()));

        /** Definition */
        String def = word.getDefinition();

        if (activity.inFavouritesMode()) {
            if (def.startsWith(NOUN_LABEL_ABV)) {
                def = def.substring(def.indexOf(" ") + 1);
                word.setType(NOUN_LABEL);
            } else if (def.startsWith(VERB_LABEL_ABV)) {
                def = def.substring(def.indexOf(" ") + 1);
                word.setType(VERB_LABEL);
            } else if (def.startsWith(ADVERB_LABEL_ABV)) {
                def = def.substring(def.indexOf(" ") + 1);
                word.setType(ADVERB_LABEL);
            } else if (def.startsWith(ADJECTIVE_LABEL_ABV)) {
                word.setType(ADJECTIVE_LABEL);
                def = def.substring(def.indexOf(" ") + 1);
            }
        }

        tv = (TextView) view.findViewById(R.id.definition_tv_definition);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(capitalize(def), BufferType.SPANNABLE);

        /** Type */
        tv = (TextView) view.findViewById(R.id.definition_tv_type);
        tv.setText(capitalize(word.getType()));

        if (word.getTagCount() == -1) {
            tv.setText("Web");
        }

        /** Synonyms */
        tv = (TextView) view.findViewById(R.id.definition_tv_synonyms);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        if (word.getSynonyms().length() > 0) {
            String synString = "Synonyms: " + word.getSynonyms();
            tv.setText(synString, BufferType.SPANNABLE);
        } else {
            tv.setVisibility(TextView.GONE);
        }

        ImageView iv = (ImageView) view.findViewById(R.id.FavIcon);

        if (word.getTagCount() == -1) {
            iv.setVisibility(View.GONE);
        }

        String d = getDefinition(word);

        if (isFavourited(word.getWord(), d)) {
            iv.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.ic_star_white_24dp));
        } else {
            iv.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.ic_star_border_white_24dp));
        }

        iv.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {

                String d = getDefinition(word);

                if (isFavourited(word.getWord(), d)) {

                    activity.getDbHelper().deleteFromFavourites(word.getWord(), d);

                    view.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.ic_star_border_white_24dp));

                    if (activity.inFavouritesMode()) {
                        activity.refreshFavourites();
                    }

                } else {
                    view.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.ic_star_white_24dp));
                    activity.getDbHelper().insertfavourite(word.getWord(), d);
                }

            }
        });

        ImageView ttsIv = (ImageView) view.findViewById(R.id.TtsIcon);

        if (word.getTagCount() == -1) {
            ttsIv.setVisibility(View.GONE);
        }

        ttsIv.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                activity.speak(word.getWord());
            }
        });

        ImageView shareIv = (ImageView) view.findViewById(R.id.ShareIcon);

        if (word.getTagCount() == -1) {
            shareIv.setVisibility(View.GONE);
        }

        shareIv.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT,
                        "Shared by Arcus Dictionary\n" + word.getWord() + ": " + word.getDefinition());

                // Can replace with view context
                activity.startActivity(Intent.createChooser(intent, "Share word"));

            }
        });

        ImageView linkIv = (ImageView) view.findViewById(R.id.LinkIcon);

        if (word.getTagCount() == -1) {
            linkIv.setVisibility(View.GONE);
        }

        linkIv.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                TextView def = (TextView) view.findViewById(R.id.definition_tv_definition);

                linkifyDefinition(def, def.getText().toString());

                if (word.getSynonyms().length() > 0) {
                    TextView syn = (TextView) view.findViewById(R.id.definition_tv_synonyms);
                    linkifySynonyms(syn, syn.getText().toString());
                }

            }
        });

        return view;
    }

    private String getDefinition(final WordModel word) {
        String d = "";

        if (word.getType().equalsIgnoreCase(NOUN_LABEL)) {
            d = NOUN_LABEL_ABV + " " + word.getDefinition();
        } else if (word.getType().equalsIgnoreCase(ADVERB_LABEL)) {
            d = ADVERB_LABEL_ABV + " " + word.getDefinition();
        } else if (word.getType().equalsIgnoreCase(ADJECTIVE_LABEL)) {
            d = ADJECTIVE_LABEL_ABV + " " + word.getDefinition();
        } else if (word.getType().equalsIgnoreCase(VERB_LABEL)) {
            d = VERB_LABEL_ABV + " " + word.getDefinition();
        } else {
            d = word.getDefinition();
        }

        if (activity.inFavouritesMode()) {
            d = word.getDefinition();
        }
        return d;
    }

    private void linkifyDefinition(TextView tv, String definition) {
        Spannable span = (Spannable) tv.getText();
        boolean done = false;
        int currentStart = 0;

        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, (tv.getTextSize() + 1.5f));

        while (!done) {
            // TODO fix "ridiculous" bug, need to check for other characters besides
            // space!, like ; for example
            int spacePos = definition.indexOf(" ", currentStart);

            if (spacePos == -1) {
                spacePos = definition.length();
                done = true;
            }

            final String currentWord = definition.substring(currentStart, spacePos).replaceAll(CLEAN_PATTERN, " ");

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    // can replace with iface
                    activity.setQuery(currentWord);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                }
            };

            if (currentWord.length() > 3 && !SIMPLE_WORDS.contains(currentWord)) {

                span.setSpan(clickableSpan, currentStart, spacePos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            }

            currentStart = spacePos + 1;
        }

    }

    private void linkifySynonyms(TextView tv, String synString) {
        Spannable span = (Spannable) tv.getText();

        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, (tv.getTextSize() + 1.5f));

        boolean done = false;
        int currentStart = synString.indexOf(" ") + 1;

        while (!done) {
            int commaPos = synString.indexOf(",", currentStart);

            if (commaPos == -1) {
                commaPos = synString.length();
                done = true;
            }

            final String currentWord = synString.substring(currentStart, commaPos);// .replaceAll(CLEAN_PATTERN,
            // "");;

            ClickableSpan clickableSpan = new ClickableSpan() {

                @Override
                public void onClick(View widget) {
                    activity.setQuery(currentWord);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                }

            };

            span.setSpan(clickableSpan, currentStart, commaPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            currentStart = commaPos + 2;

        }
    }

    private boolean isFavourited(String word, String definition) {
        return activity.getDbHelper().isFavourite(word, definition);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver arg0) {
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver arg0) {
    }

    private View createView(ViewGroup parent) {
        View view = mInflater.inflate(R.layout.definition_table, parent, false);
        return view;
    }
}
