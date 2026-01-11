package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
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
import java.util.function.Consumer

class DialogFolders(context: Context, private val externalStorage: File, private val currentFolder: File, private val folders: MutableList<File>, private val onSelect: Consumer<File>) : CustomDialog(context, R.layout.dialog_folders) {

    //Views
    private lateinit var listLayout: View
    private lateinit var list: ListView
    private lateinit var createLayout: View
    private lateinit var createInput: EditText
    private lateinit var createButton: Button

    //Adapter
    private lateinit var adapter: DialogFoldersAdapter

    //Text
    private val TEXT_CREATE: String = "Create folder"
    private val TEXT_SELECT: String = "Select folder"


    //Init
    override fun initViews() {
        //Init views
        listLayout = root.findViewById(R.id.listLayout)
        list = root.findViewById(R.id.list)
        createLayout = root.findViewById(R.id.createLayout)
        createInput = root.findViewById(R.id.createInput)
        createButton = root.findViewById(R.id.createButton)

        //Init adapter
        adapter = DialogFoldersAdapter(context, externalStorage, currentFolder, folders)
        list.adapter = adapter
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Select a folder")
            .setNeutralButton(TEXT_CREATE, null)
            .setNegativeButton("Cancel", null)
    }

    override fun initListeners() {
        //Add listeners (list)
        adapter.setOnSelectListener { index ->
            //Select folder
            onSelect.accept(folders.get(index))

            //Close dialog
            dialog.dismiss()
        }

        adapter.setOnOpenListener { index ->
            //Get folder
            val folder: File?
            if (index < 0) {
                //Back button
                folder = adapter.currentFolderParent
            } else {
                //Select folder
                folder = folders.get(index)
            }

            //Check if folder is valid
            if (folder == null) {
                //Invalid folder
                Toast.makeText(context, "Invalid folder", Toast.LENGTH_SHORT).show()
                return@setOnOpenListener
            }

            //Check if folder can be read and written to
            if (!folder.canRead() || !folder.canWrite()) {
                //Folder can't be read/written to
                Toast.makeText(
                    context,
                    "Missing permissions to use that folder",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnOpenListener
            }

            //Update adapter
            adapter.setCurrentFolder(folder)
            dialog.setTitle(adapter.currentFolderName)
            folders.clear()
            folders.addAll(Orion.listFiles(folder))
            folders.sortWith(Comparator { a: File?, b: File? -> a!!.name.compareTo(b!!.name, ignoreCase = true) })
            adapter.notifyDataSetChanged()
            list.setSelectionAfterHeaderView() //Scroll to top
        }

        //Add listeners (create)
        createButton.setOnClickListener { view ->
            //Get folder name & file
            val folderName: String = createInput.getText().toString().trim()
            val folder: File = File(adapter.currentFolderPath, folderName)

            //Check if folder exists
            if (folder.exists()) {
                //Folder already exists
                Toast.makeText(context, "Folder already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Create folder
            if (!folder.mkdir()) {
                //Failed to create folder
                Toast.makeText(context, "Failed to create folder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Accept file
            onSelect.accept(folder)

            //Close dialog
            dialog.dismiss()
        }

        //Add listeners (toggle list & create)
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
                neutralButton.text = TEXT_SELECT
            } else {
                //Toggle menus (show filters list)
                listLayout.visibility = View.VISIBLE
                createLayout.visibility = View.GONE

                //Update neutral button
                neutralButton.text = TEXT_CREATE
            }
        }
    }

}