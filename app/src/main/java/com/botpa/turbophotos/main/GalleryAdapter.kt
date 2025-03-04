package com.botpa.turbophotos.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.util.TurboImage
import com.bumptech.glide.Glide

class GalleryAdapter(private val context: Context, private val images: ArrayList<TurboImage>) : RecyclerView.Adapter<GalleryAdapter.GalleryHolder>() {

    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): GalleryHolder {
        val myView = LayoutInflater.from(context).inflate(R.layout.gallery_item, viewGroup, false)
        return GalleryHolder(myView)
    }

    override fun onBindViewHolder(holder: GalleryHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get image from images
        val image = images[position]

        //Load image in imageview
        val requestBuilder = Glide.with(holder.itemView.context).asBitmap().sizeMultiplier(0.3f)
        Glide.with(context).asBitmap().load(image.file.absolutePath).thumbnail(requestBuilder).into(holder.image)

        //Toggle missing info icon
        holder.missingInfo.visibility = if (image.album.metadata.has(image.file.name)) View.GONE else View.VISIBLE

        //Add click listeners
        holder.background.setOnClickListener { view: View ->
            onItemClickListener?.onItemClick(view, holder.bindingAdapterPosition)
        }

        holder.background.setOnLongClickListener { view: View ->
            if (onItemLongClickListener == null) return@setOnLongClickListener true
            else onItemLongClickListener!!.onItemLongClick(view, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    //Listeners
    fun interface OnItemClickListener {
        fun onItemClick(view: View, index: Int)
    }

    fun interface OnItemLongClickListener {
        fun onItemLongClick(view: View, index: Int): Boolean
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    fun setOnItemLongClickListener(onItemLongClickListener: OnItemLongClickListener?) {
        this.onItemLongClickListener = onItemLongClickListener
    }

    //Holder
    class GalleryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var background: View = itemView.findViewById(R.id.background)
        var image: ImageView = itemView.findViewById(R.id.image)
        var missingInfo: View = itemView.findViewById(R.id.missingInfo)
    }
}
