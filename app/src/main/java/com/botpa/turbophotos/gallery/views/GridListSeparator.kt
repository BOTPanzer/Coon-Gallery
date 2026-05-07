package com.botpa.turbophotos.gallery.views

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridListSeparator(private val space: Int, var spanCount: Int, private val headerCount: Int = 0) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        //Get density & gap size info
        val density = parent.context.resources.displayMetrics.density
        val gapSize = (space * density).toInt()

        //Get position (skip the header)
        val position = parent.getChildAdapterPosition(view)
        if (position < headerCount) {
            outRect.set(0, 0, 0, 0)
            return
        }

        //Adjust position so the first grid item (pos 1) acts like index 0
        val adjPosition = position - headerCount
        val column = adjPosition % spanCount

        //Apply the same logic as before, but on the adjusted position
        outRect.left = column * gapSize / spanCount
        outRect.right = gapSize - (column + 1) * gapSize / spanCount

        //Add top space if not in the first row
        if (adjPosition >= spanCount) {
            outRect.top = gapSize
        } else {
            outRect.top = 0
        }
    }

}