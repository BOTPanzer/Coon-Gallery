package com.botpa.turbophotos.backup;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.main.MainActivity;
import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.TurboImage;
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

import dev.gustavoavila.websocketclient.WebSocketClient;

public class BackupService extends Service {

    //Activity communication
    public static final String BROADCAST_ID = "backupBroadcast";

    //Notifications
    private static final int NOTIFICATION_ID = 420;
    private static final String NOTIFICATION_CHANNEL_ID = "backup_manager";
    private static final String NOTIFICATION_CHANNEL_NAME = "Backup manager";

    private NotificationManager notificationManager;
    private Notification notification;

    //Web socket
    public static final int STATUS_OFFLINE = 0;
    public static final int STATUS_CONNECTING = 1;
    public static final int STATUS_ONLINE = 2;

    private BroadcastReceiver broadcastReceiver;
    public static WebSocketClient webSocketClient;
    private static int connectStatus = STATUS_OFFLINE;

    //Service
    private boolean init = false;

    //Requests
    private int metadataRequestIndex = 0;
    private MetadataInfo metadataRequest;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Init service
        if (!init) {
            init = true;

            //Register receiver
            registerReceiver();

            //Start service with notification
            buildForegroundNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
            startForeground(NOTIFICATION_ID, notification);
        }

        //Tell the activity to start
        send("init");

