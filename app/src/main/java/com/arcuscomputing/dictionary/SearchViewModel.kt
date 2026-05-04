package com.arcuscomputing.dictionary

import androidx.lifecycle.ViewModel
import com.arcuscomputing.FavouritesDbHelper.SortOrder

class SearchViewModel : ViewModel() {
    var sortMethod = SortOrder.DATE_DESC
    var previousWord: String? = null
}
