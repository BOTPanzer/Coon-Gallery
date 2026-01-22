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

class DialogExplorerAdapter(
    context: Context,
    private val isSelectingFiles: Boolean = false,
    private val externalStorage: File,
    private var currentFolder: File,
    items: List<File>
) : ArrayAdapter<File>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //Inflate
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_explorer_item, parent, false)

        //Get views
        val icon = view.findViewById<ImageView>(R.id.itemIcon)
        val name = view.findViewById<TextView>(R.id.itemName)
        val select = view.findViewById<Button>(R.id.itemSelect)

        //Get index
        val index = position - this.positionOffset

        //Check if back button
        if (index < 0) {
            //Back button
            icon.setImageResource(R.drawable.back)
            name.text = "Previous folder"
            select.visibility = View.GONE
        } else {
            //Item
            val item = getItem(position - 1)
            if (item != null) {
                icon.setImageResource(if (item.isFile) R.drawable.file else R.drawable.folder)
                name.text = item.name
                select.visibility = if (isSelectingFiles && item.isDirectory) View.GONE else View.VISIBLE
            }
        }

        //Add listeners
        view.setOnClickListener { view -> onOpenListener?.invoke(index) }
        select.setOnClickListener { view -> onSelectListener?.invoke(index) }

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

    //Listeners
    fun interface Listener { fun invoke(index: Int) }

    private var onOpenListener: Listener? = null
    private var onSelectListener: Listener? = null

    fun setOnOpenListener(onOpenListener: Listener?) {
        this.onOpenListener = onOpenListener
    }

    fun setOnSelectListener(onSelectListener: Listener?) {
        this.onSelectListener = onSelectListener
    }

}