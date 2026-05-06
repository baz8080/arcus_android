package com.arcuscomputing.dictionary

import androidx.lifecycle.ViewModel
import com.arcuscomputing.dictionary.FavouritesDbHelper.SortOrder

class FavouritesViewModel : ViewModel() {
    var sortMethod = SortOrder.DATE_DESC
}
