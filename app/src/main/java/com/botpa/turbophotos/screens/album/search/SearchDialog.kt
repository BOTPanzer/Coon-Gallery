package com.botpa.turbophotos.screens.album.search

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.SearchMethod
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.botpa.turbophotos.gallery.views.ListSeparator
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SearchDialog(context: Context, private val onSelect: (SearchMethod) -> Unit): CustomDialog(context, R.layout.dialog_search) {

    //Views
    private lateinit var list: RecyclerView

    //Adapter
    private lateinit var adapter: SearchDialogAdapter
    private val methods: List<SearchMethod> = listOf(SearchMethod.ContainsWords, SearchMethod.ContainsText)


    //Init
    override fun onInitStart() {
        //Init adapter
        adapter = SearchDialogAdapter(context, methods)
    }

    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.list)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Search method")
            .setNegativeButton("Cancel", null)
    }

    override fun initListeners() {
        //Add listeners (list)
        adapter.onClick = { method, position ->
            //Select method
            onSelect.invoke(method)

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