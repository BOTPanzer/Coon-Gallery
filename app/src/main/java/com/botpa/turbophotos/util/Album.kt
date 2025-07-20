package com.botpa.turbophotos.util

import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

class Album(val name: String, val imagesFolder: File?, val metadataFile: File?) {

    constructor(name: String) : this(name, null, null)

    //Album info
    @JvmField var metadata: ObjectNode? = null
    @JvmField val files: ArrayList<TurboFile> = ArrayList()
    var lastModified: Long = if (imagesFolder == null) 0 else imagesFolder.lastModified()
    val imagesPath: String = if (imagesFolder == null) "" else imagesFolder.absolutePath
    val metadataPath: String = if (metadataFile == null) "" else metadataFile.absolutePath


    //Util
    fun exists(): Boolean {
        return imagesFolder != null && metadataFile != null && imagesFolder.exists() && metadataFile.exists()
    }

    //Files
    fun sort() {
        files.sortByDescending { lastModified }
    }

    fun reset() {
        files.clear()
        lastModified = if (imagesFolder != null) imagesFolder.lastModified() else 0
    }

    fun size(): Int {
        return files.size
    }

    fun isEmpty(): Boolean {
        return files.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return files.isNotEmpty()
    }

    fun get(index: Int): TurboFile {
        return files[index]
    }

    fun add(file: TurboFile) {
        files.add(file)
    }

    fun remove(index: Int): TurboFile {
        return files.removeAt(index)
    }

    fun indexOf(file: TurboFile): Int {
        return files.indexOf(file)
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
        return "$imagesPath\n$metadataPath"
    }

}