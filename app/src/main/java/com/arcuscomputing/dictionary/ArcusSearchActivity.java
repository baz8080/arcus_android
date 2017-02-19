package com.arcuscomputing.dictionary;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.arcuscomputing.ArcusApplication;
import com.arcuscomputing.adapters.QuickResultListAdapter;
import com.arcuscomputing.db.FavouritesDbHelper;
import com.arcuscomputing.dictionary.io.ArcusDictionary;
import com.arcuscomputing.dictionary.menu.IArcusMenu;
import com.arcuscomputing.dictionary.menu.impl.ArcusMenu;
import com.arcuscomputing.dictionarypro.parent.R;
import com.arcuscomputing.models.WordModel;
import com.arcuscomputing.test.ArcusDictionaryMockObjects;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.arcuscomputing.db.FavouritesDbHelper.OPTION_SORT_ALPHA_ASC;
import static com.arcuscomputing.db.FavouritesDbHelper.OPTION_SORT_ALPHA_DESC;
import static com.arcuscomputing.db.FavouritesDbHelper.OPTION_SORT_DATE_ASC;
import static com.arcuscomputing.db.FavouritesDbHelper.OPTION_SORT_DATE_DESC;
import static com.arcuscomputing.dictionary.menu.IArcusMenu.CONTEXT_GOOGLE_DICTIONARY;
import static com.arcuscomputing.dictionary.menu.IArcusMenu.MENU_ALPHA_SORT_INDEX;
import static com.arcuscomputing.dictionary.menu.IArcusMenu.MENU_DATE_SORT_INDEX;


