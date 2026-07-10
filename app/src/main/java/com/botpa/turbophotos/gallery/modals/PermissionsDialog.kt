package com.botpa.turbophotos.gallery.modals

import android.annotation.SuppressLint
import android.app.Activity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.CustomDialog
import com.botpa.turbophotos.gallery.permissions.PermissionManager
import com.botpa.turbophotos.gallery.permissions.PermissionType
import com.botpa.turbophotos.gallery.views.ListSeparator
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@SuppressLint("NotifyDataSetChanged")
class PermissionsDialog(
    private val activity: Activity,
    private val permissionManager: PermissionManager,
    private val onRequestPermission: (PermissionType) -> Unit
) : CustomDialog(activity, R.layout.dialog_permissions) {

    //Views
    private lateinit var list: RecyclerView

    //Adapter
    private lateinit var adapter: PermissionsDialogAdapter
    private var permissions: MutableList<PermissionType> = ArrayList()


    //Init
    override fun onInitStart() {
        //Refresh permissions list
        refreshPermissions()

        //Init adapter
        adapter = PermissionsDialogAdapter(context, permissions)
    }

    override fun initViews() {
        //Init views
        list = root.findViewById(R.id.permissionsList)
    }

    override fun initDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
        //Init dialog
        return builder
            .setTitle("Permissions")
            .setCancelable(false)
            .setNegativeButton("Cancel", { dialogInterface, which ->
                //Permissions missing -> Close activity
                activity.finish()
            })
    }

    override fun initListeners() {
        //Request permission
        adapter.onClick = { permission, position ->
            onRequestPermission(permission)
        }
    }

    override fun onInitEnd() {
        //Assign adapter, layout manager to list & separator gap
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)
        list.addItemDecoration(ListSeparator(3))
    }

    //Permissions
    fun refreshPermissions() {
        //Empty list
        permissions.clear()

        //Add non granted permissions
        for (permission in permissionManager.permissions.keys) {
            if (!permissionManager.hasPermission(permission)) {
                permissions.add(permission)
            }
        }

        //Update list adapter
        if (this::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

}