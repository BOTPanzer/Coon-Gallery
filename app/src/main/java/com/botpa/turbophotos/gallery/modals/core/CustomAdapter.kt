package com.botpa.turbophotos.gallery.modals.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class CustomAdapter<T, VH: RecyclerView.ViewHolder>(
    protected val context: Context,
    protected val items: List<T>
) : RecyclerView.Adapter<VH>() {

    //Adapter
    override fun onBindViewHolder(holder: VH, position: Int) {
        //Init item
        onInitViewHolder(holder, items[position])
    }

    override fun getItemCount(): Int = items.size

    //Custom
    protected open fun onInitViewHolder(holder: VH, item: T) {}

    //Util
    companion object {
        fun inflateView(context: Context, layout: Int, root: ViewGroup): View {
            return LayoutInflater.from(context).inflate(layout, root, false)
        }
    }

}