package com.botpa.turbophotos.screens.home.sorting

import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.data.SortDirection
import com.botpa.turbophotos.gallery.data.SortMethod
import com.botpa.turbophotos.gallery.data.SortRules

class SortingItem(method: SortMethod, direction: SortDirection) : SortRules(method, direction) {

    var isSelected: Boolean = Library.sortMethod == method && Library.sortDirection == direction

}