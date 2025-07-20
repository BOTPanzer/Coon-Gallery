package com.botpa.turbophotos.util

import java.io.File

class Link(imagesPath: String, metadataPath: String) {

    @JvmField var imagesFolder: File = File(imagesPath)
    @JvmField var metadataFile: File = File(metadataPath)
    @JvmField var album: Album? = null

    //Getters
    val imagesPath: String get() = imagesFolder.absolutePath
    val metadataPath: String get() = metadataFile.absolutePath


    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$imagesPath\n$metadataPath"
    }

    //Static
    companion object {

        fun parse(string: String): Link {
            //Split string into parts
            val parts = string.split("\n")

            //Create link with parts
            return Link(
                parts[0],
                if (parts.size >= 2) parts[1] else "",
            )
        }

    }

}