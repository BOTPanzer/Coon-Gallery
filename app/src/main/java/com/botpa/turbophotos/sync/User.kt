package com.botpa.turbophotos.sync

class User(@JvmField var name: String, @JvmField var address: String) {

    //Override toString to be able to save users in a string
    override fun toString(): String {
        return "$name\n$address"
    }

}