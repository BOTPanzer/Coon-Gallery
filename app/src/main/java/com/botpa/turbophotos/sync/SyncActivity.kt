package com.botpa.turbophotos.sync

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer

import com.botpa.turbophotos.gallery.views.Group
import com.botpa.turbophotos.gallery.views.GroupDivider
import com.botpa.turbophotos.gallery.views.GroupItems
import com.botpa.turbophotos.gallery.views.GroupTitle
import com.botpa.turbophotos.gallery.views.Layout
import com.botpa.turbophotos.gallery.views.groupItemPaddingHorizontal
import com.botpa.turbophotos.gallery.views.groupItemPaddingVertical
import com.botpa.turbophotos.settings.SettingsPairs
import com.botpa.turbophotos.sync.SyncEventBus.Companion.instance
import com.botpa.turbophotos.theme.CoonTheme
import com.botpa.turbophotos.theme.FONT_COMFORTAA
import com.botpa.turbophotos.theme.FONT_POPPINS
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage

import java.util.Locale

class SyncActivity : ComponentActivity() {

    //View model
    private val view: SyncViewModel by viewModels()

    //Logs
    private val logsMax = 500


    //App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Init storage
        Storage.init(this@SyncActivity)

        //Edging
        enableEdgeToEdge()

        //Load users
        loadUsers()

        //Init components
        initEventsObserver()

        //Content
        setContent {
            CoonTheme {
                SettingsLayout()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //Close service
        send("stop")
    }

    //Layout
    @Composable
    private fun SettingsLayout() {
        Layout("Sync") {
            when (view.connectionStatus) {
                SyncService.STATUS_OFFLINE -> ConnectLayout(it, this, false)
                SyncService.STATUS_CONNECTING -> ConnectLayout(it, this, true)
                SyncService.STATUS_ONLINE -> LogsLayout(it)
            }
        }
    }

    @Composable
    private fun ConnectLayout(it: PaddingValues, activity: Activity, connecting: Boolean) {
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
                GroupTitle("Connect")

                //Items
                GroupItems {
                    //Name input
                    TextField(
                        value = view.connectName,
                        label = { Text("Name (optional)") },
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
                    )

                    //Divider
                    GroupDivider()

                    //Code input
                    TextField(
                        value = view.connectCode,
                        label = { Text("Code") },
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
                    )
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
                    Button(
                        onClick = {
                            //Already trying to connect
                            if (view.connectionStatus != SyncService.STATUS_OFFLINE) return@Button

                            //Check if code is valid
                            if (view.connectCode.isEmpty()) return@Button

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
                                    view.users.add(0, User(view.connectName, view.connectCode))
                                    saveUsers()
                                }
                            }

                            //Connect
                            connect(view.connectCode)
                        },
                        modifier = Modifier
                            .weight(1.0f)
                    ) {
                        Text(
                            text = "Connect",
                            fontFamily = FONT_COMFORTAA,
                            fontSize = 14.sp
                        )
                    }

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
                        GroupTitle("Users")

                        //Items
                        GroupItems {
                            if (view.users.isEmpty()) {
                                //Empty users message
                                Text(
                                    text = "There are no saved users",
                                    modifier = Modifier
                                        .padding(horizontal = groupItemPaddingHorizontal, vertical = groupItemPaddingVertical)
                                )
                            } else {
                                view.users.forEachIndexed { index, user ->
                                    //Add item
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
    private fun LogsLayout(it: PaddingValues) {
        //List stuff
        val listState = rememberLazyListState()

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
                        fontFamily = FONT_POPPINS,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                    )
                }
            }

            //Exit button
            Button(
                onClick = { finish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Exit",
                    fontFamily = FONT_COMFORTAA,
                    fontSize = 14.sp
                )
            }
        }
    }

    //Connect & Users
    private fun loadUsers() {
        //Var to check if save needed
        var needsSave = false

        //Split users
        val userStrings: MutableList<String> = Storage.getStringList(SettingsPairs.SYNC_USERS_KEY)
        for (userString in userStrings) {
            //Check if valid
            val separatorIndex = userString.indexOf("\n")
            if (separatorIndex == -1) {
                needsSave = true
                continue
            }

            //Add user
            view.users.add(
                User(
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
        Storage.putStringList(SettingsPairs.SYNC_USERS_KEY, userStrings)
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
        Orion.hideKeyboard(this@SyncActivity)
        Orion.clearFocus(this@SyncActivity)

        //Connect
        if (code.contains(":")) {
            //Code is already an IP:PORT address
            send("connect", code)
        } else {
            //Code needs conversion to address
            send("connect", convertCodeToAddress(code))
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
    private fun initEventsObserver() {
        //Get instance
        val instance = instance

        //Observe
        instance.trigger.observe(this, Observer { t: Boolean ->
            var event: SyncEvent?
            while ((instance.eventQueue.poll().also { event = it }) != null) handleEvent(event)
        })
    }

    private fun handleEvent(event: SyncEvent?) {
        //Get command
        val command = event?.command ?: return

        //Check command
        when (command) {
            "init" -> log("Service started")
            "status" -> {
                view.connectionStatus = event.valueInt
                when (view.connectionStatus) {
                    SyncService.STATUS_OFFLINE -> log("Disconnected")
                    SyncService.STATUS_CONNECTING -> log("Connecting...")
                    SyncService.STATUS_ONLINE -> log("Connected")
                }
            }
            "snack" -> Orion.snack(this@SyncActivity, event.valueString)
            "log" -> log(event.valueString)
        }
    }

    private fun send(command: String) {
        val intent = Intent(this, SyncService::class.java)
        intent.putExtra("command", command)
        startService(intent)
    }

    private fun send(command: String, value: String) {
        val intent = Intent(this, SyncService::class.java)
        intent.putExtra("command", command)
        intent.putExtra("value", value)
        startService(intent)
    }

}