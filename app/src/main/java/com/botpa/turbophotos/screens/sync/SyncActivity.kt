package com.botpa.turbophotos.screens.sync

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.permissions.PermissionManager
import com.botpa.turbophotos.gallery.permissions.PermissionType
import com.botpa.turbophotos.gallery.jetpack.CoonTheme
import com.botpa.turbophotos.gallery.jetpack.FONT_OUTFIT
import com.botpa.turbophotos.gallery.jetpack.Group
import com.botpa.turbophotos.gallery.jetpack.GroupDivider
import com.botpa.turbophotos.gallery.jetpack.GroupItem
import com.botpa.turbophotos.gallery.jetpack.GroupItems
import com.botpa.turbophotos.gallery.jetpack.GroupTitle
import com.botpa.turbophotos.gallery.jetpack.Layout
import com.botpa.turbophotos.gallery.jetpack.SimpleButton
import com.botpa.turbophotos.gallery.jetpack.groupItemPaddingHorizontal
import com.botpa.turbophotos.gallery.jetpack.groupItemPaddingVertical
import com.botpa.turbophotos.screens.sync.service.SyncEventBus.Companion.instance
import com.botpa.turbophotos.screens.sync.service.SyncEvent
import com.botpa.turbophotos.screens.sync.service.SyncService
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import java.util.Locale

class SyncActivity : AppCompatActivity() {

    //View model
    private val view: SyncViewModel by viewModels()

    //Logs
    private val logsMax = 500

    //Routes
    object SyncRoutes {
        const val PERMISSIONS = "permissions"
        const val CONNECT = "connect"
        const val LOGS = "logs"
    }


    //App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Load users
        loadUsers()

        //Init components
        initEventsObserver(this)

        //Content
        setContent {
            CoonTheme {
                SyncLayout()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //Close service
        sendStop()

        //Check if library needs to be reset
        if (view.reloadLibraryOnExit) Library.loadLibrary(this, true)
    }

    //Layout
    @Composable
    private fun SyncLayout() {
        //Get useful stuff
        val activity = this

        //Navigation
        val navController = rememberNavController()

        //Observe status changes for screen navigation
        LaunchedEffect(view.connectionStatus) {
            when (view.connectionStatus) {
                //Not connected
                SyncService.STATUS_OFFLINE, SyncService.STATUS_CONNECTING -> {
                    //Close logs
                    if (navController.currentDestination?.route == SyncRoutes.LOGS) {
                        navController.popBackStack(SyncRoutes.LOGS, true)
                    }
                }
                //Connected
                SyncService.STATUS_ONLINE -> {
                    //Open logs
                    if (navController.currentDestination?.route != SyncRoutes.LOGS) {
                        navController.navigate(SyncRoutes.LOGS)
                    }
                }
            }
        }

        //Layout
        Layout(R.string.sync_title) {
            NavHost(
                navController = navController,
                startDestination = SyncRoutes.PERMISSIONS,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                }
            ) {
                //Get padding
                val paddingValues = it

                //Screen selection
                composable(SyncRoutes.PERMISSIONS) {
                    SyncPermissionsLayout(
                        onPermissionsGranted = {
                            navController.popBackStack()
                            navController.navigate(SyncRoutes.CONNECT)
                        }
                    )
                }
                composable(SyncRoutes.CONNECT) {
                    SyncConnectLayout(
                        paddingValues,
                        activity,
                        view.connectionStatus != SyncService.STATUS_OFFLINE
                    )
                }
                composable(SyncRoutes.LOGS) { backStackEntry ->
                    DisposableEffect(backStackEntry) {
                        onDispose {
                            if (view.connectionStatus == SyncService.STATUS_ONLINE) sendDisconnect()
                        }
                    }
                    SyncLogsLayout(paddingValues)
                }
            }
        }
    }

