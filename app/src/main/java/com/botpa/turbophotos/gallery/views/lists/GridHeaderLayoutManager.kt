package com.botpa.turbophotos.gallery.views.lists

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

class GridHeaderLayoutManager(context: Context, spanCount: Int, private val isHeader: (position: Int) -> Boolean) : GridLayoutManager(context, spanCount) {

    init {
        spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (isHeader(position)) this@GridHeaderLayoutManager.spanCount else 1
            }
        }
    }

    override fun supportsPredictiveItemAnimations(): Boolean = false

}