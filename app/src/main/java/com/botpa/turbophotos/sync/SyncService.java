package com.botpa.turbophotos.sync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.home.HomeActivity;
import com.botpa.turbophotos.gallery.Album;
import com.botpa.turbophotos.gallery.Link;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.gallery.CoonItem;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dev.gustavoavila.websocketclient.WebSocketClient;

@SuppressWarnings("SameParameterValue")
public class SyncService extends Service {

    //Notifications
    private static final int NOTIFICATION_ID = 420;
    private static final String NOTIFICATION_CHANNEL_ID = "sync_service";
    private static final String NOTIFICATION_CHANNEL_NAME = "Sync service";

    private NotificationManager notificationManager;
    private Notification notification;

    //Web socket
    public static final int STATUS_OFFLINE = 0;
    public static final int STATUS_CONNECTING = 1;
    public static final int STATUS_ONLINE = 2;

    public static WebSocketClient webSocketClient;

    //Files
    private static final int MAX_PART_SIZE = 10000000;

    //Service
    private boolean init = false;

    //Requests
    private int metadataRequestIndex = 0;
    private MetadataInfo metadataRequest;

    //Albums
    private final List<List<CoonItem>> backupItems = new ArrayList<>();


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Init service
        if (!init) {
            init = true;

            //Start service with notification
            buildForegroundNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
            startForeground(NOTIFICATION_ID, notification);

            //Init backup items
            backupItems.clear();
            for (Link link : Link.links) {
                //Get album & create items list
                Album album = link.album;
                List<CoonItem> items = new ArrayList<>();

                //Add items if album exists
                if (album != null) items.addAll(album.items);

                //Save list
                backupItems.add(items);
            }

            //Tell the activity to start
            send("init");
        }

        //Handle events
        handleEvent(intent);

