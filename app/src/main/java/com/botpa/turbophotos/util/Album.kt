package com.botpa.turbophotos.util

import com.google.gson.JsonObject
import java.io.File

class Album(imagesPath: String, metadataPath: String) {

    @JvmField var imagesFolder: File = File(imagesPath)
    @JvmField var metadataFile: File = File(metadataPath)
    lateinit var metadata: JsonObject
    @JvmField var files: ArrayList<TurboImage> = ArrayList()

    //Getters
    val absoluteImagesPath: String get() = imagesFolder.absolutePath
    val absoluteMetadataPath: String get() = metadataFile.absolutePath

    //Load & save metadata
    fun loadMetadata() {
        val metadataFile = File(absoluteMetadataPath)
        metadata =  if (!metadataFile.exists()) JsonObject() else Orion.loadJSON(metadataFile)
    }

    fun saveMetadata(): Boolean {
        return Orion.writeJSON(metadataFile, metadata)
    }

    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$absoluteImagesPath\n$absoluteMetadataPath"
    }
}
