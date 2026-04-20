package com.botpa.turbophotos.screens.album.search

import android.content.Context
import android.widget.ListView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.SearchMethod
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SearchDialog(context: Context, private val onSelect: (SearchMethod) -> Unit): CustomDialog(context, R.layout.dialog_search) {

    //Views
    private lateinit var list: ListView

    //Adapter
    private lateinit var adapter: SearchDialogAdapter
    private val methods: List<SearchMethod> = listOf(SearchMethod.ContainsWords, SearchMethod.ContainsText)


    //Init
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
        list.setOnItemClickListener { parent, view, position, id ->
            //Call on method
            onSelect.invoke(methods[position])

            //Close dialog
            dialog.dismiss()
        }
    }

    override fun onInitEnd() {
        //Init adapter
        adapter = SearchDialogAdapter(context, methods)
        list.adapter = adapter
    }

}