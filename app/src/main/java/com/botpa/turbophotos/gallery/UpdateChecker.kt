package com.botpa.turbophotos.gallery

import com.botpa.turbophotos.BuildConfig
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    const val UPDATE_URL: String = "https://api.github.com/repos/BOTPanzer/Coon-Gallery/releases/latest"

    fun checkForUpdates(): String? {
        //Create connection
        val url = URL(UPDATE_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")

        //Make request
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                //Parse response
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = Orion.loadJson(response)

                //Check version
                val currentVersion = "v${BuildConfig.VERSION_NAME}"
                val newestVersion = json.get("tag_name").asText()
                val skippedUpdate = Storage.getString(StoragePairs.APP_UPDATE_SKIPPED)

                //Check if versions are not the same
                if (currentVersion != newestVersion && skippedUpdate != newestVersion) {
                    return newestVersion
                }
            }
        } finally {
            connection.disconnect()
        }

        //Error or no update
        return null
    }

}