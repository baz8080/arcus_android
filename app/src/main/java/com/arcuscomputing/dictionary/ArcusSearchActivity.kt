package com.arcuscomputing.dictionary

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcuscomputing.dictionary.FavouritesDbHelper.SortOrder
import com.arcuscomputing.dictionarypro.ads.R
import com.arcuscomputing.dictionarypro.ads.databinding.MainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ArcusSearchActivity : AppCompatActivity(),
    TextToSpeech.OnInitListener, QuickResultListAdapter.Callbacks {

    private lateinit var binding: MainBinding
    private lateinit var imm: InputMethodManager
    private lateinit var dbHelper: FavouritesDbHelper
    private lateinit var preferences: ArcusPreferences

    private val resultsAdapter = QuickResultListAdapter(this)

    private var progress: AlertDialog? = null
    private var searchJob: Job? = null

    private var optionsMenu: Menu? = null
    private var tts: TextToSpeech? = null
    private var ttsAvailable = false
    private var ttsLoadingMessageShown = false
    private var hasShownExitWarning = false
    private val dictionary get() = (application as ArcusApplication).dictionary
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        dbHelper = FavouritesDbHelper(this)
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = resultsAdapter
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        preferences = ArcusPreferences(applicationContext)

        binding.etSearch.doAfterTextChanged { editable ->
            handleTextChanged(editable?.toString().orEmpty())
        }

        ensureResourcesLoaded()
        updateTitle()

        binding.rvResults.setOnTouchListener { _, _ ->
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            false
        }

        if (viewModel.mode == Mode.Favourites) showFavourites()

        intent.getStringExtra(INITIAL_WORD)?.takeIf { it.isNotEmpty() }?.let(::setQuery)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val prev = viewModel.previous
                when {
                    prev != null -> restorePreviousState(prev)
                    !hasShownExitWarning -> {
                        makeToast(getString(R.string.exit_message))
                        hasShownExitWarning = true
                    }
                    else -> finish()
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(INITIAL_WORD)?.takeIf { it.isNotEmpty() }?.let(::setQuery)
    }

    override fun onResume() {
        super.onResume()
        hasShownExitWarning = false
    }

    private fun ensureResourcesLoaded() {
        progress = AlertDialog.Builder(this)
            .setTitle(getString(R.string.init_caption))
            .setMessage(getString(R.string.init_text))
            .setCancelable(false)
            .show()
        lifecycleScope.launch {
            dictionary.ensureLoaded()
            progress?.dismiss()
            progress = null
        }
    }

    override fun onDestroy() {
        progress?.dismiss()
        progress = null
        tts?.stop()
        tts?.shutdown()
        dbHelper.close()
        super.onDestroy()
    }

    private fun handleTextChanged(text: String) {
        if (viewModel.mode == Mode.Favourites) {
            if (text.isEmpty()) return
            enterSearchMode()
        }
        scheduleSearch(text)
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(SEARCH_SLEEP_TIME)
            runSearch(query)
        }
    }

    private suspend fun runSearch(query: String) {
        val q = query.trim()
        if (q.length < QUICK_MIN_SEARCH_LENGTH) {
            resultsAdapter.submit(emptyList(), emptySet(), viewModel.mode == Mode.Favourites)
            return
        }
        val results = dictionary.getMatches(q, preferences.isPureAlpha)
        val favourites = withContext(Dispatchers.IO) { dbHelper.getFavouriteKeys() }
        resultsAdapter.submit(results, favourites, false)
        if (results.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_results) + q, Toast.LENGTH_SHORT).show()
            binding.etSearch.requestFocus()
        }
    }

    private fun showFavourites() {
        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                dbHelper.getAllFavourites(viewModel.sortMethod)
            }
            val favSet = results.map { it.word to it.definition }.toSet()
            resultsAdapter.submit(results, favSet, true)
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
        val favouritesMode = viewModel.mode == Mode.Favourites
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
        val results = resultsAdapter.currentList
        if (results.isEmpty()) return
        val body = results.joinToString("\n\n") { "${it.word}\n${it.definition}" }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
                    putExtra(Intent.EXTRA_TEXT, body)
                },
                getString(R.string.email_chooser_title)
            )
        )
    }

    private fun handleSearchAction() {
        binding.etSearch.requestFocus()
        binding.etSearch.selectAll()
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_FORCED)
        hasShownExitWarning = false
    }

    private fun handleSettingsAction() {
        startActivity(Intent(this, EditPreferencesActivity::class.java))
    }

    private fun handleFavouritesAction() {
        viewModel.previous = PreviousState(viewModel.mode, binding.etSearch.text.toString())
        viewModel.mode = Mode.Favourites
        invalidateOptionsMenu()
        updateTitle()
        binding.etSearch.setText("")
        showFavourites()
        hasShownExitWarning = false
    }

    private fun handleClearFavouritesAction() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.favourites_dialogue_text))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.dialogue_no), null)
            .setPositiveButton(getString(R.string.dialogue_yes)) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dbHelper.deleteAllFavourites() }
                    showFavourites()
                }
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
        showFavourites()
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
        showFavourites()
    }

    private fun enterSearchMode() {
        if (viewModel.mode == Mode.Search) return
        viewModel.mode = Mode.Search
        invalidateOptionsMenu()
        updateTitle()
    }

    private fun restorePreviousState(prev: PreviousState) {
        viewModel.previous = null
        if (viewModel.mode != prev.mode) {
            viewModel.mode = prev.mode
            invalidateOptionsMenu()
            updateTitle()
        }
        binding.etSearch.setText(prev.text)
        if (prev.mode == Mode.Favourites) showFavourites()
    }

    private fun updateTitle() {
        supportActionBar?.title = when (viewModel.mode) {
            Mode.Search -> getString(R.string.app_name)
            Mode.Favourites -> getString(R.string.menu_favourites)
        }
    }

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
    override fun onFavouriteToggled(word: String, definition: String, added: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (added) dbHelper.insertFavourite(word, definition)
                else dbHelper.deleteFromFavourites(word, definition)
            }
            if (!added && viewModel.mode == Mode.Favourites) showFavourites()
        }
    }

    override fun onWordClick(word: String) = setQuery(word)
    override fun onSpeak(word: String) = speak(word)
    override fun onShare(word: String, definition: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, word, definition))
                },
                getString(R.string.share_chooser_title)
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

    fun setQuery(newQuery: String) {
        viewModel.previous = PreviousState(viewModel.mode, binding.etSearch.text.toString())
        if (viewModel.mode != Mode.Search) {
            viewModel.mode = Mode.Search
            invalidateOptionsMenu()
            updateTitle()
        }
        binding.etSearch.setText(newQuery)
    }

    companion object {
        const val INITIAL_WORD = "initialWord"
        private const val SEARCH_SLEEP_TIME = 200L
        private const val QUICK_MIN_SEARCH_LENGTH = 2
    }
}
