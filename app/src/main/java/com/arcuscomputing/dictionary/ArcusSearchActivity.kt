package com.arcuscomputing.dictionary

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arcuscomputing.dictionarypro.ads.R
import com.arcuscomputing.dictionarypro.ads.databinding.MainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class ArcusSearchActivity : AppCompatActivity(), QuickResultListAdapter.Callbacks {

    private lateinit var binding: MainBinding
    private lateinit var dbHelper: FavouritesDbHelper
    private lateinit var preferences: ArcusPreferences

    private val resultsAdapter = QuickResultListAdapter(this)

    private var progress: AlertDialog? = null
    private var searchJob: Job? = null

    private lateinit var ttsHelper: TtsHelper
    private val dictionary get() = (application as ArcusApplication).dictionary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        dbHelper = FavouritesDbHelper(this)
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = resultsAdapter
        preferences = ArcusPreferences(applicationContext)
        ttsHelper = TtsHelper(this, lifecycleScope, preferences)

        binding.etSearch.doAfterTextChanged { editable ->
            scheduleSearch(editable?.toString().orEmpty())
        }

        ensureResourcesLoaded()

        binding.rvResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    ViewCompat.getWindowInsetsController(recyclerView)
                        ?.hide(WindowInsetsCompat.Type.ime())
                }
            }
        })

        intent.getStringExtra(INITIAL_WORD)?.takeIf { it.isNotEmpty() }?.let(::setQuery)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(INITIAL_WORD)?.takeIf { it.isNotEmpty() }?.let(::setQuery)
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
        ttsHelper.shutdown()
        dbHelper.close()
        super.onDestroy()
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
            resultsAdapter.submit(emptyList(), emptySet(), false)
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

    override fun onSearchRequested(): Boolean {
        handleSearchAction()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> { handleSearchAction(); true }
            R.id.menu_favourites -> { startActivity(Intent(this, FavouritesActivity::class.java)); true }
            R.id.menu_settings -> { startActivity(Intent(this, EditPreferencesActivity::class.java)); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleSearchAction() {
        binding.etSearch.requestFocus()
        binding.etSearch.selectAll()
        ViewCompat.getWindowInsetsController(binding.etSearch)
            ?.show(WindowInsetsCompat.Type.ime())
    }

    // QuickResultListAdapter.Callbacks
    override fun onFavouriteToggled(word: String, definition: String, added: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (added) dbHelper.insertFavourite(word, definition)
                else dbHelper.deleteFromFavourites(word, definition)
            }
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

    private fun speak(word: String) = ttsHelper.speak(word)

    private fun setQuery(newQuery: String) {
        binding.etSearch.setText(newQuery)
    }

    private fun makeToast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    companion object {
        const val INITIAL_WORD = "initialWord"
        private const val SEARCH_SLEEP_TIME = 200L
        private const val QUICK_MIN_SEARCH_LENGTH = 2
    }
}
