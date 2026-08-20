package com.botpa.turbophotos.screens.video.tracks

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.botpa.turbophotos.gallery.views.lists.ListSeparator
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TracksDialog(context: Context, tracks: List<TrackInfo>, private val title: String, private val onTrackSelected: (TrackInfo) -> Unit) : CustomDialog(context, R.layout.dialog_tracks) {

    //Views
    private lateinit var list: RecyclerView

    //Adapter
    private var adapter: TracksDialogAdapter = TracksDialogAdapter(context, tracks)


    //Init
    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.list)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle(title)
            .setNegativeButton("Cancel", null)
    }

    override fun initListeners() {
        //Add listeners
        adapter.onClick = { track, position ->
            //Select track
            onTrackSelected.invoke(track)

            //Close dialog
            dialog.dismiss()
        }
    }

    override fun onInitEnd() {
        //Assign adapter, layout manager to list & separator gap
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)
        list.addItemDecoration(ListSeparator(3))
    }

}