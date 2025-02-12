package com.botpa.turbophotos.backup;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import dev.gustavoavila.websocketclient.WebSocketClient;

public class BackupActivity extends AppCompatActivity {

    private final int STATUS_OFFLINE = 0;
    private final int STATUS_CONNECTING = 1;
    private final int STATUS_ONLINE = 2;

    //App
    private final List<User> users = new ArrayList<>();
    private WebSocketClient webSocketClient;
    private int connectStatus = STATUS_OFFLINE;

    //Connect
    private UserAdapter connectAdapter;

    private View connectLayout;
    private EditText connectName;
    private EditText connectURL;
    private View connectConnect;
    private View connectLoading;
    private RecyclerView connectList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup);

        //Get views & add listeners
        getViews();
        addListeners();


        //Load users
        loadUsers();


        //Init users list
        connectAdapter = new UserAdapter(BackupActivity.this, users);
        connectAdapter.setOnClickListener((view, index) -> {
            connect(users.get(index).URL);
        });
        connectAdapter.setOnDeleteListener((view, index) -> {
            users.remove(index);
            connectAdapter.notifyItemRemoved(index);
            saveUsers();
        });
        connectList.setAdapter(connectAdapter);
        connectList.setLayoutManager(new LinearLayoutManager(BackupActivity.this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Close web socket
        if (webSocketClient != null) webSocketClient.close(1000, 1001, "Left app");
    }

    //App
    private void getViews() {
        connectLayout = findViewById(R.id.connectLayout);
        connectName = findViewById(R.id.connectName);
        connectURL = findViewById(R.id.connectURL);
        connectConnect = findViewById(R.id.connectConnect);
        connectLoading = findViewById(R.id.connectLoading);
        connectList = findViewById(R.id.connectList);
    }

    private void addListeners() {
        //Connect
        connectURL.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                Orion.clearFocus(BackupActivity.this);
                Orion.hideKeyboard(BackupActivity.this);
                connectConnect.performClick();
            }
            return false;
        });

        connectConnect.setOnClickListener(view -> {
            //Trying to connect
            if (connectStatus != STATUS_OFFLINE) return;

            //Get URL
            String URL = connectURL.getText().toString();
            if (URL.isEmpty()) return;

            //Get name
            String name = connectName.getText().toString();
            if (!name.isEmpty()) {
                //Check if IP is saved
                boolean isSaved = false;
                for (User user: users) {
                    if (user.URL.equals(URL)) {
                        isSaved = true;
                        break;
                    }
                }

                //Save user
                if (!isSaved) {
                    users.add(new User(name, URL));
                    connectAdapter.notifyItemInserted(users.size() - 1);
                    saveUsers();
                }
            }

            //Connect
            connect(URL);
        });
    }

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

                //Send albums info
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("action", "albumsInfo");
                    obj.put("size", Library.albums.size());
                    webSocketClient.send(obj.toString());
                } catch (JSONException e) {
                    System.out.println("Error sending albums info");
                    return;
                }


                //webSocketClient.send("holiwi pititwi");

                /*File file = Library.files.get(0).file;
                int size = (int) file.length();
                byte[] bytes = new byte[size];
                try {
                    BufferedInputStream buf = new BufferedInputStream(Files.newInputStream(file.toPath()));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();
                    webSocketClient.send(bytes);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }*/
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
        webSocketClient.addHeader("Origin", "http://botpa.vercel.app/");
        //webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }

    private void setStatus(int status) {
        connectStatus = status;
        runOnUiThread(() -> {
            switch (status) {
                case STATUS_OFFLINE:
                    connectLayout.setVisibility(View.VISIBLE);
                    connectLoading.setVisibility(View.GONE);
                    break;
                case STATUS_CONNECTING:
                    connectLoading.setVisibility(View.VISIBLE);
                    break;
                case STATUS_ONLINE:
                    connectLayout.setVisibility(View.GONE);
                    connectLoading.setVisibility(View.GONE);
                    break;
            }
        });
    }

    //Messages
    private void parseStringMessage(String message) {
        try {
            JSONObject messageJSON = new JSONObject(message);
            String action = messageJSON.getString("action");
            switch (action) {
                //Send albums files lists to server
                case "requestAlbums": {
                    ArrayList<String> files = new ArrayList<>();
                    for (int i = Library.albums.size() - 1; i >= 0; i--) {
                        //Get album
                        Album album = Library.albums.get(i);

                        //Create files list
                        files.clear();
                        for (TurboImage image: album.files) {
                            files.add(image.file.getName());
                        }

                        //Create & send JSON with album file names
                        try {
                            JSONObject obj = new JSONObject();
                            obj.put("action", "album");
                            obj.put("index", i);
                            obj.put("files", new JSONArray(files));
                            webSocketClient.send(obj.toString());
                        } catch (JSONException e) {
                            System.out.println("Error creating album JSON");
                        }
                    }
                    break;
                }
            }
        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }
    }

    //Users
    private void loadUsers() {
        //Var to check if save needed
        boolean needsSave = false;

        //Split users
        ArrayList<String> userStrings = Storage.getStringList("Backup.users");
        for (String userString: userStrings) {
            //Check if valid
            int separatorIndex = userString.indexOf("\n");
            if (separatorIndex == -1) {
                needsSave = true;
                continue;
            }

            //Add user
            users.add(new User(userString.substring(0, separatorIndex), userString.substring(separatorIndex + 1)));
        }

        //Save users after change
        if (needsSave) saveUsers();
    }

    private void saveUsers() {
        //Create string users
        ArrayList<String> userStrings = new ArrayList<>();
        for (User user: users) userStrings.add(user.toString());

        //Save list
        Storage.putStringList("Backup.users", userStrings);
    }
}
