package com.botpa.turbophotos.util

import org.json.JSONObject
import java.io.File

class Album(imagesPath: String, metadataPath: String) {

    @JvmField var imagesFolder: File = File(imagesPath)
    @JvmField var metadataFile: File = File(metadataPath)
    @JvmField var metadata: JSONObject? = null
    @JvmField var files: ArrayList<TurboImage> = ArrayList()

    //Getters
    val absoluteImagesPath: String get() = imagesFolder.absolutePath
    val absoluteMetadataPath: String get() = metadataFile.absolutePath

    //Load & save metadata
    fun loadMetadata() {
        val metadataFile = File(absoluteMetadataPath)
        metadata =  if (!metadataFile.exists()) JSONObject() else Orion.loadJSON(metadataFile.absolutePath)
    }

    fun saveMetadata(): Boolean {
        return Orion.writeJSON(metadataFile, metadata)
    }

    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$absoluteImagesPath\n$absoluteMetadataPath"
    }
}
