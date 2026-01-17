package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.widget.EditText
import com.botpa.turbophotos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.function.Consumer

class DialogInput(context: Context, private val title: String, private val hint: String, private val onConfirm: Consumer<String>) : CustomDialog(context, R.layout.dialog_input) {

    //Views
    private lateinit var input: EditText


    //Init
    override fun initViews() {
        //Init views
        input = root.findViewById(R.id.input)

        //Update hint
        input.hint = hint
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle(title)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm", { dialog, which ->
                //Accept input
                onConfirm.accept(input.text.toString())
            })
    }

    //Util
    fun setText(text: String) {
        input.setText(text)
    }

}