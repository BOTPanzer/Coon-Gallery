package com.botpa.turbophotos.screens.home.sorting

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.botpa.turbophotos.gallery.views.lists.ListSeparator
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SortingDialog(context: Context, items: List<SortingItem>, private val onSelected: (SortingItem) -> Unit) : CustomDialog(context, R.layout.dialog_selectable) {

    //Views
    private lateinit var list: RecyclerView

    //Adapter
    private var adapter: SortingDialogAdapter = SortingDialogAdapter(context, items)


    //Init
    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.list)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle(R.string.dialog_sorting_title)
            .setNegativeButton(R.string.dialog_cancel, null)
    }

    override fun initListeners() {
        //Add listeners (list)
        adapter.onClick = { item, position ->
            //Update method
            Library.setSortingInfo(item)

            //Call event
            onSelected.invoke(item)

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