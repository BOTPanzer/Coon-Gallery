package com.botpa.turbophotos.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.util.TurboImage
import com.bumptech.glide.Glide

class DisplayAdapter(private val context: Context, private val images: ArrayList<TurboImage>) : RecyclerView.Adapter<DisplayAdapter.DisplayHolder>() {

    //Listeners
    private var onClickListener: Listener? = null
    private var onZoomListener: Listener? = null
    private var onPlayListener: Listener? = null


    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): DisplayHolder {
        val myView = LayoutInflater.from(context).inflate(R.layout.display_item, viewGroup, false)
        return DisplayHolder(myView)
    }

    override fun onBindViewHolder(holder: DisplayHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get image from images
        val image = images[position]

        //Load image in imageview
        Glide.with(context).asBitmap().load(image.file.absolutePath).into(holder.image)

        //Toggle play video button
        holder.play.visibility = if (image.isVideo) View.VISIBLE else View.GONE

        //Click listener
        holder.image.setOnClick {
            onClickListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
        }

        holder.image.setOnZoomChange {
            onZoomListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
        }

        holder.play.setOnClickListener {
            onPlayListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    //Listeners
    fun interface Listener {
        fun onItemClick(view: ZoomableImageView, index: Int)
    }

    fun setOnClickListener(onClickListener: Listener?) {
        this.onClickListener = onClickListener
    }

    fun setOnZoomListener(onZoomListener: Listener?) {
        this.onZoomListener = onZoomListener
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
