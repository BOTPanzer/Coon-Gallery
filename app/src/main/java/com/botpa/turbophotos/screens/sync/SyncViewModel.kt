package com.botpa.turbophotos.screens.sync

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SyncViewModel : ViewModel() {

    //Permissions
    var hasPermissionNotifications by mutableStateOf(false)
        private set
    var hasPermissionLocalNetwork by mutableStateOf(false)
        private set
    var hasPermissions by mutableStateOf(false)
        private set

    fun checkPermissions(activity: Activity) {
        //Notifications
        hasPermissionNotifications = NotificationManagerCompat.from(activity).areNotificationsEnabled()

        //Local network
        hasPermissionLocalNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        //All
        hasPermissions = hasPermissionNotifications && hasPermissionLocalNetwork
    }

    fun onNotificationPermissionResult(activity: Activity, isGranted: Boolean) {
        hasPermissionNotifications = isGranted
        checkPermissions(activity)
    }

    fun onLocalNetworkPermissionResult(activity: Activity, isGranted: Boolean) {
        hasPermissionLocalNetwork = isGranted
        checkPermissions(activity)
    }

    //Sync
    var reloadLibraryOnExit = false

    //Status
    var connectionStatus by mutableIntStateOf(SyncService.STATUS_OFFLINE)

    //Connect
    var connectName by mutableStateOf("")
    var connectCode by mutableStateOf("")

    //Users
    val users = mutableStateListOf<User>()

    //Logs
    val logs = mutableStateListOf<String>()
    private val _scrollRequest = MutableSharedFlow<Unit>()
    val scrollRequest = _scrollRequest.asSharedFlow()

    fun requestScrollToBottom() {
        viewModelScope.launch {
            _scrollRequest.emit(Unit)
        }
    }

}