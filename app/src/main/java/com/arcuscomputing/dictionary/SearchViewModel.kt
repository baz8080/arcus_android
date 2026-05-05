package com.arcuscomputing.dictionary

import androidx.lifecycle.ViewModel
import com.arcuscomputing.dictionary.FavouritesDbHelper.SortOrder

enum class Mode { Search, Favourites }

data class PreviousState(val mode: Mode, val text: String)

class SearchViewModel : ViewModel() {
    var mode: Mode = Mode.Search
    var previous: PreviousState? = null
    var sortMethod = SortOrder.DATE_DESC
}
