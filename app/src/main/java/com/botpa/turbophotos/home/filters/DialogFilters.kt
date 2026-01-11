package com.botpa.turbophotos.home.filters

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.dialogs.CustomDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.regex.Pattern

class DialogFilters(context: Context, private val filters: List<Filter>) : CustomDialog(context, R.layout.dialog_filters) {

    //Views
    private lateinit var listLayout: View
    private lateinit var list: ListView
    private lateinit var customLayout: View
    private lateinit var customInput: EditText
    private lateinit var customButton: Button

    //Adapter
    private lateinit var adapter: DialogFiltersAdapter

    //Text
    private val TEXT_CUSTOM: String = "Custom filter"
    private val TEXT_SELECT: String = "Select filter"


    //Init
    override fun initViews() {
        //Init views
        listLayout = root.findViewById(R.id.listLayout)
        list = root.findViewById(R.id.list)
        customLayout = root.findViewById(R.id.customLayout)
        customInput = root.findViewById(R.id.customInput)
        customButton = root.findViewById(R.id.customButton)

        //Init adapter
        adapter = DialogFiltersAdapter(context, filters)
        list.adapter = adapter
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Filters")
            .setNeutralButton(TEXT_CUSTOM, null)
            .setNegativeButton("Cancel", null)
    }

    override fun initListeners() {
        //Add listeners (list)
        list.setOnItemClickListener { parent, view, position, id ->
            //Apply filter
            Library.loadLibrary(context, adapter.getItem(position)!!.mimeType)

            //Close dialog
            dialog.dismiss()
        }

        //Add listeners (custom)
        customButton.setOnClickListener { view ->
            //Get folder name & file
            val mimeType = customInput.text.toString().trim()

            //Check if mime type is valid
            if (!Pattern.compile(".[/].").matcher(mimeType).find()) {
                //Mime type is invalid
                Toast.makeText(context, "Mime type is invalid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Apply filter
            Library.loadLibrary(context, mimeType)

            //Close dialog
            dialog.dismiss()
        }

        //Add listeners (toggle list & custom)
        val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        neutralButton.setOnClickListener { view ->
            if (listLayout.isVisible) {
                //Toggle menus (show custom filter)
                listLayout.visibility = View.GONE
                customLayout.visibility = View.VISIBLE

                //Reset input
                customInput.setText("")
                customInput.requestFocus()

                //Update neutral button
                neutralButton.text = TEXT_SELECT
            } else {
                //Toggle menus (show filters list)
                listLayout.visibility = View.VISIBLE
                customLayout.visibility = View.GONE

                //Update neutral button
                neutralButton.text = TEXT_CUSTOM
            }
        }
    }

}