    @Composable
    private fun SyncPermissionsLayout(onPermissionsGranted: () -> Unit) {
        //Layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 4.dp,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(40.dp)
            )
        }

        //Create permission granted events
        var permissionManager: PermissionManager? = null
        val requestPermissionNotifications = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {
            permissionManager!!.notifyPermissionChanged(PermissionType.Notifications)
            view.updatePermissions(permissionManager)
        }
        val requestLocalNetworkPermission = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {
            permissionManager!!.notifyPermissionChanged(PermissionType.LocalAreaNetwork)
            view.updatePermissions(permissionManager)
        }

        //Create request permission event
        val onRequestPermission = remember {
            { permission: PermissionType ->
                when (permission) {
                    //Notifications
                    PermissionType.Notifications -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    //Local area network
                    PermissionType.LocalAreaNetwork -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                            requestLocalNetworkPermission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                        }
                    }
                    //Other
                    else -> {}
                }
            }
        }

        //Create permissions granted event
        val onPermissionsGranted = remember {
            {
                //Mark permissions as granted
                view.updatePermissions(permissionManager)
                onPermissionsGranted.invoke()
            }
        }

        //Create permission manager & check for permissions
        permissionManager = PermissionManager(this, listOf(PermissionType.Notifications, PermissionType.LocalAreaNetwork), onRequestPermission, onPermissionsGranted)
        if (!permissionManager.hasAllPermissions) {
            //No permissions -> Ask for them
            permissionManager.showDialog(this)
        }
    }

    @Composable
    private fun SyncConnectLayout(it: PaddingValues, activity: Activity, connecting: Boolean) {
        //Layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            //Connect
            Group {
                //Title
                GroupTitle(R.string.sync_connect_title)

                //Items
                GroupItems {
                    //Name input
                    GroupItem {
                        TextField(
                            value = view.connectName,
                            label = {
                                Text(stringResource(R.string.sync_connect_name_hint))
                            },
                            maxLines = 1,
                            onValueChange = { newValue: String -> view.connectName = newValue },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        )
                    }

                    //Divider
                    GroupDivider()

                    //Code input
                    GroupItem {
                        TextField(
                            value = view.connectCode,
                            label = {
                                Text(stringResource(R.string.sync_connect_code_hint))
                            },
                            maxLines = 1,
                            onValueChange = { newValue: String -> view.connectCode = newValue },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        )
                    }
                }

                //Connect button & connecting indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(top = 10.dp)
                ) {
                    //Connect button
                    SimpleButton(
                        text = R.string.sync_connect_action_connect,
                        onClick = {
                            //Already trying to connect
                            if (view.connectionStatus != SyncService.STATUS_OFFLINE) return@SimpleButton

                            //Check if code is valid
                            if (view.connectCode.isEmpty()) return@SimpleButton

                            //Check if name should get saved
                            if (!view.connectName.isEmpty()) {
                                //Check if name is saved
                                var isSaved = false
                                for (i in view.users.indices) {
                                    //Get user
                                    val user = view.users[i]
                                    if (user.name != view.connectName) continue

                                    //Update user
                                    user.code = view.connectCode
                                    saveUsers()
                                    isSaved = true
                                    break
                                }

                                //Add new user
                                if (!isSaved) {
                                    view.users.add(0, SyncUser(view.connectName, view.connectCode))
                                    saveUsers()
                                }
                            }

                            //Connect
                            connect(view.connectCode)
                        },
                        modifier = Modifier
                            .weight(1.0f)
                    )

                    //Connecting indicator
                    if (connecting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 4.dp,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(40.dp)
                        )
                    }
                }
            }

            //Users
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Group {
                        //Title
                        GroupTitle(R.string.sync_users_title)

                        //Items
                        GroupItems {
                            if (view.users.isEmpty()) {
                                //Empty users message
                                Text(
                                    text = stringResource(R.string.sync_users_message_empty),
                                    modifier = Modifier
                                        .padding(horizontal = groupItemPaddingHorizontal, vertical = groupItemPaddingVertical)
                                )
                            } else {
                                view.users.forEachIndexed { index, user ->
                                    //Add item
                                    GroupItem {
                                        UserItem(
                                            index = index,
                                            user = user,
                                            onConnect = { index, user ->
                                                //Move user first in list
                                                view.users.removeAt(index)
                                                view.users.add(0, user)
                                                saveUsers()

                                                //Connect to user
                                                connect(user.code)
                                            },
                                            onSelect = { index, user ->
                                                //Select user info
                                                view.connectName = user.name
                                                view.connectCode = user.code

                                                //Hide keyboard
                                                Orion.hideKeyboard(activity)
                                                Orion.clearFocus(activity)
                                            },
                                            onDelete = { index ->
                                                //Delete user
                                                view.users.removeAt(index)
                                                saveUsers()
                                            }
                                        )
                                    }

                                    //Add divider between items
                                    if (index < view.users.size - 1) GroupDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SyncLogsLayout(it: PaddingValues) {
        //List stuff
        val listState = rememberLazyListState()

        //Scroll to first
        LaunchedEffect(Unit) {
            view.scrollRequest.collect {
                listState.animateScrollToItem(0)
            }
        }

        //Layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            //Logs
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(view.logs) { index, log ->
                    //Add log
                    Text(
                        text = log,
                        fontFamily = FONT_OUTFIT,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                    )
                }
            }

            //Exit button
            SimpleButton(
                text = R.string.sync_logs_action_disconnect,
                onClick = {
                    sendDisconnect()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        }
    }

    //Connect & Users
    private fun loadUsers() {
        //Var to check if save needed
        var needsSave = false

        //Split users
        val userStrings: MutableList<String> = Storage.getStringList(StoragePairs.SYNC_USERS_KEY)
        for (userString in userStrings) {
            //Check if valid
            val separatorIndex = userString.indexOf("\n")
            if (separatorIndex == -1) {
                needsSave = true
                continue
            }

            //Add user
            view.users.add(
                SyncUser(
                    userString.substring(0, separatorIndex),
                    userString.substring(separatorIndex + 1)
                )
            )
        }

        //Save users after change
        if (needsSave) saveUsers()
    }

    private fun saveUsers() {
        //Create string users
        val userStrings: MutableList<String> = ArrayList()
        for (user in view.users) userStrings.add(user.toString())

        //Save list
        Storage.putStringList(StoragePairs.SYNC_USERS_KEY, userStrings)
    }

    private fun decodeBase36(code: String): Long {
        val codeCharset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var result: Long = 0
        for (i in 0..<code.length) result = result * 36 + codeCharset.indexOf(code[i])
        return result
    }

    private fun convertCodeToAddress(code: String): String {
        val combined: Long = decodeBase36(code.uppercase(Locale.getDefault()))

        val port = (combined and 0xFFFFL).toInt()
        val ipNum = combined shr 16

        val ip = String.format(
            Locale.US,
            "%d.%d.%d.%d",
            (ipNum shr 24) and 0xFFL,
            (ipNum shr 16) and 0xFFL,
            (ipNum shr 8) and 0xFFL,
            ipNum and 0xFFL
        )

        return "$ip:$port"
    }

    private fun connect(code: String) {
        //Hide keyboard
        Orion.hideKeyboard(this)
        Orion.clearFocus(this)

        //Connect
        if (code.contains(":")) {
            //Code is already an IP:PORT address
            sendConnect(code)
        } else {
            //Code needs conversion to address
            sendConnect(convertCodeToAddress(code))
        }
    }

    //Logs
    private fun log(log: String) {
        //Reached maximum size -> Remove first
        if (view.logs.size >= logsMax) view.logs.removeAt(view.logs.size - 1)

        //Add new log
        view.logs.add(0, log)

        //Scroll to bottom
        view.requestScrollToBottom()
    }

    //Events
    private fun initEventsObserver(context: Context) {
        //Get instance
        val instance = instance

        //Observe
        instance.trigger.observe(this, Observer { t: Boolean ->
            var event: SyncEvent?
            while ((instance.eventQueue.poll().also { event = it }) != null) handleEvent(context, event)
        })
    }

    private fun handleEvent(context: Context, event: SyncEvent?) {
        //Get command
        val command = event?.command ?: return

        //Check command
        when (command) {
            "init" -> log(context.getString(R.string.sync_logs_started))
            "status" -> {
                view.connectionStatus = event.valueInt
                when (view.connectionStatus) {
                    SyncService.STATUS_OFFLINE -> log(context.getString(R.string.sync_logs_status_offline))
                    SyncService.STATUS_CONNECTING -> log(context.getString(R.string.sync_logs_status_connecting))
                    SyncService.STATUS_ONLINE -> log(context.getString(R.string.sync_logs_status_online))
                }
            }
            "snack" -> Orion.snack(this, event.valueString)
            "log" -> log(event.valueString)
            "reload" -> view.reloadLibraryOnExit = true
        }
    }

    private fun sendStop() {
        val intent = Intent(this, SyncService::class.java)
        intent.putExtra("command", "stop")
        startService(intent)
    }

    private fun sendDisconnect() {
        val intent = Intent(this, SyncService::class.java)
        intent.putExtra("command", "disconnect")
        startService(intent)
    }

    private fun sendConnect(value: String) {
        val intent = Intent(this, SyncService::class.java)
        intent.putExtra("command", "connect")
        intent.putExtra("value", value)
        startService(intent)
    }

}