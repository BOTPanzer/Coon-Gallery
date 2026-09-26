package com.botpa.turbophotos.gallery.data

enum class SortMethod { Date, Name }
enum class SortDirection { Ascending, Descending }

open class SortRules(var method: SortMethod, var direction: SortDirection)
