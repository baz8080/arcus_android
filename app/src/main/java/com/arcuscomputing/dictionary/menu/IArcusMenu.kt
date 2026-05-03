package com.arcuscomputing.dictionary.menu

import android.view.Menu
import android.view.MenuItem
import com.arcuscomputing.dictionary.ArcusSearchActivity

interface IArcusMenu {

    var menu: Menu

    fun onCreateOptionsMenu(): Boolean
    fun setMainMenuItemsVisible(visible: Boolean)
    fun setFavouritesMenuItemVisible(visible: Boolean)
    fun onOptionsItemSelected(item: MenuItem, activity: ArcusSearchActivity): Boolean

    companion object {
        const val MENU_SEARCH = 0
        const val MENU_SETTINGS = 1

        const val MENU_FAVOURITES = 2
        const val MENU_ALPHA_SORT = 3
        const val MENU_DATE_SORT = 4
        const val MENU_CLEAR_FAVOURITES = 5
        const val MENU_EMAIL_FAVOURITES = 6

        const val MENU_SEARCH_INDEX = 0
        const val MENU_FAVOURITES_INDEX = 1
        const val MENU_SETTINGS_INDEX = 2
        const val MENU_ALPHA_SORT_INDEX = 3
        const val MENU_DATE_SORT_INDEX = 4
        const val MENU_CLEAR_FAVOURITES_INDEX = 5
        const val MENU_EMAIL_FAVOURITES_INDEX = 6

        const val MENUGROUP_INFO = 0
        const val MENUGROUP_ACTIONS = 1
    }
}
