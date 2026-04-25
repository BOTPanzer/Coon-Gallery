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
import com.botpa.turbophotos.gallery.modals.core.SimpleCustomAdapter

class AlbumsDialogAdapter(context: Context, albums: List<Album>) : SimpleCustomAdapter<Album, AlbumsDialogAdapter.AlbumHolder>(context, albums) {

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
            onClick?.run(album, holder.bindingAdapterPosition)
        }
    }

    //Holder
    class AlbumHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root.findViewById(R.id.albumItem)
        val image: ImageView = root.findViewById(R.id.albumImage)
        val name: TextView = root.findViewById(R.id.albumName)
        val info: TextView = root.findViewById(R.id.albumInfo)

    }

}