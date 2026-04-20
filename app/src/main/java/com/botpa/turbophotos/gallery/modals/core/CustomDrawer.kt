package com.botpa.turbophotos.gallery.modals.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.botpa.turbophotos.R
import com.google.android.material.bottomsheet.BottomSheetDialog

open class CustomDrawer(protected val context: Context, private val resource: Int) {

    //Views
    private lateinit var base: View
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
        base = LayoutInflater.from(context).inflate(R.layout.drawer_base, null)
        root = LayoutInflater.from(context).inflate(resource, null)
        base.findViewById<ViewGroup>(R.id.content).addView(root)

        //Start
        onInitStart()

        //Init views
        initViews()

        //Init dialog
        dialog = BottomSheetDialog(context)
        dialog.setContentView(base)
        initDialog(dialog)
        dialog.show()

        //Init listeners
        initListeners()

        //End
        onInitEnd()
    }
}