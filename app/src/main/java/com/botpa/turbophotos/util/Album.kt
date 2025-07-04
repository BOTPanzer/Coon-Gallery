package com.botpa.turbophotos.util

import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

class Album(var imagesFolder: File, var metadataFile: File, val id: String, val name: String, var lastModified: Long) {

    @JvmField var metadata: ObjectNode? = null
    @JvmField var files: ArrayList<TurboImage> = ArrayList()

    val absoluteImagesPath: String get() = imagesFolder.absolutePath
    val absoluteMetadataPath: String get() = metadataFile.absolutePath


    //Util
    fun exists(): Boolean {
        return imagesFolder.exists() && metadataFile.exists()
    }

    fun clearFiles() {
        //Remove album files from all files & clear album
        for (image in files) Library.allFiles.remove(image)
        files.clear()
    }

    //Load & save metadata
    fun loadMetadata() {
        metadata = Orion.loadJson(metadataFile)
    }

    fun saveMetadata(): Boolean {
        if (hasMetadata())
            return Orion.writeJson(metadataFile, metadata)
        else
            return false
    }

    //Metadata actions
    fun hasMetadata(): Boolean {
        return metadata != null
    }

    fun hasMetadataKey(key: String): Boolean {
        return if (hasMetadata())
            metadata!!.has(key)
        else
            false
    }

    fun getMetadataKey(key: String): ObjectNode? {
        return if (hasMetadata())
            metadata!!.get(key) as ObjectNode?
        else
            null
    }

    fun removeMetadataKey(key: String) {
        if (hasMetadata())
            metadata!!.remove(key)
    }

    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$absoluteImagesPath\n$absoluteMetadataPath"
    }

}