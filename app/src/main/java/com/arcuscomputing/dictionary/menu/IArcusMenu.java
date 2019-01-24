package com.arcuscomputing.dictionary.menu;


import android.view.Menu;
import android.view.MenuItem;

import com.arcuscomputing.dictionary.ArcusSearchActivity;

public interface IArcusMenu {

    // MENU
    int MENU_SEARCH = 0;

    int MENU_SETTINGS = 1;
    int MENU_HELP = 2;
    int MENU_FAVOURITES = 3;
    int MENU_RANDOM = 4;

    int MENU_ALPHA_SORT = 5;
    int MENU_DATE_SORT = 6;
    int MENU_CLEAR_FAVOURITES = 7;
    int MENU_EMAIL_FAVOURITES = 8;

    //MENU ORDER
    int MENU_SEARCH_INDEX = 0;
    int MENU_FAVOURITES_INDEX = 1;
    int MENU_SETTINGS_INDEX = 2;
    int MENU_HELP_INDEX = 3;

    int MENU_RANDOM_INDEX = 4;

    int MENU_ALPHA_SORT_INDEX = 5;
    int MENU_DATE_SORT_INDEX = 6;
    int MENU_CLEAR_FAVOURITES_INDEX = 7;
    int MENU_EMAIL_FAVOURITES_INDEX = 8;

    // MENU GROUP
    int MENUGROUP_INFO = 0;
    int MENUGROUP_ACTIONS = 1;


    // CONTEXT
    int CONTEXT_GOOGLE_DICTIONARY = 0;
    int CONTEXT_WIKITIONARY = 1;

    boolean onCreateOptionsMenu();

    void setMainMenuItemsVisible(boolean visible);

    void setFavouritesMenuItemVisible(boolean visible);

    boolean onOptionsItemSelected(MenuItem item, ArcusSearchActivity activity);

    Menu getMenu();

    void setMenu(Menu menu);
}
