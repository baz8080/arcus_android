package com.arcuscomputing.dictionary.menu.impl;

import android.app.SearchManager;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.arcuscomputing.dictionary.ArcusSearchActivity;
import com.arcuscomputing.dictionary.menu.IArcusMenu;
import com.arcuscomputing.dictionarypro.parent.R;


public class ArcusMenu implements IArcusMenu {

    private Context context;
    private Menu menu;

    public ArcusMenu(Context context) {
        this.context = context;
    }

    public Menu getMenu() {
        return this.menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    private String getString(int id) {
        return this.context.getString(id);
    }

    @Override
    public boolean onCreateOptionsMenu() {
        menu.add(MENUGROUP_ACTIONS, MENU_SEARCH, MENU_SEARCH_INDEX, getString(R.string.menu_search))
                .setIcon(R.drawable.ic_search_white_24dp)
                .setAlphabeticShortcut(SearchManager.MENU_KEY).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(MENUGROUP_ACTIONS, MENU_FAVOURITES, MENU_FAVOURITES_INDEX, getString(R.string.menu_favourites))
                .setIcon(R.drawable.ic_star_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(MENUGROUP_ACTIONS, MENU_SETTINGS, MENU_SETTINGS_INDEX, getString(R.string.menu_settings))
                .setIcon(R.drawable.ic_settings_white_24dp);

        menu.add(MENUGROUP_INFO, MENU_HELP, MENU_HELP_INDEX, getString(R.string.menu_help))
                .setIcon(R.drawable.ic_help_white_24dp);

        menu.add(MENUGROUP_INFO, MENU_RANDOM, MENU_RANDOM_INDEX, getString(R.string.menu_random))
                .setIcon(R.drawable.ic_shuffle_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        // favs menus
        menu.add(MENUGROUP_INFO, MENU_ALPHA_SORT, MENU_ALPHA_SORT_INDEX, getString(R.string.menu_sort_alpha_asc))
                .setIcon(android.R.drawable.ic_menu_sort_alphabetically);

        menu.add(MENUGROUP_INFO, MENU_DATE_SORT, MENU_DATE_SORT_INDEX, getString(R.string.menu_sort_date_asc))
                .setIcon(android.R.drawable.ic_menu_month);

        menu.add(MENUGROUP_INFO, MENU_CLEAR_FAVOURITES, MENU_CLEAR_FAVOURITES_INDEX, getString(R.string.menu_clear_favourites))
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        menu.add(MENUGROUP_INFO, MENU_EMAIL_FAVOURITES, MENU_EMAIL_FAVOURITES_INDEX, getString(R.string.menu_email_favourites))
                .setIcon(android.R.drawable.ic_menu_send);

        menu.add(MENUGROUP_INFO, MENU_BACKUP, MENU_EMAIL_BACKUP_INDEX, "Backup & Restore")
                .setIcon(android.R.drawable.ic_menu_send);

        return true;
    }

    @Override
    public void setMainMenuItemsVisible(boolean visible) {
        menu.getItem(MENU_SEARCH_INDEX).setEnabled(visible);
        menu.getItem(MENU_SEARCH_INDEX).setVisible(visible);

        menu.getItem(MENU_FAVOURITES_INDEX).setEnabled(visible);
        menu.getItem(MENU_FAVOURITES_INDEX).setVisible(visible);

        menu.getItem(MENU_SETTINGS_INDEX).setEnabled(visible);
        menu.getItem(MENU_SETTINGS_INDEX).setVisible(visible);

        menu.getItem(MENU_HELP_INDEX).setEnabled(visible);
        menu.getItem(MENU_HELP_INDEX).setVisible(visible);

        menu.getItem(MENU_RANDOM_INDEX).setEnabled(visible);
        menu.getItem(MENU_RANDOM_INDEX).setVisible(visible);
    }

    @Override
    public void setFavouritesMenuItemVisible(boolean visible) {
        menu.getItem(MENU_ALPHA_SORT_INDEX).setEnabled(visible);
        menu.getItem(MENU_ALPHA_SORT_INDEX).setVisible(visible);

        menu.getItem(MENU_DATE_SORT_INDEX).setEnabled(visible);
        menu.getItem(MENU_DATE_SORT_INDEX).setVisible(visible);

        menu.getItem(MENU_CLEAR_FAVOURITES_INDEX).setEnabled(visible);
        menu.getItem(MENU_CLEAR_FAVOURITES_INDEX).setVisible(visible);

        menu.getItem(MENU_EMAIL_FAVOURITES_INDEX).setEnabled(visible);
        menu.getItem(MENU_EMAIL_FAVOURITES_INDEX).setVisible(visible);

        menu.getItem(MENU_EMAIL_BACKUP_INDEX).setEnabled(visible);
        menu.getItem(MENU_EMAIL_BACKUP_INDEX).setVisible(visible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item, ArcusSearchActivity activity) {
        switch (item.getItemId()) {
            case MENU_SEARCH:
                activity.handleSearchAction();
                return true;
            case MENU_FAVOURITES:
                activity.handleFavouritesAction();
                return true;
            case MENU_HELP:
                activity.handleHelpAction();
                return true;
            case MENU_SETTINGS:
                activity.handleSettingsAction();
                return true;
            case MENU_ALPHA_SORT:
                activity.handleAlphaSortAction();
                return true;
            case MENU_DATE_SORT:
                activity.handleDateSortAction();
                return true;
            case MENU_CLEAR_FAVOURITES:
                activity.handleClearFavouritesAction();
                return true;
            case MENU_EMAIL_FAVOURITES:
                activity.handleEmailFavouritesAction();
                return true;
            case MENU_RANDOM:
                activity.handleRandom();
                return true;
            case MENU_BACKUP:
                activity.handleBackup();
                return true;
        }

        return false;
    }

}
