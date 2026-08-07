package com.botpa.turbophotos.screens.viewer

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class ViewerLayoutManager(context: Context) : LinearLayoutManager(context) {

    private var isScrollEnabled = true

    fun setScrollEnabled(flag: Boolean) {
        this.isScrollEnabled = flag
    }

    override fun canScrollHorizontally(): Boolean {
        return isScrollEnabled && super.canScrollHorizontally()
    }

}