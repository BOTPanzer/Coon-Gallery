package com.botpa.turbophotos.gallery

import com.botpa.turbophotos.util.Orion
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

class Album(val name: String, val imagesFolder: File?, var metadataFile: File?) {

    constructor(name: String) : this(name, null, null)

    //Album info
    @JvmField val items: MutableList<CoonItem> = ArrayList()
    @JvmField var metadata: ObjectNode? = null

    val isEspecial: Boolean = imagesFolder == null

    val imagesPath: String = imagesFolder?.absolutePath ?: ""
    val metadataPath: String = metadataFile?.absolutePath ?: ""


    //Util
    fun exists(): Boolean {
        return imagesFolder != null && metadataFile != null && imagesFolder.exists() && metadataFile!!.exists()
    }

    //Files
    fun sort() {
        items.sortByDescending { it }
    }

    fun reset() {
        items.clear()
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

    fun get(index: Int): CoonItem {
        return items[index]
    }

    fun add(item: CoonItem) {
        items.add(item)
    }

    fun addSorted(item: CoonItem): Int {
        val searchResult = items.binarySearch(item, reverseOrder())
        val index = if (searchResult < 0) -searchResult - 1 else searchResult
        items.add(index, item)
        return index
    }

    fun remove(index: Int): CoonItem {
        return items.removeAt(index)
    }

    fun indexOf(item: CoonItem): Int {
        return items.indexOf(item)
    }

    //Load & save metadata
    fun updateMetadataFile(newFile: File?) {
        metadataFile = newFile;
        metadata = null;
    }

    fun loadMetadata() {
        metadata = if (metadataFile != null)
            Orion.loadJson(metadataFile!!)
        else
            Orion.emptyJson
    }

    fun saveMetadata(): Boolean {
        return if (hasMetadata() && metadataFile != null)
            Orion.writeJson(metadataFile!!, metadata!!)
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

    fun setMetadataKey(key: String, node: ObjectNode?) {
        if (hasMetadata())
            metadata!!.replace(key, node)
    }

    //Override toString to be able to save albums in a string
    override fun toString(): String {
        return "$imagesPath\n$metadataPath"
    }

}