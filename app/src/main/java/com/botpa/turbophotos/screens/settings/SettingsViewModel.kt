package com.botpa.turbophotos.screens.settings

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.screens.home.HomeActivity
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

class SettingsViewModel : ViewModel() {

    //App
    var appModifyMetadata by mutableStateOf(Storage.getBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION))

    //Home screen
    var homeItemsPerRow by mutableFloatStateOf(Storage.getInt(StoragePairs.HOME_ITEMS_PER_ROW).toFloat())

    //Album screen
    var albumItemsPerRow by mutableFloatStateOf(Storage.getInt(StoragePairs.ALBUM_ITEMS_PER_ROW).toFloat())
    var albumShowMissingMetadataIcon by  mutableStateOf(Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON))

    //Video screen
    var videoSkipBackwardsAmount by mutableFloatStateOf(Storage.getLong(StoragePairs.VIDEO_SKIP_BACKWARDS).toFloat())
    var videoSkipForwardAmount by mutableFloatStateOf(Storage.getLong(StoragePairs.VIDEO_SKIP_FORWARD).toFloat())


    //App
    private fun addSettingsToJson(json: ObjectNode) {
        //Library
        json.put(StoragePairs.LIBRARY_LINKS_KEY, Storage.getString(StoragePairs.LIBRARY_LINKS_KEY, ""))

        //App
        json.put(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION.key, Storage.getBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION))

        //Home
        json.put(StoragePairs.HOME_ITEMS_PER_ROW.key, Storage.getInt(StoragePairs.HOME_ITEMS_PER_ROW))

        //Album
        json.put(StoragePairs.ALBUM_ITEMS_PER_ROW.key, Storage.getInt(StoragePairs.ALBUM_ITEMS_PER_ROW))
        json.put(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key, Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON))

        //Video
        json.put(StoragePairs.VIDEO_LOOP.key, Storage.getBool(StoragePairs.VIDEO_LOOP))
        json.put(StoragePairs.VIDEO_SKIP_BACKWARDS.key, Storage.getLong(StoragePairs.VIDEO_SKIP_BACKWARDS))
        json.put(StoragePairs.VIDEO_SKIP_FORWARD.key, Storage.getLong(StoragePairs.VIDEO_SKIP_FORWARD))

        //Sync
        json.put(StoragePairs.SYNC_USERS_KEY, Storage.getString(StoragePairs.SYNC_USERS_KEY, ""))
    }

    fun createSettingsBackup(context: Context) {
        //Create empty json
        val json = Orion.emptyJson

        //Add settings to json
        addSettingsToJson(json)

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

    private fun loadSettingsFromJson(json: ObjectNode) {
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

        //Video (these settings get loaded in video activity)
        if (json.has(StoragePairs.VIDEO_LOOP.key) && json.get(StoragePairs.VIDEO_LOOP.key).isBoolean) {
            Storage.putBool(StoragePairs.VIDEO_LOOP.key, json.get(StoragePairs.VIDEO_LOOP.key).asBoolean())
        }
        if (json.has(StoragePairs.VIDEO_SKIP_BACKWARDS.key) && json.get(StoragePairs.VIDEO_SKIP_BACKWARDS.key).isLong) {
            Storage.putLong(StoragePairs.VIDEO_SKIP_BACKWARDS.key, json.get(StoragePairs.VIDEO_SKIP_BACKWARDS.key).asLong())
        }
        if (json.has(StoragePairs.VIDEO_SKIP_FORWARD.key) && json.get(StoragePairs.VIDEO_SKIP_FORWARD.key).isLong) {
            Storage.putLong(StoragePairs.VIDEO_SKIP_FORWARD.key, json.get(StoragePairs.VIDEO_SKIP_FORWARD.key).asLong())
        }

        //Sync (these settings get loaded in sync activity)
        if (json.has(StoragePairs.SYNC_USERS_KEY) && json.get(StoragePairs.SYNC_USERS_KEY).isTextual) {
            Storage.putString(StoragePairs.SYNC_USERS_KEY, json.get(StoragePairs.SYNC_USERS_KEY).asText())
        }
    }

    fun restoreSettingsBackup(context: Context, activity: Activity, file: File) {
        //Load json from backup file
        val json = Orion.loadJson(file)

        //Load settings from json
        loadSettingsFromJson(json)

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
    fun saveHomeItemsPerRow() {
        Storage.putInt(StoragePairs.HOME_ITEMS_PER_ROW.key, homeItemsPerRow.toInt())
    }

    //Album screen
    fun saveAlbumItemsPerRow() {
        Storage.putInt(StoragePairs.ALBUM_ITEMS_PER_ROW.key, albumItemsPerRow.toInt())
    }

    fun updateAlbumShowMissingMetadataIcon(isChecked: Boolean) {
        albumShowMissingMetadataIcon = isChecked
        Storage.putBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key, isChecked)
    }

    //Video screen
    fun saveVideoSkipBackwardsAmount() {
        Storage.putLong(StoragePairs.VIDEO_SKIP_BACKWARDS.key, videoSkipBackwardsAmount.toLong())
    }

    fun saveVideoSkipForwardAmount() {
        Storage.putLong(StoragePairs.VIDEO_SKIP_FORWARD.key, videoSkipForwardAmount.toLong())
    }

    //Links
    fun updateLinkAlbumFolder(activity: Activity, index: Int, folder: File) {
        //Update link folder
        val updated = Link.updateLinkFolder(index, folder)

        //Check if update was successful
        if (!updated) {
            //Failed -> There is another link with the same album
            Orion.snack(activity, "A link with that album already exists")
        } else {
            //Success -> Reload library on home resume
            HomeActivity.reloadOnResume()
        }
    }

    fun updateLinkMetadataFile(index: Int, file: File) {
        //Update link with selected file
        Link.updateLinkFile(index, file)

        //Reload library on home resume
        HomeActivity.reloadOnResume()
    }

    fun removeLink(index: Int) {
        //Remove link
        if (!Link.removeLink(index)) return

        //Save links
        Link.saveLinks()

        //Reload library on home resume
        HomeActivity.reloadOnResume()
    }

    fun addLink(activity: Activity) {
        //Try to add new empty link
        if (!Link.addLink(Link("", ""))) {
            //Not added -> There is another link with the same album
            Orion.snack(activity, "A link with that album already exists")
            return
        }

        //Save links
        Link.saveLinks()

        //Reload library on home resume
        HomeActivity.reloadOnResume()
    }

}