package com.botpa.turbophotos.util

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner

class BackManager(owner: LifecycleOwner, dispatcher: OnBackPressedDispatcher) {

    //Events lists
    private val onBackPressedFunctions: MutableMap<String, BackAnimationEvent> = HashMap()
    private val onBackPressedOrder: MutableList<String> = ArrayList()

    //Events listener
    private val onBackPressed: OnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            getCurrentEvent()?.onStarted(backEvent)
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            getCurrentEvent()?.onProgress(backEvent)
        }

        override fun handleOnBackPressed() {
            getCurrentEvent()?.onInvoked()
        }

        override fun handleOnBackCancelled() {
            getCurrentEvent()?.onCancelled()
        }
    }


    //Constructor
    init {
        dispatcher.addCallback(owner, onBackPressed)
    }

    //Get events
    private fun getCurrentEvent(): BackAnimationEvent? {
        //No listeners
        if (onBackPressedOrder.isEmpty()) return null

        //Return listener
        val name = onBackPressedOrder.last()
        return onBackPressedFunctions[name]
    }

    //Register/unregister events
    fun register(name: String, listener: BackAnimationEvent) {
        //Register event
        onBackPressedFunctions[name] = listener
        onBackPressedOrder.remove(name)
        onBackPressedOrder.add(name)

        //Toggle back button
        onBackPressed.isEnabled = true
    }

    fun register(name: String, runnable: () -> Unit) {
        //Register simple event with runnable & no animation
        register(name, object : BackAnimationEvent {
            override fun onInvoked() {
                runnable.invoke()
            }
        })
    }

    fun unregister(name: String) {
        //Remove from dictionary & list
        onBackPressedFunctions.remove(name)
        onBackPressedOrder.remove(name)

        //Toggle back button
        onBackPressed.isEnabled = onBackPressedOrder.isNotEmpty()
    }

}

interface BackAnimationEvent {
    fun onStarted(backEvent: BackEventCompat) {}
    fun onProgress(backEvent: BackEventCompat) {}
    fun onInvoked()
    fun onCancelled() {}
}
