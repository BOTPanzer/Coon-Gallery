package com.botpa.turbophotos.screens.home.sorting

import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.data.SortDirection
import com.botpa.turbophotos.gallery.data.SortMethod

class SortInfo(var method: SortMethod, var direction: SortDirection) {

    var isSelected: Boolean = Library.sortMethod == method && Library.sortDirection == direction

}