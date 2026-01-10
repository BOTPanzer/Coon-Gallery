package com.botpa.turbophotos.sync

class SyncEvent {

    @JvmField val command: String
    @JvmField val valueString: String
    @JvmField val valueInt: Int

    constructor(command: String, value: String) {
        this.command = command
        this.valueString = value
        this.valueInt = 0
    }

    constructor(command: String, value: Int) {
        this.command = command
        this.valueString = ""
        this.valueInt = value
    }

}