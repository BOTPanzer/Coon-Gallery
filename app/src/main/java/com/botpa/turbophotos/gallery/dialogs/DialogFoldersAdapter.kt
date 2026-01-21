package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.botpa.turbophotos.R
import java.io.File

class DialogFoldersAdapter(
    context: Context,
    private val externalStorage: File,
    private var currentFolder: File,
    folders: List<File>
) : ArrayAdapter<File>(context, 0, folders) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_folders_item, parent, false)

        //Get views
        val icon = view.findViewById<ImageView>(R.id.folderIcon)
        val name = view.findViewById<TextView>(R.id.folderName)
        val select = view.findViewById<Button>(R.id.folderSelect)

        //Get index
        val index = position - this.positionOffset

        //Check if back button
        if (index < 0) {
            //Back button
            icon.setImageResource(R.drawable.back)
            name.text = "Previous folder"
            select.visibility = View.GONE
        } else {
            //Folder
            icon.setImageResource(R.drawable.folder)
            val folder = getItem(position - 1)
            if (folder != null) name.text = folder.name
            select.visibility = View.VISIBLE
        }

        //Add listeners
        view.setOnClickListener { v ->
            if (onOpenListener != null) onOpenListener!!.invoke(index)
        }
        select.setOnClickListener { v ->
            if (onSelectListener != null) onSelectListener!!.invoke(index)
        }

        return view
    }

    //Count & indexes
    override fun getCount(): Int {
        return super.getCount() + this.positionOffset
    }

    val positionOffset: Int get() = 1

    //Current folder
    val currentFolderName: String get() = if (currentFolder == externalStorage) "External storage" else currentFolder.name
    val currentFolderPath: String get() = currentFolder.absolutePath
    val currentFolderParent: File? get() = currentFolder.parentFile

    fun setCurrentFolder(folder: File) {
        currentFolder = folder
    }

    //Listeners
    fun interface Listener {
        fun invoke(index: Int)
    }

    private var onOpenListener: Listener? = null
    private var onSelectListener: Listener? = null

    fun setOnOpenListener(onOpenListener: Listener?) {
        this.onOpenListener = onOpenListener
    }

    fun setOnSelectListener(onSelectListener: Listener?) {
        this.onSelectListener = onSelectListener
    }

}