@SuppressLint("HandlerLeak")
public class ArcusSearchActivity extends AppCompatActivity implements
        TextWatcher, Runnable, OnInitListener,
        android.view.View.OnClickListener {

    private InputMethodManager imm;

    private EditText et;

    private ListView lvQuickResults;

    private SearchHandler sh;

    private ProgressDialog progress;

    private AlertDialog.Builder alertDialog;

    private FavouritesDbHelper dbHelper;

    private String previousWord;

    // private Menu menu;
    private IArcusMenu arcusMenu;

    private String sortMethod;

    private TextToSpeech tts;

    private boolean ttsAvailable = false;
    private boolean ttsLoadingMessageShown = false;

    private boolean hasShownExitWarning;

    private ArcusPreferences preferences;

    private ArcusDictionary dictionary;
    /**
     * Init Handler
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                progress.dismiss();
            } catch (Exception e) {
                makeToast("App was closed before progress dialogue completed");
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sh = new SearchHandler(this);

        if (tts == null) {
            tts = new TextToSpeech(this, this);
        }

        setContentView(R.layout.main_ads);

        AdView adview = (AdView) findViewById(R.id.adView);

        if (adview == null) {
            throw new IllegalStateException("plumbus");
        }

        String packageName = getPackageName();
        if (packageName.equals("com.arcuscomputing.dictionarypro")) {
            adview.setVisibility(View.GONE);
        } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            adview.loadAd(adRequest);
        }

        dbHelper = new FavouritesDbHelper(this);

        this.et = (EditText) findViewById(R.id.etSearch);
        this.lvQuickResults = (ListView) findViewById(R.id.lvQuickResults);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        dictionary = ArcusApplication.getDictionary();
        preferences = new ArcusPreferences(getApplicationContext());

        this.arcusMenu = new ArcusMenu(this);

        this.sortMethod = OPTION_SORT_DATE_DESC;

        et.addTextChangedListener(this);

        ensureResourcesLoaded();

        // Hide the keyboard if user touches the list.
        lvQuickResults.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                return false; // don't consume
            }
        });

        String initialWord = getIntent().getStringExtra(
                DictionaryConstants.INITIAL_WORD);

        if (initialWord != null && initialWord.length() > 0) {
            setQuery(initialWord);
        }
    }

    /**
     * Handle the click on the start recognition button.
     */
    public void onClick(View v) {
        if (v.getId() == R.id.btnSpeak) {

            if (!preferences.isMoneyWarningShown()) {
                alertDialog = new AlertDialog.Builder(this);
                alertDialog.setTitle(getResources().getString(
                        R.string.web_lookup_warning_header));
                alertDialog.setMessage(getResources().getString(
                        R.string.web_lookup_warning_message));

                alertDialog.setPositiveButton(
                        getResources().getString(R.string.dialogue_ok),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                startVoiceRecognitionActivity();
                            }
                        });

                alertDialog.setNegativeButton(
                        getResources().getString(R.string.dialogue_cancel),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {}
                        });

                alertDialog.show();

                preferences.setMoneyWarningShown();

            } else {
                startVoiceRecognitionActivity();
            }
        }
    }

    /**
     * Fire an intent to start the speech recognition activity.
     */
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.voice_recognition_blurb));
        startActivityForResult(intent, DictionaryConstants.VOICE_REQUEST_CODE);
    }

    private void doWarningNoOp(android.view.MenuItem menuItem) {

        if (menuItem != null) {
            onContextItemSelected(menuItem);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onContextItemSelected(final android.view.MenuItem aItem) {

        if (preferences.isInternetDisabled()) {
            return true;
        }

        if (!preferences.isMoneyWarningShown()) {
            alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(getResources().getString(
                    R.string.web_lookup_warning_header));
            alertDialog.setMessage(getResources().getString(
                    R.string.web_lookup_warning_message));

            alertDialog.setPositiveButton(
                    getResources().getString(R.string.dialogue_ok),
                    new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            doWarningNoOp(aItem);
                        }
                    });

            alertDialog.setNegativeButton(
                    getResources().getString(R.string.dialogue_cancel),
                    new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            doWarningNoOp(null);
                        }
                    });

            alertDialog.show();

            preferences.setMoneyWarningShown();
            return true;
        }

        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) aItem
                .getMenuInfo();

        WordModel wm = (WordModel) lvQuickResults.getAdapter().getItem(
                menuInfo.position);

        Intent i = new Intent(Intent.ACTION_VIEW);

        switch (aItem.getItemId()) {
            case CONTEXT_GOOGLE_DICTIONARY:
                i.setData(Uri.parse("http://www.google.com/search?q=define:"
                        + URLEncoder.encode(wm.getWord())));
                startActivity(i);

                return true;
            case IArcusMenu.CONTEXT_WIKITIONARY:

                i.setData(getWikiWord(wm));
                startActivity(i);

                return true;
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    private Uri getWikiWord(WordModel wm) {
        String word = wm.getWord();
        word = word.replaceAll(" ", "_");

        word = URLEncoder.encode(word);

        return Uri.parse("http://en.wiktionary.org/wiki/" + word);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (preferences.useAutoCorrect()) {
            this.et.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {

            this.et.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }

        ImageView speakButton = (ImageView) findViewById(R.id.btnSpeak);

        if (preferences.isInternetDisabled()) {
            if (speakButton != null) {
                speakButton.setEnabled(false);
                speakButton.setVisibility(View.GONE);
            }

            lvQuickResults.setOnCreateContextMenuListener(null);

        } else {
            // context
            lvQuickResults
                    .setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

                        @Override
                        public void onCreateContextMenu(ContextMenu menu,
                                                        View v, ContextMenuInfo menuInfo) {
                            menu.setHeaderTitle(getString(R.string.web_lookup_header));
                            menu.add(1, CONTEXT_GOOGLE_DICTIONARY, 1,
                                    getString(R.string.web_lookup_google));
                            menu.add(1, IArcusMenu.CONTEXT_WIKITIONARY, 2,
                                    getString(R.string.web_lookup_wikitionary));
                        }
                    });

            if (speakButton != null) {
                speakButton.setEnabled(true);
                speakButton.setVisibility(View.VISIBLE);
            }

            // Check to see if a recognition activity is present
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
            if (activities.size() != 0) {
                if (speakButton != null) {
                    speakButton.setOnClickListener(this);
                }
            } else {
                if (speakButton != null) {
                    speakButton.setEnabled(false);
                    speakButton.setVisibility(View.GONE);
                }

            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        hasShownExitWarning = false;
    }

    public FavouritesDbHelper getDbHelper() {
        return dbHelper;
    }

    private void ensureResourcesLoaded() {

        if (!externalStorageAvailable()) {

            et.setText("");

            alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(getResources().getString(
                    R.string.no_sd_card_title));
            alertDialog.setMessage(getResources().getString(
                    R.string.no_sd_card_message));
            alertDialog.setPositiveButton(
                    getResources()
                            .getString(R.string.no_sd_card_button_message),
                    new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            dictionary.closeFileHandles();
                            finish();
                        }
                    });
            alertDialog.show();
        }

        progress = ProgressDialog.show(this,
                getResources().getString(R.string.init_caption), getResources()
                        .getString(R.string.init_text), true, false);

        new Thread(this).start();
    }

    @Override
    public void run() {
        dictionary.ensureLoaded(getApplication()
                .getApplicationInfo().publicSourceDir, true);

        handler.sendEmptyMessage(0);
    }

    @Override
    public void onDestroy() {

        dictionary.closeFileHandles();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }

    @Override
    public void afterTextChanged(Editable query) {
        sh.setQuery(query.toString());
        sh.sleep(DictionaryConstants.SEARCH_SLEEP_TIME);
    }

    /******************************************
     * **** OVERRIDES *****
     ******************************************/
    @Override
    public boolean onSearchRequested() {
        handleSearchAction();
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        arcusMenu.setMenu(menu);
        arcusMenu.onCreateOptionsMenu();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (inFavouritesMode()) {
            setMainMenuItemsVisible(false);
            setFavouritesMenuItemVisible(true);
        } else {
            setMainMenuItemsVisible(true);
            setFavouritesMenuItemVisible(false);
        }

        return true;
    }

    private void setMainMenuItemsVisible(boolean visible) {
        arcusMenu.setMainMenuItemsVisible(visible);
    }

    private void setFavouritesMenuItemVisible(boolean visible) {
        arcusMenu.setFavouritesMenuItemVisible(visible);
    }

    /***************************************************
     * **** MENU DISPATCH *****
     ***************************************************/

    @Override
    public boolean onOptionsItemSelected(
            MenuItem item) {
        return arcusMenu.onOptionsItemSelected(item, this) || super.onOptionsItemSelected(item);

    }

    public void handleRandom() {

        String currentText = et.getText().toString().trim();

        if (currentText.length() > 0) {
            if (!currentText.equals(getString(R.string.random_mode))) {
                this.previousWord = currentText;
            }
        } else {
            this.previousWord = "";
        }

        this.et.setText(getString(R.string.random_mode));
        hasShownExitWarning = false;
    }

    public void handleEmailFavouritesAction() {

        QuickResultListAdapter la = (QuickResultListAdapter) this.lvQuickResults
                .getAdapter();

        List<WordModel> results = la.getResults();

        if (results != null && results.size() > 0) {

            StringBuilder stringBuilder = new StringBuilder(500);

            for (int i = 0; i < results.size(); i++) {
                stringBuilder.append(results.get(i).getWord());
                stringBuilder.append("\n");
                stringBuilder.append(results.get(i).getDefinition());

                if (i != results.size() - 1) {
                    stringBuilder.append("\n");
                    stringBuilder.append("\n");
                }
            }

            final Intent emailIntent = new Intent(
                    android.content.Intent.ACTION_SEND);

            emailIntent.setType("plain/text");

            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                    "My Favourites from Arcus Dictionary");

            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                    stringBuilder.toString());

            this.startActivity(Intent
                    .createChooser(emailIntent, "Send mail..."));
        }
    }

    /***************************************************
     * **** MENU ACTIONS *****
     ***************************************************/
    public void handleSearchAction() {

        et.requestFocus();
        et.selectAll();
        imm.showSoftInput(et, InputMethodManager.SHOW_FORCED);
        hasShownExitWarning = false;
    }

    public void handleSettingsAction() {
        startActivity(new Intent(this, EditPreferencesActivity.class));
    }

    public void handleHelpAction() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.help_dialog, null);

        WebView helpWeb = (WebView) view.findViewById(R.id.help_webview);
        helpWeb.loadUrl("file:///android_asset/help.html");

        builder.setView(view);

        builder.setPositiveButton(getString(R.string.dialogue_ok),
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {

                    }
                });

        builder.show();
    }

    public void handleFavouritesAction() {

        String currentText = et.getText().toString().trim();

        if (currentText.length() > 0) {
            if (!currentText.equals(getString(R.string.favourites_mode))) {
                this.previousWord = currentText;
            }
        } else {
            this.previousWord = "";
        }

        this.et.setText(getString(R.string.favourites_mode));

        hasShownExitWarning = false;
    }


    public void handleClearFavouritesAction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.favourites_dialogue_text))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.dialogue_no),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }
                        })
                .setPositiveButton(getString(R.string.dialogue_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dbHelper.deleteAllFavourites();
                                refreshFavourites();
                            }
                        });
        builder.show();
    }

    public void handleBackup() {
        Intent intent = new Intent(this, BackupActivity.class);

        startActivityForResult(intent, 0);
    }

    public void handleDateSortAction() {

        Menu menu = arcusMenu.getMenu();

        String currentTitle = menu.getItem(MENU_DATE_SORT_INDEX).getTitle()
                .toString();

        if (currentTitle.equals(getString(R.string.menu_sort_date_asc))) {
            this.sortMethod = OPTION_SORT_DATE_ASC;
            menu.getItem(MENU_DATE_SORT_INDEX).setTitle(
                    R.string.menu_sort_date_desc);
        } else {
            this.sortMethod = OPTION_SORT_DATE_DESC;
            menu.getItem(MENU_DATE_SORT_INDEX).setTitle(
                    R.string.menu_sort_date_asc);
        }

        this.refreshFavourites();
    }

    public void handleAlphaSortAction() {

        Menu menu = arcusMenu.getMenu();

        String currentTitle = menu.getItem(MENU_ALPHA_SORT_INDEX).getTitle()
                .toString();

        if (currentTitle.equals(getString(R.string.menu_sort_alpha_asc))) {
            this.sortMethod = OPTION_SORT_ALPHA_ASC;
            menu.getItem(MENU_ALPHA_SORT_INDEX).setTitle(
                    R.string.menu_sort_alpha_desc);
        } else {
            this.sortMethod = OPTION_SORT_ALPHA_DESC;
            menu.getItem(MENU_ALPHA_SORT_INDEX).setTitle(
                    R.string.menu_sort_alpha_asc);
        }

        this.refreshFavourites();


    }

    private boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState());
    }

    private void doSearchResult(String query) {

        query = query.trim();
        int queryLength = query.length();

        if (queryLength >= DictionaryConstants.QUICK_MIN_SEARCH_LENGTH) {

            if (query.equals(getString(R.string.favourites_mode))) {

                List<WordModel> favourites = dbHelper
                        .getAllFavourites(this.sortMethod);

                lvQuickResults.setAdapter(new QuickResultListAdapter(
                        favourites, this));

            } else if (query.equals(getString(R.string.random_mode))) {
                List<WordModel> random = dictionary.getRandom();

                lvQuickResults.setAdapter(new QuickResultListAdapter(random,
                        this));
            } else {

                QuickResultListAdapter results = new QuickResultListAdapter(
                        dictionary.getMatches(query, preferences.isPureAlpha()),
                        this);

                lvQuickResults.setAdapter(results);

                if (results.getCount() == 0) {
                    Toast.makeText(this,
                            getString(R.string.no_results) + query,
                            Toast.LENGTH_SHORT).show();
                    this.et.requestFocus();
                }
            }

        } else {
            lvQuickResults.setAdapter(new QuickResultListAdapter(
                    ArcusDictionaryMockObjects.quickResultsFromWord(0, ""),
                    this));
        }

    }

    /**
     * Handle the case where we are in favourites view and back should go to the
     * previous word
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (this.previousWord != null || inFavouritesMode()
                    || inRandomMode()) {
                this.et.setText(this.previousWord);
                this.previousWord = null; // consume it
            } else {

                if (!hasShownExitWarning) {
                    makeToast(getString(R.string.exit_message));
                    hasShownExitWarning = true;
                } else {
                    super.onKeyDown(keyCode, event);
                }
            }

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean inFavouritesMode() {
        return this.et.getText().toString().trim()
                .equals(getString(R.string.favourites_mode));
    }

    public boolean inRandomMode() {
        return this.et.getText().toString().trim()
                .equals(getString(R.string.random_mode));
    }

    public void refreshFavourites() {
        this.et.setText(getString(R.string.favourites_mode));
    }

    /********************************************
     * **** NOT IMPLEMENTING *****
     *******************************************/
    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                  int arg3) {
    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    public void makeToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsAvailable = true;

            if (preferences.useUSEnglish()) {
                tts.setLanguage(Locale.US);
            } else {
                tts.setLanguage(Locale.UK);
            }

        } else {
            ttsAvailable = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data != null && data.hasExtra("reset")) {

            this.dbHelper.close();
            dbHelper = new FavouritesDbHelper(this);

            refreshFavourites();
        }

        if (requestCode == DictionaryConstants.VOICE_REQUEST_CODE
                && resultCode == RESULT_OK) {
            ArrayList<String> matches = data != null ? data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) : null;

            if (matches != null && matches.size() > 0) {
                setQuery(matches.get(0));
            } else {
                makeToast(getString(R.string.voice_recognition_no_results));
            }
        }
    }

    public void speak(String word) {
        if (ttsAvailable && tts != null) {
            if (!ttsLoadingMessageShown) {
                makeToast(getString(R.string.tts_loading_message));
                ttsLoadingMessageShown = true;
            }

            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void setQuery(String newQuery) {
        if (newQuery != null && this.et != null) {
            this.previousWord = this.et.getText().toString();
            this.et.setText(newQuery);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

        if (intent.getAction().equals(ArcusWotdWidget.WIDGET_LAUNCH)) {
            if (intent.getExtras() != null
                    && intent.getExtras().getString(
                    DictionaryConstants.INITIAL_WORD) != null) {
                setQuery(intent.getExtras().getString(
                        DictionaryConstants.INITIAL_WORD));
            }
        }
    }

    static class SearchHandler extends Handler {

        private String query;

        private WeakReference<ArcusSearchActivity> mActivity;

        public SearchHandler(ArcusSearchActivity mActivity) {
            this.mActivity = new WeakReference<>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            ArcusSearchActivity act = mActivity.get();

            if (act != null) {
                act.doSearchResult(this.query);
            }
        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}