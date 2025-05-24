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
import com.bumptech.glide.Glide

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

            //Load image in imageview
            if (Library.allFiles.isNotEmpty()) {
                val requestBuilder = Glide.with(holder.itemView.context).asBitmap().sizeMultiplier(0.3f)
                Glide.with(context).asBitmap().load(Library.allFiles[0].file.absolutePath).thumbnail(requestBuilder).into(holder.image)
            }

            //Check if loaded
            if (Library.allFilesUpToDate) {
                holder.image.alpha =  1.0f;
                holder.loading.visibility = View.GONE;
            } else {
                holder.image.alpha =  0.2f;
                holder.loading.visibility = View.VISIBLE;
            }

            //Update album name
            holder.name.text = "All"
        } else {
            //Get album
            val album = albums[position]

            //Load image in imageview
            if (album.files.isNotEmpty()) {
                val requestBuilder = Glide.with(holder.itemView.context).asBitmap().sizeMultiplier(0.3f)
                Glide.with(context).asBitmap().load(album.files[0].file.absolutePath).thumbnail(requestBuilder).into(holder.image)
            }

            //Check if loaded
            if (album.isUpToDate) {
                holder.image.alpha =  1.0f;
                holder.loading.visibility = View.GONE;
            } else {
                holder.image.alpha =  0.2f;
                holder.loading.visibility = View.VISIBLE;
            }

            //Update album name
            holder.name.text = album.name
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
        var loading: View = itemView.findViewById(R.id.loading)
    }
}
