package com.botpa.turbophotos.backup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class BackupEventBus {

    private static BackupEventBus instance;
    private final MutableLiveData<BackupEvent> event = new MutableLiveData<>();

    public static synchronized BackupEventBus getInstance() {
        if (instance == null) instance = new BackupEventBus();
        return instance;
    }

    public LiveData<BackupEvent> getEvent() {
        return event;
    }

    public void postEvent(String command, String value) {
        event.postValue(new BackupEvent(command, value));
    }

    public void postEvent(String command, int value) {
        event.postValue(new BackupEvent(command, value));
    }

}