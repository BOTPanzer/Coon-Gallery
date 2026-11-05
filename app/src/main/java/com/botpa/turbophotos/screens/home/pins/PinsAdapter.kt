package com.botpa.turbophotos.screens.home.pins

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
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
    private val isReordering: Boolean
) : CustomHeaderAdapter<Album, RecyclerView.ViewHolder, PinsAdapter.AlbumHolder, PinsAdapter.FooterHolder>(
    context,
    items,
    false,
    !isReordering
) {

    //Adapter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOOTER -> {
                FooterHolder(inflateView(context, R.layout.home_pins_footer, parent))
            }
            else -> {
                AlbumHolder(inflateView(context, if (isReordering) R.layout.drawer_pins_reorder_item else R.layout.home_pins_item, parent))
            }
        }
    }

    override fun onInitFooterHolder(holder: FooterHolder) {
        //Add listeners
        holder.card.setOnClickListener { view ->
            onEditPins?.run()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onInitItemHolder(holder: AlbumHolder, album: Album) {
        //Update image
        if (album.isEmpty()) {
            //Default
            holder.mainContainer.visibility = View.VISIBLE
            holder.mainIcon.setImageResource(getAlbumIcon(album))

            //Alt
            holder.altContainer.visibility = View.GONE
        } else {
            //Default
            holder.mainContainer.visibility = View.GONE

            //Alt
            holder.altContainer.visibility = View.VISIBLE
            Item.load(context, holder.altImage, album.get(0))
            holder.altIcon?.setImageResource(getAlbumIcon(album))
        }

        //Update name
        holder.name.text = getAlbumName(album)

        //Add listeners
        holder.card.setOnClickListener { view ->
            onClick?.run(album, holder.bindingAdapterPosition)
        }

        holder.handle?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                touchHelper?.startDrag(holder)
            }
            false
        }
    }

    //List & touch helper
    var list: RecyclerView? = null
        private set
    var touchHelper: ItemTouchHelper? = null

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
            else -> R.drawable.icon_album
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
        val name: TextView = root.findViewById(R.id.itemName)
        val handle: View? = root.findViewById(R.id.itemHandle)

        //Main
        val mainContainer: View = root.findViewById(R.id.itemMainContainer)
        val mainIcon: ImageView = root.findViewById(R.id.itemMainIcon)

        //Alt
        val altContainer: View = root.findViewById(R.id.itemAltContainer)
        val altIcon: ImageView? = root.findViewById(R.id.itemAltIcon)
        val altImage: ImageView = root.findViewById(R.id.itemAltImage)

    }

}