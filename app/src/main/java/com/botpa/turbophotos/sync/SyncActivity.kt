package com.botpa.turbophotos.sync

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.botpa.turbophotos.R
import com.botpa.turbophotos.sync.SyncEventBus.Companion.instance
import com.botpa.turbophotos.sync.logs.LogAdapter
import com.botpa.turbophotos.sync.users.User
import com.botpa.turbophotos.sync.users.UserAdapter
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage

import java.util.Locale

@SuppressWarnings("SameParameterValue")
class SyncActivity : AppCompatActivity() {

    //Status
    private var connectionStatus: Int = SyncService.STATUS_OFFLINE

    //Users
    private val users: MutableList<User> = ArrayList()
    private lateinit var usersAdapter: UserAdapter

    private lateinit var usersLayout: View
    private lateinit var usersName: EditText
    private lateinit var usersCode: EditText
    private lateinit var usersConnect: View
    private lateinit var usersLoading: View
    private lateinit var usersList: RecyclerView

    //Logs
    private val logs: MutableList<String> = ArrayList()
    private val logsMax = 500
    private lateinit var logsAdapter: LogAdapter

    private lateinit var logsList: RecyclerView
    private lateinit var logsExit: View


    //State
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sync_screen)

        //Load users
        loadUsers()

        //Init components
        initViews()
        initListeners()
        initUsersList()
        initLogsList()
        initEventsObserver()

        //Start service
        val intent = Intent(this, SyncService::class.java)
        startService(intent)
    }

    override fun onDestroy() {
        //Close service
        send("stop")

        super.onDestroy()
    }

    private fun initViews() {
        //Views (users)
        usersLayout = findViewById  (R.id.usersLayout)
        usersName = findViewById    (R.id.usersName)
        usersCode = findViewById    (R.id.usersCode)
        usersConnect = findViewById (R.id.usersConnect)
        usersLoading = findViewById (R.id.usersLoading)
        usersList = findViewById    (R.id.usersList)

        //Views (logs)
        logsList = findViewById (R.id.logsList)
        logsExit = findViewById (R.id.logsExit)

        //Insets
        Orion.addInsetsChangedListener(
            window.decorView,
            intArrayOf( WindowInsetsCompat.Type.systemBars(), WindowInsetsCompat.Type.ime())
        )
    }

    private fun initListeners() {
        //Connect
        usersCode.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                Orion.clearFocus(this@SyncActivity)
                Orion.hideKeyboard(this@SyncActivity)
                usersConnect.performClick()
            }
            false
        }

        usersConnect.setOnClickListener { view: View ->
            //Already trying to connect
            if (connectionStatus != SyncService.STATUS_OFFLINE) return@setOnClickListener

            //Get code
            val code = usersCode.text.toString()
            if (code.isEmpty()) return@setOnClickListener

            //Get name
            val name = usersName.text.toString()
            if (!name.isEmpty()) {
                //Check if name is saved
                var isSaved = false
                for (i in users.indices) {
                    //Get user
                    val user = users[i]
                    if (user.name != name) continue

                    //Update user
                    user.code = code
                    usersAdapter.notifyItemChanged(i)
                    saveUsers()
                    isSaved = true
                    break
                }

                //Add new user
                if (!isSaved) {
                    users.add(0, User(name, code))
                    usersAdapter.notifyItemInserted(0)
                    saveUsers()
                }
            }

            //Connect
            connect(code)
        }

        //Logs
        logsExit.setOnClickListener { view: View -> finish() }
    }

    //Users
    private fun initUsersList() {
        //Create adapter
        usersAdapter = UserAdapter(this@SyncActivity, users)

        //Add connect listener
        usersAdapter.setOnClickListener { view: View, index: Int ->
            //Get user
            val user = users[index]

            //Move user first in list
            users.removeAt(index)
            usersAdapter.notifyItemRemoved(index)
            users.add(0, user)
            usersAdapter.notifyItemInserted(0)
            saveUsers()

            //Connect to user
            connect(user.code)
        }

        //Add select user listener
        usersAdapter.setOnLongClickListener { view: View, index: Int ->
            //Get user
            val user = users[index]

            //Select user info
            usersName.setText(user.name)
            usersCode.setText(user.code)

            //Hide keyboard
            Orion.hideKeyboard(this@SyncActivity)
            Orion.clearFocus(this@SyncActivity)
        }

        //Add delete user listener
        usersAdapter.setOnDeleteListener { view: View, index: Int ->
            //Delete user
            users.removeAt(index)
            usersAdapter.notifyItemRemoved(index)
            saveUsers()
        }

        //Add adapter to list
        usersList.setAdapter(usersAdapter)
        usersList.setLayoutManager(LinearLayoutManager(this@SyncActivity))
    }

    private fun loadUsers() {
        //Var to check if save needed
        var needsSave = false

        //Split users
        val userStrings: MutableList<String> = Storage.getStringList("Sync.users")
        for (userString in userStrings) {
            //Check if valid
            val separatorIndex = userString.indexOf("\n")
            if (separatorIndex == -1) {
                needsSave = true
                continue
            }

            //Add user
            users.add(
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
        for (user in users) userStrings.add(user.toString())

        //Save list
        Storage.putStringList("Sync.users", userStrings)
    }

    private fun decodeBase36(code: String): Long {
        val CODE_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var result: Long = 0
        for (i in 0..<code.length) result = result * 36 + CODE_CHARSET.indexOf(code[i])
        return result
    }

    private fun convertCodeToAddress(code: String): String {
        val combined: Long = decodeBase36(code.uppercase(Locale.getDefault()))

        val port = (combined and 0xFFFFL).toInt()
        val ipNum = combined shr 16

        val ip = String.format(
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
    private fun initLogsList() {
        //Create adapter
        logsAdapter = LogAdapter(this@SyncActivity, logs)

        //Add adapter to list
        logsList.setAdapter(logsAdapter)
        logsList.setLayoutManager(LinearLayoutManager(this@SyncActivity, LinearLayoutManager.VERTICAL, true))
        logsList.setItemAnimator(null)
    }

    private fun log(log: String) {
        //Reached maximum size -> Remove first
        if (logs.size >= logsMax) {
            logs.removeAt(logs.size - 1)
            logsAdapter.notifyItemRemoved(logs.size - 1)
        }

        //Add new log
        logs.add(0, log)
        logsAdapter.notifyItemInserted(0)

        //Scroll to bottom
        logsList.scrollToPosition(0)
    }

    //Events
    private fun initEventsObserver() {
        //Get instance
        val instance = instance

        //Observe
        instance.trigger.observe(this, Observer { t: Boolean? ->
            var event: SyncEvent?
            while ((instance.eventQueue.poll().also { event = it }) != null) handleEvent(event)
        })
    }

    private fun handleEvent(event: SyncEvent?) {
        if (event == null) return
        val command = event.command

        //Check command
        when (command) {
            "init" -> log("Service started")
            "status" -> {
                connectionStatus = event.valueInt
                when (connectionStatus) {
                    SyncService.STATUS_OFFLINE -> {
                        log("Disconnected")
                        usersLayout.visibility = View.VISIBLE
                        usersLoading.visibility = View.GONE
                    }
                    SyncService.STATUS_CONNECTING -> {
                        log("Connecting...")
                        usersLoading.visibility = View.VISIBLE
                    }
                    SyncService.STATUS_ONLINE -> {
                        log("Connected")
                        usersLayout.visibility = View.GONE
                        usersLoading.visibility = View.GONE
                    }
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
