package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.botpa.turbophotos.gallery.views.ListSeparator
import com.botpa.turbophotos.util.Orion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class ExplorerDialog(
    context: Context,
    private val isSelectingFiles: Boolean = false,
    private val onSelect: (File) -> Unit,
    private val startingFolder: File? = null,
    private val fileExtension: String = "",
) : CustomDialog(context, R.layout.dialog_explorer) {

    //Views
    private lateinit var folderPath: TextView
    private lateinit var foldersLayout: View
    private lateinit var foldersList: RecyclerView
    private lateinit var createLayout: View
    private lateinit var createInput: EditText
    private lateinit var createButton: Button

    //Adapter
    private lateinit var adapter: ExplorerDialogAdapter

    //Files & folders
    private lateinit var currentFolder: File
    private val externalStorage: File = Environment.getExternalStorageDirectory()
    private val items: MutableList<File> = ArrayList()

    //Text
    private val buttonSelect: String = context.getString(R.string.dialog_explorer_select)
    private val buttonFileCreate: String = context.getString(R.string.dialog_explorer_file_create)
    private val buttonFolderCreate: String = context.getString(R.string.dialog_explorer_folder_create)
    private val buttonCreate: String = if (isSelectingFiles) buttonFileCreate else buttonFolderCreate
    private val hintFile: String = context.getString(R.string.dialog_explorer_file_hint)
    private val hintFolder: String = context.getString(R.string.dialog_explorer_folder_hint)


    //Init
    override fun onInitStart() {
        //Init current folder
        currentFolder = startingFolder ?: externalStorage

        //Init adapter
        adapter = ExplorerDialogAdapter(context, items, isSelectingFiles, externalStorage, currentFolder)

        //Init items list
        updateItemsList(currentFolder)
    }

    override fun initViews() {
        //Init views
        folderPath = root.findViewById(R.id.folderPath)
        foldersLayout = root.findViewById(R.id.foldersLayout)
        foldersList = root.findViewById(R.id.foldersList)
        createLayout = root.findViewById(R.id.createLayout)
        createInput = root.findViewById(R.id.createInput)
        createButton = root.findViewById(R.id.createButton)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder.apply {
            setTitle(if (isSelectingFiles) R.string.dialog_explorer_file_title else R.string.dialog_explorer_folder_title)
            setNeutralButton(buttonCreate, null)
            setNegativeButton(R.string.dialog_cancel, null)
        }
    }

    override fun initListeners() {
        //List (select & open a folder)
        adapter.onSelect = { index ->
            //Select file
            onSelect(items[index])

            //Close dialog
            dialog.dismiss()
        }

        adapter.onOpen = { index ->
            //Get item
            val item = if (index < 0) {
                //Back button
                adapter.currentFolderParent
            } else {
                //Select item
                items[index]
            }

            //Check if item is valid
            if (item == null) {
                //Invalid item
                Toast.makeText(context, R.string.dialog_explorer_error_invalid_item, Toast.LENGTH_SHORT).show()
            } else if (item.isDirectory) {
                //Check if folder can be read and written to
                if (!item.canRead() || !item.canWrite()) {
                    //Folder can't be read/written to
                    Toast.makeText(context, R.string.dialog_explorer_error_missing_permissions, Toast.LENGTH_SHORT).show()
                } else {
                    //Update adapter & dialog
                    updateItemsList(item)
                    adapter.setCurrentFolder(item)
                    adapter.notifyDataSetChanged()
                    foldersList.scrollToPosition(0)
                    updateCurrentFolderName()
                }
            }
        }

        //Create a folder
        createButton.setOnClickListener { view ->
            //Get input value
            val inputValue = createInput.text.toString().trim()

            //Check if item exists
            if (inputValue.isEmpty()) {
                //Name is empty
                Toast.makeText(context, R.string.dialog_explorer_error_empty_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Get item file
            val itemName = if (isSelectingFiles && !fileExtension.isEmpty()) "$inputValue.$fileExtension" else inputValue
            val item = File(adapter.currentFolderPath, itemName)

            //Create item
            if (isSelectingFiles) {
                //Check if file exists
                if (item.exists()) {
                    //File already exists
                    Toast.makeText(context, R.string.dialog_explorer_error_file_exists, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                //Create file
                if (!item.createNewFile()) {
                    //Failed to create file
                    Toast.makeText(context, R.string.dialog_explorer_error_file_create_fail, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                //Check if folder exists
                if (item.exists()) {
                    //Folder already exists
                    Toast.makeText(context, R.string.dialog_explorer_error_folder_exists, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                //Create folder
                if (!item.mkdir()) {
                    //Failed to create folder
                    Toast.makeText(context, R.string.dialog_explorer_error_folder_create_fail, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            //Select file
            onSelect(item)

            //Close dialog
            dialog.dismiss()
        }

        //Toggle list & create menus (adding listener like this prevent the button from dismissing the dialog)
        val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        neutralButton.setOnClickListener { view ->
            if (foldersLayout.isVisible) {
                //Toggle menus (show create folder)
                foldersLayout.visibility = View.GONE
                createLayout.visibility = View.VISIBLE

                //Reset input
                createInput.setText("")
                createInput.requestFocus()

                //Update neutral button
                neutralButton.text = buttonSelect
            } else {
                //Toggle menus (show filters list)
                foldersLayout.visibility = View.VISIBLE
                createLayout.visibility = View.GONE

                //Reset focus
                createInput.clearFocus()

                //Update neutral button
                neutralButton.text = buttonCreate
            }
        }
    }

    override fun onInitEnd() {
        //Assign adapter, layout manager to list & separator gap
        foldersList.adapter = adapter
        foldersList.layoutManager = LinearLayoutManager(context)
        foldersList.addItemDecoration(ListSeparator(3))

        //Update current folder name
        updateCurrentFolderName()

        //Update create menu
        createInput.hint = if (isSelectingFiles) hintFile else hintFolder
        createButton.text = buttonCreate
    }

    //Helpers
    private fun updateCurrentFolderName() {
        folderPath.text = adapter.getCurrentPath()
    }

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