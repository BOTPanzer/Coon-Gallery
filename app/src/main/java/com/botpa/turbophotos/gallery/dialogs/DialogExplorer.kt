package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.botpa.turbophotos.R
import com.botpa.turbophotos.util.Orion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class DialogExplorer(
    context: Context,
    private val isSelectingFiles: Boolean = false,
    private val onSelect: (File) -> Unit,
    private val startingFolder: File? = null,
    private val fileExtension: String = "",
) : CustomDialog(context, R.layout.dialog_explorer) {

    //Views
    private lateinit var listLayout: View
    private lateinit var list: ListView
    private lateinit var createLayout: View
    private lateinit var createInput: EditText
    private lateinit var createButton: Button

    //Adapter
    private lateinit var adapter: DialogExplorerAdapter

    //Files & folders
    private lateinit var currentFolder: File
    private val externalStorage: File = Environment.getExternalStorageDirectory()
    private val items: MutableList<File> = ArrayList()

    //Text
    companion object {
        //File picker
        private const val TEXT_INPUT_FILE: String = "File name"
        private const val TEXT_CREATE_FILE: String = "Create file"
        private const val TEXT_SELECT_FILE: String = "Select file"
        //Folder picker
        private const val TEXT_INPUT_FOLDER: String = "Folder name"
        private const val TEXT_CREATE_FOLDER: String = "Create folder"
        private const val TEXT_SELECT_FOLDER: String = "Select folder"
    }


    //Init
    override fun onInitStart() {
        //Init current folder
        currentFolder = startingFolder ?: externalStorage

        //Init adapter
        adapter = DialogExplorerAdapter(context, isSelectingFiles, externalStorage, currentFolder, items)

        //Init items list
        updateItemsList(currentFolder)
    }

    override fun initViews() {
        //Init views
        listLayout = root.findViewById(R.id.listLayout)
        list = root.findViewById(R.id.list)
        createLayout = root.findViewById(R.id.createLayout)
        createInput = root.findViewById(R.id.createInput)
        createButton = root.findViewById(R.id.createButton)

        //Assign adapter to list
        list.adapter = adapter
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder.apply {
            setTitle(adapter.currentFolderName)
            setNeutralButton(if (isSelectingFiles) TEXT_CREATE_FILE else TEXT_CREATE_FOLDER, null)
            setNegativeButton("Cancel", null)
        }
    }

    override fun initListeners() {
        //List (select & open a folder)
        adapter.setOnSelectListener { index ->
            //Select folder
            onSelect(items[index])

            //Close dialog
            dialog.dismiss()
        }

        adapter.setOnOpenListener { index ->
            //Get folder
            val item = if (index < 0) {
                //Back button
                adapter.currentFolderParent
            } else {
                //Select item
                items[index]
            }

            //Check if item is valid
            if (item == null) {
                //Invalid folder
                Toast.makeText(context, "Invalid item", Toast.LENGTH_SHORT).show()
                return@setOnOpenListener
            }

            //Check item type
            if (item.isFile) {
                //File -> Select file
                onSelect(item)

                //Close dialog
                dialog.dismiss()
            } else {
                //Folder -> Check if folder can be read and written to
                if (!item.canRead() || !item.canWrite()) {
                    //Folder can't be read/written to
                    Toast.makeText(context, "Missing permissions to use that folder", Toast.LENGTH_SHORT).show()
                    return@setOnOpenListener
                }

                //Update adapter & dialog
                updateItemsList(item)
                adapter.setCurrentFolder(item)
                adapter.notifyDataSetChanged()
                list.setSelectionAfterHeaderView() //Scroll to top
                dialog.setTitle(adapter.currentFolderName)
            }
        }

        //Create a folder
        createButton.setOnClickListener { view ->
            //Get item name
            val itemName = createInput.text.toString().trim()

            //Check if item exists
            if (itemName.isEmpty()) {
                //Name is empty
                Toast.makeText(context, "Name can't be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Get item file
            val item = File(adapter.currentFolderPath, if (isSelectingFiles && !fileExtension.isEmpty()) "$itemName.$fileExtension" else itemName)

            //Check if item exists
            if (item.exists()) {
                //Item already exists
                Toast.makeText(context, "${if (isSelectingFiles) "File" else "Folder"} already exists.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Create item
            if (isSelectingFiles) {
                //Create file
                if (!item.createNewFile()) {
                    //Failed to create folder
                    Toast.makeText(context, "Failed to create file.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                //Create folder
                if (!item.mkdir()) {
                    //Failed to create folder
                    Toast.makeText(context, "Failed to create folder.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            //Accept file
            onSelect(item)

            //Close dialog
            dialog.dismiss()
        }

        //Toggle list & create menus (adding listener like this prevent the button from dismissing the dialog)
        val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        neutralButton.setOnClickListener { view ->
            if (listLayout.isVisible) {
                //Toggle menus (show create folder)
                listLayout.visibility = View.GONE
                createLayout.visibility = View.VISIBLE

                //Reset input
                createInput.setText("")
                createInput.requestFocus()

                //Update neutral button
                neutralButton.text = if (isSelectingFiles) TEXT_SELECT_FILE else TEXT_SELECT_FOLDER
            } else {
                //Toggle menus (show filters list)
                listLayout.visibility = View.VISIBLE
                createLayout.visibility = View.GONE

                //Reset focus
                createInput.clearFocus()

                //Update neutral button
                neutralButton.text = if (isSelectingFiles) TEXT_CREATE_FILE else TEXT_CREATE_FOLDER
            }
        }
    }

    override fun onInitEnd() {
        //Update create menu
        createInput.hint = if (isSelectingFiles) TEXT_INPUT_FILE else TEXT_INPUT_FOLDER
        createButton.text = if (isSelectingFiles) TEXT_CREATE_FILE else TEXT_CREATE_FOLDER
    }

    //Helpers
    private fun updateItemsList(folder: File) {
        //Clear old items
        items.clear()

        //Add new items
        if (isSelectingFiles) {
            //Get files & files
            val temp = Orion.listFilesAndFolders(folder)

            //Check if filtering for a specific file extension
            if (fileExtension.isEmpty()) {
                //Not filtering -> Add all
                items.addAll(temp)
            } else {
                //Filtering -> Add directories & files ending in extension
                val extension = fileExtension.lowercase().trim()
                for (file in temp) {
                    if (file.isDirectory || Orion.getExtension(file.name).lowercase() == extension) {
                        items.add(file)
                    }
                }
            }
        } else {
            //Get folders
            items.addAll(Orion.listFolders(folder))
        }

        //Sort items
        items.sortWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

}