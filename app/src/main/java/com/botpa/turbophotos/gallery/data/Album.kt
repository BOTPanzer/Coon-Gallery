package com.botpa.turbophotos.gallery.data

import com.botpa.turbophotos.util.Orion
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

class Album(val name: String, val albumFolder: File? = null, private var link: Link? = null) {

    //Album info
    val items: List<Item> field: MutableList<Item> = ArrayList()
    val isSpecial: Boolean = albumFolder == null
    var metadata: ObjectNode? = null

    //Files info
    val albumPath: String = albumFolder?.absolutePath ?: ""
    val metadataFile: File? get() = link?.metadataFile
    val metadataPath: String get() = link?.metadataPath ?: ""
    val vectorsFile: File? get() = link?.vectorsFile
    val vectorsPath: String get() = link?.vectorsPath ?: ""


    //Items
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

    fun get(index: Int): Item {
        return items[index]
    }

    fun add(item: Item) {
        items.add(item)
    }

    fun addSorted(item: Item): Int {
        val searchResult = items.binarySearch(item, reverseOrder())
        val index = if (searchResult < 0) -searchResult - 1 else searchResult
        items.add(index, item)
        return index
    }

    fun removeAt(index: Int): Item {
        return items.removeAt(index)
    }

    fun indexOf(item: Item): Int {
        return items.indexOf(item)
    }

    //Link management
    fun setLink(newLink: Link?) {
        //Check if link changes
        if (newLink == link) return

        //Update link
        link = newLink
        onMetadataFileChanged()
    }

    //Metadata management
    fun onMetadataFileChanged() {
        metadata = null
    }

    fun canLoadMetadata(): Boolean {
        return metadataFile != null && metadataFile!!.exists()
    }

    fun loadMetadata() {
        metadata = if (metadataFile != null) {
            Orion.loadJson(metadataFile!!)
        } else {
            Orion.emptyJson
        }
    }

    fun saveMetadata(): Boolean {
        return if (hasMetadata() && metadataFile != null) {
            Orion.writeJson(metadataFile!!, metadata!!)
        } else {
            false
        }
    }

    //Metadata actions
    fun hasMetadata(): Boolean {
        return metadata != null
    }

    fun hasMetadataKey(key: String): Boolean {
        return if (hasMetadata()) {
            metadata!!.has(key)
        } else {
            false
        }
    }

    fun getMetadataKey(key: String): ObjectNode? {
        return if (hasMetadata()) {
            metadata!!.get(key) as ObjectNode?
        } else {
            null
        }
    }

    fun removeMetadataKey(key: String) {
        if (hasMetadata()) {
            metadata!!.remove(key)
        }
    }

    fun setMetadataKey(key: String, node: ObjectNode?) {
        if (hasMetadata()) {
            metadata!!.replace(key, node)
        }
    }

}