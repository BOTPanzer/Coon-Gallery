package com.botpa.turbophotos.screens.album

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.modals.core.CustomHeaderAdapter
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

@SuppressLint("SetTextI18n")
class AlbumAdapter(
    context: Context,
    items: List<Item>,
    var title: String,
    var subtitle: String,
    var topMargin: Int,
    private val selectedIndexes: Set<Int>,
    val showMissingMetadataIcon: Boolean
) : CustomHeaderAdapter<Item, AlbumAdapter.HeaderHolder, AlbumAdapter.ItemHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflateView(context, R.layout.album_header, parent))
        } else {
            ItemHolder(inflateView(context, R.layout.album_item, parent))
        }
    }

    override fun onInitHeaderHolder(holder: HeaderHolder) {
        //Load header image
        if (items.isNotEmpty()) {
            Item.load(context, holder.image, items.first())
        } else {
            holder.image.setImageDrawable(null)
        }

        //Update text
        holder.title.text = title
        holder.info.text = subtitle

        //Update top margin
        val params = holder.topMargin.layoutParams as LinearLayout.LayoutParams
        params.height = this@AlbumAdapter.topMargin
        holder.topMargin.layoutParams = params
    }

    override fun onInitItemHolder(holder: ItemHolder, item: Item) {
        //Load item preview
        Item.load(context, holder.image, item)

        //Toggle is video & missing info icons
        holder.isVideo.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        holder.isFavourite.visibility = if (item.isFavourite) View.VISIBLE else View.GONE
        holder.missingInfo.visibility = if (!showMissingMetadataIcon || item.album.hasMetadataKey(item.name)) View.GONE else View.VISIBLE

        //Toggle is selected
        if (selectedIndexes.contains(holder.bindingAdapterPosition - positionOffset)) {
            holder.isSelected.visibility = View.VISIBLE
            holder.imageCard.scaleX = 0.8f
            holder.imageCard.scaleY = 0.8f
            holder.imageCard.radius = 10.0f * context.resources.displayMetrics.density
        } else {
            holder.isSelected.visibility = View.GONE
            holder.imageCard.scaleX = 1.0f
            holder.imageCard.scaleY = 1.0f
            holder.imageCard.radius = 0.0f
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        //Cancel pending loads
        if (holder is ItemHolder) {
            Glide.with(context).clear(holder.image)
        }
    }

    //Holders
    class HeaderHolder(root: View) : RecyclerView.ViewHolder(root) {

        val topMargin: View = root.findViewById(R.id.variableHeight)
        val image: ImageView = root.findViewById(R.id.image)
        val title: TextView = root.findViewById(R.id.title)
        val info: TextView = root.findViewById(R.id.info)

    }

    class ItemHolder(root: View) : RecyclerView.ViewHolder(root) {

        val imageCard: MaterialCardView = root.findViewById(R.id.imageCard)
        val image: ImageView = root.findViewById(R.id.image)
        val isVideo: View = root.findViewById(R.id.isVideo)
        val isFavourite: View = root.findViewById(R.id.isFavourite)
        val missingInfo: View = root.findViewById(R.id.missingInfo)
        val isSelected: View = root.findViewById(R.id.isSelected)

    }

}