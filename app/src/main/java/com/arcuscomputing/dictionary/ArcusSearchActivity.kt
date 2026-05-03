package com.arcuscomputing.dictionary

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.arcuscomputing.ArcusApplication
import com.arcuscomputing.ArcusDictionaryMockObjects
import com.arcuscomputing.FavouritesDbHelper
import com.arcuscomputing.FavouritesDbHelper.Companion.OPTION_SORT_ALPHA_ASC
import com.arcuscomputing.FavouritesDbHelper.Companion.OPTION_SORT_ALPHA_DESC
import com.arcuscomputing.FavouritesDbHelper.Companion.OPTION_SORT_DATE_ASC
import com.arcuscomputing.FavouritesDbHelper.Companion.OPTION_SORT_DATE_DESC
import com.arcuscomputing.QuickResultListAdapter
import com.arcuscomputing.dictionary.DictionaryConstants.QUICK_MIN_SEARCH_LENGTH
import com.arcuscomputing.dictionary.DictionaryConstants.SEARCH_SLEEP_TIME
import com.arcuscomputing.dictionary.DictionaryConstants.VOICE_REQUEST_CODE
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.CONTEXT_GOOGLE_DICTIONARY
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.CONTEXT_WIKITIONARY
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_ALPHA_SORT_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_DATE_SORT_INDEX
import com.arcuscomputing.dictionary.menu.impl.ArcusMenu
import com.arcuscomputing.dictionary.io.ArcusDictionary
import com.arcuscomputing.dictionarypro.ads.R
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.Locale

