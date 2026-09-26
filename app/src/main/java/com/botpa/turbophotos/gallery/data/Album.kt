package com.botpa.turbophotos.gallery.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.botpa.turbophotos.util.Orion
import java.io.File
import androidx.core.database.sqlite.transaction
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Album(val name: String, val albumFolder: File? = null, private var link: Link? = null) {

    //Album info
    val items: List<Item> field: MutableList<Item> = ArrayList()
    val isSpecial: Boolean = albumFolder == null

    val size: Int get() = items.size
    val lastModified: Long get() = if (items.isEmpty()) 0 else items[0].lastModified

    //Album metadata
    private val metadataModifiedKeys: HashSet<String> = HashSet()

    var isMetadataLoaded: Boolean = false
        private set

    val metadata: Map<String, ItemMetadataInfo> field: MutableMap<String, ItemMetadataInfo> = HashMap()

    //Files
    val albumPath: String = albumFolder?.absolutePath ?: ""
    val metadataFile: File? get() = link?.metadataFile
    val metadataPath: String get() = link?.metadataPath ?: ""


    //Items
    fun sort(sortMethod: SortMethod, sortDirection: SortDirection) {
        items.sortWith<Item>(Item.getComparator(sortMethod, sortDirection))
    }

    fun clear() {
        items.clear()
    }

    fun clearAndCopy(newItems: List<Item>) {
        clear()
        items.addAll(newItems)
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

    fun addSorted(item: Item, sortMethod: SortMethod, sortDirection: SortDirection): Int {
        val searchIndex = items.binarySearch(item, Item.getComparator(sortMethod, sortDirection))
        val index = if (searchIndex < 0) -searchIndex - 1 else searchIndex
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
        //Clear metadata
        metadata.clear()

        //Mark as not loaded
        isMetadataLoaded = false
    }

    fun canLoadMetadata(): Boolean {
        val metadataFile = metadataFile
        return metadataFile != null && metadataFile.exists()
    }

    fun loadMetadata() {
        //Load metadata
        metadata.clear()
        try {
            SQLiteDatabase.openDatabase(metadataPath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT name, caption, labels, text, embedding FROM items", null).use { cursor ->
                    val nameIdx = cursor.getColumnIndexOrThrow("name")
                    val captionIdx = cursor.getColumnIndexOrThrow("caption")
                    val labelsIdx = cursor.getColumnIndexOrThrow("labels")
                    val textIdx = cursor.getColumnIndexOrThrow("text")
                    val embeddingIdx = cursor.getColumnIndexOrThrow("embedding")

                    while (cursor.moveToNext()) {
                        //Get info
                        val name = cursor.getString(nameIdx) ?: continue
                        val caption = if (cursor.isNull(captionIdx)) {
                            null
                        } else {
                            cursor.getString(captionIdx)
                        }
                        val labels = if (cursor.isNull(labelsIdx)) {
                            null
                        } else {
                            Orion.loadStringList(cursor.getString(labelsIdx))
                        }
                        val text = if (cursor.isNull(textIdx)) {
                            null
                        } else {
                            Orion.loadStringList(cursor.getString(textIdx))
                        }
                        val embedding = if (cursor.isNull(embeddingIdx)) {
                            null
                        } else {
                            bytesToFloatArray(cursor.getBlob(embeddingIdx))
                        }

                        //Create metadata info
                        val metadataInfo = ItemMetadataInfo(caption, labels, text, embedding)
                        metadata[name] = metadataInfo
                    }
                }
            }
        } catch (_: Exception) {
            metadata.clear()
        }

        //Mark as loaded
        isMetadataLoaded = true
    }

    fun saveMetadata(): Boolean {
        //No modified keys
        if (metadataModifiedKeys.isEmpty()) return true

        //Update database
        return try {
            //Update modified keys
            SQLiteDatabase.openDatabase(metadataPath, null, SQLiteDatabase.OPEN_READWRITE). use { db ->
                db.transaction {
                    for (key in metadataModifiedKeys) {
                        //Get metadata
                        val info = getMetadataKey(key)

                        //Check action
                        if (info == null) {
                            //Key was removed
                            delete("items", "name = ?", arrayOf(key))
                        } else {
                            //Prepare values
                            val labels = if (info.labels == null) null else Orion.objectMapper.writeValueAsString(info.labels!!)
                            val text = if (info.text == null) null else Orion.objectMapper.writeValueAsString(info.text!!)
                            val embedding = floatArrayToByteArray(info.embedding)

                            //Key was modified
                            val values = ContentValues().apply {
                                put("name", key)
                                put("caption", info.caption)
                                put("labels", labels)
                                put("text", text)
                                put("embedding", embedding)
                            }
                            insertWithOnConflict("items", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                        }
                    }
                }
                db.close()
            }

            //Clear modified keys
            metadataModifiedKeys.clear()
            true
        } catch (_: Exception) {
            false
        }
    }

    //Metadata actions
    fun hasMetadataKey(key: String): Boolean {
        return metadata.containsKey(key)
    }

    fun getMetadataKey(key: String): ItemMetadataInfo? {
        return metadata.getOrDefault(key, null)
    }

    fun removeMetadataKey(key: String) {
        //Remove key
        metadata.remove(key)

        //Mark key as modified
        metadataModifiedKeys.add(key)
    }

    fun setMetadataKey(key: String, info: ItemMetadataInfo?) {
        //Modify key
        if (info != null) {
            metadata[key] = info
        } else {
            metadata.remove(key)
        }

        //Mark key as modified
        metadataModifiedKeys.add(key)
    }

    //Metadata util
    private fun bytesToFloatArray(bytes: ByteArray?): FloatArray? {
        //Not valid
        if (bytes == null) return null

        //Convert
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floatArray = FloatArray(bytes.size / 4)
        buffer.asFloatBuffer().get(floatArray)
        return floatArray
    }

    private fun floatArrayToByteArray(floats: FloatArray?): ByteArray? {
        //Not valid
        if (floats == null) return null

        //Convert
        val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (f in floats) {
            buffer.putFloat(f)
        }
        return buffer.array()
    }

}