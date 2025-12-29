package com.botpa.turbophotos.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.TurboItem
import com.google.android.material.card.MaterialCardView

class GalleryAdapter(private val context: Context, private val items: ArrayList<TurboItem>, private val selected: HashSet<Int>, var showMissingMetadataIcon: Boolean) : RecyclerView.Adapter<GalleryAdapter.GalleryHolder>() {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): GalleryHolder {
        return GalleryHolder(LayoutInflater.from(context).inflate(R.layout.gallery_item, viewGroup, false))
    }

    override fun onBindViewHolder(holder: GalleryHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get item
        val item = items[position]

        //Load item preview
        TurboItem.Companion.load(context, holder.image, item)

        //Toggle is video & missing info icons
        holder.isVideo.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        holder.missingInfo.visibility = if (!showMissingMetadataIcon || item.album.hasMetadataKey(item.name)) View.GONE else View.VISIBLE

        //Toggle selected border
        holder.imageCard.strokeColor = if (selected.contains(position)) Orion.getColor(context, com.google.android.material.R.attr.colorOnSurface) else 0x00000000

        //Add click listeners
        holder.background.setOnClickListener { view: View ->
            onClickListener?.onClick(view, holder.bindingAdapterPosition)
        }

        holder.background.setOnLongClickListener { view: View ->
            onLongClickListener?.onLongClick(view, holder.bindingAdapterPosition) ?: true   //Return true when no long click listener
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Listeners
    private var onClickListener: OnClickListener? = null
    private var onLongClickListener: OnLongClickListener? = null

    fun interface OnClickListener {
        fun onClick(view: View, index: Int)
    }

    fun interface OnLongClickListener {
        fun onLongClick(view: View, index: Int): Boolean
    }

    fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    fun setOnLongClickListener(onLongClickListener: OnLongClickListener?) {
        this.onLongClickListener = onLongClickListener
    }

    //Holder
    class GalleryHolder(view: View) : RecyclerView.ViewHolder(view) {

        var background: View = view.findViewById(R.id.background)
        var imageCard: MaterialCardView = view.findViewById(R.id.imageCard)
        var image: ImageView = view.findViewById(R.id.image)
        var isVideo: View = view.findViewById(R.id.isVideo)
        var missingInfo: View = view.findViewById(R.id.missingInfo)

    }

}