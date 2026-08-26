package com.botpa.turbophotos.gallery.search.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import kotlin.math.floor

class ModelDownloadWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val successModel = downloadFile(
            ModelManager.MODEL_URL,
            ModelManager.MODEL_FILE_NAME,
            progressOffset = 0,
            progressWeight = 0.8f
        )

        if (!successModel) return Result.failure()

        val successTokenizer = downloadFile(
            ModelManager.TOKENIZER_URL,
            ModelManager.TOKENIZER_FILE_NAME,
            progressOffset = 80,
            progressWeight = 0.2f
        )

        if (!successTokenizer) return Result.failure()

        return Result.success()
    }

    private suspend fun downloadFile(downloadUrl: String, fileName: String, progressOffset: Int, progressWeight: Float): Boolean {
        //Get necessary files and folders
        val parentFolder = ModelManager.getParentFile(context)
        val targetFile = File(parentFolder, fileName)
        val tempFile = File(parentFolder, "$fileName.tmp")

        //Target file already exists -> Ignore
        if (targetFile.exists() && targetFile.length() > 0) return true

        //Download file
        return try {
            withContext(Dispatchers.IO) {
                //Prepare download
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                //Download failed
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext false
                }

                //Get file info
                val fileLength = connection.contentLength
                var lastEmittedProgressTimestamp = Date().time - 1000

                //Start downloading
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        //Write to file
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            //Check if should update progress
                            val currentTimestamp = Date().time
                            if (fileLength > 0 && currentTimestamp >= lastEmittedProgressTimestamp + 1000) {
                                lastEmittedProgressTimestamp = currentTimestamp

                                //Update progress
                                val fileProgress = (totalBytesRead.toFloat() / fileLength.toFloat()) * 100f
                                val overallProgress = progressOffset + (fileProgress * progressWeight)
                                val roundedProgress = floor(overallProgress * 10f) / 10f
                                setProgress(workDataOf("PROGRESS" to roundedProgress, "SIZE" to fileLength.toLong()))
                            }
                        }
                    }
                }
                tempFile.renameTo(targetFile)
            }
        } catch (_: Exception) {
            //Failed -> Delete temp file
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }

    companion object {
        const val WORK_NAME = "model_download_work"
    }

}
