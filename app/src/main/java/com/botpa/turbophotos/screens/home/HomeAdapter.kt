package com.botpa.turbophotos.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.Item
import com.botpa.turbophotos.gallery.modals.core.CustomHeaderAdapter
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import com.bumptech.glide.Glide
import java.io.File

@SuppressLint("SetTextI18n")
class HomeAdapter(
    context: Context,
    albums: List<Album>
) : CustomHeaderAdapter<Album, HomeAdapter.HeaderHolder, HomeAdapter.AlbumHolder>(context, albums) {

    //Adapter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflateView(context, R.layout.home_header, parent))
        } else {
            AlbumHolder(inflateView(context, R.layout.home_item, parent))
        }
    }

    override fun onInitHeaderHolder(holder: HeaderHolder) {
        //Get pinned album
        val pinnedFile = File(Storage.getString(StoragePairs.HOME_PINNED_ALBUM) ?: "")
        val pinnedAlbum = Library.albumsMap.getOrDefault(pinnedFile.absolutePath, null)
        val isPinnedValid = pinnedFile.exists() && pinnedFile.isDirectory
        val pinnedName = if (isPinnedValid) pinnedFile.name else context.getString(R.string.library_album_pinned)
        val pinnedSize = pinnedAlbum?.size ?: 0

        //Load album covers
        val isAllLoaded = loadAlbumCover(holder.allImage, Library.all)
        holder.allImage.visibility = if (isAllLoaded) View.VISIBLE else View.GONE
        holder.allIcon.visibility = if (isAllLoaded) View.GONE else View.VISIBLE
        val isTrashLoaded = loadAlbumCover(holder.trashImage, Library.trash)
        holder.trashImage.visibility = if (isTrashLoaded) View.VISIBLE else View.GONE
        holder.trashIcon.visibility = if (isTrashLoaded) View.GONE else View.VISIBLE
        val isFavouritesLoaded = loadAlbumCover(holder.favouritesImage, Library.favourites)
        holder.favouritesImage.visibility = if (isFavouritesLoaded) View.VISIBLE else View.GONE
        holder.favouritesIcon.visibility = if (isFavouritesLoaded) View.GONE else View.VISIBLE
        val isPinnedLoaded = loadAlbumCover(holder.pinnedImage, pinnedAlbum)
        holder.pinnedImage.visibility = if (isPinnedLoaded) View.VISIBLE else View.GONE
        holder.pinnedIcon.visibility = if (isPinnedLoaded) View.GONE else View.VISIBLE

        //Update text
        holder.allInfo.text = context.getString(R.string.items, Library.all.size)
        holder.trashInfo.text = context.getString(R.string.items, Library.trash.size)
        holder.favouritesInfo.text = context.getString(R.string.items, Library.favourites.size)
        holder.pinnedName.text = pinnedName
        holder.pinnedInfo.text = context.getString(R.string.items, pinnedSize)

        //Add listeners
        addAlbumListener(holder.all, Library.all)
        addAlbumListener(holder.trash, Library.trash)
        addAlbumListener(holder.favourites, Library.favourites)

        holder.pinned.setOnClickListener { view ->
            if (!isPinnedValid) {
                //Tell user how to pin an album
                Orion.snack(context as Activity, R.string.home_pinned_tutorial, duration = Orion.snackDurationLong)
            } else if (pinnedAlbum != null && pinnedAlbum.isNotEmpty()) {
                //Open album
                onClick?.run(view, pinnedAlbum)
            }
        }

        holder.pinned.setOnLongClickListener { view ->
            if (isPinnedValid) {
                //Reset pinned album
                Storage.putString(StoragePairs.HOME_PINNED_ALBUM, "")
                notifyItemChanged(0)
                Orion.snack(context as Activity, R.string.home_pinned_reset, duration = Orion.snackDurationLong)
            }
            true
        }
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

    private fun addAlbumListener(view: View, album: Album?) {
        view.setOnClickListener { view ->
            if (album != null && album.isNotEmpty()) onClick?.run(view, album)
        }

        view.setOnLongClickListener { view ->
            if (album != null && album.isNotEmpty()) onLongClick?.run(view, album)
            true
        }
    }

    fun getIndexFromAlbum(album: Album): Int = items.indexOf(album)

    //Listeners
    var onClick: ClickListener? = null
    var onLongClick: ClickListener? = null

    fun interface ClickListener {
        fun run(view: View, album: Album)
    }

    //Holders
    class HeaderHolder(root: View) : RecyclerView.ViewHolder(root) {

        val all: View = root.findViewById(R.id.all)
        val allImage: ImageView = root.findViewById(R.id.allImage)
        val allIcon: ImageView = root.findViewById(R.id.allIcon)
        val allInfo: TextView = root.findViewById(R.id.allInfo)

        val trash: View = root.findViewById(R.id.trash)
        val trashImage: ImageView = root.findViewById(R.id.trashImage)
        val trashIcon: ImageView = root.findViewById(R.id.trashIcon)
        val trashInfo: TextView = root.findViewById(R.id.trashInfo)

        val favourites: View = root.findViewById(R.id.favourites)
        val favouritesImage: ImageView = root.findViewById(R.id.favouritesImage)
        val favouritesIcon: ImageView = root.findViewById(R.id.favouritesIcon)
        val favouritesInfo: TextView = root.findViewById(R.id.favouritesInfo)

        val pinned: View = root.findViewById(R.id.pinned)
        val pinnedImage: ImageView = root.findViewById(R.id.pinnedImage)
        val pinnedIcon: ImageView = root.findViewById(R.id.pinnedIcon)
        val pinnedName: TextView = root.findViewById(R.id.pinnedName)
        val pinnedInfo: TextView = root.findViewById(R.id.pinnedInfo)

    }

    class AlbumHolder(val root: View) : RecyclerView.ViewHolder(root) {

        val image: ImageView = root.findViewById(R.id.image)
        val name: TextView = root.findViewById(R.id.name)
        val info: TextView = root.findViewById(R.id.info)

    }

}
