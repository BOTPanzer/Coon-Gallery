package com.botpa.turbophotos.gallery.modals.core

import android.content.Context
import androidx.recyclerview.widget.RecyclerView

abstract class SimpleCustomAdapter<T, VH: RecyclerView.ViewHolder>(
    context: Context,
    items: List<T>
) : CustomAdapter<T, VH>(context, items) {

    //Listeners
    var onClick: ClickListener<T>? = null
    var onLongClick: LongClickListener<T>? = null

    fun interface ClickListener<T> {
        fun run(item: T, position: Int)
    }

    fun interface LongClickListener<T> {
        fun run(item: T, position: Int): Boolean
    }

}