@SuppressLint("HandlerLeak")
@Suppress("DEPRECATION")
class ArcusSearchActivity : AppCompatActivity(), TextWatcher, Runnable,
    TextToSpeech.OnInitListener, View.OnClickListener {

    private lateinit var imm: InputMethodManager
    private lateinit var et: EditText
    private lateinit var lvQuickResults: ListView
    private lateinit var sh: SearchHandler

    private lateinit var progress: ProgressDialog
    private var alertDialog: AlertDialog.Builder? = null

    lateinit var dbHelper: FavouritesDbHelper
        private set

    private var previousWord: String? = null
    private lateinit var arcusMenu: ArcusMenu
    private var sortMethod = OPTION_SORT_DATE_DESC
    private var tts: TextToSpeech? = null
    private var ttsAvailable = false
    private var ttsLoadingMessageShown = false
    private var hasShownExitWarning = false
    private lateinit var preferences: ArcusPreferences
    private lateinit var dictionary: ArcusDictionary

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            try {
                progress.dismiss()
            } catch (e: Exception) {
                makeToast("App was closed before progress dialogue completed")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sh = SearchHandler(this)
        if (tts == null) tts = TextToSpeech(this, this)

        setContentView(R.layout.main_ads)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        dbHelper = FavouritesDbHelper(this)
        et = findViewById(R.id.etSearch)
        lvQuickResults = findViewById(R.id.lvQuickResults)
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        dictionary = ArcusApplication.dictionary
        preferences = ArcusPreferences(applicationContext)
        arcusMenu = ArcusMenu(this)
        sortMethod = OPTION_SORT_DATE_DESC

        et.addTextChangedListener(this)
        ensureResourcesLoaded()

        lvQuickResults.setOnTouchListener { _, _ ->
            imm.hideSoftInputFromWindow(et.windowToken, 0)
            false
        }

        val initialWord = intent.getStringExtra(DictionaryConstants.INITIAL_WORD)
        if (!initialWord.isNullOrEmpty()) setQuery(initialWord)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btnSpeak) {
            if (!preferences.isMoneyWarningShown) {
                alertDialog = AlertDialog.Builder(this).apply {
                    setTitle(getString(R.string.web_lookup_warning_header))
                    setMessage(getString(R.string.web_lookup_warning_message))
                    setPositiveButton(getString(R.string.dialogue_ok)) { _, _ -> startVoiceRecognitionActivity() }
                    setNegativeButton(getString(R.string.dialogue_cancel), null)
                    show()
                }
                preferences.setMoneyWarningShown()
            } else {
                startVoiceRecognitionActivity()
            }
        }
    }

    private fun startVoiceRecognitionActivity() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_recognition_blurb))
        }
        startActivityForResult(intent, VOICE_REQUEST_CODE)
    }

    private fun doWarningNoOp(menuItem: android.view.MenuItem?) {
        menuItem?.let { onContextItemSelected(it) }
    }

    override fun onContextItemSelected(aItem: android.view.MenuItem): Boolean {
        if (preferences.isInternetDisabled) return true

        if (!preferences.isMoneyWarningShown) {
            alertDialog = AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.web_lookup_warning_header))
                setMessage(getString(R.string.web_lookup_warning_message))
                setPositiveButton(getString(R.string.dialogue_ok)) { _, _ -> doWarningNoOp(aItem) }
                setNegativeButton(getString(R.string.dialogue_cancel)) { _, _ -> doWarningNoOp(null) }
                show()
            }
            preferences.setMoneyWarningShown()
            return true
        }

        val menuInfo = aItem.menuInfo as AdapterView.AdapterContextMenuInfo
        val wm = lvQuickResults.adapter.getItem(menuInfo.position) as com.arcuscomputing.WordModel
        val intent = Intent(Intent.ACTION_VIEW)

        return when (aItem.itemId) {
            CONTEXT_GOOGLE_DICTIONARY -> {
                intent.data = Uri.parse("http://www.google.com/search?q=define:" + URLEncoder.encode(wm.word, "UTF-8"))
                startActivity(intent)
                true
            }
            CONTEXT_WIKITIONARY -> {
                intent.data = Uri.parse("http://en.wiktionary.org/wiki/" + URLEncoder.encode(wm.word.replace(" ", "_"), "UTF-8"))
                startActivity(intent)
                true
            }
            else -> false
        }
    }

    override fun onStart() {
        super.onStart()
        et.inputType = if (preferences.useAutoCorrect) InputType.TYPE_CLASS_TEXT
                       else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        val speakButton = findViewById<View>(R.id.btnSpeak)
        if (preferences.isInternetDisabled) {
            speakButton?.isEnabled = false
            speakButton?.visibility = View.GONE
            lvQuickResults.setOnCreateContextMenuListener(null)
        } else {
            lvQuickResults.setOnCreateContextMenuListener { menu, _, _ ->
                menu.setHeaderTitle(getString(R.string.web_lookup_header))
                menu.add(1, CONTEXT_GOOGLE_DICTIONARY, 1, getString(R.string.web_lookup_google))
                menu.add(1, CONTEXT_WIKITIONARY, 2, getString(R.string.web_lookup_wikitionary))
            }
            val activities = packageManager.queryIntentActivities(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
            )
            if (activities.isNotEmpty()) {
                speakButton?.isEnabled = true
                speakButton?.visibility = View.VISIBLE
                speakButton?.setOnClickListener(this)
            } else {
                speakButton?.isEnabled = false
                speakButton?.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasShownExitWarning = false
    }

    private fun ensureResourcesLoaded() {
        if (!externalStorageAvailable()) {
            et.setText("")
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.no_sd_card_title))
                .setMessage(getString(R.string.no_sd_card_message))
                .setPositiveButton(getString(R.string.no_sd_card_button_message)) { _, _ ->
                    dictionary.closeFileHandles()
                    finish()
                }
                .show()
        }
        progress = ProgressDialog.show(this, getString(R.string.init_caption), getString(R.string.init_text), true, false)
        Thread(this).start()
    }

    override fun run() {
        dictionary.ensureLoaded(applicationContext)
        handler.sendEmptyMessage(0)
    }

    override fun onDestroy() {
        dictionary.closeFileHandles()
        tts?.stop()
        tts?.shutdown()
        dbHelper.close()
        super.onDestroy()
    }

    override fun afterTextChanged(query: Editable) {
        sh.setQuery(query.toString())
        sh.sleep(SEARCH_SLEEP_TIME)
    }

    override fun onSearchRequested(): Boolean {
        handleSearchAction()
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        arcusMenu.menu = menu
        arcusMenu.onCreateOptionsMenu()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (inFavouritesMode()) {
            arcusMenu.setMainMenuItemsVisible(false)
            arcusMenu.setFavouritesMenuItemVisible(true)
        } else {
            arcusMenu.setMainMenuItemsVisible(true)
            arcusMenu.setFavouritesMenuItemVisible(false)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        arcusMenu.onOptionsItemSelected(item, this) || super.onOptionsItemSelected(item)

    fun handleRandom() {
        val currentText = et.text.toString().trim()
        previousWord = if (currentText.isNotEmpty() && currentText != getString(R.string.random_mode)) currentText else ""
        et.setText(getString(R.string.random_mode))
        hasShownExitWarning = false
    }

    fun handleEmailFavouritesAction() {
        val adapter = lvQuickResults.adapter as? QuickResultListAdapter ?: return
        val results = adapter.getResults()
        if (results.isEmpty()) return

        val body = results.joinToString("\n\n") { "${it.word}\n${it.definition}" }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "plain/text"
                    putExtra(Intent.EXTRA_SUBJECT, "My Favourites from Arcus Dictionary")
                    putExtra(Intent.EXTRA_TEXT, body)
                },
                "Send mail..."
            )
        )
    }

    fun handleSearchAction() {
        et.requestFocus()
        et.selectAll()
        imm.showSoftInput(et, InputMethodManager.SHOW_FORCED)
        hasShownExitWarning = false
    }

    fun handleSettingsAction() {
        startActivity(Intent(this, EditPreferencesActivity::class.java))
    }

    fun handleHelpAction() {
        @SuppressLint("InflateParams")
        val view = LayoutInflater.from(this).inflate(R.layout.help_dialog, null)
        view.findViewById<WebView>(R.id.help_webview).loadUrl("file:///android_asset/help.html")
        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(getString(R.string.dialogue_ok), null)
            .show()
    }

    fun handleFavouritesAction() {
        val currentText = et.text.toString().trim()
        previousWord = if (currentText.isNotEmpty() && currentText != getString(R.string.favourites_mode)) currentText else ""
        et.setText(getString(R.string.favourites_mode))
        hasShownExitWarning = false
    }

    fun handleClearFavouritesAction() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.favourites_dialogue_text))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.dialogue_no), null)
            .setPositiveButton(getString(R.string.dialogue_yes)) { _, _ ->
                dbHelper.deleteAllFavourites()
                refreshFavourites()
            }
            .show()
    }

    fun handleDateSortAction() {
        val item = arcusMenu.menu.getItem(MENU_DATE_SORT_INDEX)
        if (item.title.toString() == getString(R.string.menu_sort_date_asc)) {
            sortMethod = OPTION_SORT_DATE_ASC
            item.setTitle(R.string.menu_sort_date_desc)
        } else {
            sortMethod = OPTION_SORT_DATE_DESC
            item.setTitle(R.string.menu_sort_date_asc)
        }
        refreshFavourites()
    }

    fun handleAlphaSortAction() {
        val item = arcusMenu.menu.getItem(MENU_ALPHA_SORT_INDEX)
        if (item.title.toString() == getString(R.string.menu_sort_alpha_asc)) {
            sortMethod = OPTION_SORT_ALPHA_ASC
            item.setTitle(R.string.menu_sort_alpha_desc)
        } else {
            sortMethod = OPTION_SORT_ALPHA_DESC
            item.setTitle(R.string.menu_sort_alpha_asc)
        }
        refreshFavourites()
    }

    private fun externalStorageAvailable() =
        android.os.Environment.MEDIA_MOUNTED == android.os.Environment.getExternalStorageState()

    private fun doSearchResult(query: String) {
        val q = query.trim()
        if (q.length < QUICK_MIN_SEARCH_LENGTH) {
            lvQuickResults.adapter = QuickResultListAdapter(
                ArcusDictionaryMockObjects.quickResultsFromWord(0, ""), this
            )
            return
        }
        when (q) {
            getString(R.string.favourites_mode) ->
                lvQuickResults.adapter = QuickResultListAdapter(dbHelper.getAllFavourites(sortMethod), this)
            getString(R.string.random_mode) ->
                lvQuickResults.adapter = QuickResultListAdapter(dictionary.getRandom(), this)
            else -> {
                val adapter = QuickResultListAdapter(dictionary.getMatches(q, preferences.isPureAlpha), this)
                lvQuickResults.adapter = adapter
                if (adapter.count == 0) {
                    Toast.makeText(this, getString(R.string.no_results) + q, Toast.LENGTH_SHORT).show()
                    et.requestFocus()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (previousWord != null || inFavouritesMode() || inRandomMode()) {
                et.setText(previousWord)
                previousWord = null
            } else {
                if (!hasShownExitWarning) {
                    makeToast(getString(R.string.exit_message))
                    hasShownExitWarning = true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun inFavouritesMode() = et.text.toString().trim() == getString(R.string.favourites_mode)
    fun inRandomMode() = et.text.toString().trim() == getString(R.string.random_mode)

    fun refreshFavourites() {
        et.setText(getString(R.string.favourites_mode))
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    fun makeToast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsAvailable = true
            tts?.language = if (preferences.useUSEnglish) Locale.US else Locale.UK
        } else {
            ttsAvailable = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && data.hasExtra("reset")) {
            dbHelper.close()
            dbHelper = FavouritesDbHelper(this)
            refreshFavourites()
        }
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) setQuery(matches[0])
            else makeToast(getString(R.string.voice_recognition_no_results))
        }
    }

    fun speak(word: String) {
        if (ttsAvailable && tts != null) {
            if (!ttsLoadingMessageShown) {
                makeToast(getString(R.string.tts_loading_message))
                ttsLoadingMessageShown = true
            }
            tts!!.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun setQuery(newQuery: String?) {
        if (newQuery != null) {
            previousWord = et.text.toString()
            et.setText(newQuery)
        }
    }

    class SearchHandler(activity: ArcusSearchActivity) : Handler(Looper.getMainLooper()) {
        private val mActivity = WeakReference(activity)
        private var query = ""

        override fun handleMessage(msg: Message) {
            mActivity.get()?.doSearchResult(query)
        }

        fun sleep(delayMillis: Long) {
            removeMessages(0)
            sendMessageDelayed(obtainMessage(0), delayMillis)
        }

        fun setQuery(q: String) { query = q }
    }
}
