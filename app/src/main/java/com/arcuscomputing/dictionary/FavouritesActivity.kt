package com.arcuscomputing.dictionary

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcuscomputing.dictionary.FavouritesDbHelper.SortOrder
import com.arcuscomputing.dictionarypro.ads.R
import com.arcuscomputing.dictionarypro.ads.databinding.FavouritesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavouritesActivity : AppCompatActivity(), QuickResultListAdapter.Callbacks {

    private lateinit var binding: FavouritesBinding
    private lateinit var dbHelper: FavouritesDbHelper
    private lateinit var preferences: ArcusPreferences
    private val viewModel: FavouritesViewModel by viewModels()
    private val resultsAdapter = QuickResultListAdapter(this)

    private lateinit var ttsHelper: TtsHelper
    private var optionsMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FavouritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = FavouritesDbHelper(this)
        preferences = ArcusPreferences(applicationContext)
        ttsHelper = TtsHelper(this, lifecycleScope, preferences)

        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = resultsAdapter

        showFavourites()
    }

    override fun onDestroy() {
        ttsHelper.shutdown()
        dbHelper.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_favourites, menu)
        optionsMenu = menu
        updateSortIcons()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.menu_sort_field -> { handleSortFieldToggle(); true }
            R.id.menu_sort_direction -> { handleSortDirectionToggle(); true }
            R.id.menu_clear_favourites -> { handleClearFavouritesAction(); true }
            R.id.menu_email_favourites -> { handleEmailFavouritesAction(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFavourites() {
        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                dbHelper.getAllFavourites(viewModel.sortMethod)
            }
            val favSet = results.map { it.word to it.definition }.toSet()
            resultsAdapter.submit(results, favSet, true)
            binding.rvResults.scrollToPosition(0)
            val empty = results.isEmpty()
            binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvResults.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    private fun handleSortFieldToggle() {
        viewModel.sortMethod = when (viewModel.sortMethod) {
            SortOrder.ALPHA_ASC -> SortOrder.DATE_ASC
            SortOrder.ALPHA_DESC -> SortOrder.DATE_DESC
            SortOrder.DATE_ASC -> SortOrder.ALPHA_ASC
            SortOrder.DATE_DESC -> SortOrder.ALPHA_DESC
        }
        updateSortIcons()
        showFavourites()
    }

    private fun handleSortDirectionToggle() {
        viewModel.sortMethod = when (viewModel.sortMethod) {
            SortOrder.ALPHA_ASC -> SortOrder.ALPHA_DESC
            SortOrder.ALPHA_DESC -> SortOrder.ALPHA_ASC
            SortOrder.DATE_ASC -> SortOrder.DATE_DESC
            SortOrder.DATE_DESC -> SortOrder.DATE_ASC
        }
        updateSortIcons()
        showFavourites()
    }

    private fun updateSortIcons() {
        val menu = optionsMenu ?: return
        val fieldIcon = when (viewModel.sortMethod) {
            SortOrder.ALPHA_ASC, SortOrder.ALPHA_DESC -> R.drawable.ic_sort_by_alpha
            SortOrder.DATE_ASC, SortOrder.DATE_DESC -> R.drawable.ic_calendar_today
        }
        val directionIcon = when (viewModel.sortMethod) {
            SortOrder.ALPHA_ASC, SortOrder.DATE_ASC -> R.drawable.ic_arrow_upward
            SortOrder.ALPHA_DESC, SortOrder.DATE_DESC -> R.drawable.ic_arrow_downward
        }
        menu.findItem(R.id.menu_sort_field)?.setIcon(fieldIcon)
        menu.findItem(R.id.menu_sort_direction)?.setIcon(directionIcon)
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

    // QuickResultListAdapter.Callbacks
    override fun onWordClick(word: String) {
        startActivity(Intent(this, ArcusSearchActivity::class.java).apply {
            putExtra(ArcusSearchActivity.INITIAL_WORD, word)
        })
    }

    override fun onFavouriteToggled(word: String, definition: String, added: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (added) dbHelper.insertFavourite(word, definition)
                else dbHelper.deleteFromFavourites(word, definition)
            }
            if (!added) showFavourites()
        }
    }

    override fun onSpeak(word: String) = ttsHelper.speak(word)

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

}
