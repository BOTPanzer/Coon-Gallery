package com.botpa.turbophotos.sync;

public class SyncEvent {

    public final String command;
    public final String stringValue;
    public final int intValue;

    public SyncEvent(String command, String value) {
        this.command = command;
        this.stringValue = value;
        this.intValue = 0;
    }

    public SyncEvent(String command, int value) {
        this.command = command;
        this.stringValue = null;
        this.intValue = value;
    }

}