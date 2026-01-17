package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class CustomDialog(protected val context: Context, private val resource: Int) {

    //Views
    protected lateinit var root: View

    //Dialog
    protected lateinit var dialog: AlertDialog


    //Init
    protected open fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        return builder
    }

    protected open fun initViews() {}

    protected open fun initListeners() {}

    //Show
    fun show() {
        //Init root
        root = LayoutInflater.from(context).inflate(resource, null)

        //Init views
        initViews()

        //Init dialog
        dialog = initDialog(MaterialAlertDialogBuilder(context).setView(root)).show()

        //Init listeners
        initListeners()
    }

}