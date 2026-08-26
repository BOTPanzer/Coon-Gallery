package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.view.View
import android.widget.ListView
import android.widget.TextView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BulletPointsDialog(
    context: Context,
    private val title: Int,
    private val text: Int,
    private val points: List<Int>,
    private val textAfter: Int = -1,
) : CustomDialog(context, R.layout.dialog_points) {

    //Views
    private lateinit var info: TextView
    private lateinit var list: ListView
    private lateinit var infoAfter: TextView

    //Adapter
    private lateinit var adapter: BulletPointsDialogAdapter


    //Init
    override fun onInitStart() {
        //Init adapter
        adapter = BulletPointsDialogAdapter(context, points)
    }

    override fun initViews() {
        //Init views
        info = root.findViewById(R.id.pointsInfo)
        list = root.findViewById(R.id.pointsList)
        infoAfter = root.findViewById(R.id.pointsInfoAfter)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle(title)
            .setPositiveButton(R.string.dialog_close, null)
    }

    override fun onInitEnd() {
        //Update info text
        info.text = context.getString(text)
        if (textAfter != -1) {
            infoAfter.text = context.getString(textAfter)
        } else {
            infoAfter.visibility = View.GONE
        }

        //Assign adapter to list
        list.adapter = adapter
    }

}