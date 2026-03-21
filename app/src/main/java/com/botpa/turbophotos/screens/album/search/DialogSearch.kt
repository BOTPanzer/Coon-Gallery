package com.botpa.turbophotos.screens.album.search

import android.content.Context
import android.widget.ListView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.dialogs.CustomDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogSearch(context: Context, private val onSelect: (Library.SearchMethod) -> Unit): CustomDialog(context, R.layout.dialog_search) {

    //Views
    private lateinit var list: ListView

    //Adapter
    private lateinit var adapter: DialogSearchAdapter
    private val methods: List<Library.SearchMethod> = listOf(Library.SearchMethod.ContainsText, Library.SearchMethod.ContainsWords)


    //Init
    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.list)

        //Init adapter
        adapter = DialogSearchAdapter(context, methods)
        list.adapter = adapter
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Search")
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

}