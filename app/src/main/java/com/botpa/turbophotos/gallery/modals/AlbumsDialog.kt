package com.botpa.turbophotos.gallery.modals

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class AlbumsDialog(
    context: Context,
    private val albums: List<Album>,
    private val onSelectAlbum: (Album) -> Unit,
    private val onSelectFolder: (File) -> Unit
) : CustomDialog(context, R.layout.dialog_albums) {

    //Views
    private lateinit var list: RecyclerView

    //Adapter
    private lateinit var adapter: AlbumsDialogAdapter
    private lateinit var layoutManager: GridLayoutManager


    //Init
    override fun onInitStart() {
        //Init adapter & layout manager
        adapter = AlbumsDialogAdapter(context, albums)
        layoutManager = GridLayoutManager(context, 2)
    }

    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.albumsList)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle(R.string.dialog_albums_title)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setNeutralButton(R.string.dialog_albums_folder,{ dialogInterface, which ->
                //Select from folder
                ExplorerDialog(context, isSelectingFiles = false, allowCreation = true, onSelect = onSelectFolder).buildAndShow()
            })
    }

    override fun initListeners() {
        //Select albums
        adapter.onClick = { album, position ->
            //Select album
            onSelectAlbum(album)

            //Close dialog
            dialog.dismiss()
        }
    }

    override fun onInitEnd() {
        //Assign adapter & layout manager to list
        list.adapter = adapter
        list.layoutManager = layoutManager
    }

}