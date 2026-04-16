package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.widget.ListView
import android.widget.TextView
import com.botpa.turbophotos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogBulletpoints(
    context: Context,
    private val title: String,
    private val text: String,
    private val points: List<String>
) : CustomDialog(context, R.layout.dialog_points) {

    //Views
    private lateinit var info: TextView
    private lateinit var list: ListView

    //Adapter
    private lateinit var adapter: DialogBulletpointsAdapter


    //Init
    override fun onInitStart() {
        //Init adapter
        adapter = DialogBulletpointsAdapter(context, points)
    }

    override fun initViews() {
        //Init views
        info = root.findViewById(R.id.pointsInfo)
        list = root.findViewById(R.id.pointsList)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle(title)
            .setPositiveButton("Close", null)
    }

    override fun onInitEnd() {
        //Update info text
        info.text = text

        //Assign adapter to list
        list.adapter = adapter
    }

}