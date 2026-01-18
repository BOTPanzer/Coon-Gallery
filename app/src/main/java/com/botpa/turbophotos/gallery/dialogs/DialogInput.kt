package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.botpa.turbophotos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogInput(
    context: Context,
    private val title: String,
    private val hint: String,
    private val onValidate: (String) -> Boolean,
    private val onConfirm: (String) -> Unit
) : CustomDialog(context, R.layout.dialog_input) {

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
            .setPositiveButton("Confirm", null)
    }

    override fun initListeners() {
        //Rename (adding listener like this prevent the button from dismissing the dialog)
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener { view ->
            //Get input value
            val value = input.text.toString().trim()

            //Check if value is valid
            val isValid = onValidate(value)
            if (!isValid) return@setOnClickListener

            //Accept input
            onConfirm(value)
            dialog.dismiss()
        }
    }

    //Util
    fun setText(text: String) {
        input.setText(text)
    }

}