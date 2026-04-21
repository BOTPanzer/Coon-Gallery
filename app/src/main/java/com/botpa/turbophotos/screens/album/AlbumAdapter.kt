package com.botpa.turbophotos.screens.album

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

class AlbumAdapter(
    context: Context,
    items: List<Item>,
    private val selected: Set<Int>,
    private var showMissingMetadataIcon: Boolean
) : CustomAdapter<Item, AlbumAdapter.ItemHolder>(context, items) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(inflateView(context, R.layout.album_item, viewGroup))
    }

    override fun onInitViewHolder(holder: ItemHolder, item: Item) {
        //Load item preview
        Item.load(context, holder.image, item)

        //Toggle is video & missing info icons
        holder.isVideo.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        holder.isFavourite.visibility = if (item.isFavourite) View.VISIBLE else View.GONE
        holder.missingInfo.visibility = if (!showMissingMetadataIcon || item.album.hasMetadataKey(item.name)) View.GONE else View.VISIBLE

        //Toggle is selected
        if (selected.contains(holder.bindingAdapterPosition)) {
            holder.isSelected.visibility = View.VISIBLE
            holder.imageCard.scaleX = 0.8f
            holder.imageCard.scaleY = 0.8f
        } else {
            holder.isSelected.visibility = View.GONE
            holder.imageCard.scaleX = 1.0f
            holder.imageCard.scaleY = 1.0f
        }
    }

    override fun onViewRecycled(holder: ItemHolder) {
        super.onViewRecycled(holder)

        //Cancel pending loads
        Glide.with(context).clear(holder.image)
    }

    //Holder
    class ItemHolder(root: View) : RecyclerView.ViewHolder(root) {

        val background: View = root.findViewById(R.id.background)
        val imageCard: MaterialCardView = root.findViewById(R.id.imageCard)
        val image: ImageView = root.findViewById(R.id.image)
        val isVideo: View = root.findViewById(R.id.isVideo)
        val isFavourite: View = root.findViewById(R.id.isFavourite)
        val missingInfo: View = root.findViewById(R.id.missingInfo)
        val isSelected: View = root.findViewById(R.id.isSelected)

    }

}