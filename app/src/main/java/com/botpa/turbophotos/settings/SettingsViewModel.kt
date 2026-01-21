package com.botpa.turbophotos.settings

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.botpa.turbophotos.gallery.Link
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel : ViewModel() {

    //Links
    enum class PickerAction { SelectFolder, SelectFile, CreateFile }

    //App settings
    var appModifyMetadata by mutableStateOf(Storage.getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION))

    //Home settings
    var homeItemsPerRow by mutableFloatStateOf(Storage.getInt(SettingsPairs.HOME_ITEMS_PER_ROW).toFloat())

    //Album settings
    var albumItemsPerRow by mutableFloatStateOf(Storage.getInt(SettingsPairs.ALBUM_ITEMS_PER_ROW).toFloat())
    var albumShowMissingMetadataIcon by  mutableStateOf(Storage.getBool(SettingsPairs.ALBUM_SHOW_MISSING_METADATA_ICON))

    //Link item file picker actions
    var filePickerIndex by  mutableIntStateOf(-1)
    var filePickerAction by mutableStateOf(PickerAction.SelectFolder)


    //App settings
    fun backupSettings(activity: Activity) {
        Orion.snack(activity, "Backup (not implemented)")
    }

    fun restoreSettings(activity: Activity) {
        Orion.snack(activity, "Restore (not implemented)")
    }

    fun updateAppModifyMetadata(isChecked: Boolean) {
        appModifyMetadata = isChecked
        Storage.putBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION.key, isChecked)
    }

    //Home settings
    fun updateHomeItemsPerRow(value: Float) {
        homeItemsPerRow = value
    }

    fun saveHomeItemsPerRow() {
        Storage.putInt(SettingsPairs.HOME_ITEMS_PER_ROW.key, homeItemsPerRow.toInt())
    }

    //Album settings
    fun updateAlbumItemsPerRow(value: Float) {
        albumItemsPerRow = value
    }

    fun saveAlbumItemsPerRow() {
        Storage.putInt(SettingsPairs.ALBUM_ITEMS_PER_ROW.key, albumItemsPerRow.toInt())
    }

    fun updateAlbumShowMissingMetadataIcon(isChecked: Boolean) {
        albumShowMissingMetadataIcon = isChecked
        Storage.putBool(SettingsPairs.ALBUM_SHOW_MISSING_METADATA_ICON.key, isChecked)
    }

    //Link item file picker actions
    fun handleFileResult(uri: Uri, context: Context, activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //Parse file path from URI
                val path = Orion.getFilePathFromDocumentProviderUri(context, uri) ?: throw Exception("Path was null")
                val file = File(path)

                //Update the link based on the action
                val link = Link.links[filePickerIndex]
                when (filePickerAction) {
                    //Select folder
                    PickerAction.SelectFolder -> {
                        val updated = Link.updateLinkFolder(filePickerIndex, file)
                        if (!updated) {
                            Orion.snack(activity, "Album already exists")
                            return@launch
                        }
                    }

                    //Select file
                    PickerAction.SelectFile -> {
                        //Update link with selected file
                        Link.updateLinkFile(filePickerIndex, file)
                    }

                    //Create file
                    PickerAction.CreateFile -> {
                        //Create file based in album name
                        var metadataFile: File
                        var name: String
                        var i = 0
                        do {
                            name = "${link.albumFolder.name.lowercase().replace(" ", "-")}${if (i > 0) " ($i)" else ""}.json"
                            metadataFile = File("${file.absolutePath}/${name}")
                            i++
                        } while (metadataFile.exists())
                        Orion.writeFile(metadataFile, "{}")

                        //Update link with created file
                        Link.updateLinkFile(filePickerIndex, metadataFile)
                    }
                }

                //Save links
                Link.saveLinks()
            } catch (e: Exception) {
                //Feedback toast
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

}