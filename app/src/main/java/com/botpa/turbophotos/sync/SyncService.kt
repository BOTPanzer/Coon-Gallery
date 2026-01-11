package com.botpa.turbophotos.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

import androidx.core.app.NotificationCompat

import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.CoonItem
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.home.HomeActivity.Companion.reloadOnResume
import com.botpa.turbophotos.util.Orion

import com.fasterxml.jackson.databind.JsonNode

import dev.gustavoavila.websocketclient.WebSocketClient

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.time.Instant

import kotlin.math.min

class SyncService : Service() {

    //Service
    private var isInit = false

    //Notification
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification

    //Requests
    private var metadataRequestIndex = 0
    private var metadataRequest: MetadataInfo? = null

    //Albums
    private val backupItems: MutableList<MutableList<CoonItem>> = ArrayList()

    //Web socket
    private var webSocketClient: WebSocketClient? = null


    //Events
    override fun onBind(intent: Intent): IBinder? { return null }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //Init service
        if (!isInit) {
            //Start service with notification
            buildForegroundNotification()
            notificationManager.notify(NOTIFICATION_ID, notification)
            startForeground(NOTIFICATION_ID, notification)

            //Init backup items
            backupItems.clear()
            for (link in Link.links) {
                //Get album & create items list
                val album = link.album
                val items: MutableList<CoonItem> = ArrayList()

                //Add items if album exists
                if (album != null) items.addAll(album.items)

                //Save list
                backupItems.add(items)
            }

            //Tell the activity to start
            sendInit()

            //Mark as init
            isInit = true
        }

        //Handle events
        handleEvent(intent)

