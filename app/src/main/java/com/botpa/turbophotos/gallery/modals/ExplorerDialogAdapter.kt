package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomAdapter
import java.io.File

class ExplorerDialogAdapter(
    private val context: Context,
    private val items: List<File>,
    private val isSelectingFiles: Boolean = false,
    private val externalStorage: File,
    private var currentFolder: File
) : RecyclerView.Adapter<ExplorerDialogAdapter.ExplorerHolder>() {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ExplorerHolder {
        return ExplorerHolder(CustomAdapter.inflateView(context, R.layout.dialog_explorer_item, viewGroup))
    }

    override fun onBindViewHolder(holder: ExplorerHolder, position: Int) {
        //Get position
        val position = holder.bindingAdapterPosition - positionOffset

        //Check if back button
        if (position < 0) {
            //Back button
            holder.icon.setImageResource(R.drawable.back)
            holder.name.text = "Previous folder"
            holder.select.visibility = View.GONE
        } else {
            //Item
            val item = items[position]
            holder.icon.setImageResource(if (item.isFile) R.drawable.file else R.drawable.folder)
            holder.name.text = item.name
            holder.select.visibility = if (isSelectingFiles && item.isDirectory) View.GONE else View.VISIBLE
        }

        //Add listeners
        holder.root.setOnClickListener { view ->
            onOpen?.invoke(position)
        }
        holder.select.setOnClickListener { view ->
            onSelect?.invoke(position)
        }
    }

    override fun getItemCount(): Int = items.size + positionOffset

    private val positionOffset: Int get() = 1

    //Listeners
    var onOpen: ClickListener? = null
    var onSelect: ClickListener? = null

    fun interface ClickListener {
        fun invoke(index: Int)
    }

    //Holder
    class ExplorerHolder(val root: View) : RecyclerView.ViewHolder(root) {

        val icon: ImageView = root.findViewById(R.id.itemIcon)
        val name: TextView = root.findViewById(R.id.itemName)
        val select: Button = root.findViewById(R.id.itemSelect)

    }

    //Current folder
    val currentFolderPath: String get() = currentFolder.absolutePath
    val currentFolderParent: File? get() = currentFolder.parentFile

    fun setCurrentFolder(newFolder: File) {
        currentFolder = newFolder
    }

    fun getCurrentPath(): String {
        //Get parent
        val currentPath = currentFolder.absolutePath
        val storagePath = externalStorage.absolutePath
        val prettyPath = if (currentPath.startsWith(storagePath)) {
            "External storage${currentPath.substring(storagePath.length)}"
        } else {
            currentPath
        }
        return prettyPath.replace("/", " > ")
    }

}