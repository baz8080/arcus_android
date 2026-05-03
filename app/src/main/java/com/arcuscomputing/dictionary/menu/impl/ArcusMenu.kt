package com.arcuscomputing.dictionary.menu.impl

import android.app.SearchManager
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import com.arcuscomputing.dictionary.ArcusSearchActivity
import com.arcuscomputing.dictionary.menu.IArcusMenu
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.CONTEXT_GOOGLE_DICTIONARY
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_ALPHA_SORT
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_ALPHA_SORT_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_CLEAR_FAVOURITES
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_CLEAR_FAVOURITES_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_DATE_SORT
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_DATE_SORT_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_EMAIL_FAVOURITES
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_EMAIL_FAVOURITES_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_FAVOURITES
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_FAVOURITES_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_HELP
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_HELP_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_RANDOM
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_RANDOM_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_SEARCH
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_SEARCH_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_SETTINGS
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENU_SETTINGS_INDEX
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENUGROUP_ACTIONS
import com.arcuscomputing.dictionary.menu.IArcusMenu.Companion.MENUGROUP_INFO
import com.arcuscomputing.dictionarypro.ads.R

class ArcusMenu(private val context: Context) : IArcusMenu {

    override lateinit var menu: Menu

    private fun getString(id: Int) = context.getString(id)

    override fun onCreateOptionsMenu(): Boolean {
        menu.add(MENUGROUP_ACTIONS, MENU_SEARCH, MENU_SEARCH_INDEX, getString(R.string.menu_search))
            .setIcon(R.drawable.ic_search_white_24dp)
            .setAlphabeticShortcut(SearchManager.MENU_KEY)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        menu.add(MENUGROUP_ACTIONS, MENU_FAVOURITES, MENU_FAVOURITES_INDEX, getString(R.string.menu_favourites))
            .setIcon(R.drawable.ic_star_white_24dp)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        menu.add(MENUGROUP_ACTIONS, MENU_SETTINGS, MENU_SETTINGS_INDEX, getString(R.string.menu_settings))
            .setIcon(R.drawable.ic_settings_white_24dp)

        menu.add(MENUGROUP_INFO, MENU_HELP, MENU_HELP_INDEX, getString(R.string.menu_help))
            .setIcon(R.drawable.ic_help_white_24dp)

        menu.add(MENUGROUP_INFO, MENU_RANDOM, MENU_RANDOM_INDEX, getString(R.string.menu_random))
            .setIcon(R.drawable.ic_shuffle_white_24dp)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        menu.add(MENUGROUP_INFO, MENU_ALPHA_SORT, MENU_ALPHA_SORT_INDEX, getString(R.string.menu_sort_alpha_asc))
            .setIcon(android.R.drawable.ic_menu_sort_alphabetically)

        menu.add(MENUGROUP_INFO, MENU_DATE_SORT, MENU_DATE_SORT_INDEX, getString(R.string.menu_sort_date_asc))
            .setIcon(android.R.drawable.ic_menu_month)

        menu.add(MENUGROUP_INFO, MENU_CLEAR_FAVOURITES, MENU_CLEAR_FAVOURITES_INDEX, getString(R.string.menu_clear_favourites))
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel)

        menu.add(MENUGROUP_INFO, MENU_EMAIL_FAVOURITES, MENU_EMAIL_FAVOURITES_INDEX, getString(R.string.menu_email_favourites))
            .setIcon(android.R.drawable.ic_menu_send)

        return true
    }

    override fun setMainMenuItemsVisible(visible: Boolean) {
        listOf(MENU_SEARCH_INDEX, MENU_FAVOURITES_INDEX, MENU_SETTINGS_INDEX, MENU_HELP_INDEX, MENU_RANDOM_INDEX)
            .forEach { index ->
                menu.getItem(index).isEnabled = visible
                menu.getItem(index).isVisible = visible
            }
    }

    override fun setFavouritesMenuItemVisible(visible: Boolean) {
        listOf(MENU_ALPHA_SORT_INDEX, MENU_DATE_SORT_INDEX, MENU_CLEAR_FAVOURITES_INDEX, MENU_EMAIL_FAVOURITES_INDEX)
            .forEach { index ->
                menu.getItem(index).isEnabled = visible
                menu.getItem(index).isVisible = visible
            }
    }

    override fun onOptionsItemSelected(item: MenuItem, activity: ArcusSearchActivity): Boolean {
        return when (item.itemId) {
            MENU_SEARCH -> { activity.handleSearchAction(); true }
            MENU_FAVOURITES -> { activity.handleFavouritesAction(); true }
            MENU_HELP -> { activity.handleHelpAction(); true }
            MENU_SETTINGS -> { activity.handleSettingsAction(); true }
            MENU_ALPHA_SORT -> { activity.handleAlphaSortAction(); true }
            MENU_DATE_SORT -> { activity.handleDateSortAction(); true }
            MENU_CLEAR_FAVOURITES -> { activity.handleClearFavouritesAction(); true }
            MENU_EMAIL_FAVOURITES -> { activity.handleEmailFavouritesAction(); true }
            MENU_RANDOM -> { activity.handleRandom(); true }
            else -> false
        }
    }
}