        //Finish
        return START_STICKY
    }

    private fun onStop() {
        //Close web socket
        if (webSocketClient != null) webSocketClient!!.close(1000, 1001, "Left app")

        //Stop service
        stopSelf()
    }

    //Web socket
    private fun connect(address: String?) {
        //Try to connect
        sendStatus(STATUS_CONNECTING)

        //Connect
        val uri: URI?
        try {
            uri = URI("ws://$address")
        } catch (_: URISyntaxException) {
            sendStatus(STATUS_OFFLINE)
            return
        }

        //Create client
        webSocketClient = object : WebSocketClient(uri) {

            override fun onOpen() {
                //Update status
                sendStatus(STATUS_ONLINE)

                //Create albums object
                val obj = Orion.getEmptyJson()
                obj.put("action", "albums")
                val albumsJsonArray = Orion.getEmptyJsonArray()
                for (i in backupItems.indices) {
                    //Get items
                    val items = backupItems[i]

                    //Create items list
                    val itemsArray = arrayOfNulls<String>(items.size)
                    for (j in items.indices) itemsArray[j] = items[j].name

                    //Add array with album items
                    albumsJsonArray.add(Orion.arrayToJson(itemsArray))
                }
                obj.set<JsonNode?>("albums", albumsJsonArray)

                //Send albums
                sendThroughWebSocket(obj.toString())
            }

            override fun onTextReceived(message: String?) {
                parseStringMessage(message)
            }

            override fun onBinaryReceived(data: ByteArray) {
                parseBinaryMessage(data)
            }

            override fun onPingReceived(data: ByteArray?) {
                println("ping received");
                sendPong(data);
            }

            override fun onPongReceived(data: ByteArray?) {
                println("pong received");
            }

            override fun onException(e: Exception) {
                //Update status
                sendStatus(STATUS_OFFLINE)

                //Log error
                val errorMessage = e.message
                if (errorMessage != null) Log.e("WebSocket Exception", errorMessage)
            }

            override fun onCloseReceived(reason: Int, description: String?) {
                //Update status
                sendStatus(STATUS_OFFLINE)
            }

        }

        //Connect client
        webSocketClient!!.setConnectTimeout(10000)
        webSocketClient!!.connect()
    }

    private fun parseStringMessage(messageString: String?) {
        //Show notification again
        notificationManager.notify(NOTIFICATION_ID, notification)

        //Check message
        try {
            val message = Orion.loadJson(messageString)

            //No action
            if (!message.has("action")) throw Exception("Message has no action key")

            //Parse action
            when (message.get("action").asText()) {
                //End sync
                "endSync" -> {
                    //Log
                    sendLog("Finished sync")
                }

                //Send item info
                "requestItemInfo" -> {
                    //Get album
                    val albumIndex = message.get("albumIndex").asInt()
                    val album = backupItems[albumIndex]

                    //Get item & file
                    val itemIndex = message.get("itemIndex").asInt()
                    val item = album[itemIndex]
                    val file = item.file

                    //Log
                    sendLog("- Sending item \"${item.name}\"...")

                    //Create message
                    val obj = Orion.getEmptyJson()
                    obj.put("action", "itemInfo")
                    obj.put("albumIndex", albumIndex)
                    obj.put("itemIndex", itemIndex)
                    if (file.exists()) {
                        obj.put("lastModified", item.lastModified)
                        obj.put("size", item.size)
                        obj.put("parts", (item.size / MAX_PART_SIZE) + 1)
                        obj.put("maxPartSize", MAX_PART_SIZE)
                    }

                    //Send message
                    sendThroughWebSocket(obj.toString())
                }

                //Send item data
                "requestItemData" -> {
                    //Get album
                    val albumIndex = message.get("albumIndex").asInt()
                    val album = backupItems[albumIndex]

                    //Get item & file
                    val itemIndex = message.get("itemIndex").asInt()
                    val item = album[itemIndex]
                    val file = item.file

                    //Prepare log
                    val requestIndex = message.get("requestIndex").asInt()
                    val requestCount = message.get("requestCount").asInt()
                    val requestText = "(${requestIndex + 1}/${requestCount})"

                    //Get offset & length
                    val offset: Int = message.get("part").asInt() * MAX_PART_SIZE
                    val length = min(item.size.toInt() - offset, MAX_PART_SIZE)

                    //Send file data
                    try {
                        //Read file
                        val bytes = ByteArray(length)
                        val buffer = BufferedInputStream(Files.newInputStream(file.toPath()))
                        buffer.skip(offset.toLong())
                        buffer.read(bytes, 0, bytes.size)
                        buffer.close()

                        //Log
                        sendLog("$requestText Success")

                        //Send message
                        sendThroughWebSocket(bytes)
                    } catch (e: IOException) {
                        //Error
                        val errorMessage = e.message
                        if (errorMessage != null) Log.e("Send item data", errorMessage)

                        //Log
                        sendLog("$requestText Error reading item data")

                        //Send empty blob
                        sendThroughWebSocket(ByteArray(0))
                    }
                }

                //Send metadata info
                "requestMetadataInfo" -> {
                    //Get info
                    val albumIndex = message.get("albumIndex").asInt()
                    val file = Link.links[albumIndex].metadataFile

                    //Log
                    sendLog("- Sending metadata for album $albumIndex...")

                    //Create message
                    val obj = Orion.getEmptyJson()
                    obj.put("action", "metadataInfo")
                    obj.put("albumIndex", albumIndex)
                    if (file.exists()) {
                        obj.put("lastModified", file.lastModified() / 1000) //Millis to seconds
                    }

                    //Send message
                    sendThroughWebSocket(obj.toString())
                }

                //Send metadata data
                "requestMetadataData" -> {
                    //Get info
                    val albumIndex = message.get("albumIndex").asInt()
                    val link = Link.links.get(albumIndex)
                    val file = link.metadataFile

                    //Prepare log
                    val requestText = "(${albumIndex + 1}/${Link.links.size})"

                    //Send metadata data
                    try {
                        //Read file
                        val bytes = ByteArray(file.length().toInt())
                        val buffer = BufferedInputStream(Files.newInputStream(file.toPath()))
                        buffer.read(bytes, 0, bytes.size)
                        buffer.close()

                        //Log
                        sendLog("$requestText Success")

                        //Send message
                        sendThroughWebSocket(bytes)
                    } catch (e: IOException) {
                        //Error
                        val errorMessage = e.message
                        if (errorMessage != null) Log.e("Send metadata data", errorMessage)

                        //Log
                        sendLog("$requestText Error reading metadata data")

                        //Send empty blob
                        sendThroughWebSocket(ByteArray(0))
                    }
                }

                //Start requesting metadata
                "startMetadataRequest" -> {
                    requestNextMetadata(true)
                }

                //Received metadata info
                "metadataInfo" -> {
                    //Get info
                    val albumIndex = message.get("albumIndex").asInt()
                    val lastModified = message.get("lastModified").asLong() * 1000 //Seconds to millis

                    //Save request
                    metadataRequest = MetadataInfo(albumIndex, lastModified)

                    //Create message
                    val obj = Orion.getEmptyJson()
                    obj.put("action", "requestMetadataData")
                    obj.put("albumIndex", albumIndex)

                    //Send message
                    sendThroughWebSocket(obj.toString())
                }

            }
        } catch (e: Exception) {
            //Error
            val errorMessage = e.message
            if (errorMessage != null) Log.e("Parse message", errorMessage)

            //Log
            sendLog("Error parsing message")
        }
    }

    private fun parseBinaryMessage(data: ByteArray) {
        //Show notification again
        notificationManager.notify(NOTIFICATION_ID, notification)

        //Get request
        val metadataRequest = this.metadataRequest ?: return

        //Get info
        val albumIndex = metadataRequest.albumIndex
        val lastModified = metadataRequest.lastModified
        val link = Link.links[albumIndex]
        val file = link.metadataFile

        //Prepare log
        val requestText = "(${albumIndex + 1}/${Link.links.size})"

        //Save file
        try {
            //Try to save file
            val buffer = BufferedOutputStream(Files.newOutputStream(file.toPath()))
            buffer.write(data, 0, data.size)
            buffer.close()
            Files.setLastModifiedTime(Paths.get(file.absolutePath), FileTime.from(Instant.ofEpochMilli(lastModified)))

            //File modified -> Should restart
            reloadOnResume()

            //Log
            sendLog("$requestText Success")
        } catch (e: IOException) {
            //Error
            val errorMessage = e.message
            if (errorMessage != null) Log.e("Save metadata data", errorMessage)

            //Log
            sendLog("$requestText Error saving metadata")
        }

        //Request next
        requestNextMetadata(false)
    }

    private fun sendThroughWebSocket(message: String) {
        webSocketClient!!.send(message)
    }

    private fun sendThroughWebSocket(data: ByteArray) {
        webSocketClient!!.send(data)
    }

    //Notifications
    private fun buildForegroundNotification() {
        //Get notification manager
        if (!isInit) notificationManager = getSystemService(NotificationManager::class.java)

        //Create notification channel
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        channel.description = "Appears when the sync service is active"
        notificationManager.createNotificationChannel(channel)

        //Create notification
        val builder = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Sync")
            .setContentText("The sync service is active")
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, SyncActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .setOngoing(true)
        notification = builder.build()
    }

    //Events
    private fun handleEvent(intent: Intent) {
        //Check intent
        if (intent.extras == null) return

        //Get command
        val command = intent.getStringExtra("command") ?: return

        //Parse command
        when (command) {
            //Connect to address
            "connect" -> {
                val address = intent.getStringExtra("value")
                if (address != null) connect(address)
            }

            //Stop service
            "stop" -> onStop()

            //Notification dismissed
            "notification" -> notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun sendInit() {
        //Send event
        SyncEventBus.instance.postEvent("init")
    }

    private fun sendLog(value: String) {
        //Send event
        SyncEventBus.instance.postEvent("log", value)
    }

    private fun sendStatus(status: Int) {
        //Send event
        SyncEventBus.instance.postEvent("status", status)
    }

    //Metadata requests
    private fun requestNextMetadata(isFirst: Boolean) {
        //Reset index
        if (isFirst) metadataRequestIndex = 0
        else metadataRequestIndex++

        //Finished
        if (metadataRequestIndex >= Link.links.size) {
            //Log
            sendLog("Finished metadata sync")

            //Create message
            val obj = Orion.getEmptyJson()
            obj.put("action", "endSync")

            //Send message
            sendThroughWebSocket(obj.toString())
            return
        }

        //Check if metadata file exists
        val file = Link.links[metadataRequestIndex].metadataFile
        if (!file.exists()) {
            //Log
            sendLog("- Skipping metadata for album $metadataRequestIndex... (file does not exist)")

            //Request next
            requestNextMetadata(false)
            return
        }

        //Log
        sendLog("- Requesting metadata for album $metadataRequestIndex...")

        //Request next metadata
        val obj = Orion.getEmptyJson()
        obj.put("action", "requestMetadataInfo")
        obj.put("albumIndex", metadataRequestIndex)

        //Send message
        sendThroughWebSocket(obj.toString())
    }

    private class MetadataInfo(var albumIndex: Int, var lastModified: Long)

    companion object {

        //Connection
        const val STATUS_OFFLINE: Int = 0
        const val STATUS_CONNECTING: Int = 1
        const val STATUS_ONLINE: Int = 2

        //Notifications
        private const val NOTIFICATION_ID = 420
        private const val NOTIFICATION_CHANNEL_ID = "sync_service"
        private const val NOTIFICATION_CHANNEL_NAME = "Sync service"

        //Files
        private const val MAX_PART_SIZE = 10000000

    }

}
