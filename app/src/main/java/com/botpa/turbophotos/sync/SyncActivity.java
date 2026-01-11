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
import com.botpa.turbophotos.sync.logs.LogAdapter;
import com.botpa.turbophotos.sync.users.User;
import com.botpa.turbophotos.sync.users.UserAdapter;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public class SyncActivity extends AppCompatActivity {

    //Connection
    private static final int STATUS_OFFLINE = 0;
    private static final int STATUS_CONNECTING = 1;
    private static final int STATUS_ONLINE = 2;

    private static final String CODE_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private int connectionStatus = STATUS_OFFLINE;

    //Users
    private final List<User> users = new ArrayList<>();
    private UserAdapter usersAdapter;

    //Logs
    private static final int logsMax = 200;
    private final List<String> logs = new ArrayList<>();
    private LogAdapter logsAdapter;

    //Views (connect)
    private View usersLayout;
    private EditText usersName;
    private EditText usersCode;
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
        usersCode = findViewById(R.id.usersCode);
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
        usersCode.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                Orion.clearFocus(SyncActivity.this);
                Orion.hideKeyboard(SyncActivity.this);
                usersConnect.performClick();
            }
            return false;
        });

        usersConnect.setOnClickListener(view -> {
            //Already trying to connect
            if (connectionStatus != STATUS_OFFLINE) return;

            //Get code
            String code = usersCode.getText().toString();
            if (code.isEmpty()) return;

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
                    user.code = code;
                    usersAdapter.notifyItemChanged(i);
                    saveUsers();
                    isSaved = true;
                    break;
                }

                //Add new user
                if (!isSaved) {
                    users.add(0, new User(name, code));
                    usersAdapter.notifyItemInserted(0);
                    saveUsers();
                }
            }

            //Connect
            connect(code);
        });

        //Logs
        logsExit.setOnClickListener(view -> finish());
    }

    //Users
    private void initUsersList() {
        //Create adapter
        usersAdapter = new UserAdapter(SyncActivity.this, users);

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
            connect(user.code);
        });

        //Add select user listener
        usersAdapter.setOnLongClickListener((view, index) -> {
            //Get user
            User user = users.get(index);

            //Select user info
            usersName.setText(user.name);
            usersCode.setText(user.code);

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
        List<String> userStrings = Storage.getStringList("Sync.users");
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
        List<String> userStrings = new ArrayList<>();
        for (User user: users) userStrings.add(user.toString());

        //Save list
        Storage.putStringList("Sync.users", userStrings);
    }

    //Connection
    private static long decodeBase36(String code) {
        long result = 0;
        for (int i = 0; i < code.length(); i++) {
            result = result * 36 + CODE_CHARSET.indexOf(code.charAt(i));
        }
        return result;
    }

    private String convertCodeToAddress(String code) {
        long combined = decodeBase36(code.toUpperCase());

        int port = (int) (combined & 0xFFFF);
        long ipNum = combined >> 16;

        String ip = String.format("%d.%d.%d.%d", (ipNum >> 24) & 0xFF, (ipNum >> 16) & 0xFF, (ipNum >> 8) & 0xFF, ipNum & 0xFF);

        return ip + ":" + port;
    }

    private void connect(String code) {
        //Hide keyboard
        Orion.hideKeyboard(SyncActivity.this);
        Orion.clearFocus(SyncActivity.this);

        //Connect
        if (code.contains(":")) {
            //Code is already an IP:PORT address
            send("connect", code);
        } else {
            //Code needs conversion to address
            send("connect", convertCodeToAddress(code));
        }
    }

    //Logs
    private void initLogsList() {
        //Create adapter
        logsAdapter = new LogAdapter(SyncActivity.this, logs);

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
        //Get instance
        SyncEventBus instance = SyncEventBus.getInstance();

        //Observe
        instance.trigger.observe(this, t -> {
            SyncEvent event;
            while ((event = instance.eventQueue.poll()) != null) handleEvent(event);
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
                connectionStatus = event.valueInt;
                switch (connectionStatus) {
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
                Orion.snack(SyncActivity.this, event.valueString);
                break;

            //Log
            case "log":
                log(event.valueString);
                break;
        }
    }

    private void send(String command) {
        Intent intent = new Intent(this, SyncService.class);
        intent.putExtra("command", command);
        startService(intent);
    }

    private void send(String command, String value) {
        Intent intent = new Intent(this, SyncService.class);
        intent.putExtra("command", command);
        intent.putExtra("value", value);
        startService(intent);
    }

}
