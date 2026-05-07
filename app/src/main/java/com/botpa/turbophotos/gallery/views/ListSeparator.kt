package com.botpa.turbophotos.gallery.views

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ListSeparator(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        //Get density & gap size info
        val density = parent.context.resources.displayMetrics.density
        val gapSize = (space * density).toInt()

        //Add space
        val position = parent.getChildAdapterPosition(view)
        if (position != 0) outRect.top = gapSize
    }

}