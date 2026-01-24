package com.botpa.turbophotos.screens.sync

class User(@JvmField var name: String, @JvmField var code: String) {

    //Override toString to be able to save users in a string
    override fun toString(): String {
        return "$name\n$code"
    }

}