package com.botpa.turbophotos.settings

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.home.HomeActivity
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import java.io.File

class SettingsViewModel : ViewModel() {

    //App
    var appModifyMetadata by mutableStateOf(Storage.getBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION))

    //Home screen
    var homeItemsPerRow by mutableFloatStateOf(Storage.getInt(StoragePairs.HOME_ITEMS_PER_ROW).toFloat())

    //Album screen
    var albumItemsPerRow by mutableFloatStateOf(Storage.getInt(StoragePairs.ALBUM_ITEMS_PER_ROW).toFloat())
    var albumShowMissingMetadataIcon by  mutableStateOf(Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON))


    //App
    fun createSettingsBackup(context: Context) {
        //Create empty json
        val json = Orion.getEmptyJson()

        //Library
        json.put(
            StoragePairs.LIBRARY_LINKS_KEY,
            Storage.getString(StoragePairs.LIBRARY_LINKS_KEY, "")
        )

        //App
        json.put(
            StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION.key,
            Storage.getBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION)
        )

        //Home
        json.put(
            StoragePairs.HOME_ITEMS_PER_ROW.key,
            Storage.getInt(StoragePairs.HOME_ITEMS_PER_ROW)
        )

        //Album
        json.put(
            StoragePairs.ALBUM_ITEMS_PER_ROW.key,
            Storage.getInt(StoragePairs.ALBUM_ITEMS_PER_ROW)
        )
        json.put(
            StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key,
            Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON)
        )

        //Sync
        json.put(
            StoragePairs.SYNC_USERS_KEY,
            Storage.getString(StoragePairs.SYNC_USERS_KEY, "")
        )

        //Create backup file
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CoonGalleryBackup-${System.currentTimeMillis()}.json")
        if (Orion.writeJsonPretty(file, json)) {
            //Success creating file
            Toast.makeText(context, "Backup saved to downloads folder.", Toast.LENGTH_SHORT).show()
        } else {
            //Error creating file
            Toast.makeText(context, "Error creating backup file.", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreSettingsBackup(context: Context, activity: Activity, file: File) {
        //Load json from backup file
        val json = Orion.loadJson(file)

        //Library
        if (json.has(StoragePairs.LIBRARY_LINKS_KEY) && json.get(StoragePairs.LIBRARY_LINKS_KEY).isTextual) {
            //Too lazy to recreate all links so the library gets reloaded after finishing (✿◡‿◡)
            Storage.putString(StoragePairs.LIBRARY_LINKS_KEY, json.get(StoragePairs.LIBRARY_LINKS_KEY).asText())
        }

        //App
        if (json.has(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION.key) && json.get(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION.key).isBoolean) {
            appModifyMetadata = json.get(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION.key).asBoolean()
            Storage.putBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION.key, appModifyMetadata)
        }

        //Home
        if (json.has(StoragePairs.HOME_ITEMS_PER_ROW.key) && json.get(StoragePairs.HOME_ITEMS_PER_ROW.key).isInt) {
            val value = json.get(StoragePairs.HOME_ITEMS_PER_ROW.key).asInt()
            homeItemsPerRow = value.toFloat()
            Storage.putInt(StoragePairs.HOME_ITEMS_PER_ROW.key, value)
        }

        //Album
        if (json.has(StoragePairs.ALBUM_ITEMS_PER_ROW.key) && json.get(StoragePairs.ALBUM_ITEMS_PER_ROW.key).isInt) {
            val value = json.get(StoragePairs.ALBUM_ITEMS_PER_ROW.key).asInt()
            albumItemsPerRow = value.toFloat()
            Storage.putInt(StoragePairs.ALBUM_ITEMS_PER_ROW.key, value)
        }
        if (json.has(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key) && json.get(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key).isBoolean) {
            albumShowMissingMetadataIcon = json.get(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key).asBoolean()
            Storage.putBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key, albumShowMissingMetadataIcon)
        }

        //Sync
        if (json.has(StoragePairs.SYNC_USERS_KEY) && json.get(StoragePairs.SYNC_USERS_KEY).isTextual) {
            //Users list gets loaded in sync activity so no need to do anything here
            Storage.putString(StoragePairs.SYNC_USERS_KEY, json.get(StoragePairs.SYNC_USERS_KEY).asText())
        }

        //Success restoring backup
        Toast.makeText(context, "Backup restored.", Toast.LENGTH_SHORT).show()

        //Reload library on home resume & close settings screen
        HomeActivity.reloadOnResume()
        activity.finish()
    }

    fun updateAppModifyMetadata(isChecked: Boolean) {
        appModifyMetadata = isChecked
        Storage.putBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION.key, isChecked)
    }

    //Home screen
    fun updateHomeItemsPerRow(value: Float) {
        homeItemsPerRow = value
    }

    fun saveHomeItemsPerRow() {
        Storage.putInt(StoragePairs.HOME_ITEMS_PER_ROW.key, homeItemsPerRow.toInt())
    }

    //Album screen
    fun updateAlbumItemsPerRow(value: Float) {
        albumItemsPerRow = value
    }

    fun saveAlbumItemsPerRow() {
        Storage.putInt(StoragePairs.ALBUM_ITEMS_PER_ROW.key, albumItemsPerRow.toInt())
    }

    fun updateAlbumShowMissingMetadataIcon(isChecked: Boolean) {
        albumShowMissingMetadataIcon = isChecked
        Storage.putBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key, isChecked)
    }

}