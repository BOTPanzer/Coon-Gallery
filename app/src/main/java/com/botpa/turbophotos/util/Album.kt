package com.botpa.turbophotos.util

import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import java.util.Collections

class Album(val name: String, val imagesFolder: File?, val metadataFile: File?) {

    constructor(name: String) : this(name, null, null)

    //Album info
    @JvmField var metadata: ObjectNode? = null
    @JvmField val items: ArrayList<TurboItem> = ArrayList()
    var lastModified: Long = if (imagesFolder == null) 0 else imagesFolder.lastModified()
    val imagesPath: String = if (imagesFolder == null) "" else imagesFolder.absolutePath
    val metadataPath: String = if (metadataFile == null) "" else metadataFile.absolutePath


    //Util
    fun exists(): Boolean {
        return imagesFolder != null && metadataFile != null && imagesFolder.exists() && metadataFile.exists()
    }

    //Files
    fun sort() {
        items.sortByDescending { it }
    }

    fun reset() {
        items.clear()
        lastModified = imagesFolder?.lastModified() ?: 0
    }

    fun size(): Int {
        return items.size
    }

    fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return items.isNotEmpty()
    }

    fun get(index: Int): TurboItem {
        return items[index]
    }

    fun add(item: TurboItem) {
        items.add(item)
    }

    fun addSorted(item: TurboItem) {
        val index = Collections.binarySearch(items, item)
        val insertionPoint = if (index < 0) -(index + 1) else index
        items.add(insertionPoint, item)
    }

    fun remove(index: Int): TurboItem {
        return items.removeAt(index)
    }

    fun indexOf(item: TurboItem): Int {
        return items.indexOf(item)
    }

    //Load & save metadata
    fun loadMetadata() {
        metadata = Orion.loadJson(metadataFile)
    }

    fun saveMetadata(): Boolean {
        return if (hasMetadata())
            Orion.writeJson(metadataFile, metadata)
        else
            false
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