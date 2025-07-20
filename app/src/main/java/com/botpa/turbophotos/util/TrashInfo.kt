package com.botpa.turbophotos.util

import java.io.File

class TrashInfo(private val originalPath: String, private val trashPath: String, val isVideo: Boolean) {

    //Files
    @JvmField var originalFile: File = File(originalPath)
    @JvmField var trashFile: File = File(trashPath)


    //Override toString to be able to save trash info in a string
    override fun toString(): String {
        return "$originalPath\n$trashPath\n$isVideo"
    }

    //Static
    companion object {

        fun parse(string: String): TrashInfo {
            //Split string into parts
            val parts = string.split("\n")

            //Create trash info with parts
            return TrashInfo(
                parts[0],
                if (parts.size >= 2) parts[1] else "",
                if (parts.size >= 3) parts[2].lowercase() == "true" else false
            )
        }

    }

}