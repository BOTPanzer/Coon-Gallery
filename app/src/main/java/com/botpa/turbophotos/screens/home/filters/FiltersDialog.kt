package com.botpa.turbophotos.screens.home.filters

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.botpa.turbophotos.gallery.views.ListSeparator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.regex.Pattern

class FiltersDialog(context: Context, filters: List<Filter>) : CustomDialog(context, R.layout.dialog_filters) {

    //Views
    private lateinit var listLayout: View
    private lateinit var list: RecyclerView
    private lateinit var customLayout: View
    private lateinit var customInput: EditText
    private lateinit var customButton: Button

    //Adapter
    private var adapter: FiltersDialogAdapter = FiltersDialogAdapter(context, filters)

    //Text
    private val buttonTextCustom: String = context.getString(R.string.dialog_filters_custom_button)
    private val buttonTextSelect: String = context.getString(R.string.dialog_filters_select_button)


    //Init
    override fun initViews() {
        //Init views
        listLayout = root.findViewById(R.id.listLayout)
        list = root.findViewById(R.id.list)
        customLayout = root.findViewById(R.id.customLayout)
        customInput = root.findViewById(R.id.customInput)
        customButton = root.findViewById(R.id.customButton)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle(R.string.dialog_filters_title)
            .setNeutralButton(buttonTextCustom, null)
            .setNegativeButton(R.string.dialog_cancel, null)
    }

    override fun initListeners() {
        //Add listeners (list)
        adapter.onClick = { filter, position ->
            //Apply filter
            Library.loadLibrary(context, filter.mimeType)

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
                Toast.makeText(context, R.string.dialog_filters_error_invalid_mime, Toast.LENGTH_SHORT).show()
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
                neutralButton.text = buttonTextSelect
            } else {
                //Toggle menus (show filters list)
                listLayout.visibility = View.VISIBLE
                customLayout.visibility = View.GONE

                //Update neutral button
                neutralButton.text = buttonTextCustom
            }
        }
    }

    override fun onInitEnd() {
        //Assign adapter, layout manager to list & separator gap
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)
        list.addItemDecoration(ListSeparator(3))
    }

}