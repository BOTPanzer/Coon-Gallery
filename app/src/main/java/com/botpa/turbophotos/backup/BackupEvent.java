package com.botpa.turbophotos.backup;

public class BackupEvent {

    public final String command;
    public final String stringValue;
    public final int intValue;

    public BackupEvent(String command, String value) {
        this.command = command;
        this.stringValue = value;
        this.intValue = 0;
    }

    public BackupEvent(String command, int value) {
        this.command = command;
        this.stringValue = null;
        this.intValue = value;
    }

}