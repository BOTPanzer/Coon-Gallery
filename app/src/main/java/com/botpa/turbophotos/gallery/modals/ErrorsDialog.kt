package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.widget.ListView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.actions.ActionError
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.botpa.turbophotos.gallery.views.ListSeparator
import com.botpa.turbophotos.util.Orion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ErrorsDialog(
    context: Context,
    private val errors: List<ActionError>
) : CustomDialog(context, R.layout.dialog_errors) {

    //Views
    private lateinit var list: RecyclerView

    //Adapter
    private lateinit var adapter: ErrorsDialogAdapter


    //Init
    override fun onInitStart() {
        //Init adapter
        adapter = ErrorsDialogAdapter(context, errors)
    }

    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.errorsList)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Errors")
            .setPositiveButton("Close", null)
    }

    override fun initListeners() {
        //Copy errors
        adapter.onClick = { error, position ->
            //Copy reason to clipboard
            Orion.copyToClip(context, "${error.item.name}: ${error.reason}")
        }
    }

    override fun onInitEnd() {
        //Assign adapter, layout manager to list & separator gap
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)
        list.addItemDecoration(ListSeparator(3))
    }

}