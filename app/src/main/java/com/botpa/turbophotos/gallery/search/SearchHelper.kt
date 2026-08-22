package com.botpa.turbophotos.gallery.search

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.Item
import com.botpa.turbophotos.gallery.data.Link
import com.botpa.turbophotos.util.Orion
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer
import kotlin.math.sqrt

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

    //Vectors
    private const val MODEL_URL = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx"
    private const val MODEL_FILE_NAME = "model.onnx"
    private const val TOKENIZER_URL = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/tokenizer.json"
    private const val TOKENIZER_FILE_NAME = "tokenizer.json"

    fun filterAlbumVectors(normalizedQuery: String, album: Album, context: Context): MutableList<Item> {
        //Create new list
        val filteredAlbum = ArrayList<Item>()

        //Prepare vectors database
        val link = Link.getLink(album.albumPath)
        val vectorsFile = link?.vectorsFile ?: return filteredAlbum
        if (!vectorsFile.exists()) return filteredAlbum

        //Prepare search query
        val modelFile = ensureDownloaded(context, MODEL_URL, MODEL_FILE_NAME) ?: return filteredAlbum
        val tokenizerFile = ensureDownloaded(context, TOKENIZER_URL, TOKENIZER_FILE_NAME) ?: return filteredAlbum
        val queryVector = generateTextVector(normalizedQuery, modelFile, tokenizerFile)

        //Search vectors
        val vectorSearchResults = searchVectors(vectorsFile, queryVector)

        //Look for items in search results
        for (item in album.items) {
            if (vectorSearchResults.contains(item.name)) {
                filteredAlbum.add(item)
            }
        }

        //Return list
        return filteredAlbum
    }

    //Vectors util
    private fun ensureDownloaded(context: Context, downloadUrl: String, fileName: String): File? {
        //Check if parent folder exists
        val parentFolder = File(context.filesDir, "models/text_embeddings")
        if (!parentFolder.exists()) {
            //Doesn't exist -> Create it
            parentFolder.mkdirs()
        }

        //Check if file exists
        val targetFile = File(parentFolder, fileName)
        if (targetFile.exists()) {
            //File exists -> Return it
            return targetFile
        }

        //Download file
        val tempFile = File(parentFolder, "$fileName.tmp")
        try {
            //Start download
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            //Failed to connect
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }

            //Write file to disk
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            //Rename temp file to target
            if (tempFile.renameTo(targetFile)) {
                return targetFile
            }
        } catch (e: Exception) {
            //Delete temp file
            if (tempFile.exists()) {
                tempFile.delete()
            }
            return null
        }

        //Failed
        return null
    }

    private fun searchVectors(databaseFile: File, queryVector: FloatArray, limit: Float = 0.5f): Set<String> {
        //Create results list
        val results: MutableSet<String> = HashSet()

        //Read database
        val db = SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.rawQuery("SELECT name, vector FROM items", null).use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            val vectorIdx = cursor.getColumnIndexOrThrow("vector")

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx)
                val blob = cursor.getBlob(vectorIdx)
                val itemVector = bytesToFloatArray(blob)

                val similarity = cosineSimilarity(queryVector, itemVector)
                if (similarity < limit) continue
                results.add(name)
            }
        }
        db.close()

        //Return results
        return results
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        return (dotProduct / (sqrt(normA.toDouble()) * sqrt(normB.toDouble()))).toFloat()
    }

    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floatArray = FloatArray(bytes.size / 4)
        buffer.asFloatBuffer().get(floatArray)
        return floatArray
    }

    private fun generateTextVector(text: String, modelFile: File, tokenizerFile: File): FloatArray {
        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = OrtSession.SessionOptions()

        val tokenizer = StandardSentencePieceTokenizer(tokenizerFile)
        val (inputIds, attentionMask) = tokenizer.tokenize(text)
        val tokenTypeIds = LongArray(inputIds.size) { 0L }

        val shape = longArrayOf(1, inputIds.size.toLong())

        val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape)
        val attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape)
        val tokenTypeIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape)

        val inputs = mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionMaskTensor,
            "token_type_ids" to tokenTypeIdsTensor
        )

        env.createSession(modelFile.absolutePath, sessionOptions).use { ortSession ->
            ortSession.run(inputs).use { result ->
                @Suppress("UNCHECKED_CAST")
                val outputTensor = result.get(0).value as Array<Array<FloatArray>>
                val tokenEmbeddings = outputTensor[0]

                val hiddenSize = tokenEmbeddings[0].size
                val pooledEmbedding = FloatArray(hiddenSize)
                var validTokenCount = 0f

                for (i in tokenEmbeddings.indices) {
                    if (attentionMask[i] == 1L) {
                        validTokenCount += 1f
                        for (j in 0 until hiddenSize) {
                            pooledEmbedding[j] += tokenEmbeddings[i][j]
                        }
                    }
                }

                if (validTokenCount > 0f) {
                    for (j in 0 until hiddenSize) {
                        pooledEmbedding[j] /= validTokenCount
                    }
                }

                inputIdsTensor.close()
                attentionMaskTensor.close()
                tokenTypeIdsTensor.close()

                return normalizeL2(pooledEmbedding)
            }
        }
    }

    private fun normalizeL2(vector: FloatArray): FloatArray {
        var sum = 0.0f
        for (v in vector) {
            sum += v * v
        }
        val norm = sqrt(sum.toDouble()).toFloat()
        if (norm > 0) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }

    private class StandardSentencePieceTokenizer(tokenizerFile: File) {
        private val vocabMap = mutableMapOf<String, Long>()
        private val clsId: Long
        private val sepId: Long
        private val padId: Long
        private val unkId: Long

        init {
            val jsonString = tokenizerFile.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val modelObj = root.getJSONObject("model")

            if (modelObj.has("vocab") && modelObj.get("vocab") is JSONObject) {
                val vocabObj = modelObj.getJSONObject("vocab")
                vocabObj.keys().forEach { key ->
                    vocabMap[key] = vocabObj.getLong(key)
                }
            } else if (modelObj.has("vocab")) {
                val vocabArray = modelObj.getJSONArray("vocab")
                for (i in 0 until vocabArray.length()) {
                    val entry = vocabArray.getJSONArray(i)
                    vocabMap[entry.getString(0)] = i.toLong()
                }
            }

            // MiniLM-L12 explicit special token mapping
            clsId = vocabMap["<s>"] ?: 0L
            padId = vocabMap["<pad>"] ?: 1L
            sepId = vocabMap["</s>"] ?: 2L
            unkId = vocabMap["<unk>"] ?: 3L
        }

        fun tokenize(text: String, maxLength: Int = 128): Pair<LongArray, LongArray> {
            val tokens = mutableListOf<Long>()
            tokens.add(clsId)

            val words = text.lowercase().trim().split(Regex("\\s+"))
            for (word in words) {
                if (tokens.size >= maxLength - 1) break

                // SentencePiece U+2581 tokenization symbol
                val prefixedWord = "\u2581$word"
                if (vocabMap.containsKey(prefixedWord)) {
                    tokens.add(vocabMap[prefixedWord]!!)
                } else if (vocabMap.containsKey(word)) {
                    tokens.add(vocabMap[word]!!)
                } else {
                    tokens.add(unkId)
                }
            }

            tokens.add(sepId)

            val inputIds = LongArray(maxLength) { padId }
            val attentionMask = LongArray(maxLength) { 0L }

            for (i in tokens.indices) {
                if (i >= maxLength) break
                inputIds[i] = tokens[i]
                attentionMask[i] = 1L
            }

            return Pair(inputIds, attentionMask)
        }
    }

}