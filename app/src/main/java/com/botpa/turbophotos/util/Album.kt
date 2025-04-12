package com.botpa.turbophotos.util

import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

class Album(imagesPath: String, metadataPath: String) {

    @JvmField var imagesFolder: File = File(imagesPath)
    @JvmField var metadataFile: File = File(metadataPath)
    @JvmField var metadata: ObjectNode? = null
    @JvmField var files: ArrayList<TurboImage> = ArrayList()

    //Getters
    val absoluteImagesPath: String get() = imagesFolder.absolutePath
    val absoluteMetadataPath: String get() = metadataFile.absolutePath

    //Load & save metadata
    fun loadMetadata() {
        val metadataFile = File(absoluteMetadataPath)
        metadata = Orion.loadJson(metadataFile)
    }

    fun saveMetadata(): Boolean {
        return Orion.writeJson(metadataFile, metadata)
    }

    //Metadata actions
    fun hasMetadata(): Boolean {
        return metadata != null
    }

    fun hasMetadataKey(key: String): Boolean {
        return if (metadata != null)
            metadata!!.has(key)
        else
            false
    }

    fun getMetadataKey(key: String): ObjectNode? {
        return metadata!!.get(key) as ObjectNode?
    }

    fun removeMetadataKey(key: String) {
        metadata!!.remove(key);
    }

    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$absoluteImagesPath\n$absoluteMetadataPath"
    }
}
