package com.botpa.turbophotos.backup

class User(@JvmField var name: String, @JvmField var URL: String) {

    //Override toString to be able to save users in a string
    override fun toString(): String {
        return "$name\n$URL"
    }

}