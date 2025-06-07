package com.botpa.turbophotos.util

import java.io.File

class Link(imagesPath: String, metadataPath: String) {

    @JvmField var imagesFolder: File = File(imagesPath)
    @JvmField var metadataFile: File = File(metadataPath)
    @JvmField var album: Album? = null

    //Getters
    val absoluteImagesPath: String get() = imagesFolder.absolutePath
    val absoluteMetadataPath: String get() = metadataFile.absolutePath


    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$absoluteImagesPath\n$absoluteMetadataPath"
    }

}