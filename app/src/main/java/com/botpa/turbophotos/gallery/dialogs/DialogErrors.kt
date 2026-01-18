package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.widget.ListView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.actions.ActionError
import com.botpa.turbophotos.util.Orion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogErrors(
    context: Context,
    private val errors: List<ActionError>
) : CustomDialog(context, R.layout.dialog_errors) {

    //Views
    private lateinit var list: ListView

    //Adapter
    private lateinit var adapter: DialogErrorsAdapter


    //Init
    override fun onInitStart() {
        //Init adapter
        adapter = DialogErrorsAdapter(context, errors)
    }

    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.list)

        //Assign adapter to list
        list.adapter = adapter
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Errors")
            .setPositiveButton("Close", null)
    }

    override fun initListeners() {
        //Copy errors
        list.setOnItemClickListener { parent, view, position, id ->
            //Copy reason to clipboard
            val error: ActionError = errors[position]
            Orion.copyToClip(context, "${error.item.name}: ${error.reason}")
        }
    }

}