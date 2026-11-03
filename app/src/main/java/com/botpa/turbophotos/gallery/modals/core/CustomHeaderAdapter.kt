package com.botpa.turbophotos.gallery.modals.core

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class CustomHeaderAdapter<T, HeaderVH: RecyclerView.ViewHolder, ItemVH: RecyclerView.ViewHolder, FooterVH: RecyclerView.ViewHolder>(
    protected val context: Context,
    protected val items: List<T>,
    protected var hasHeader: Boolean,
    protected var hasFooter: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //Adapter
    companion object {
        protected const val TYPE_HEADER = 0
        protected const val TYPE_ITEM = 1
        protected const val TYPE_FOOTER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasHeader) {
            if (position == 0) {
                TYPE_HEADER
            } else if (hasFooter && position >= items.size + 1) {
                TYPE_FOOTER
            } else {
                TYPE_ITEM
            }
        } else if (hasFooter && position >= items.size) {
            TYPE_FOOTER
        } else {
            TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //Get view type
        val fixedPosition = holder.bindingAdapterPosition
        val type = getItemViewType(fixedPosition)

        //Init item
        when (type) {
            TYPE_HEADER -> {
                onInitHeaderHolder(holder as HeaderVH)
            }
            TYPE_ITEM -> {
                val index = fixedPosition - positionOffset
                onInitItemHolder(holder as ItemVH, items[index])
            }
            TYPE_FOOTER -> {
                onInitFooterHolder(holder as FooterVH)
            }
        }
    }

    override fun getItemCount(): Int = items.size + positionOffset + if (hasFooter) 1 else 0

    //Custom
    protected open fun onInitHeaderHolder(holder: HeaderVH) {}

    protected open fun onInitItemHolder(holder: ItemVH, item: T) {}

    protected open fun onInitFooterHolder(holder: FooterVH) {}

    //Util
    val positionOffset: Int get() = if (hasHeader) 1 else 0

    fun getPositionFromIndex(index: Int): Int = index + positionOffset

    fun getIndexFromPosition(position: Int): Int = position - positionOffset

    fun inflateView(context: Context, layout: Int, root: ViewGroup): View {
        return CustomAdapter.inflateView(context, layout, root)
    }

}