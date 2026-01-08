package com.botpa.turbophotos.backup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BackupEventBus {

    private static BackupEventBus instance;

    private final ConcurrentLinkedQueue<BackupEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final MutableLiveData<Boolean> trigger = new MutableLiveData<>();

    public static synchronized BackupEventBus getInstance() {
        if (instance == null) instance = new BackupEventBus();
        return instance;
    }

    public ConcurrentLinkedQueue<BackupEvent> getEventQueue() {
        return eventQueue;
    }

    public MutableLiveData<Boolean> getTrigger() {
        return trigger;
    }

    public void postEvent(String command, String value) {
        eventQueue.add(new BackupEvent(command, value));
        trigger.postValue(true);
    }

    public void postEvent(String command, int value) {
        eventQueue.add(new BackupEvent(command, value));
        trigger.postValue(true);
    }

}