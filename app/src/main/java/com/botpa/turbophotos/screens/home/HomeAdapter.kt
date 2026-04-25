package com.botpa.turbophotos.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter
import com.bumptech.glide.Glide

@SuppressLint("SetTextI18n")
class HomeAdapter(
    private val context: Context,
    private val albums: List<Album>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //Adapter
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ALBUM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ALBUM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(CustomAdapter.inflateView(context, R.layout.home_header, parent))
        } else {
            AlbumHolder(CustomAdapter.inflateView(context, R.layout.home_item, parent))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //Get position
        val position = holder.bindingAdapterPosition - positionOffset

        //Check holder type
        if (holder is HeaderHolder) {
            //Load album covers
            val allImageLoaded = loadAlbumCover(holder.allImage, Library.all)
            holder.allImage.visibility = if (allImageLoaded) View.VISIBLE else View.GONE
            holder.allIcon.visibility = if (allImageLoaded) View.GONE else View.VISIBLE
            val allFavouritesLoaded = loadAlbumCover(holder.favouritesImage, Library.favourites)
            holder.favouritesImage.visibility = if (allFavouritesLoaded) View.VISIBLE else View.GONE
            holder.favouritesIcon.visibility = if (allFavouritesLoaded) View.GONE else View.VISIBLE
            val allTrashLoaded = loadAlbumCover(holder.trashImage, Library.trash)
            holder.trashImage.visibility = if (allTrashLoaded) View.VISIBLE else View.GONE
            holder.trashIcon.visibility = if (allTrashLoaded) View.GONE else View.VISIBLE

            //Update text
            holder.allInfo.text = "${Library.all.size()} items"
            holder.favouritesInfo.text = "${Library.favourites.size()} items"
            holder.trashInfo.text = "${Library.trash.size()} items"

            //Add listeners
            addAlbumListener(holder.all, Library.all)
            addAlbumListener(holder.favourites, Library.favourites)
            addAlbumListener(holder.trash, Library.trash)
        } else if (holder is AlbumHolder) {
            //Get album
            val album = albums[position]

            //Load album cover
            loadAlbumCover(holder.image, album)

            //Update text
            holder.name.text = album.name
            holder.info.text = "${album.size()} items"

            //Add listeners
            addAlbumListener(holder.background, album)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        //Cancel pending loads
        if (holder is AlbumHolder) {
            Glide.with(context).clear(holder.image)
        }
    }

    override fun getItemCount(): Int = albums.size + positionOffset

    //Util
    private val positionOffset: Int get() = 1

    private fun loadAlbumCover(image: ImageView, album: Album): Boolean {
        if (album.isEmpty()) {
            image.setImageDrawable(null)
            return false
        } else {
            Item.load(context, image, album.get(0))
            return true
        }
    }

    private fun addAlbumListener(view: View, album: Album) {
        view.setOnClickListener { view ->
            if (album.isNotEmpty()) onClick?.run(view, album)
        }
    }

    fun getPositionFromIndex(index: Int): Int = index + positionOffset

    fun getIndexFromAlbum(album: Album): Int = albums.indexOf(album)

    //Listeners
    var onClick: ClickListener? = null

    fun interface ClickListener {
        fun run(view: View, album: Album)
    }

    //Holders
    class HeaderHolder(root: View) : RecyclerView.ViewHolder(root) {

        val all: View = root.findViewById(R.id.all)
        val allImage: ImageView = root.findViewById(R.id.allImage)
        val allIcon: ImageView = root.findViewById(R.id.allIcon)
        val allInfo: TextView = root.findViewById(R.id.allInfo)

        val favourites: View = root.findViewById(R.id.favourites)
        val favouritesImage: ImageView = root.findViewById(R.id.favouritesImage)
        val favouritesIcon: ImageView = root.findViewById(R.id.favouritesIcon)
        val favouritesInfo: TextView = root.findViewById(R.id.favouritesInfo)

        val trash: View = root.findViewById(R.id.trash)
        val trashImage: ImageView = root.findViewById(R.id.trashImage)
        val trashIcon: ImageView = root.findViewById(R.id.trashIcon)
        val trashInfo: TextView = root.findViewById(R.id.trashInfo)

    }

    class AlbumHolder(root: View) : RecyclerView.ViewHolder(root) {

        val background: View = root.findViewById(R.id.background)
        val image: ImageView = root.findViewById(R.id.image)
        val name: TextView = root.findViewById(R.id.name)
        val info: TextView = root.findViewById(R.id.info)

    }

}
