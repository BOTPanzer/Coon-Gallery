package com.botpa.turbophotos.gallery.search

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.Item
import com.botpa.turbophotos.gallery.search.models.ModelManager
import com.botpa.turbophotos.util.Orion
import java.io.File

object SearchHelper {

    //Words
    fun filterAlbumWords(query: String, queryTokens: List<String>, album: Album): MutableList<Item> {
        //Create new list
        val filteredAlbum = ArrayList<Item>()

        //Look for items that match the filter
        for (item in album.items) {
            //Check item name
            if (Orion.normalizeText(item.name).contains(query)) {
                filteredAlbum.add(item)
                continue
            }

            //Get metadata
            val metadata = item.getMetadataInfo() ?: continue

            //Check if query tokens are contained
            if (queryTokens.all { qToken ->
                    Orion.tokenizeText(metadata.caption).contains(qToken) ||
                            metadata.labels.any { Orion.tokenizeText(it).contains(qToken) } ||
                            metadata.text.any { Orion.tokenizeText(it).contains(qToken) }
                }) {
                filteredAlbum.add(item)
            }
        }

        //Return list
        return filteredAlbum
    }

    //Text
    fun filterAlbumText(normalizedQuery: String, album: Album): MutableList<Item> {
        //Create new list
        val filteredAlbum = ArrayList<Item>()

        //Look for items that match the filter
        for (item in album.items) {
            //Check item name
            if (Orion.normalizeText(item.name).contains(normalizedQuery)) {
                filteredAlbum.add(item)
                continue
            }

            //Get metadata
            val metadata = item.getMetadataInfo() ?: continue

            //Check if query is contained
            if (Orion.normalizeText(metadata.caption).contains(normalizedQuery) ||
                metadata.labels.any { Orion.normalizeText(it).contains(normalizedQuery) } ||
                metadata.text.any { Orion.normalizeText(it).contains(normalizedQuery) }
            ) {
                filteredAlbum.add(item)
            }
        }

        //Return list
        return filteredAlbum
    }

    //Natural
    fun filterAlbumNatural(context: Context, query: String, album: Album): MutableList<Item> {
        //Create new list
        val filteredAlbum = ArrayList<Item>()

        //Prepare database
        val embeddingsFile = album.embeddingsFile ?: return filteredAlbum
        if (!embeddingsFile.exists() || !embeddingsFile.isFile) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, R.string.library_search_error_embeddings, Toast.LENGTH_LONG).show()
            }
            return filteredAlbum
        }

        //Prepare search query
        val modelFile = ModelManager.getFile(context, ModelManager.MODEL_FILE_NAME)
        val tokenizerFile = ModelManager.getFile(context, ModelManager.TOKENIZER_FILE_NAME)
        if (modelFile == null || tokenizerFile == null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, R.string.library_search_error_model, Toast.LENGTH_LONG).show()
            }
            return filteredAlbum
        }

        //Search database
        val databaseSearchResults = searchDatabase(embeddingsFile, query, modelFile, tokenizerFile, 0.55f)
        if (databaseSearchResults.isEmpty()) return filteredAlbum

        //Look for items in search results
        for (item in album.items) {
            if (databaseSearchResults.contains(item.name)) {
                filteredAlbum.add(item)
            }
        }

        //Return list
        return filteredAlbum
    }

    private fun searchDatabase(databaseFile: File, query: String, modelFile: File, tokenizerFile: File, threshold: Float): Set<String> {
        //Create results list
        val results: MutableSet<String> = HashSet()

        //Open database
        val db = try {
            SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (_: Exception) {
            return results
        }

        //Check that table exists
        if (!doesTableExist(db, "items")) {
            db.close()
            return results
        }

        //Create query embedding
        val queryEmbedding = ModelManager.getEmbedding(query, modelFile, tokenizerFile)

        //Read database
        try {
            db.rawQuery("SELECT name, embedding FROM items", null).use { cursor ->
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                val embeddingIdx = cursor.getColumnIndexOrThrow("embedding")

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx)
                    val blob = cursor.getBlob(embeddingIdx)
                    val itemVector = ModelManager.bytesToFloatArray(blob)

                    val similarity = ModelManager.cosineSimilarity(queryEmbedding, itemVector)
                    if (similarity < threshold) continue
                    results.add(name)
                }
            }
        } catch (_: Exception) {
            return results
        } finally {
            db.close()
        }

        //Return results
        return results
    }

    //Helpers
    private fun doesTableExist(db: SQLiteDatabase, tableName: String): Boolean {
        val cursor = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

}