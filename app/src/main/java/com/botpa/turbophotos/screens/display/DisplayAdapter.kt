package com.botpa.turbophotos.screens.display

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter
import com.botpa.turbophotos.gallery.views.ZoomableLayout

class DisplayAdapter(context: Context, items: List<Item>) : CustomAdapter<Item, DisplayAdapter.ItemHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(inflateView(context, R.layout.display_item, viewGroup))
    }

    override fun onInitViewHolder(holder: ItemHolder, item: Item) {
        //Load image
        Item.load(context, holder.image, item)

        //Toggle play video button
        holder.play.visibility = if (item.isVideo) View.VISIBLE else View.GONE

        //Add listeners
        holder.zoom.onClick = {
            onClick?.run(holder.zoom, holder.image, holder.bindingAdapterPosition)
        }

        holder.zoom.onZoomChanged = {
            onZoomChanged?.run(holder.zoom, holder.image, holder.bindingAdapterPosition)
        }

        holder.zoom.onPointersChanged = {
            onPointersChanged?.run(holder.zoom, holder.image, holder.bindingAdapterPosition)
        }

        holder.play.setOnClickListener {
            onPlay?.run(holder.zoom, holder.image, holder.bindingAdapterPosition)
        }
    }

    //Listeners
    var onClick: DisplayListener? = null
    var onZoomChanged: DisplayListener? = null
    var onPointersChanged: DisplayListener? = null
    var onPlay: DisplayListener? = null

    fun interface DisplayListener {
        fun run(zoom: ZoomableLayout, image: ImageView, position: Int)
    }

    //Holder
    class ItemHolder(root: View) : RecyclerView.ViewHolder(root) {

        val background: View = root.findViewById(R.id.background)
        val zoom: ZoomableLayout = root.findViewById(R.id.zoom)
        val image: ImageView = root.findViewById(R.id.image)
        val play: View = root.findViewById(R.id.play)

    }

}