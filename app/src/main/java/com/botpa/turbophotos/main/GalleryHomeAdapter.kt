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
        val myView = LayoutInflater.from(context).inflate(R.layout.gallery_album, viewGroup, false)
        return AlbumHolder(myView)
    }

    override fun onBindViewHolder(holder: AlbumHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition - getIndexOffset()

        //Get album
        val album = getAlbumFromIndex(position)

        //Get album from albums
        if (album == null) {
            //No album -> Is all files

            //Load cover
            if (Library.allFiles.isNotEmpty()) TurboFile.load(context, holder.image, Library.allFiles[0])

            //Update album name
            holder.name.text = "All"
            holder.info.text = Library.allFiles.size.toString() + " items"
        } else {
            //Load cover
            if (album.isNotEmpty()) TurboFile.load(context, holder.image, album.get(0))

            //Update album name
            holder.name.text = album.name
            holder.info.text = album.size().toString() + " items"
        }

        //Add click listeners
        holder.background.setOnClickListener { view: View ->
            onClickListener?.onItemClick(view, album)
        }
    }

    override fun getItemCount(): Int {
        return albums.size + getIndexOffset()
    }

    //Helpers
    private fun getIndexOffset(): Int {
        return if (Library.trash.isEmpty()) 1 else 2
    }

    private fun getAlbumFromIndex(index: Int): Album? {
        return when (index) {
            -2 -> Library.trash
            -1 -> null
            else -> albums[index]
        }
    }

    //Listeners
    private var onClickListener: ClickListener? = null

    fun interface ClickListener {
        fun onItemClick(view: View, album: Album?)
    }

    fun setOnClickListener(onItemClickListener: ClickListener?) {
        this.onClickListener = onItemClickListener
    }

    //Holder
    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var background: View = itemView.findViewById(R.id.background)
        var image: ImageView = itemView.findViewById(R.id.image)
        var name: TextView = itemView.findViewById(R.id.name)
        var info: TextView = itemView.findViewById(R.id.info)
    }

}
