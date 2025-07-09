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
import com.botpa.turbophotos.util.TurboImage

class AlbumsAdapter(private val context: Context, private val albums: ArrayList<Album>) : RecyclerView.Adapter<AlbumsAdapter.AlbumHolder>() {

    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): AlbumHolder {
        val myView = LayoutInflater.from(context).inflate(R.layout.gallery_album_item, viewGroup, false)
        return AlbumHolder(myView)
    }

    override fun onBindViewHolder(holder: AlbumHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition - 1

        //Get album from albums
        if (position < 0) {
            //First album is all files

            //Load cover
            if (Library.allFiles.isNotEmpty()) TurboImage.load(context, holder.image, Library.allFiles[0])

            //Update album name
            holder.name.text = "All"
        } else {
            //Get album
            val album = albums[position]

            //Load cover
            if (album.files.isNotEmpty()) TurboImage.load(context, holder.image, album.files[0])

            //Update album name
            holder.name.text = album.name + " (" + album.files.size + ")"
        }

        //Add click listeners
        holder.background.setOnClickListener { view: View ->
            onItemClickListener?.onItemClick(view, holder.bindingAdapterPosition - 1)
        }

        holder.background.setOnLongClickListener { view: View ->
            if (onItemLongClickListener == null) return@setOnLongClickListener true
            else onItemLongClickListener!!.onItemLongClick(view, holder.bindingAdapterPosition - 1)
        }
    }

    override fun getItemCount(): Int {
        return albums.size + 1
    }

    //Listeners
    fun interface OnItemClickListener {
        fun onItemClick(view: View, index: Int)
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
    }
}
