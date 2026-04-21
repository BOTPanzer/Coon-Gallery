package com.botpa.turbophotos.gallery.views

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ListSeparator(private val height: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val density = parent.context.resources.displayMetrics.density
        val gapPx = (height * density).toInt()

        val position = parent.getChildAdapterPosition(view)
        if (position != 0) outRect.top = gapPx
    }

}