        //Finish
        return Service.START_STICKY;
    }

    private void onStop() {
        //Close web socket
        if (webSocketClient != null) webSocketClient.close(1000, 1001, "Left app");

        //Stop service
        stopSelf();
    }

    //Web socket
    private void setStatus(int status) {
        //Send status
        send("status", status);
    }

    private void connect(String address) {
        //Try to connect
        setStatus(STATUS_CONNECTING);

        //Connect
        URI uri;
        try {
            uri = new URI("ws://" + address);
        } catch (URISyntaxException e) {
            setStatus(STATUS_OFFLINE);
            return;
        }

        //Create client
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                setStatus(STATUS_ONLINE);

                //Create albums object
                ObjectNode obj = Orion.getEmptyJson();
                obj.put("action", "albums");
                ArrayNode albumsJsonArray = Orion.getEmptyJsonArray();
                for (int i = 0; i < backupItems.size(); i++) {
                    //Get items
                    List<CoonItem> items = backupItems.get(i);

                    //Create items list
                    String[] itemsArray = new String[items.size()];
                    for (int j = 0; j < items.size(); j++) itemsArray[j] = items.get(j).name;

                    //Add array with album items
                    albumsJsonArray.add(Orion.arrayToJson(itemsArray));
                }
                obj.set("albums", albumsJsonArray);

                //Send albums
                webSocketClient.send(obj.toString());
            }

            @Override
            public void onTextReceived(String message) {
                parseStringMessage(message);
            }

            @Override
            public void onBinaryReceived(byte[] data) { parseBinaryMessage(data); }

            @Override
            public void onPingReceived(byte[] data) {
                System.out.println("ping received");
                sendPong(data);
            }

            @Override
            public void onPongReceived(byte[] data) {
                System.out.println("pong received");
            }

            @Override
            public void onException(Exception e) {
                setStatus(STATUS_OFFLINE);
                String errorMessage = e.getMessage();
                if (errorMessage != null) Log.e("WebSocket Exception", e.getMessage());
            }

            @Override
            public void onCloseReceived(int reason, String description) {
                setStatus(STATUS_OFFLINE);
            }
        };

        //Connect client
        webSocketClient.setConnectTimeout(10000);
        webSocketClient.connect();
    }

    private void parseStringMessage(String messageString) {
        //Show notification again
        notificationManager.notify(NOTIFICATION_ID, notification);

        //Check message
        try {
            ObjectNode message = Orion.loadJson(messageString);

            //No action
            if (!message.has("action")) throw new Exception("Message has no action key");

            //Parse action
            switch (message.get("action").asText()) {

                //End sync
                case "endSync": {
                    //Log
                    send("log", "Finished sync");
                    break;
                }

                //Send item info
                case "requestItemInfo": {
                    //Get album
                    int albumIndex = message.get("albumIndex").asInt();
                    List<CoonItem> album = backupItems.get(albumIndex);

                    //Get item & file
                    int itemIndex = message.get("itemIndex").asInt();
                    CoonItem item = album.get(itemIndex);
                    File file = item.file;

                    //Log
                    send("log", "- Sending item \"" + item.name + "\"...");

                    //Create message
                    ObjectNode obj = Orion.getEmptyJson();
                    obj.put("action", "itemInfo");
                    obj.put("albumIndex", albumIndex);
                    obj.put("itemIndex", itemIndex);
                    if (file.exists()) {
                        obj.put("lastModified", item.lastModified);
                        obj.put("size", item.size);
                        obj.put("parts", (item.size / MAX_PART_SIZE) + 1);
                        obj.put("maxPartSize", MAX_PART_SIZE);
                    }

                    //Send message
                    webSocketClient.send(obj.toString());
                    break;
                }

                //Send item data
                case "requestItemData": {
                    //Get album
                    int albumIndex = message.get("albumIndex").asInt();
                    List<CoonItem> album = backupItems.get(albumIndex);

                    //Get item & file
                    int itemIndex = message.get("itemIndex").asInt();
                    CoonItem item = album.get(itemIndex);
                    File file = item.file;

                    //Prepare log
                    int requestIndex = message.get("requestIndex").asInt();
                    int requestCount = message.get("requestCount").asInt();
                    String requestText = "(" + (requestIndex + 1) + "/" + requestCount + ") ";

                    //Get offset & length
                    int offset = message.get("part").asInt() * MAX_PART_SIZE;
                    int length = Math.min((int) item.size - offset, MAX_PART_SIZE);

                    //Send file data
                    try {
                        //Read file
                        byte[] bytes = new byte[length];
                        BufferedInputStream buffer = new BufferedInputStream(Files.newInputStream(file.toPath()));
                        long skipped = buffer.skip(offset);
                        int read = buffer.read(bytes, 0, bytes.length);
                        buffer.close();

                        //Log
                        send("log", requestText + "Success");

                        //Send message
                        webSocketClient.send(bytes);
                    } catch (IOException e) {
                        //Error
                        String errorMessage = e.getMessage();
                        if (errorMessage != null) Log.e("Send item data", errorMessage);

                        //Log
                        send("log", requestText + "Error reading item data");

                        //Send empty blob
                        webSocketClient.send(new byte[0]);
                    }
                    break;
                }

                //Send metadata info
                case "requestMetadataInfo": {
                    //Get info
                    int albumIndex = message.get("albumIndex").asInt();
                    File file = Link.links.get(albumIndex).metadataFile;

                    //Log
                    send("log", "- Sending metadata for album " + albumIndex + "...");

                    //Create message
                    ObjectNode obj = Orion.getEmptyJson();
                    obj.put("action", "metadataInfo");
                    obj.put("albumIndex", albumIndex);
                    if (file.exists()) {
                        obj.put("lastModified", file.lastModified() / 1000); //Millis to seconds
                    }

                    //Send message
                    webSocketClient.send(obj.toString());
                    break;
                }

                //Send metadata data
                case "requestMetadataData": {
                    //Get info
                    int albumIndex = message.get("albumIndex").asInt();
                    Link link = Link.links.get(albumIndex);
                    File file = link.metadataFile;

                    //Prepare log
                    String requestText = "(" + (albumIndex + 1) + "/" + Link.links.size() + ") ";

                    //Send metadata data
                    try {
                        //Read file
                        byte[] bytes = new byte[(int) file.length()];
                        BufferedInputStream buffer = new BufferedInputStream(Files.newInputStream(file.toPath()));
                        int read = buffer.read(bytes, 0, bytes.length);
                        buffer.close();

                        //Log
                        send("log", requestText + "Success");

                        //Send message
                        webSocketClient.send(bytes);
                    } catch (IOException e) {
                        //Error
                        String errorMessage = e.getMessage();
                        if (errorMessage != null) Log.e("Send metadata data", errorMessage);

                        //Log
                        send("log", requestText + "Error reading metadata data");

                        //Send empty blob
                        webSocketClient.send(new byte[0]);
                    }
                    break;
                }

                //Start requesting metadata
                case "startMetadataRequest": {
                    requestNextMetadata(true);
                    break;
                }

                //Received metadata info
                case "metadataInfo": {
                    //Get info
                    int albumIndex = message.get("albumIndex").asInt();
                    long lastModified = message.get("lastModified").asLong() * 1000; //Seconds to millis

                    //Save request
                    metadataRequest = new MetadataInfo(albumIndex, lastModified);

                    //Create message
                    ObjectNode obj = Orion.getEmptyJson();
                    obj.put("action", "requestMetadataData");
                    obj.put("albumIndex", albumIndex);

                    //Send message
                    webSocketClient.send(obj.toString());
                    break;
                }

            }
        } catch (Exception e) {
            //Error
            String errorMessage = e.getMessage();
            if (errorMessage != null) Log.e("Parse message", errorMessage);

            //Log
            send("log", "Error parsing message");
        }
    }

    private void parseBinaryMessage(byte[] data) {
        //Show notification again
        notificationManager.notify(NOTIFICATION_ID, notification);

        //No request -> Ignore
        if (metadataRequest == null) return;

        //Get info
        int albumIndex = metadataRequest.albumIndex;
        long lastModified = metadataRequest.lastModified;
        Link link = Link.links.get(albumIndex);
        File file = link.metadataFile;

        //Prepare log
        String requestText = "(" + (albumIndex + 1) + "/" + Link.links.size() + ") ";

        //Save file
        try {
            //Try to save file
            BufferedOutputStream buffer = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            buffer.write(data, 0, data.length);
            buffer.close();
            Files.setLastModifiedTime(Paths.get(file.getAbsolutePath()), FileTime.from(Instant.ofEpochMilli(lastModified)));

            //File modified -> Should restart
            HomeActivity.reloadOnResume();

            //Log
            send("log", requestText + "Success");
        } catch (IOException e) {
            //Error
            String errorMessage = e.getMessage();
            if (errorMessage != null) Log.e("Save metadata data", errorMessage);

            //Log
            send("log", requestText + "Error saving metadata");
        }

        //Request next
        requestNextMetadata(false);
    }

    //Notifications
    private void buildForegroundNotification() {
        //Get notification manager
        if (notificationManager == null) notificationManager = getSystemService(NotificationManager.class);

        //Create notification channel
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Appears when the sync service is active");
        notificationManager.createNotificationChannel(channel);

        //Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel.getId());

        builder
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Sync")
            .setContentText("The sync service is active")
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, SyncActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .setOngoing(true);

        notification = builder.build();
    }

    //Events
    private void handleEvent(Intent intent) {
        //Check intent
        if (intent == null || intent.getExtras() == null) return;

        //Get command
        String command = intent.getStringExtra("command");
        if (command == null) return;

        //Parse command
        switch (command) {
            //Connect to address
            case "connect":
                String address = intent.getStringExtra("value");
                if (address != null) connect(address);
                break;

            //Stop service
            case "stop":
                onStop();
                break;

            //Notification dismissed
            case "notification":
                notificationManager.notify(NOTIFICATION_ID, notification);
                break;
        }
    }

    private void send(String command) {
        //Send event
        SyncEventBus.getInstance().postEvent(command);
    }

    private void send(String command, String value) {
        //Send event
        SyncEventBus.getInstance().postEvent(command, value);
    }

    private void send(String command, int value) {
        //Send event
        SyncEventBus.getInstance().postEvent(command, value);
    }

    //Metadata requests
    private void requestNextMetadata(boolean isFirst) {
        //Reset index
        if (isFirst)
            metadataRequestIndex = 0;
        else
            metadataRequestIndex++;

        //Finished
        if (metadataRequestIndex >= Link.links.size()) {
            //Log
            send("log", "Finished metadata sync");

            //Create message
            ObjectNode obj = Orion.getEmptyJson();
            obj.put("action", "endSync");

            //Send message
            webSocketClient.send(obj.toString());
            return;
        }

        //Check if metadata file exists
        File file = Link.links.get(metadataRequestIndex).metadataFile;
        if (!file.exists()) {
            //Log
            send("log", "- Skipping metadata for album " + metadataRequestIndex + "... (file does not exist)");

            //Request next
            requestNextMetadata(false);
            return;
        }

        //Log
        send("log", "- Requesting metadata for album " + metadataRequestIndex + "...");

        //Request next metadata
        ObjectNode obj = Orion.getEmptyJson();
        obj.put("action", "requestMetadataInfo");
        obj.put("albumIndex", metadataRequestIndex);

        //Send message
        webSocketClient.send(obj.toString());
    }

    private static class MetadataInfo {

        public int albumIndex;
        public long lastModified;

        public MetadataInfo(int albumIndex, long lastModified) {
            this.albumIndex = albumIndex;
            this.lastModified = lastModified;
        }

    }

}
