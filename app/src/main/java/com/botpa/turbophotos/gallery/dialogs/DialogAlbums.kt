package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.widget.ListView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.Library
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class DialogAlbums(
    context: Context,
    private val albums: List<Album>,
    private val onSelectAlbum: (Album) -> Unit,
    private val onSelectFolder: (File) -> Unit
) : CustomDialog(context, R.layout.dialog_albums) {

    //Views
    private lateinit var list: ListView

    //Adapter
    private lateinit var adapter: DialogAlbumsAdapter


    //Init
    override fun onInitStart() {
        //Init adapter
        adapter = DialogAlbumsAdapter(context, albums)
    }

    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.list)

        //Assign adapter to list
        list.adapter = adapter
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Select an album")
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Select folder",{ dialogInterface, which ->
                //Select from folder
                DialogExplorer(context, false, onSelectFolder).buildAndShow()
            })
    }

    override fun initListeners() {
        //Select albums
        list.setOnItemClickListener { parent, view, position, id ->
            //Select album
            onSelectAlbum(albums[position])

            //Close dialog
            dialog.dismiss()
        }
    }

}