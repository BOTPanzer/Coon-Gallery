package com.botpa.turbophotos.util

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner

class BackManager(owner: LifecycleOwner, dispatcher: OnBackPressedDispatcher) {

    private val onBackPressedFunctions: MutableMap<String, Runnable> = HashMap()
    private val onBackPressedOrder: MutableList<String> = ArrayList()
    private val onBackPressed: OnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            //Get function name
            val last = onBackPressedOrder.size - 1
            val name = onBackPressedOrder[last]

            //Run function
            val runnable = onBackPressedFunctions[name]
            runnable?.run()
        }
    }

    //Constructor
    init {
        dispatcher.addCallback(owner, onBackPressed)
    }

    //Register / unregister functions
    fun register(name: String, runnable: Runnable) {
        //Add to dictionary & list
        onBackPressedFunctions[name] = runnable
        onBackPressedOrder.remove(name)
        onBackPressedOrder.add(name)

        //Toggle back button
        onBackPressed.isEnabled = true
    }

    fun unregister(name: String) {
        //Remove from dictionary & list
        onBackPressedFunctions.remove(name)
        onBackPressedOrder.remove(name)

        //Toggle back button
        onBackPressed.isEnabled = onBackPressedOrder.isNotEmpty()
    }

}
