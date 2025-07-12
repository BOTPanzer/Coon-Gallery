package com.botpa.turbophotos.settings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.util.Link

class LinksAdapter(private val context: Context, private val links: ArrayList<Link>) : RecyclerView.Adapter<LinksAdapter.AlbumHolder>() {

    private var onChooseFolderListener: OnChooseFolderListener? = null
    private var onChooseFileListener: OnChooseFileListener? = null
    private var onDeleteListener: OnDeleteListener? = null


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): AlbumHolder {
        val myView = LayoutInflater.from(context).inflate(R.layout.album_item, viewGroup, false)
        return AlbumHolder(myView)
    }

    override fun onBindViewHolder(holder: AlbumHolder, i: Int) {
        //Get holder position
        val position = holder.bindingAdapterPosition

        //Get album images
        val link = links[position]

        //Load album folder & metadata file names
        holder.name.text = "Album ${position}"
        holder.imagesFolder.text = link.imagesFolder.name
        holder.metadataFile.text = link.metadataFile.name

        //Add listeners
        holder.imagesOpen.setOnClickListener { view: View ->
            onChooseFolderListener?.onChoose(holder.imagesFolder, position)
        }

        holder.metadataOpen.setOnClickListener { view: View ->
            onChooseFileListener?.onChoose(holder.metadataFile, position)
        }

        holder.delete.setOnClickListener { view: View ->
            onDeleteListener?.onDelete(view, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return links.size
    }

    //Listeners
    fun interface OnChooseFolderListener {
        fun onChoose(view: TextView, index: Int)
    }

    fun setOnChooseFolderListener(onChooseFolderListener: OnChooseFolderListener?) {
        this.onChooseFolderListener = onChooseFolderListener
    }

    fun interface OnChooseFileListener {
        fun onChoose(view: TextView, index: Int)
    }

    fun setOnChooseFileListener(onChooseFileListener: OnChooseFileListener?) {
        this.onChooseFileListener = onChooseFileListener
    }

    fun interface OnDeleteListener {
        fun onDelete(view: View, index: Int)
    }

    fun setOnDeleteListener(onDeleteListener: OnDeleteListener?) {
        this.onDeleteListener = onDeleteListener
    }

    //Holder
    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView =
            itemView.findViewById(R.id.name)
        var imagesOpen: View =
            itemView.findViewById(R.id.imagesOpen)
        var imagesFolder: TextView =
            itemView.findViewById(R.id.imagesFolder)
        var metadataOpen: View =
            itemView.findViewById(R.id.metadataOpen)
        var metadataFile: TextView =
            itemView.findViewById(R.id.metadataFile)
        var delete: CardView = itemView.findViewById(R.id.delete)
    }

}
