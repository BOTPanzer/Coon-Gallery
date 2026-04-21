package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.Item
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter

class AlbumsDialogAdapter(context: Context, albums: List<Album>) : CustomAdapter<Album, AlbumsDialogAdapter.AlbumHolder>(context, albums) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): AlbumHolder {
        return AlbumHolder(inflateView(context, R.layout.dialog_albums_item, viewGroup))
    }

    override fun onInitViewHolder(holder: AlbumHolder, album: Album) {
        //Load album cover
        if (album.isEmpty()) {
            holder.image.setImageDrawable(null)
        } else {
            Item.load(context, holder.image, album.get(0))
        }

        //Update text
        holder.name.text = album.name
        holder.info.text = "${album.size()} items"

        //Add listeners
        holder.item.setOnClickListener { view: View ->
            onClick?.run(view, album)
        }
    }

    //Listeners
    var onClick: ClickListener? = null

    fun interface ClickListener {
        fun run(view: View, album: Album)
    }

    //Holder
    class AlbumHolder(root: View) : RecyclerView.ViewHolder(root) {

        var item: View = root.findViewById(R.id.albumItem)
        var image: ImageView = root.findViewById(R.id.albumImage)
        var name: TextView = root.findViewById(R.id.albumName)
        var info: TextView = root.findViewById(R.id.albumInfo)

    }

}