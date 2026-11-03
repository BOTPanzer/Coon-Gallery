package com.botpa.turbophotos.gallery.views.lists

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ListSeparator(private val space: Int, private val isHorizontal: Boolean = false) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        //Get density & gap size info
        val density = parent.context.resources.displayMetrics.density
        val gapSize = (space * density).toInt()

        //Add space
        val position = parent.getChildAdapterPosition(view)
        if (position != 0) {
            if (isHorizontal) outRect.left = gapSize else outRect.top = gapSize
        }
    }

}