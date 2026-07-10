package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.modals.core.SimpleCustomAdapter
import com.botpa.turbophotos.gallery.permissions.PermissionType

class PermissionsDialogAdapter(
    context: Context,
    permissions: List<PermissionType>
) : SimpleCustomAdapter<PermissionType, PermissionsDialogAdapter.PermissionHolder>(context, permissions) {

    //Adapter
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): PermissionHolder {
        return PermissionHolder(inflateView(context, R.layout.dialog_permissions_item, viewGroup))
    }

    override fun onInitItemHolder(holder: PermissionHolder, permission: PermissionType) {
        //Update info
        holder.name.text = getPermissionName(permission)

        //Add listeners
        holder.request.setOnClickListener { view ->
            onClick?.run(permission, holder.bindingAdapterPosition)
        }
    }

    //Permissions
    fun getPermissionName(permission: PermissionType): String {
        return when (permission) {
            PermissionType.Storage -> "Storage"
            PermissionType.Media -> "Media"
            PermissionType.Notifications -> "Notifications"
            PermissionType.LocalAreaNetwork -> "Local area network"
        }
    }

    //Holder
    class PermissionHolder(root: View) : RecyclerView.ViewHolder(root) {

        val item: View = root
        val name: TextView = root.findViewById(R.id.permissionName)
        val request: Button = root.findViewById(R.id.permissionRequest)

    }

}