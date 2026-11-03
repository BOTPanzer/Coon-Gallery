package com.botpa.turbophotos.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.Item
import com.botpa.turbophotos.gallery.modals.core.CustomHeaderAdapter
import com.botpa.turbophotos.gallery.views.lists.ListSeparator
import com.botpa.turbophotos.screens.home.pins.PinsAdapter
import com.bumptech.glide.Glide

@SuppressLint("SetTextI18n")
class HomeAdapter(
    context: Context,
    albums: List<Album>,
    private val pins: MutableList<Album>
) : CustomHeaderAdapter<Album, HomeAdapter.HeaderHolder, HomeAdapter.AlbumHolder, RecyclerView.ViewHolder>(
    context,
    albums,
    true,
    false
) {

    //Pins
    val pinsAdapter = PinsAdapter(context, pins, false)

    //Adapter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                HeaderHolder(inflateView(context, R.layout.home_header, parent))
            }
            else -> {
                AlbumHolder(inflateView(context, R.layout.home_item, parent))
            }
        }
    }

    override fun onInitHeaderHolder(holder: HeaderHolder) {
        //Create adapter
        pinsAdapter.onClick = { album, position ->
            onItemClick(holder.list.layoutManager?.findViewByPosition(position)!!, album)
        }

        pinsAdapter.onEditPins = {
            onEditPins?.run()
        }

        //Init list
        holder.list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        holder.list.adapter = pinsAdapter
        for (i in 0 until holder.list.itemDecorationCount) holder.list.removeItemDecorationAt(0)
        holder.list.addItemDecoration(ListSeparator(3, true))
    }

    override fun onInitItemHolder(holder: AlbumHolder, album: Album) {
        //Load album cover
        loadAlbumCover(holder.image, album)

        //Update text
        holder.name.text = album.name
        holder.info.text = context.getString(R.string.items, album.size)

        //Add listeners
        addAlbumListener(holder.root, album)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        //Cancel pending loads
        if (holder is AlbumHolder) {
            Glide.with(context).clear(holder.image)
        }
    }

    //Util
    private fun loadAlbumCover(image: ImageView, album: Album?): Boolean {
        if (album == null || album.isEmpty()) {
            image.setImageDrawable(null)
            return false
        } else {
            Item.load(context, image, album.get(0))
            return true
        }
    }

    private fun addAlbumListener(view: View, album: Album) {
        view.setOnClickListener { view ->
            onItemClick(view, album)
        }

        view.setOnLongClickListener { view ->
            onItemLongClick(view, album)
        }
    }

    private fun onItemClick(view: View, album: Album) {
        if (album.isNotEmpty()) onClick?.run(view, album)
    }

    private fun onItemLongClick(view: View, album: Album): Boolean {
        if (album.isNotEmpty()) onLongClick?.run(view, album)
        return true
    }

    fun getIndexFromAlbum(album: Album): Int = items.indexOf(album)

    //Listeners
    var onEditPins: SimpleClickListener? = null

    fun interface SimpleClickListener {
        fun run()
    }

    var onClick: ClickListener? = null
    var onLongClick: ClickListener? = null

    fun interface ClickListener {
        fun run(view: View, album: Album)
    }

    //Holders
    class HeaderHolder(root: View) : RecyclerView.ViewHolder(root) {

        val list: RecyclerView = root.findViewById(R.id.pinsList)

    }

    class AlbumHolder(val root: View) : RecyclerView.ViewHolder(root) {

        val image: ImageView = root.findViewById(R.id.image)
        val name: TextView = root.findViewById(R.id.name)
        val info: TextView = root.findViewById(R.id.info)

    }

}
