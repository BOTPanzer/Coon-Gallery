package com.botpa.turbophotos.gallery.modals.core

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class CustomHeaderAdapter<T, HeaderVH, ItemVH: RecyclerView.ViewHolder>(
    protected val context: Context,
    protected val items: List<T>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //Adapter
    companion object {
        protected const val TYPE_HEADER = 0
        protected const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //Get position
        val position = holder.bindingAdapterPosition - positionOffset

        //Init item
        if (position < 0) {
            onInitHeaderHolder(holder as HeaderVH)
        } else {
            onInitItemHolder(holder as ItemVH, items[position])
        }
    }

    override fun getItemCount(): Int = items.size + positionOffset

    //Custom
    protected open fun onInitHeaderHolder(holder: HeaderVH) {}

    protected open fun onInitItemHolder(holder: ItemVH, item: T) {}

    //Util
    val positionOffset: Int get() = 1

    fun getPositionFromIndex(index: Int): Int = index + positionOffset

    fun inflateView(context: Context, layout: Int, root: ViewGroup): View {
        return CustomAdapter.inflateView(context, layout, root)
    }

}