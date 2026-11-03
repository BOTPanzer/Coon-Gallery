package com.botpa.turbophotos.screens.home.pins

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.Item
import com.botpa.turbophotos.gallery.modals.core.CustomHeaderAdapter
import com.google.android.material.card.MaterialCardView

class PinsAdapter(
    context: Context,
    items: List<Album>,
    private val isSorting: Boolean
) : CustomHeaderAdapter<Album, RecyclerView.ViewHolder, PinsAdapter.AlbumHolder, PinsAdapter.FooterHolder>(
    context,
    items,
    false,
    !isSorting
) {

    //Adapter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOOTER -> {
                FooterHolder(inflateView(context, R.layout.home_pins_footer, parent))
            }
            else -> {
                AlbumHolder(inflateView(context, if (isSorting) R.layout.home_pins_sort_item else R.layout.home_pins_item, parent))
            }
        }
    }

    override fun onInitFooterHolder(holder: FooterHolder) {
        //Add listeners
        holder.card.setOnClickListener { view ->
            onEditPins?.run()
        }
    }

    override fun onInitItemHolder(holder: AlbumHolder, album: Album) {
        //Update image
        if (album.isEmpty()) {
            holder.image.setImageDrawable(null)
            holder.icon.setImageResource(getAlbumIcon(album))
            holder.image.visibility = View.GONE
            holder.icon.visibility = View.VISIBLE
        } else {
            Item.load(context, holder.image, album.get(0))
            holder.image.visibility = View.VISIBLE
            holder.icon.visibility = View.GONE
        }

        //Update name
        holder.name.text = getAlbumName(album)

        //Add listeners
        holder.card.setOnClickListener { view ->
            onClick?.run(album, holder.bindingAdapterPosition)
        }
    }

    //List
    var list: RecyclerView? = null
        private set

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.list = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.list = null
    }

    //Listeners
    var onEditPins: SimpleClickListener? = null
    var onClick: ClickListener<Album>? = null

    fun interface SimpleClickListener {
        fun run()
    }

    fun interface ClickListener<T> {
        fun run(item: T, position: Int)
    }

    //Util
    private fun getAlbumIcon(album: Album?): Int {
        return when (album) {
            null -> R.drawable.icon_action_edit
            Library.all -> R.drawable.icon_filter_all
            Library.trash -> R.drawable.icon_action_trash
            Library.favourites -> R.drawable.icon_action_favourite_off
            else -> R.drawable.icon_filter_all
        }
    }

    private fun getAlbumName(album: Album): String {
        return when (album) {
            Library.all -> context.getString(R.string.library_album_all)
            Library.trash -> context.getString(R.string.library_album_trash)
            Library.favourites -> context.getString(R.string.library_album_favourites)
            else -> album.name
        }
    }

    //Footers
    class FooterHolder(root: View) : RecyclerView.ViewHolder(root) {

        val card: MaterialCardView = root.findViewById(R.id.itemCard)

    }

    class AlbumHolder(root: View) : RecyclerView.ViewHolder(root) {

        val card: View = root.findViewById(R.id.itemCard)
        val image: ImageView = root.findViewById(R.id.itemImage)
        val icon: ImageView = root.findViewById(R.id.itemIcon)
        val name: TextView = root.findViewById(R.id.itemName)

    }

}