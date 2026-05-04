package com.arcuscomputing.dictionary

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arcuscomputing.ArcusApplication
import com.arcuscomputing.FavouritesDbHelper
import com.arcuscomputing.FavouritesDbHelper.SortOrder
import com.arcuscomputing.QuickResultListAdapter
import com.arcuscomputing.dictionarypro.ads.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ArcusSearchActivity : AppCompatActivity(), TextWatcher,
    TextToSpeech.OnInitListener, QuickResultListAdapter.Callbacks {

    private lateinit var imm: InputMethodManager
    private lateinit var et: EditText
    private lateinit var rvResults: RecyclerView

    private var progress: AlertDialog? = null
    private var searchJob: Job? = null

    lateinit var dbHelper: FavouritesDbHelper
        private set

    private var optionsMenu: Menu? = null
    private var tts: TextToSpeech? = null
    private var ttsAvailable = false
    private var ttsLoadingMessageShown = false
    private var hasShownExitWarning = false
    private lateinit var preferences: ArcusPreferences
    private val dictionary get() = (application as ArcusApplication).dictionary
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContentView(R.layout.main)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        dbHelper = FavouritesDbHelper(this)
        et = findViewById(R.id.etSearch)
        rvResults = findViewById(R.id.rvResults)
        rvResults.layoutManager = LinearLayoutManager(this)
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        preferences = ArcusPreferences(applicationContext)

        et.addTextChangedListener(this)
        ensureResourcesLoaded()

        rvResults.setOnTouchListener { _, _ ->
            imm.hideSoftInputFromWindow(et.windowToken, 0)
            false
        }

        val initialWord = intent.getStringExtra(INITIAL_WORD)
        if (!initialWord.isNullOrEmpty()) setQuery(initialWord)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewModel.previousWord != null || inFavouritesMode() -> {
                        et.setText(viewModel.previousWord)
                        viewModel.previousWord = null
                    }
                    !hasShownExitWarning -> {
                        makeToast(getString(R.string.exit_message))
                        hasShownExitWarning = true
                    }
                    else -> finish()
                }
            }
        })
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
        progress = AlertDialog.Builder(this)
            .setTitle(getString(R.string.init_caption))
            .setMessage(getString(R.string.init_text))
            .setCancelable(false)
            .show()
        lifecycleScope.launch(Dispatchers.IO) {
            dictionary.ensureLoaded()
            withContext(Dispatchers.Main) { progress?.dismiss() }
        }
    }

    override fun onDestroy() {
        dictionary.closeFileHandles()
        tts?.stop()
        tts?.shutdown()
        dbHelper.close()
        super.onDestroy()
    }

    override fun afterTextChanged(query: Editable) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(SEARCH_SLEEP_TIME)
            doSearchResult(query.toString())
        }
    }

    override fun onSearchRequested(): Boolean {
        handleSearchAction()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        optionsMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val favouritesMode = inFavouritesMode()
        listOf(R.id.menu_search, R.id.menu_favourites, R.id.menu_settings).forEach {
            menu.findItem(it)?.isVisible = !favouritesMode
        }
        listOf(R.id.menu_alpha_sort, R.id.menu_date_sort, R.id.menu_clear_favourites, R.id.menu_email_favourites).forEach {
            menu.findItem(it)?.isVisible = favouritesMode
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> { handleSearchAction(); true }
            R.id.menu_favourites -> { handleFavouritesAction(); true }
            R.id.menu_settings -> { handleSettingsAction(); true }
            R.id.menu_alpha_sort -> { handleAlphaSortAction(); true }
            R.id.menu_date_sort -> { handleDateSortAction(); true }
            R.id.menu_clear_favourites -> { handleClearFavouritesAction(); true }
            R.id.menu_email_favourites -> { handleEmailFavouritesAction(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleEmailFavouritesAction() {
        val results = (rvResults.adapter as? QuickResultListAdapter)?.getResults() ?: return
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

    private fun handleSearchAction() {
        et.requestFocus()
        et.selectAll()
        imm.showSoftInput(et, InputMethodManager.SHOW_FORCED)
        hasShownExitWarning = false
    }

    private fun handleSettingsAction() {
        startActivity(Intent(this, EditPreferencesActivity::class.java))
    }

    private fun handleFavouritesAction() {
        val currentText = et.text.toString().trim()
        viewModel.previousWord = if (currentText.isNotEmpty() && currentText != getString(R.string.favourites_mode)) currentText else ""
        et.setText(getString(R.string.favourites_mode))
        hasShownExitWarning = false
    }

    private fun handleClearFavouritesAction() {
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

    private fun handleDateSortAction() {
        val item = optionsMenu?.findItem(R.id.menu_date_sort) ?: return
        if (item.title.toString() == getString(R.string.menu_sort_date_asc)) {
            viewModel.sortMethod = SortOrder.DATE_ASC
            item.setTitle(R.string.menu_sort_date_desc)
        } else {
            viewModel.sortMethod = SortOrder.DATE_DESC
            item.setTitle(R.string.menu_sort_date_asc)
        }
        refreshFavourites()
    }

    private fun handleAlphaSortAction() {
        val item = optionsMenu?.findItem(R.id.menu_alpha_sort) ?: return
        if (item.title.toString() == getString(R.string.menu_sort_alpha_asc)) {
            viewModel.sortMethod = SortOrder.ALPHA_ASC
            item.setTitle(R.string.menu_sort_alpha_desc)
        } else {
            viewModel.sortMethod = SortOrder.ALPHA_DESC
            item.setTitle(R.string.menu_sort_alpha_asc)
        }
        refreshFavourites()
    }

    private fun externalStorageAvailable() =
        android.os.Environment.MEDIA_MOUNTED == android.os.Environment.getExternalStorageState()

    private fun doSearchResult(query: String) {
        val q = query.trim()
        if (q.length < QUICK_MIN_SEARCH_LENGTH) {
            rvResults.adapter = QuickResultListAdapter(emptyList(), this)
            return
        }
        when (q) {
            getString(R.string.favourites_mode) ->
                rvResults.adapter = QuickResultListAdapter(dbHelper.getAllFavourites(viewModel.sortMethod), this)
            else -> {
                val adapter = QuickResultListAdapter(dictionary.getMatches(q, preferences.isPureAlpha), this)
                rvResults.adapter = adapter
                if (adapter.itemCount == 0) {
                    Toast.makeText(this, getString(R.string.no_results) + q, Toast.LENGTH_SHORT).show()
                    et.requestFocus()
                }
            }
        }
    }

    override fun inFavouritesMode() = et.text.toString().trim() == getString(R.string.favourites_mode)

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

    // QuickResultListAdapter.Callbacks
    override fun isFavourited(word: String, definition: String) = dbHelper.isFavourite(word, definition)

    override fun onFavouriteToggled(word: String, definition: String, added: Boolean) {
        if (added) {
            dbHelper.insertFavourite(word, definition)
        } else {
            dbHelper.deleteFromFavourites(word, definition)
            if (inFavouritesMode()) refreshFavourites()
        }
    }

    override fun onWordClick(word: String) = setQuery(word)
    override fun onSpeak(word: String) = speak(word)
    override fun onShare(word: String, definition: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Shared by Arcus Dictionary\n$word: $definition")
                },
                "Share word"
            )
        )
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
            viewModel.previousWord = et.text.toString()
            et.setText(newQuery)
        }
    }

    companion object {
        const val INITIAL_WORD = "initialWord"
        private const val SEARCH_SLEEP_TIME = 200L
        private const val QUICK_MIN_SEARCH_LENGTH = 2
    }
}
