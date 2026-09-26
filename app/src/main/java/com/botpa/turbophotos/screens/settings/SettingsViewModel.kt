package com.botpa.turbophotos.screens.settings

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.Library
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.gallery.data.Link
import com.botpa.turbophotos.gallery.search.models.DownloadState
import com.botpa.turbophotos.gallery.search.models.ModelDownloadWorker
import com.botpa.turbophotos.gallery.search.models.ModelManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    //Settings
    var reloadLibraryOnExit = false

    val context: Context get() = getApplication<Application>().applicationContext
    val workManager = WorkManager.getInstance(context)

    //App
    var appCheckForUpdates by mutableStateOf(Storage.getBool(StoragePairs.APP_UPDATE_CHECK))

    //Metadata
    var libraryMetadataModification by mutableStateOf(Storage.getBool(StoragePairs.LIBRARY_AUTOMATIC_METADATA_MODIFICATION))

    val searchModelDownloadState: Flow<DownloadState> = workManager
        .getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.WORK_NAME)
        .map { workInfoList ->
            val workInfo = workInfoList.firstOrNull() ?: run {
                val isDownloaded = ModelManager.isDownloaded(context)
                return@map if (isDownloaded) DownloadState.Downloaded else DownloadState.Missing
            }

            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getFloat("PROGRESS", 0f)
                    val size = workInfo.progress.getLong("SIZE", 0L)
                    DownloadState.Downloading(progress, size)
                }
                WorkInfo.State.SUCCEEDED -> DownloadState.Downloaded
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> DownloadState.Failed
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.Checking
            }
        }

    //Home screen
    var homeItemsPerRow by mutableFloatStateOf(Storage.getInt(StoragePairs.HOME_ITEMS_PER_ROW).toFloat())

    //Album screen
    var albumItemsPerRow by mutableFloatStateOf(Storage.getInt(StoragePairs.ALBUM_ITEMS_PER_ROW).toFloat())
    var albumShowMissingMetadataIcon by  mutableStateOf(Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON))

    //Viewer screen
    var viewerShowInfo by mutableStateOf(Storage.getBool(StoragePairs.VIEWER_SHOW_PROPERTIES))
    var viewerShowEdit by mutableStateOf(Storage.getBool(StoragePairs.VIEWER_SHOW_EDIT))
    var viewerShowShare by mutableStateOf(Storage.getBool(StoragePairs.VIEWER_SHOW_SHARE))
    var viewerShowFavourite by mutableStateOf(Storage.getBool(StoragePairs.VIEWER_SHOW_FAVOURITE))

    //Video player
    var videoSkipBackwardsAmount by mutableFloatStateOf(Storage.getLong(StoragePairs.VIDEO_SKIP_BACKWARDS).toFloat())
    var videoSkipForwardAmount by mutableFloatStateOf(Storage.getLong(StoragePairs.VIDEO_SKIP_FORWARD).toFloat())
    var videoUseInternalPlayer by  mutableStateOf(Storage.getBool(StoragePairs.VIDEO_USE_INTERNAL_PLAYER))
    var videoIgnoreAudioFocus by  mutableStateOf(Storage.getBool(StoragePairs.VIDEO_IGNORE_AUDIO_FOCUS))
    var videoShowControlsOnStart by  mutableStateOf(Storage.getBool(StoragePairs.VIDEO_SHOW_CONTROLS_ON_START))
    var videoAutomaticPiP by  mutableStateOf(Storage.getBool(StoragePairs.VIDEO_AUTOMATIC_PIP))


    //App
    fun updateCheckForUpdates(isChecked: Boolean) {
        appCheckForUpdates = isChecked
        Storage.putBool(StoragePairs.APP_UPDATE_CHECK, isChecked)
    }

    private fun addSettingsToJson(json: ObjectNode) {
        //App
        json.put(StoragePairs.APP_UPDATE_CHECK.key, Storage.getBool(StoragePairs.APP_UPDATE_CHECK))
        json.put(StoragePairs.APP_UPDATE_SKIPPED.key, Storage.getString(StoragePairs.APP_UPDATE_SKIPPED))

        //Library
        json.put(StoragePairs.LIBRARY_LINKS_KEY, Storage.getString(StoragePairs.LIBRARY_LINKS_KEY, ""))
        json.put(StoragePairs.LIBRARY_AUTOMATIC_METADATA_MODIFICATION.key, Storage.getBool(StoragePairs.LIBRARY_AUTOMATIC_METADATA_MODIFICATION))
        json.put(StoragePairs.LIBRARY_SORT_METHOD.key, Storage.getString(StoragePairs.LIBRARY_SORT_METHOD))
        json.put(StoragePairs.LIBRARY_SORT_DIRECTION.key, Storage.getString(StoragePairs.LIBRARY_SORT_DIRECTION))

        //Home screen
        json.put(StoragePairs.HOME_ITEMS_PER_ROW.key, Storage.getInt(StoragePairs.HOME_ITEMS_PER_ROW))

        //Album screen
        json.put(StoragePairs.ALBUM_ITEMS_PER_ROW.key, Storage.getInt(StoragePairs.ALBUM_ITEMS_PER_ROW))
        json.put(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key, Storage.getBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON))
        json.put(StoragePairs.ALBUM_SEARCH_METHOD.key, Storage.getString(StoragePairs.ALBUM_SEARCH_METHOD))

        //Video player
        json.put(StoragePairs.VIDEO_LOOP.key, Storage.getBool(StoragePairs.VIDEO_LOOP))
        json.put(StoragePairs.VIDEO_SKIP_BACKWARDS.key, Storage.getLong(StoragePairs.VIDEO_SKIP_BACKWARDS))
        json.put(StoragePairs.VIDEO_SKIP_FORWARD.key, Storage.getLong(StoragePairs.VIDEO_SKIP_FORWARD))
        json.put(StoragePairs.VIDEO_USE_INTERNAL_PLAYER.key, Storage.getBool(StoragePairs.VIDEO_USE_INTERNAL_PLAYER))
        json.put(StoragePairs.VIDEO_IGNORE_AUDIO_FOCUS.key, Storage.getBool(StoragePairs.VIDEO_IGNORE_AUDIO_FOCUS))
        json.put(StoragePairs.VIDEO_SHOW_CONTROLS_ON_START.key, Storage.getBool(StoragePairs.VIDEO_SHOW_CONTROLS_ON_START))

        //Sync screen
        json.put(StoragePairs.SYNC_USERS_KEY, Storage.getString(StoragePairs.SYNC_USERS_KEY, ""))
    }

    fun createSettingsBackup(context: Context, folder: File) {
        //Create empty json
        val json = Orion.emptyJson

        //Add settings to json
        addSettingsToJson(json)

        //Create backup file
        val name = "CoonGalleryBackup_${SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(System.currentTimeMillis())}.json"
        val file = File(folder, name)
        if (Orion.writeJsonPretty(file, json)) {
            //Success creating file
            Toast.makeText(context, R.string.settings_message_backup_create_success, Toast.LENGTH_SHORT).show()
        } else {
            //Error creating file
            Toast.makeText(context, R.string.settings_error_backup_create, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadStringSettingFromJson(json: ObjectNode, key: String, onValue: (String) -> Unit) {
        val value = json.get(key) ?: return
        if (value.isTextual) onValue(value.asText())
    }

    private fun loadBoolSettingFromJson(json: ObjectNode, key: String, onValue: (Boolean) -> Unit) {
        val value = json.get(key) ?: return
        if (value.isBoolean) onValue(value.asBoolean())
    }

    private fun loadIntSettingFromJson(json: ObjectNode, key: String, onValue: (Int) -> Unit) {
        val value = json.get(key) ?: return
        if (value.isInt) onValue(value.asInt())
    }

    private fun loadLongSettingFromJson(json: ObjectNode, key: String, onValue: (Long) -> Unit) {
        val value = json.get(key) ?: return
        if (value.isInt || value.isLong) onValue(value.asLong())
    }

    private fun loadSettingsFromJson(json: ObjectNode) {
        //App
        loadBoolSettingFromJson(json, StoragePairs.APP_UPDATE_CHECK.key) { value ->
            appCheckForUpdates = value
            Storage.putBool(StoragePairs.APP_UPDATE_CHECK, value)
        }
        loadStringSettingFromJson(json, StoragePairs.APP_UPDATE_SKIPPED.key) { value ->
            Storage.putString(StoragePairs.APP_UPDATE_SKIPPED, value)
        }

        //Library
        loadStringSettingFromJson(json, StoragePairs.LIBRARY_LINKS_KEY) { value ->
            Storage.putString(StoragePairs.LIBRARY_LINKS_KEY, value)
        }
        loadBoolSettingFromJson(json, StoragePairs.LIBRARY_AUTOMATIC_METADATA_MODIFICATION.key) { value ->
            libraryMetadataModification = value
            Storage.putBool(StoragePairs.LIBRARY_AUTOMATIC_METADATA_MODIFICATION, value)
        }
        loadStringSettingFromJson(json, StoragePairs.ALBUM_SEARCH_METHOD.key) { value ->
            Storage.putString(StoragePairs.LIBRARY_SORT_METHOD, value)
        }
        loadStringSettingFromJson(json, StoragePairs.ALBUM_SEARCH_METHOD.key) { value ->
            Storage.putString(StoragePairs.LIBRARY_SORT_DIRECTION, value)
        }
        Library.refreshSortingInfo()

        //Home screen
        loadIntSettingFromJson(json, StoragePairs.HOME_ITEMS_PER_ROW.key) { value ->
            homeItemsPerRow = value.toFloat()
            Storage.putInt(StoragePairs.HOME_ITEMS_PER_ROW, value)
        }

        //Album screen
        loadIntSettingFromJson(json, StoragePairs.ALBUM_ITEMS_PER_ROW.key) { value ->
            albumItemsPerRow = value.toFloat()
            Storage.putInt(StoragePairs.ALBUM_ITEMS_PER_ROW, value)
        }
        loadBoolSettingFromJson(json, StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON.key) { value ->
            albumShowMissingMetadataIcon = value
            Storage.putBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON, value)
        }
        loadStringSettingFromJson(json, StoragePairs.ALBUM_SEARCH_METHOD.key) { value ->
            Storage.putString(StoragePairs.ALBUM_SEARCH_METHOD, value)
        }

        //Video player (these settings get loaded in video activity)
        loadBoolSettingFromJson(json, StoragePairs.VIDEO_LOOP.key) { value ->
            Storage.putBool(StoragePairs.VIDEO_LOOP, value)
        }
        loadLongSettingFromJson(json, StoragePairs.VIDEO_SKIP_BACKWARDS.key) { value ->
            Storage.putLong(StoragePairs.VIDEO_SKIP_BACKWARDS, value)
        }
        loadLongSettingFromJson(json, StoragePairs.VIDEO_SKIP_FORWARD.key) { value ->
            Storage.putLong(StoragePairs.VIDEO_SKIP_FORWARD, value)
        }
        loadBoolSettingFromJson(json, StoragePairs.VIDEO_USE_INTERNAL_PLAYER.key) { value ->
            Storage.putBool(StoragePairs.VIDEO_USE_INTERNAL_PLAYER, value)
        }
        loadBoolSettingFromJson(json, StoragePairs.VIDEO_IGNORE_AUDIO_FOCUS.key) { value ->
            Storage.putBool(StoragePairs.VIDEO_IGNORE_AUDIO_FOCUS, value)
        }
        loadBoolSettingFromJson(json, StoragePairs.VIDEO_SHOW_CONTROLS_ON_START.key) { value ->
            Storage.putBool(StoragePairs.VIDEO_SHOW_CONTROLS_ON_START, value)
        }

        //Sync screen (these settings get loaded in sync activity)
        loadStringSettingFromJson(json, StoragePairs.SYNC_USERS_KEY) { value ->
            Storage.putString(StoragePairs.SYNC_USERS_KEY, value)
        }
    }

    fun restoreSettingsBackup(context: Context, activity: Activity, file: File) {
        //Load json from backup file
        val json = Orion.loadJson(file)

        //Load settings from json
        loadSettingsFromJson(json)

        //Success restoring backup
        Toast.makeText(context, R.string.settings_message_backup_restore_success, Toast.LENGTH_SHORT).show()

        //Reload library on exit & close settings screen
        reloadLibraryOnExit = true
        activity.finish()
    }

    //Metadata
    fun updateLibraryMetadataModification(isChecked: Boolean) {
        libraryMetadataModification = isChecked
        Storage.putBool(StoragePairs.LIBRARY_AUTOMATIC_METADATA_MODIFICATION, isChecked)
    }

    fun downloadSearchModel() {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>().build()
        workManager.enqueueUniqueWork(
            ModelDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun deleteSearchModel() {
        workManager.cancelUniqueWork(ModelDownloadWorker.WORK_NAME)
        ModelManager.deleteFiles(context)
        workManager.pruneWork()
    }

    //Home screen
    fun saveHomeItemsPerRow() {
        Storage.putInt(StoragePairs.HOME_ITEMS_PER_ROW, homeItemsPerRow.toInt())
    }

    //Album screen
    fun saveAlbumItemsPerRow() {
        Storage.putInt(StoragePairs.ALBUM_ITEMS_PER_ROW, albumItemsPerRow.toInt())
    }

    fun updateAlbumShowMissingMetadataIcon(isChecked: Boolean) {
        albumShowMissingMetadataIcon = isChecked
        Storage.putBool(StoragePairs.ALBUM_SHOW_MISSING_METADATA_ICON, isChecked)
    }

    //Viewer screen
    fun updateViewerShowInfo(isChecked: Boolean) {
        viewerShowInfo = isChecked
        Storage.putBool(StoragePairs.VIEWER_SHOW_PROPERTIES, isChecked)
    }

    fun updateViewerShowEdit(isChecked: Boolean) {
        viewerShowEdit = isChecked
        Storage.putBool(StoragePairs.VIEWER_SHOW_EDIT, isChecked)
    }

    fun updateViewerShowShare(isChecked: Boolean) {
        viewerShowShare = isChecked
        Storage.putBool(StoragePairs.VIEWER_SHOW_SHARE, isChecked)
    }

    fun updateViewerShowFavourite(isChecked: Boolean) {
        viewerShowFavourite = isChecked
        Storage.putBool(StoragePairs.VIEWER_SHOW_FAVOURITE, isChecked)
    }

    //Video player
    fun saveVideoSkipBackwardsAmount() {
        Storage.putLong(StoragePairs.VIDEO_SKIP_BACKWARDS, videoSkipBackwardsAmount.toLong())
    }

    fun saveVideoSkipForwardAmount() {
        Storage.putLong(StoragePairs.VIDEO_SKIP_FORWARD, videoSkipForwardAmount.toLong())
    }

    fun updateVideoUseInternalPlayer(isChecked: Boolean) {
        videoUseInternalPlayer = isChecked
        Storage.putBool(StoragePairs.VIDEO_USE_INTERNAL_PLAYER, isChecked)
    }

    fun updateVideoIgnoreAudioFocus(isChecked: Boolean) {
        videoIgnoreAudioFocus = isChecked
        Storage.putBool(StoragePairs.VIDEO_IGNORE_AUDIO_FOCUS, isChecked)
    }

    fun updateVideoShowControlsOnStart(isChecked: Boolean) {
        videoShowControlsOnStart = isChecked
        Storage.putBool(StoragePairs.VIDEO_SHOW_CONTROLS_ON_START, isChecked)
    }

    fun updateVideoAutomaticPiP(isChecked: Boolean) {
        videoAutomaticPiP = isChecked
        Storage.putBool(StoragePairs.VIDEO_AUTOMATIC_PIP, isChecked)
    }

    //Links
    fun updateLinkAlbumFolder(activity: Activity, index: Int, albumFolder: File) {
        //Update link album folder
        val success = Link.updateLinkAlbumFolder(index, albumFolder)

        //Check if update was successful
        if (success) {
            //Success -> Save links
            Link.saveLinks()
        } else {
            //Failed -> There is another link with the same album
            Orion.snack(activity, R.string.settings_error_link_album_exists)
        }
    }

    fun updateLinkMetadataFile(index: Int, metadataFile: File) {
        //Update link with selected file
        Link.updateLinkMetadataFile(index, metadataFile)

        //Save links
        Link.saveLinks()
    }

    fun moveLinkUp(index: Int) {
        //Move link
        if (!Link.moveLinkUp(index)) return

        //Save links
        Link.saveLinks()
    }

    fun moveLinkDown(index: Int) {
        //Move link
        if (!Link.moveLinkDown(index)) return

        //Save links
        Link.saveLinks()
    }

    fun removeLink(context: Context, index: Int) {
        //Ask for confirmation
        MaterialAlertDialogBuilder(context)
            .setMessage(context.getString(R.string.settings_metadata_links_remove_content, index))
            .setPositiveButton(R.string.settings_metadata_links_remove_action_remove) { dialog, which ->
                //Remove link
                if (!Link.removeLink(index)) return@setPositiveButton

                //Save links
                Link.saveLinks()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    fun addLink(activity: Activity) {
        //Try to add new empty link
        if (!Link.addLink(Link("", ""))) {
            //Not added -> There is another link with the same album
            Orion.snack(activity, R.string.settings_error_link_album_exists)
            return
        }

        //Save links
        Link.saveLinks()
    }

}