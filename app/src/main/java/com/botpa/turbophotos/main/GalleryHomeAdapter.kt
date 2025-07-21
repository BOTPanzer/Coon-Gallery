package com.botpa.turbophotos.main

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.util.Album
import com.botpa.turbophotos.util.Library
import com.botpa.turbophotos.util.TurboFile

@SuppressLint("SetTextI18n")
class GalleryHomeAdapter(private val context: Context, private val albums: ArrayList<Album>) : RecyclerView.Adapter<GalleryHomeAdapter.AlbumHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): AlbumHolder {
        return AlbumHolder(LayoutInflater.from(context).inflate(R.layout.gallery_album, viewGroup, false))
    }

    override fun onBindViewHolder(holder: AlbumHolder, i: Int) {
        //Get holder position & album
        val position = holder.bindingAdapterPosition - getIndexOffset()
        val album = getAlbumFromIndex(position)

        //Load cover
        if (album.isNotEmpty()) TurboFile.load(context, holder.image, album.get(0))

        //Update icons
        holder.isTrash.visibility = if (album == Library.trash) View.VISIBLE else View.GONE
        holder.isAll.visibility = if (album == Library.all) View.VISIBLE else View.GONE

        //Update text
        holder.name.text = album.name
        holder.info.text = album.size().toString() + " items"

        //Add listeners
        holder.background.setOnClickListener { view: View -> onClickListener?.onClick(view, album) }
    }

    override fun getItemCount(): Int {
        return albums.size + getIndexOffset()
    }

    //Helpers
    private fun getIndexOffset(): Int {
        return if (Library.trash.isEmpty()) 1 else 2
    }

    private fun getAlbumFromIndex(index: Int): Album {
        return when (index) {
            -2 -> Library.trash
            -1 -> Library.all
            else -> albums[index]
        }
    }

    //Listeners
    private var onClickListener: OnClickListener? = null

    fun interface OnClickListener {
        fun onClick(view: View, album: Album)
    }

    fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    //Holder
    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var background: View = itemView.findViewById(R.id.background)
        var image: ImageView = itemView.findViewById(R.id.image)
        var isTrash: View = itemView.findViewById(R.id.isTrash)
        var isAll: View = itemView.findViewById(R.id.isAll)
        var name: TextView = itemView.findViewById(R.id.name)
        var info: TextView = itemView.findViewById(R.id.info)

    }

}
