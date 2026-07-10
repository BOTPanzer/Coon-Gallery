package com.botpa.turbophotos.gallery.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.botpa.turbophotos.gallery.modals.PermissionsDialog
import java.util.EnumMap

class PermissionManager(val context: Context, managedPermissions: List<PermissionType>, private val onRequestPermission: (PermissionType) -> Unit) {

    private val _permissions: MutableMap<PermissionType, Boolean> = EnumMap(PermissionType::class.java)

    private var permissionsDialog: PermissionsDialog? = null

    val permissions: Map<PermissionType, Boolean>
        get() = _permissions

    var hasAllPermissions: Boolean = false
        private set


    init {
        //Init permissions
        for (permission in managedPermissions) {
            _permissions[permission] = checkPermission(permission)
        }

        //Check if has all
        updateHasAllPermissions()
    }

    private fun checkPermission(permission: PermissionType): Boolean {
        return when (permission) {
            PermissionType.Storage -> {
                Environment.isExternalStorageManager()
            }
            PermissionType.Media -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
            PermissionType.Notifications -> {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
            PermissionType.LocalAreaNetwork -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
    }

    private fun updateHasAllPermissions() {
        //Update value
        hasAllPermissions = permissions.isEmpty() || permissions.values.all { it }

        //Update dialog
        if (hasAllPermissions) {
            permissionsDialog?.hide()
            permissionsDialog = null
        } else {
            permissionsDialog?.refreshPermissions()
        }
    }

    fun hasPermission(permission: PermissionType): Boolean {
        return permissions[permission] == true
    }

    fun notifyPermissionChanged(permission: PermissionType) {
        //Not managed
        if (!permissions.contains(permission)) return

        //Recheck
        _permissions[permission] = checkPermission(permission)
        updateHasAllPermissions()
    }

    fun showDialog(activity: Activity) {
        permissionsDialog?.hide()
        permissionsDialog = PermissionsDialog(activity, this, onRequestPermission)
        permissionsDialog?.buildAndShow()
    }

}

enum class PermissionType {
    Storage,
    Media,
    Notifications,
    LocalAreaNetwork
}