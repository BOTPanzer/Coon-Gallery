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
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.TurboImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;

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

                //Send albums
                JSONObject obj = new JSONObject();
                try {
                    obj.put("action", "albums");
                    JSONArray albums = new JSONArray();
                    for (int i = Library.albums.size() - 1; i >= 0; i--) {
                        //Get album
                        Album album = Library.albums.get(i);

                        //Create files list
                        ArrayList<String> files = new ArrayList<>();
                        for (TurboImage image: album.files) {
                            files.add(image.file.getName());
                        }

                        //Add array with album files
                        albums.put(i, new JSONArray(files));
                    }
                    obj.put("albums", albums);
                } catch (JSONException e) {
                    BackupService.this.send("snack", "Error creating albums JSON");
                    System.out.println(e.getMessage());
                }
                webSocketClient.send(obj.toString());
            }

            @Override
            public void onTextReceived(String message) {
                parseStringMessage(message);
            }

            @Override
            public void onBinaryReceived(byte[] data) {
                System.out.println("binary received");
            }

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
                System.out.println(e.getMessage());
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
            JSONObject message = new JSONObject(messageString);
            String action = message.getString("action");
            switch (action) {
                //Send image info (last modified)
                case "requestImageInfo": {
                    //Get album
                    int albumIndex = message.getInt("albumIndex");
                    Album album = Library.albums.get(albumIndex);

                    //Get image
                    int imageIndex = message.getInt("imageIndex");
                    TurboImage image = album.files.get(imageIndex);

                    //Send image info
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("action", "imageInfo");
                        obj.put("albumIndex", albumIndex);
                        obj.put("imageIndex", imageIndex);
                        obj.put("lastModified", image.file.lastModified());
                        webSocketClient.send(obj.toString());
                    } catch (JSONException e) {
                        send("snack", "Error sending image info");
                        System.out.println(e.getMessage());
                    }
                    break;
                }

                //Send image data
                case "requestImageData": {
                    //Get album
                    int albumIndex = message.getInt("albumIndex");
                    Album album = Library.albums.get(albumIndex);

                    //Get image
                    int imageIndex = message.getInt("imageIndex");
                    TurboImage image = album.files.get(imageIndex);

                    //Send image data
                    File file = image.file;
                    byte[] bytes = new byte[(int) file.length()];
                    try {
                        BufferedInputStream buf = new BufferedInputStream(Files.newInputStream(file.toPath()));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();
                        webSocketClient.send(bytes);
                    } catch (IOException e) {
                        send("snack", "Error sending image data");
                        System.out.println(e.getMessage());
                    }
                    break;
                }

                //Send image data
                case "requestMetadataData": {
                    //Get album
                    int albumIndex = message.getInt("albumIndex");
                    Album album = Library.albums.get(albumIndex);

                    //Send metadata data
                    File file = album.metadataFile;
                    byte[] bytes = new byte[(int) file.length()];
                    try {
                        BufferedInputStream buf = new BufferedInputStream(Files.newInputStream(file.toPath()));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();
                        webSocketClient.send(bytes);
                    } catch (IOException e) {
                        send("snack", "Error sending metadata data");
                        System.out.println(e.getMessage());
                    }
                    break;
                }
            }
        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }
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
}
