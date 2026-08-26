package com.botpa.turbophotos.gallery.search.models

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer
import kotlin.math.sqrt
import kotlin.use

object ModelManager {

    const val MODEL_URL = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx"
    const val MODEL_FILE_NAME = "model.onnx"
    const val TOKENIZER_URL = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/tokenizer.json"
    const val TOKENIZER_FILE_NAME = "tokenizer.json"


    //Files
    fun getParentFile(context: Context): File {
        val parentFolder = File(context.filesDir, "models/text_embeddings")
        if (!parentFolder.exists()) parentFolder.mkdirs()
        return parentFolder
    }

    fun getFile(context: Context, fileName: String): File? {
        val parentFolder = getParentFile(context)
        val targetFile = File(parentFolder, fileName)
        return if (targetFile.exists()) {
            targetFile
        } else {
            null
        }
    }

    fun deleteFiles(context: Context): Boolean {
        val parentFolder = getParentFile(context)
        return parentFolder.deleteRecursively()
    }

    //Embedding generation
    private class StandardSentencePieceTokenizer(tokenizerFile: File) {

        private val vocabMap = mutableMapOf<String, Long>()
        private val clsId: Long
        private val sepId: Long
        private val padId: Long
        private val unkId: Long

        init {
            val jsonString = tokenizerFile.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            if (root.has("model")) {
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
            }

            clsId = vocabMap["<s>"] ?: vocabMap["[CLS]"] ?: 0L
            padId = vocabMap["<pad>"] ?: vocabMap["[PAD]"] ?: 1L
            sepId = vocabMap["</s>"] ?: vocabMap["[SEP]"] ?: 2L
            unkId = vocabMap["<unk>"] ?: vocabMap["[UNK]"] ?: 3L
        }

        fun tokenize(text: String, maxLength: Int = 128): Pair<LongArray, LongArray> {
            val tokens = mutableListOf<Long>()
            tokens.add(clsId)

            val words = text.lowercase().trim().split(Regex("\\s+"))
            for (word in words) {
                if (tokens.size >= maxLength - 1) break
                tokens.addAll(tokenizeWord(word))
            }

            if (tokens.size > maxLength - 1) {
                val truncated = tokens.subList(0, maxLength - 1)
                tokens.clear()
                tokens.addAll(truncated)
            }

            tokens.add(sepId)

            val inputIds = LongArray(maxLength) { padId }
            val attentionMask = LongArray(maxLength) { 0L }

            for (i in tokens.indices) {
                inputIds[i] = tokens[i]
                attentionMask[i] = 1L
            }

            return Pair(inputIds, attentionMask)
        }

        private fun tokenizeWord(word: String): List<Long> {
            val subwords = mutableListOf<Long>()
            var start = 0

            // Handle SentencePiece whitespace prefix representation
            val prepended = "\u2581$word"
            if (vocabMap.containsKey(prepended)) {
                return listOf(vocabMap[prepended]!!)
            }

            while (start < word.length) {
                var end = word.length
                var curSubwordId: Long? = null

                while (start < end) {
                    val sub = word.substring(start, end)
                    val candidate = if (start == 0) "\u2581$sub" else sub
                    if (vocabMap.containsKey(candidate)) {
                        curSubwordId = vocabMap[candidate]
                        break
                    }
                    end--
                }

                if (curSubwordId == null) {
                    subwords.add(unkId)
                    start++
                } else {
                    subwords.add(curSubwordId)
                    start = end
                }
            }
            return subwords
        }
    }

    private fun normalizeL2(embedding: FloatArray): FloatArray {
        var sum = 0.0f
        for (v in embedding) {
            sum += v * v
        }
        val norm = sqrt(sum.toDouble()).toFloat()
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
        return embedding
    }

    fun getEmbedding(text: String, modelFile: File, tokenizerFile: File): FloatArray {
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

    //Embedding comparisons
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        //Ensure same vector size
        if (v1.size != v2.size || v1.isEmpty()) return 0.0f

        //Compute cosine similarity
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

}
