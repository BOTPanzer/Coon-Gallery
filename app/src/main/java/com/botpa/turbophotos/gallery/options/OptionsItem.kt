package com.botpa.turbophotos.gallery.options

class OptionsItem(
    val icon: Int,
    val name: String,
    val action: Runnable?
) {

    var isSeparator: Boolean = false

    constructor() : this(0, "", null) {
        this.isSeparator = true
    }

}