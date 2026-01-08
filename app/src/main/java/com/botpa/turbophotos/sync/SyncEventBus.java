package com.botpa.turbophotos.sync;

import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SyncEventBus {

    private static SyncEventBus instance;

    private final ConcurrentLinkedQueue<SyncEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final MutableLiveData<Boolean> trigger = new MutableLiveData<>();

    public static synchronized SyncEventBus getInstance() {
        if (instance == null) instance = new SyncEventBus();
        return instance;
    }

    public ConcurrentLinkedQueue<SyncEvent> getEventQueue() {
        return eventQueue;
    }

    public MutableLiveData<Boolean> getTrigger() {
        return trigger;
    }

    public void postEvent(String command, String value) {
        eventQueue.add(new SyncEvent(command, value));
        trigger.postValue(true);
    }

    public void postEvent(String command, int value) {
        eventQueue.add(new SyncEvent(command, value));
        trigger.postValue(true);
    }

}