        return Service.START_STICKY;
    }

    private void onStop() {
        //Close web socket
        if (webSocketClient != null) webSocketClient.close(1000, 1001, "Left app");

        //Unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        //Stop service
        stopSelf();
    }

    //Web socket
    private void connect(String URL) {
        //Try to connect
        setStatus(STATUS_CONNECTING);

        //Connect
        URI uri;
        try {
            uri = new URI("ws://" + URL);
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
                ArrayNode albums = Orion.getEmptyJsonArray();
                for (int i = 0; i < Library.albums.size(); i++) {
                    //Get album
                    Album album = Library.albums.get(i);

                    //Create files list
                    String[] files = new String[album.files.size()];
                    for (int j = 0; j < album.files.size(); j++) files[j] = album.files.get(j).file.getName();

                    //Add array with album files
                    albums.add(Orion.arrayToJson(files));
                }
                obj.set("albums", albums);

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
        //webSocketClient.setReadTimeout(5000);
        //webSocketClient.addHeader("Origin", "http://botpa.vercel.app/");
        //webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }

    private void setStatus(int status) {
        connectStatus = status;
        send("status", connectStatus);
    }

    private void parseStringMessage(String messageString) {
        //Show notification again
        notificationManager.notify(NOTIFICATION_ID, notification);

        //Check message
        try {
            ObjectNode message = Orion.loadJson(messageString);
            if (!message.has("action")) throw new Exception("Message has no action key");
            String action = message.get("action").asText();
            switch (action) {

                //Send image info
                case "requestImageInfo": {
                    //Request index & count
                    if (message.has("requestIndex") && message.has("requestCount")) {
                        //Get request index & count
                        int requestIndex = message.get("requestIndex").asInt();
                        int requestCount = message.get("requestCount").asInt();

                        //Log
                        send("log", "Request (" + requestIndex + "/" + requestCount + ")");
                    }

                    //Get album
                    int albumIndex = message.get("albumIndex").asInt();
                    Album album = Library.albums.get(albumIndex);

                    //Get image
                    int imageIndex = message.get("imageIndex").asInt();

                    //Send image info
                    File file = album.files.get(imageIndex).file;

                    //Log
                    send("log", "Sending image info for: " + file.getName());

                    //Create message
                    ObjectNode obj = Orion.getEmptyJson();
                    obj.put("action", "imageInfo");
                    obj.put("albumIndex", albumIndex);
                    obj.put("imageIndex", imageIndex);
                    if (file.exists()) obj.put("lastModified", file.lastModified());

                    //Send message
                    webSocketClient.send(obj.toString());
                    break;
                }

                //Send image data
                case "requestImageData": {
                    //Get album
                    int albumIndex = message.get("albumIndex").asInt();
                    Album album = Library.albums.get(albumIndex);

                    //Get image
                    int imageIndex = message.get("imageIndex").asInt();
                    TurboImage image = album.files.get(imageIndex);

                    //Send image data
                    File file = image.file;
                    byte[] bytes = new byte[(int) file.length()];
                    try {
                        //Log
                        send("log", "Sending image data for: " + file.getName());

                        //Read file bytes
                        BufferedInputStream buffer = new BufferedInputStream(Files.newInputStream(file.toPath()));
                        int read = buffer.read(bytes, 0, bytes.length);
                        buffer.close();

                        //Send message
                        webSocketClient.send(bytes);
                    } catch (IOException e) {
                        //Error sending message
                        String errorMessage = e.getMessage();
                        if (errorMessage != null) Log.e("Send image data", errorMessage);
                        send("log", "Error sending image data");

                        //Send empty blob
                        webSocketClient.send(new byte[0]);
                    }
                    break;
                }

                //Send metadata info
                case "requestMetadataInfo": {
                    //Get album
                    int albumIndex = message.get("albumIndex").asInt();
                    Album album = Library.albums.get(albumIndex);

                    //Send metadata info
                    File file = album.metadataFile;

                    //Log
                    send("log", "Sending metadata info for: " + file.getName());

                    //Create message
                    ObjectNode obj = Orion.getEmptyJson();
                    obj.put("action", "metadataInfo");
                    obj.put("albumIndex", albumIndex);
                    if (file.exists()) obj.put("lastModified", file.lastModified());

                    //Send message
                    webSocketClient.send(obj.toString());
                    break;
                }

                //Send metadata data
                case "requestMetadataData": {
                    //Get album
                    int albumIndex = message.get("albumIndex").asInt();
                    Album album = Library.albums.get(albumIndex);

                    //Send metadata data
                    File file = album.metadataFile;
                    byte[] bytes = new byte[(int) file.length()];
                    try {
                        //Log
                        send("log", "Sending metadata data for: " + file.getName());

                        //Read file bytes
                        BufferedInputStream buffer = new BufferedInputStream(Files.newInputStream(file.toPath()));
                        int read = buffer.read(bytes, 0, bytes.length);
                        buffer.close();

                        //Send message
                        webSocketClient.send(bytes);
                    } catch (IOException e) {
                        //Error sending message
                        String errorMessage = e.getMessage();
                        if (errorMessage != null) Log.e("Send metadata data", errorMessage);
                        send("log", "Error sending metadata data");

                        //Send empty blob
                        webSocketClient.send(new byte[0]);
                    }
                    break;
                }

                //Request metadata
                case "startMetadataRequest": {
                    requestNextMetadata(true);
                    break;
                }

                //Received metadata info
                case "metadataInfo": {
                    //Invalid info -> Request next
                    if (!message.has("lastModified")) {
                        requestNextMetadata(false);
                        return;
                    }

                    //Get album
                    int albumIndex = message.get("albumIndex").asInt();

                    //Get last modified
                    long lastModified = message.get("lastModified").asLong();

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
            String errorMessage = e.getMessage();
            if (errorMessage != null) Log.e("Parse message", errorMessage);
        }
    }

    private void parseBinaryMessage(byte[] data) {
        //Show notification again
        notificationManager.notify(NOTIFICATION_ID, notification);

        //No request
        if (metadataRequest == null) return;

        //Save file
        File file = Library.albums.get(metadataRequest.albumIndex).metadataFile;
        try {
            BufferedOutputStream buffer = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            buffer.write(data, 0, data.length);
            buffer.close();
            Files.setLastModifiedTime(Paths.get(file.getAbsolutePath()), FileTime.from(Instant.ofEpochMilli(metadataRequest.lastModified)));

            //File modified -> Should restart
            MainActivity.shouldRestart();
        } catch (IOException e) {
            //Error sending message
            String errorMessage = e.getMessage();
            if (errorMessage != null) Log.e("Save metadata data", errorMessage);
            send("log", "Error saving metadata data");
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
        channel.setDescription("Appears when the backup service is active");
        notificationManager.createNotificationChannel(channel);

        //Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel.getId());

        builder
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Backup")
            .setContentText("The backup service is active")
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, BackupActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .setDeleteIntent(PendingIntent.getBroadcast(this, 1, new Intent(this, NotificationDeleteReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true);

        notification = builder.build();
    }

    public static class NotificationDeleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BackupService.BROADCAST_ID).putExtra("command", "notification"));
        }
    }

    //Broadcasts
    private void registerReceiver() {
        //Create broadcast receiver
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                //Invalid command
                if (intent.getExtras() == null) return;
                String command = intent.getStringExtra("command");
                if (command == null) return;

                //Check command
                switch (command) {
                    //Connect to URL
                    case "connect":
                        connect(intent.getStringExtra(command));
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
        };

        /*//Register receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, new IntentFilter("musicNotificationBroadcast"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, new IntentFilter("musicNotificationBroadcast"));
        }*/

        //Register receiver filter
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(BackupService.BROADCAST_ID));
    }

    private void send(String name) {
        Intent intent = new Intent(BackupService.BROADCAST_ID);
        intent.putExtra("command", name);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void send(String name, String value) {
        Intent intent = new Intent(BackupService.BROADCAST_ID);
        intent.putExtra("command", name);
        intent.putExtra(name, value);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void send(String name, boolean value) {
        Intent intent = new Intent(BackupService.BROADCAST_ID);
        intent.putExtra("command", name);
        intent.putExtra(name, value);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void send(String name, int value) {
        Intent intent = new Intent(BackupService.BROADCAST_ID);
        intent.putExtra("command", name);
        intent.putExtra(name, value);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    //Metadata requests
    private void requestNextMetadata(boolean isFirst) {
        //Reset index
        if (isFirst)
            metadataRequestIndex = 0;
        else
            metadataRequestIndex++;

        //Finished
        if (metadataRequestIndex >= Library.albums.size()) {
            //Create message
            ObjectNode obj = Orion.getEmptyJson();
            obj.put("action", "endSync");
            obj.put("message", "Finished metadata sync");

            //Send message
            webSocketClient.send(obj.toString());
            return;
        }

        //Check if metadata file exists
        File file = Library.albums.get(metadataRequestIndex).metadataFile;
        if (!file.exists()) {
            Log.e("Metadata missing", "Metadata file " + metadataRequestIndex + " does not exist");
            requestNextMetadata(false);
            return;
        }

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
