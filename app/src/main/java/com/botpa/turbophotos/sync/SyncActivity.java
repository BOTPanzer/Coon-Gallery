package com.botpa.turbophotos.sync;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;

import java.util.ArrayList;
import java.util.List;

public class SyncActivity extends AppCompatActivity {

    //Connection
    private final int STATUS_OFFLINE = 0;
    private final int STATUS_CONNECTING = 1;
    private final int STATUS_ONLINE = 2;

    private int connectStatus = STATUS_OFFLINE;

    //Users
    private final List<User> users = new ArrayList<>();
    private SyncUserAdapter usersAdapter;

    //Logs
    private static final int logsMax = 200;
    private final List<String> logs = new ArrayList<>();
    private SyncLogAdapter logsAdapter;

    //Views (connect)
    private View usersLayout;
    private EditText usersName;
    private EditText usersAddress;
    private View usersConnect;
    private View usersLoading;
    private RecyclerView usersList;

    //Views (logs)
    private RecyclerView logsList;
    private View logsExit;


    //State
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sync_screen);

        //Get views & add listeners
        getViews();
        addListeners();

        //Load users
        loadUsers();

        //Init users list
        initUsersList();

        //Init logs list
        initLogsList();

        //Init events observer
        initEventsObserver();

        //Start service
        Intent intent = new Intent(this, SyncService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        //Close service
        send("stop");

        super.onDestroy();
    }

    //App
    private void getViews() {
        //Views (connect)
        usersLayout = findViewById(R.id.usersLayout);
        usersName = findViewById(R.id.usersName);
        usersAddress = findViewById(R.id.usersAddress);
        usersConnect = findViewById(R.id.usersConnect);
        usersLoading = findViewById(R.id.usersLoading);
        usersList = findViewById(R.id.usersList);

        //Views (logs)
        logsList = findViewById(R.id.logsList);
        logsExit = findViewById(R.id.logsExit);

        //Insets
        Orion.addInsetsChangedListener(getWindow().getDecorView(), new int[] { WindowInsetsCompat.Type.systemBars(), WindowInsetsCompat.Type.ime() });
    }

    private void addListeners() {
        //Connect
        usersAddress.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                Orion.clearFocus(SyncActivity.this);
                Orion.hideKeyboard(SyncActivity.this);
                usersConnect.performClick();
            }
            return false;
        });

        usersConnect.setOnClickListener(view -> {
            //Already trying to connect
            if (connectStatus != STATUS_OFFLINE) return;

            //Get address
            String address = usersAddress.getText().toString();
            if (address.isEmpty()) return;

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
                    user.address = address;
                    usersAdapter.notifyItemChanged(i);
                    saveUsers();
                    isSaved = true;
                    break;
                }

                //Add new user
                if (!isSaved) {
                    users.add(0, new User(name, address));
                    usersAdapter.notifyItemInserted(0);
                    saveUsers();
                }
            }

            //Connect
            connect(address);
        });

        //Logs
        logsExit.setOnClickListener(view -> finish());
    }

    //Users
    private void initUsersList() {
        //Create adapter
        usersAdapter = new SyncUserAdapter(SyncActivity.this, users);

        //Add connect listener
        usersAdapter.setOnClickListener((view, index) -> {
            //Get user
            User user = users.get(index);

            //Move user first in list
            users.remove(index);
            usersAdapter.notifyItemRemoved(index);
            users.add(0, user);
            usersAdapter.notifyItemInserted(0);
            saveUsers();

            //Connect to user
            connect(user.address);
        });

        //Add select user listener
        usersAdapter.setOnLongClickListener((view, index) -> {
            //Get user
            User user = users.get(index);

            //Select name & address
            usersName.setText(user.name);
            usersAddress.setText(user.address);

            //Hide keyboard
            Orion.hideKeyboard(SyncActivity.this);
            Orion.clearFocus(SyncActivity.this);
        });

        //Add delete user listener
        usersAdapter.setOnDeleteListener((view, index) -> {
            //Delete user
            users.remove(index);
            usersAdapter.notifyItemRemoved(index);
            saveUsers();
        });

        //Add adapter to list
        usersList.setAdapter(usersAdapter);
        usersList.setLayoutManager(new LinearLayoutManager(SyncActivity.this));
    }

    private void loadUsers() {
        //Var to check if save needed
        boolean needsSave = false;

        //Split users
        ArrayList<String> userStrings = Storage.getStringList("Sync.users");
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
        Storage.putStringList("Sync.users", userStrings);
    }

    //Connection
    private void connect(String address) {
        //Hide keyboard
        Orion.hideKeyboard(SyncActivity.this);
        Orion.clearFocus(SyncActivity.this);

        //Connect
        send("connect", address);
    }

    //Logs
    private void initLogsList() {
        //Create adapter
        logsAdapter = new SyncLogAdapter(SyncActivity.this, logs);

        //Add adapter to list
        logsList.setAdapter(logsAdapter);
        logsList.setLayoutManager(new LinearLayoutManager(SyncActivity.this, LinearLayoutManager.VERTICAL, true));
        logsList.setItemAnimator(null);
    }

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

    //Events
    private void initEventsObserver() {
        SyncEventBus instance = SyncEventBus.getInstance();
        instance.getTrigger().observe(this, t -> {
            SyncEvent e;
            while ((e = instance.getEventQueue().poll()) != null) {
                handleEvent(e);
            }
        });
    }

    private void handleEvent(SyncEvent event) {
        if (event == null) return;
        String command = event.command;

        //Check command
        switch (command) {
            //Service started
            case "init":
                log("Service started");
                break;

            //Status changed
            case "status":
                connectStatus = event.intValue;
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
                Orion.snack(SyncActivity.this, event.stringValue);
                break;

            //Log
            case "log":
                log(event.stringValue);
                break;
        }
    }

    private void send(String name) {
        Intent intent = new Intent(this, SyncService.class);
        intent.putExtra("command", name);
        startService(intent);
    }

    private void send(String name, String value) {
        Intent intent = new Intent(this, SyncService.class);
        intent.putExtra("command", name);
        intent.putExtra("value", value);
        startService(intent);
    }

    private void send(String name, boolean value) {
        Intent intent = new Intent(this, SyncService.class);
        intent.putExtra("command", name);
        intent.putExtra("value", value);
        startService(intent);
    }

    private void send(String name, int value) {
        Intent intent = new Intent(this, SyncService.class);
        intent.putExtra("command", name);
        intent.putExtra("value", value);
        startService(intent);
    }

}
