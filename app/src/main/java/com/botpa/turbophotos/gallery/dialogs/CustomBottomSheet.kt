package com.botpa.turbophotos.gallery.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog

open class CustomBottomSheet(protected val context: Context, private val resource: Int) {

    //Views
    protected lateinit var root: View

    //Dialog
    protected lateinit var dialog: BottomSheetDialog


    //Init
    protected open fun onInitStart() {}

    protected open fun initDialog(dialog: BottomSheetDialog): BottomSheetDialog {
        return dialog
    }

    protected open fun initViews() {}

    protected open fun initListeners() {}

    protected open fun onInitEnd() {}

    //Show
    fun buildAndShow() {
        //Init root
        root = LayoutInflater.from(context).inflate(resource, null)

        //Start
        onInitStart()

        //Init views
        initViews()

        //Init dialog
        dialog = BottomSheetDialog(context)
        dialog.setContentView(root)
        initDialog(dialog)
        dialog.show()

        //Init listeners
        initListeners()

        //End
        onInitEnd()
    }
}