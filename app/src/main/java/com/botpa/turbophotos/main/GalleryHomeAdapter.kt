package com.botpa.turbophotos.main

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

class GalleryHomeAdapter(private val context: Context, private val albums: ArrayList<Album>) : RecyclerView.Adapter<GalleryHomeAdapter.AlbumHolder>() {

    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): AlbumHolder {
        val myView = LayoutInflater.from(context).inflate(R.layout.gallery_album, viewGroup, false)
        return AlbumHolder(myView)
    }

    override fun onBindViewHolder(holder: AlbumHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition - 1

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
            if (album.files.isNotEmpty()) TurboFile.load(context, holder.image, album.files[0])

            //Update album name
            holder.name.text = album.name
            holder.info.text = album.files.size.toString() + " items"
        }

        //Add click listeners
        holder.background.setOnClickListener { view: View ->
            onItemClickListener?.onItemClick(view, album)
        }

        holder.background.setOnLongClickListener { view: View ->
            if (onItemLongClickListener == null) return@setOnLongClickListener true
            else onItemLongClickListener!!.onItemLongClick(view, holder.bindingAdapterPosition - 1)
        }
    }

    override fun getItemCount(): Int {
        return albums.size + if (Library.trash.files.isEmpty()) 1 else 2
    }

    //Helpers
    fun getAlbumFromIndex(index: Int): Album? {
        return if (index < 0)
            null
        else if (index >= albums.size)
            Library.trash
        else
            albums[index]
    }

    //Listeners
    fun interface OnItemClickListener {
        fun onItemClick(view: View, album: Album?)
    }

    fun interface OnItemLongClickListener {
        fun onItemLongClick(view: View, index: Int): Boolean
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    fun setOnItemLongClickListener(onItemLongClickListener: OnItemLongClickListener?) {
        this.onItemLongClickListener = onItemLongClickListener
    }

    //Holder
    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var background: View = itemView.findViewById(R.id.background)
        var image: ImageView = itemView.findViewById(R.id.image)
        var name: TextView = itemView.findViewById(R.id.name)
        var info: TextView = itemView.findViewById(R.id.info)
    }

}
