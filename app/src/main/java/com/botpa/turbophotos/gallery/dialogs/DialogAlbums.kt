package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Album
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class DialogAlbums(
    context: Context,
    private val albums: List<Album>,
    private val onSelectAlbum: (Album) -> Unit,
    private val onSelectFolder: (File) -> Unit
) : CustomDialog(context, R.layout.dialog_albums) {

    //Views
    private lateinit var list: RecyclerView

    //Adapter
    private lateinit var adapter: DialogAlbumsAdapter
    private lateinit var layoutManager: GridLayoutManager


    //Init
    override fun onInitStart() {
        //Init adapter & layout manager
        adapter = DialogAlbumsAdapter(context, albums)
        layoutManager = GridLayoutManager(context, 2)
    }

    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.list)

        //Assign adapter & layout manager to list
        list.adapter = adapter
        list.layoutManager = layoutManager
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Select an album")
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Select from folder",{ dialogInterface, which ->
                //Select from folder
                DialogExplorer(context, false, onSelectFolder).buildAndShow()
            })
    }

    override fun initListeners() {
        //Select albums
        adapter.setOnClickListener { view, album ->
            //Select album
            onSelectAlbum(album)

            //Close dialog
            dialog.dismiss()
        }
    }

}