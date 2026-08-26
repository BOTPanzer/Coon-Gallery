package com.botpa.turbophotos.gallery.search

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.Item
import com.botpa.turbophotos.gallery.search.models.ModelManager
import com.botpa.turbophotos.util.Orion

object SearchHelper {

    //Methods
    fun filterAlbumWords(normalizedQuery: String, queryTokens: List<String>, album: Album): MutableList<Item> {
        //Create new list
        val filteredAlbum = ArrayList<Item>()

        //Look for items that match the query
        for (item in album.items) {
            //Check item name
            if (Orion.normalizeText(item.name).contains(normalizedQuery)) {
                filteredAlbum.add(item)
                continue
            }

            //Get metadata
            val metadata = item.getMetadata() ?: continue

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

    fun filterAlbumText(normalizedQuery: String, album: Album): MutableList<Item> {
        //Create new list
        val filteredAlbum = ArrayList<Item>()

        //Look for items that match the query
        for (item in album.items) {
            //Check item name
            if (Orion.normalizeText(item.name).contains(normalizedQuery)) {
                filteredAlbum.add(item)
                continue
            }

            //Get metadata
            val metadata = item.getMetadata() ?: continue

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

    fun filterAlbumNatural(context: Context, normalizedQuery: String, album: Album, threshold: Float = 0.55f): MutableList<Item> {
        //Create new list
        val filteredAlbum = ArrayList<Item>()

        //Prepare search query
        val modelFile = ModelManager.getFile(context, ModelManager.MODEL_FILE_NAME)
        val tokenizerFile = ModelManager.getFile(context, ModelManager.TOKENIZER_FILE_NAME)
        if (modelFile == null || tokenizerFile == null) {
            showSnack(context, R.string.library_search_error_model)
            return filteredAlbum
        }

        //Create query embedding
        val queryEmbedding = ModelManager.getEmbedding(normalizedQuery, modelFile, tokenizerFile)

        //Look for items that match the query
        for (item in album.items) {
            //Check item name
            if (Orion.normalizeText(item.name).contains(normalizedQuery)) {
                filteredAlbum.add(item)
                continue
            }

            //Get item metadata
            val metadata = item.getMetadata() ?: continue

            //Check if item embedding matches the query
            val similarity = ModelManager.cosineSimilarity(queryEmbedding, metadata.embedding)
            if (similarity >= threshold) filteredAlbum.add(item)
        }

        //Return list
        return filteredAlbum
    }

    //Helpers
    private fun showSnack(context: Context, message: Int) {
        Handler(Looper.getMainLooper()).post {
            Orion.snack(context as Activity, message, duration = Orion.snackDurationLong)
        }
    }

}