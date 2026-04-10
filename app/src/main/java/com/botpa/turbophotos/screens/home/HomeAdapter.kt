package com.botpa.turbophotos.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.CoonItem

@SuppressLint("SetTextI18n")
class HomeAdapter(
    private val context: Context,
    private val albums: List<Album>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ALBUM = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        // Return a unique constant for the header, and the album's hash or index for others
        return if (position == 0) -1L else albums[position - getPositionOffset()].hashCode().toLong()
    }

    //Adapter
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ALBUM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(LayoutInflater.from(context).inflate(R.layout.home_header, parent, false))
        } else {
            AlbumHolder(LayoutInflater.from(context).inflate(R.layout.home_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //Get holder position & album
        val position = holder.bindingAdapterPosition - getPositionOffset()

        //Check holder type
        if (holder is HeaderHolder) {
            //Load album covers
            loadAlbumCover(holder.allImage, Library.all)
            loadAlbumCover(holder.favouritesImage, Library.favourites)
            loadAlbumCover(holder.trashImage, Library.trash)

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

    override fun getItemCount(): Int {
        return albums.size + getPositionOffset()
    }

    //Helpers
    private fun loadAlbumCover(image: ImageView, album: Album) {
        if (album.isEmpty()) {
            image.setImageDrawable(null)
        } else {
            CoonItem.load(context, image, album.get(0))
        }
    }

    private fun addAlbumListener(view: View, album: Album) {
        if (album.isEmpty()) return
        view.setOnClickListener { view -> onClickListener?.onClick(view, album) }
    }

    private fun getPositionOffset(): Int {
        //return if (Library.trash.isEmpty()) 1 else 2
        return 1
    }

    fun getPositionFromIndex(index: Int): Int {
        return index + getPositionOffset()
    }

    fun getIndexFromAlbum(album: Album): Int {
        return when (album) {
            Library.trash -> -2
            Library.all -> -1
            else -> albums.indexOf(album)
        }
    }

    //Listeners
    private var onClickListener: OnClickListener? = null

    fun interface OnClickListener { fun onClick(view: View, album: Album) }

    fun setOnClickListener(onClickListener: OnClickListener?) { this.onClickListener = onClickListener }

    //Holders
    class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var all: View = itemView.findViewById(R.id.all)
        var allImage: ImageView = itemView.findViewById(R.id.allImage)
        var allInfo: TextView = itemView.findViewById(R.id.allInfo)

        var favourites: View = itemView.findViewById(R.id.favourites)
        var favouritesImage: ImageView = itemView.findViewById(R.id.favouritesImage)
        var favouritesInfo: TextView = itemView.findViewById(R.id.favouritesInfo)

        var trash: View = itemView.findViewById(R.id.trash)
        var trashImage: ImageView = itemView.findViewById(R.id.trashImage)
        var trashInfo: TextView = itemView.findViewById(R.id.trashInfo)

    }

    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var background: View = itemView.findViewById(R.id.background)
        var image: ImageView = itemView.findViewById(R.id.image)
        var name: TextView = itemView.findViewById(R.id.name)
        var info: TextView = itemView.findViewById(R.id.info)

    }

}
