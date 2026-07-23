package com.botpa.turbophotos.screens.display

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter
import com.botpa.turbophotos.gallery.views.ZoomableLayout
import com.botpa.turbophotos.util.Orion

class DisplayAdapter(
    context: Context,
    items: List<Item>,
    private var showOverlay: Boolean,
    var bottomMargin: Int
) : CustomAdapter<Item, DisplayAdapter.ItemHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(inflateView(context, R.layout.display_item, viewGroup))
    }

    override fun onInitItemHolder(holder: ItemHolder, item: Item) {
        //Load image
        Item.load(context, holder.image, item)

        //Toggle play video button
        holder.play.visibility = if (item.isVideo) View.VISIBLE else View.GONE

        //Toggle overlay
        holder.toggleOverlay(showOverlay)

        //Update bottom margin
        val params = holder.bottomMargin.layoutParams as LinearLayout.LayoutParams
        params.height = bottomMargin
        holder.bottomMargin.layoutParams = params

        //Add listeners
        holder.zoom.onSingleClick = {
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

    //Actions
    fun toggleOverlay(show: Boolean, recyclerView: RecyclerView) {
        showOverlay = show
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val holder = recyclerView.getChildViewHolder(child) as? ItemHolder
            holder?.toggleOverlay(show)
        }
    }

    //Holder
    class ItemHolder(root: View) : RecyclerView.ViewHolder(root) {

        val background: View = root.findViewById(R.id.background)
        val zoom: ZoomableLayout = root.findViewById(R.id.zoom)
        val image: ImageView = root.findViewById(R.id.image)
        val overlay: View = root.findViewById(R.id.overlay)
        val play: View = root.findViewById(R.id.play)
        val bottomMargin: View = root.findViewById(R.id.bottomMargin)

        fun toggleOverlay(show: Boolean) {
            if (show) {
                Orion.animateShow(overlay)
            } else {
                Orion.animateHide(overlay)
            }
        }

    }

}