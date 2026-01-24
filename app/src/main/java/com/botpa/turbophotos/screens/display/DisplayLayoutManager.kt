package com.botpa.turbophotos.screens.display

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class DisplayLayoutManager(context: Context) : LinearLayoutManager(context) {

    private var isScrollEnabled = true

    fun setScrollEnabled(flag: Boolean) {
        this.isScrollEnabled = flag
    }

    override fun canScrollHorizontally(): Boolean {
        return isScrollEnabled && super.canScrollHorizontally()
    }

}