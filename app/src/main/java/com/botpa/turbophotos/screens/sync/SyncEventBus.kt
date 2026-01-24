package com.botpa.turbophotos.screens.sync

import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ConcurrentLinkedQueue

class SyncEventBus {

    @JvmField val eventQueue: ConcurrentLinkedQueue<SyncEvent> = ConcurrentLinkedQueue<SyncEvent>()
    @JvmField val trigger: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    fun postEvent(command: String) {
        eventQueue.add(SyncEvent(command, 0)) //Use int as dummy value
        trigger.postValue(true)
    }

    fun postEvent(command: String, value: String) {
        eventQueue.add(SyncEvent(command, value))
        trigger.postValue(true)
    }

    fun postEvent(command: String, value: Int) {
        eventQueue.add(SyncEvent(command, value))
        trigger.postValue(true)
    }

    companion object {

        @JvmStatic
        @get:Synchronized
        var instance: SyncEventBus = SyncEventBus()

    }
}