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

    private var onClickListener: OnClickListener? = null
    private var onZoomListener: OnClickListener? = null


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

        //Click listener
        holder.image.setOnClick {
            onClickListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
        }

        holder.image.setOnZoomChange {
            onZoomListener?.onItemClick(holder.image, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    //Listeners
    fun interface OnClickListener {
        fun onItemClick(view: ZoomableImageView, index: Int)
    }

    fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    fun setOnZoomListener(onZoomListener: OnClickListener?) {
        this.onZoomListener = onZoomListener
    }

    //Holder
    class DisplayHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var background: View = itemView.findViewById(R.id.background)
        var image: ZoomableImageView = itemView.findViewById(R.id.image)
    }
}
