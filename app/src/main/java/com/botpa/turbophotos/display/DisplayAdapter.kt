package com.botpa.turbophotos.display

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.CoonItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.signature.ObjectKey

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

        //Load image in imageview
        Glide.with(context)
            .asBitmap()
            .load(item.file.absolutePath)
            .signature(ObjectKey(item.lastModified))
            .into(holder.image)

        //Toggle play video button
        holder.play.visibility = if (item.isVideo) View.VISIBLE else View.GONE

        //Click listener
        holder.image.setOnClick {
            onClickListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
        }

        holder.image.setOnZoomChanged {
            onZoomChangedListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
        }

        holder.image.setOnPointersChanged {
            onPointersChangedListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
        }

        holder.play.setOnClickListener {
            onPlayListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
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
        fun onItemClick(view: ZoomableImageView, index: Int)
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
        var image: ZoomableImageView = itemView.findViewById(R.id.image)
        var play: View = itemView.findViewById(R.id.play)

    }

}