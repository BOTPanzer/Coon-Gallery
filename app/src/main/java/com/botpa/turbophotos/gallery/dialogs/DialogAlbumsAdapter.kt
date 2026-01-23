package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.CoonItem

class DialogAlbumsAdapter(
    private val context: Context,
    private val albums: List<Album>
) : RecyclerView.Adapter<DialogAlbumsAdapter.AlbumHolder>() {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): AlbumHolder {
        return AlbumHolder(LayoutInflater.from(context).inflate(R.layout.dialog_albums_item, viewGroup, false))
    }

    override fun onBindViewHolder(holder: AlbumHolder, i: Int) {
        //Get holder position & album
        val position = holder.bindingAdapterPosition
        val album = albums[position]

        //Load album cover
        if (album.isEmpty())
            holder.image.setImageDrawable(null)
        else
            CoonItem.load(context, holder.image, album.get(0))

        //Update text
        holder.name.text = album.name
        holder.info.text = "${album.size()} items"

        //Add listeners
        holder.background.setOnClickListener { view: View -> onClickListener?.onClick(view, album) }
    }

    override fun getItemCount(): Int {
        return albums.size
    }

    //Listeners
    private var onClickListener: OnClickListener? = null

    fun interface OnClickListener { fun onClick(view: View, album: Album) }

    fun setOnClickListener(onClickListener: OnClickListener?) { this.onClickListener = onClickListener }

    //Holder
    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var background: View = itemView.findViewById(R.id.background)
        var image: ImageView = itemView.findViewById(R.id.image)
        var name: TextView = itemView.findViewById(R.id.name)
        var info: TextView = itemView.findViewById(R.id.info)

    }

}