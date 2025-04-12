package com.botpa.turbophotos.backup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.settings.SettingsActivity;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;

import java.util.ArrayList;
import java.util.List;

public class BackupActivity extends AppCompatActivity {

    private final int STATUS_OFFLINE = 0;
    private final int STATUS_CONNECTING = 1;
    private final int STATUS_ONLINE = 2;

    //Connection
    private int connectStatus = STATUS_OFFLINE;

    //Users
    private final List<User> users = new ArrayList<>();
    private UserAdapter usersAdapter;

    //Connect
    private BroadcastReceiver broadcastReceiver;

    private View usersLayout;
    private EditText usersName;
    private EditText usersURL;
    private View usersConnect;
    private View usersLoading;
    private RecyclerView usersList;

    //Logs
    private static final int logsMax = 200;
    private final List<String> logs = new ArrayList<>();
    private LogAdapter logsAdapter;

    private RecyclerView logsList;


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
        usersAdapter = new UserAdapter(BackupActivity.this, users);
        usersAdapter.setOnClickListener((view, index) -> connect(users.get(index).URL));
        usersAdapter.setOnLongClickListener((view, index) -> {
            User user = users.get(index);
            usersName.setText(user.name);
            usersURL.setText(user.URL);
            Orion.hideKeyboard(BackupActivity.this);
            Orion.clearFocus(BackupActivity.this);
        });
        usersAdapter.setOnDeleteListener((view, index) -> {
            users.remove(index);
            usersAdapter.notifyItemRemoved(index);
            saveUsers();
        });
        usersList.setAdapter(usersAdapter);
        usersList.setLayoutManager(new LinearLayoutManager(BackupActivity.this));


        //Init logs list
        logsAdapter = new LogAdapter(BackupActivity.this, logs);
        logsList.setAdapter(logsAdapter);
        logsList.setLayoutManager(new LinearLayoutManager(BackupActivity.this, LinearLayoutManager.VERTICAL, true));
        logsList.setItemAnimator(null);


        //Register receiver
        registerReceiver();

        //Start service
        Intent intent = new Intent(this, BackupService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        //Close service
        send("stop");

        //Unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        super.onDestroy();
    }

    //App
    private void getViews() {
        usersLayout = findViewById(R.id.usersLayout);
        usersName = findViewById(R.id.usersName);
        usersURL = findViewById(R.id.usersURL);
        usersConnect = findViewById(R.id.usersConnect);
        usersLoading = findViewById(R.id.usersLoading);
        usersList = findViewById(R.id.usersList);
        logsList = findViewById(R.id.logsList);
    }

    private void addListeners() {
        //Connect
        usersURL.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                Orion.clearFocus(BackupActivity.this);
                Orion.hideKeyboard(BackupActivity.this);
                usersConnect.performClick();
            }
            return false;
        });

        usersConnect.setOnClickListener(view -> {
            //Already trying to connect
            if (connectStatus != STATUS_OFFLINE) return;

            //Get URL
            String URL = usersURL.getText().toString();
            if (URL.isEmpty()) return;

            //Get name
            String name = usersName.getText().toString();
            if (!name.isEmpty()) {
                //Check if name is saved
                boolean isSaved = false;
                for (int i = 0; i < users.size(); i++) {
                    //Get user
                    User user = users.get(i);
                    if (!user.name.equals(name)) continue;

                    //Update user
                    user.URL = URL;
                    usersAdapter.notifyItemChanged(i);
                    saveUsers();
                    isSaved = true;
                    break;
                }

                //Add new user
                if (!isSaved) {
                    users.add(0, new User(name, URL));
                    usersAdapter.notifyItemInserted(0);
                    saveUsers();
                }
            }

            //Connect
            connect(URL);
        });

        //Logs
        findViewById(R.id.logsExit).setOnClickListener(view -> finish());
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

    //Connection
    private void connect(String URL) {
        //Hide keyboard
        Orion.hideKeyboard(BackupActivity.this);
        Orion.clearFocus(BackupActivity.this);

        //Connect
        send("connect", URL);
    }

    //Logs
    private void log(String log) {
        //Reached maximum size -> Remove first
        if (logs.size() >= logsMax) {
            logs.remove(logs.size() - 1);
            logsAdapter.notifyItemRemoved(logs.size() - 1);
        }

        //Add new log
        logs.add(0, log);
        logsAdapter.notifyItemInserted(0);

        //Scroll to bottom
        logsList.scrollToPosition(0);
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
                    //Service started
                    case "init":
                        log("Service started");
                        break;

                    //Status changed
                    case "status":
                        connectStatus = intent.getIntExtra(command, STATUS_OFFLINE);
                        switch (connectStatus) {
                            case STATUS_OFFLINE:
                                log("Disconnected");
                                usersLayout.setVisibility(View.VISIBLE);
                                usersLoading.setVisibility(View.GONE);
                                break;
                            case STATUS_CONNECTING:
                                log("Connecting...");
                                usersLoading.setVisibility(View.VISIBLE);
                                break;
                            case STATUS_ONLINE:
                                log("Connected");
                                usersLayout.setVisibility(View.GONE);
                                usersLoading.setVisibility(View.GONE);
                                break;
                        }
                        break;

                    //Snack
                    case "snack":
                        Orion.snack(BackupActivity.this, intent.getStringExtra(command));
                        break;

                    //Log
                    case "log":
                        log(intent.getStringExtra(command));
                        break;
                }
            }
        };

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
