package com.botpa.turbophotos.screens.album

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.data.Item
import com.botpa.turbophotos.gallery.data.SortMethod
import com.botpa.turbophotos.gallery.modals.core.CustomHeaderAdapter
import com.botpa.turbophotos.gallery.views.lists.fastscroller.IFastScrollerPopup
import com.botpa.turbophotos.util.Orion
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import java.util.Date

@SuppressLint("SetTextI18n")
class AlbumAdapter(
    context: Context,
    items: List<Item>,
    var title: String,
    var subtitle: String,
    var topMargin: Int,
    private val selectedIndexes: Set<Int>,
    val showMissingMetadataIcon: Boolean
) : IFastScrollerPopup, CustomHeaderAdapter<Item, AlbumAdapter.HeaderHolder, AlbumAdapter.ItemHolder, RecyclerView.ViewHolder>(
    context,
    items,
    true,
    false
) {

    //Adapter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                HeaderHolder(inflateView(context, R.layout.album_header, parent))
            }
            else -> {
                ItemHolder(inflateView(context, R.layout.album_item, parent))
            }
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
        params.height = topMargin
        holder.topMargin.layoutParams = params
    }

    override fun onInitItemHolder(holder: ItemHolder, item: Item) {
        //Load item preview
        Item.load(context, holder.image, item)

        //Toggle is video & missing info icons
        holder.isVideo.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        holder.isFavourite.visibility = if (item.isFavourite) View.VISIBLE else View.GONE
        holder.isMissingInfo.visibility = if (showMissingMetadataIcon && item.album.isMetadataLoaded && !item.album.hasMetadataOfKey(item.name)) View.VISIBLE else View.GONE
        holder.badges.visibility = if (holder.isVideo.isVisible || holder.isFavourite.isVisible || holder.isMissingInfo.isVisible) View.VISIBLE else View.GONE

        //Toggle is selected
        val index = holder.bindingAdapterPosition - positionOffset
        if (selectedIndexes.contains(index)) {
            holder.isSelected.visibility = View.VISIBLE
            holder.imageCard.scaleX = 0.8f
            holder.imageCard.scaleY = 0.8f
            holder.imageCard.radius = 10.0f * context.resources.displayMetrics.density
            holder.isSelectedNumber.text = "${(selectedIndexes.indexOf(index) + 1)}"
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

    override fun getPopupStringForPosition(position: Int): String? {
        //Get item
        var index = getIndexFromPosition(position)
        if (index < 0) {
            index = 0
        } else if (index >= items.size) {
            return null
        }
        val item = items[index]

        //Return popup string
        return when (Library.sortMethod) {
            //Date
            SortMethod.Date -> {
                Orion.formatDate(context, Date(item.lastModified * 1000))
            }
            //Name
            SortMethod.Name -> {
                item.name
            }
            //Size
            SortMethod.Size -> {
                Orion.formatSize(item.size)
            }
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
        val badges: View = root.findViewById(R.id.badges)
        val isVideo: View = root.findViewById(R.id.isVideo)
        val isFavourite: View = root.findViewById(R.id.isFavourite)
        val isMissingInfo: View = root.findViewById(R.id.isMissingInfo)
        val isSelected: View = root.findViewById(R.id.isSelected)
        val isSelectedNumber: TextView = root.findViewById(R.id.isSelectedNumber)

    }

}