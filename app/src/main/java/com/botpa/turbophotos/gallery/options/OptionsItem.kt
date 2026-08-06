package com.botpa.turbophotos.gallery.options

import android.content.Context

class OptionsItem(val icon: Int, val name: String, val action: () -> Unit) {

    constructor(context: Context, icon: Int, name: Int, action: () -> Unit) : this(icon, context.getString(name), action)

}