package com.botpa.turbophotos.screens.display

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.CoonItem
import com.botpa.turbophotos.gallery.views.ZoomableLayout

class DisplayAdapter(
    private val context: Context,
    private val items: List<CoonItem>
) : RecyclerView.Adapter<DisplayAdapter.DisplayHolder>() {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): DisplayHolder {
        val myView = LayoutInflater.from(context).inflate(R.layout.display_item, viewGroup, false)
        return DisplayHolder(myView)
    }

    override fun onBindViewHolder(holder: DisplayHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get item
        val item = items[position]

        //Load image
        CoonItem.load(context, holder.image, item, true)

        //Toggle play video button
        holder.play.visibility = if (item.isVideo) View.VISIBLE else View.GONE

        //Click listener
        holder.zoom.setOnClick {
            onClickListener?.onItemClick(holder.zoom, holder.image, holder.bindingAdapterPosition)
        }

        holder.zoom.setOnZoomChanged {
            onZoomChangedListener?.onItemClick(holder.zoom, holder.image, holder.bindingAdapterPosition)
        }

        holder.zoom.setOnPointersChanged {
            onPointersChangedListener?.onItemClick(holder.zoom, holder.image, holder.bindingAdapterPosition)
        }

        holder.play.setOnClickListener {
            onPlayListener?.onItemClick(holder.zoom, holder.image, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Listeners
    private var onClickListener: Listener? = null
    private var onZoomChangedListener: Listener? = null
    private var onPointersChangedListener: Listener? = null
    private var onPlayListener: Listener? = null

    fun interface Listener {
        fun onItemClick(zoom: ZoomableLayout, image: ImageView, index: Int)
    }

    fun setOnClickListener(onClickListener: Listener?) {
        this.onClickListener = onClickListener
    }

    fun setOnZoomChangedListener(onZoomListener: Listener?) {
        this.onZoomChangedListener = onZoomListener
    }

    fun setOnPointersChangedListener(onZoomListener: Listener?) {
        this.onPointersChangedListener = onZoomListener
    }

    fun setOnPlayListener(onClickListener: Listener?) {
        this.onPlayListener = onClickListener
    }

    //Holder
    class DisplayHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var background: View = itemView.findViewById(R.id.background)
        var zoom: ZoomableLayout = itemView.findViewById(R.id.zoom)
        var image: ImageView = itemView.findViewById(R.id.image)
        var play: View = itemView.findViewById(R.id.play)

    }

}