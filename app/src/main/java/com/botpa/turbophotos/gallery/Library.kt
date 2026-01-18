package com.botpa.turbophotos.gallery

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.MimeTypeFilter

import com.botpa.turbophotos.gallery.Link.Companion.loadLinks
import com.botpa.turbophotos.gallery.Link.Companion.relinkWithAlbum
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.actions.ActionError
import com.botpa.turbophotos.gallery.actions.ActionHelper
import com.botpa.turbophotos.gallery.dialogs.DialogAlbums
import com.botpa.turbophotos.gallery.dialogs.DialogErrors
import com.botpa.turbophotos.gallery.dialogs.DialogFolders
import com.botpa.turbophotos.gallery.dialogs.DialogInput
import com.botpa.turbophotos.settings.SettingsPairs
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage.getBool

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import java.io.File
import java.util.Locale
import java.util.function.BiConsumer
import java.util.function.Consumer

import kotlin.Array
import kotlin.Boolean
import kotlin.Exception
import kotlin.Int
import kotlin.String
import kotlin.arrayOf
import kotlin.checkNotNull

object Library {

    //Logging
    private const val LOG_LIBRARY = "LIBRARY"

    //Events
    private val onRefresh = ArrayList<RefreshEvent>()
    private val onAction = ArrayList<ActionEvent>()

    //Albums
    private var lastUpdate: Long = 0

    val albumsMap: MutableMap<String, Album> = HashMap() //Uses album path as key for easy finding
    val albums: MutableList<Album> = ArrayList()
    val trashMap: MutableMap<Album, Int> = HashMap() //A map to store the amount of trashed items in each album
    val trash: Album = Album("Trash")
    val all: Album = Album("All")

    //Gallery
    private val recentlyAddedFiles: MutableCollection<File> = HashSet() //List of items recently added that should be ignored when refreshing to avoid duplicates

    val gallery: MutableList<CoonItem> = ArrayList() //Currently open album items (could be filtered or the same items)
    var galleryFilter: String = "*/*" //Mime type used to filter the gallery
        private set


    //Gallery (events)
    private fun invokeOnRefresh(updated: Boolean) {
        for (listener in onRefresh) listener.invoke(updated)
    }

    fun interface RefreshEvent {
        fun invoke(updated: Boolean)
    }

    fun addOnRefreshEvent(listener: RefreshEvent) {
        onRefresh.add(listener)
    }

    fun removeOnRefreshEvent(listener: RefreshEvent) {
        onRefresh.remove(listener)
    }

    //Gallery (sort/load/refresh)
    private fun getMediaCursor(context: Context): Cursor {
        //Get content resolver
        val contentResolver = context.contentResolver

        //Create projection
        val projection = arrayOf<String?>(
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.IS_TRASHED
        )

        //Create selection
        val selection = "${MediaStore.Files.FileColumns.DATE_ADDED} > ? AND (${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"

        //Create selection args
        val selectionArgs = arrayOf<String?>(
            lastUpdate.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        //Create bundle with selection, sorting order & include trashed items
        val queryArgs = Bundle()
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
        queryArgs.putString( ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)

        //Get cursor resolver
        val cursor = contentResolver.query(
            //Collection
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            //Projection
            projection,
            //Query
            queryArgs,
            //Cancellation signal
            null
        )

        //Return cursor
        return cursor!!
    }

    private fun sortLibrary(refresh: Boolean) {
        //Sort albums
        trash.sort()
        all.sort()
        for (album in albums) album.sort()
        sortAlbumsList()

        //Invoke on refresh
        invokeOnRefresh(refresh)
    }

    fun sortLibrary() {
        //Sort albums
        sortLibrary(true)
    }

    private fun loadLibrary(context: Context, reset: Boolean, filterMimeType: String) {
        //Save filter
        galleryFilter = filterMimeType

        //Load links & trash
        loadLinks(reset)

        //Reset
        if (reset) {
            //Reset last update timestamp
            lastUpdate = 0

            //Clear items from albums
            trash.reset()
            all.reset()
            for (album in albums) album.reset()

            //Albums map doesn't get cleared so that the gallery can stay on the selected album on reload :D
        }

        //Get album items
        var itemsAdded = 0
        try {
            getMediaCursor(context).use { cursor ->
                //Save last update timestamp
                lastUpdate = System.currentTimeMillis() / 1000L

                //Get columns for query
                val columnLastModified = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val columnMimeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val columnSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val columnData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val columnIsTrashed = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.IS_TRASHED)

                //Check items
                while (cursor.moveToNext()) {
                    //Get file
                    val file = File(cursor.getString(columnData))

                    //Check if file is in recently added items
                    if (recentlyAddedFiles.contains(file)) {
                        //Is in recently added items -> Remove it & continue
                        recentlyAddedFiles.remove(file)
                        continue
                    }

                    //Get type & apply filter
                    val mimeType = cursor.getString(columnMimeType)
                    if (!MimeTypeFilter.matches(mimeType, filterMimeType)) continue

                    //Get other info
                    val lastModified = cursor.getLong(columnLastModified)
                    val size = cursor.getLong(columnSize)
                    val isTrashed = cursor.getInt(columnIsTrashed) == 1

                    //Check if is trashed
                    val album = getOrCreateAlbumFromItemFile(file)
                    if (isTrashed) {
                        //Trashed -> Create item with trash as album
                        val item = CoonItem(file, trash, lastModified, mimeType!!, size, true)

                        //Add to trash
                        addItemToTrash(item, album)
                    } else {
                        //Not trashed -> Create item with normal album
                        val item = CoonItem(file, album, lastModified, mimeType!!, size, false)

                        //Add to all items list & its album
                        all.add(item)
                        item.album.add(item)
                    }

                    //Added an item
                    itemsAdded++
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_LIBRARY, "Error loading albums: " + e.message)
        }

        //Remove unused albums & fill albums list
        albums.clear()
        val iterator = albumsMap.entries.iterator()
        while (iterator.hasNext()) {
            //Get album
            val album = iterator.next().value

            //Check if album is empty & not used in trash
            if (album.isEmpty() && !trashMap.contains(album)) {
                //Is empty & no trashed items are from this album -> Remove it
                iterator.remove()
            } else {
                //Not empty -> Add it to albums list
                albums.add(album)
            }
        }

        //Sort albums
        sortLibrary(reset || itemsAdded > 0)
    }

    fun loadLibrary(context: Context, reset: Boolean) {
        loadLibrary(context, reset, galleryFilter)
    }

    fun loadLibrary(context: Context, filterType: String) {
        //Filtering requires a reset of albums
        loadLibrary(context, true, filterType)
    }

    //Albums
    private fun createAlbum(imagesFolder: File, name: String): Album {
        //Get folder path
        val folderPath = imagesFolder.absolutePath

        //Create new album
        val album = Album(name, imagesFolder, null)

        //Add album to albums map
        albumsMap[album.imagesPath] = album

        //Get album link
        val link = Link.linksMap.getOrDefault(folderPath, null)
        if (link != null) relinkWithAlbum(link)

        //Return album
        return album
    }

    private fun getOrCreateAlbumFromFolder(folder: File): Album {
        //Temp album var
        val album: Album?

        //Get folder path
        val folderPath = folder.absolutePath

        //Check if an album exists for the path
        if (albumsMap.containsKey(folderPath)) {
            //Album exists -> Take it
            album = albumsMap[folderPath]
            checkNotNull(album)
        } else {
            //Album does not exist -> Create it
            album = createAlbum(folder, folder.name)
        }

        //Return album
        return album
    }

    private fun getOrCreateAlbumFromItemFile(file: File): Album {
        //Get file parent folder
        val fileParent = checkNotNull(file.parentFile)

        //Create album from parent folder
        return getOrCreateAlbumFromFolder(fileParent)
    }

    private fun removeAlbum(index: Int) {
        //Remove album from albums list
        val album = albums.removeAt(index)

        //Check if trash contains items from this album
        if (!trashMap.containsKey(album)) {
            //No items from this album in trash -> Remove album completely
            albumsMap.remove(album.imagesPath)
        }
    }

    private fun sortAlbumsList() {
        albums.sortByDescending { it.get(0).lastModified }
    }

    //Trash
    private fun addItemToTrash(item: CoonItem, originalAlbum: Album) {
        //Add to trash
        trash.addSorted(item)
        trashMap[originalAlbum] = trashMap.getOrDefault(originalAlbum, 0) + 1
    }

    private fun removeItemFromTrash(itemIndex: Int, originalAlbum: Album) {
        //Remove from trash
        trash.remove(itemIndex)

        //Calculate new trash amount
        val newTrashAmount = trashMap.getOrDefault(originalAlbum, 0) - 1
        if (newTrashAmount <= 0) {
            //No more items from this album in trash -> Remove album from trash
            trashMap.remove(originalAlbum)

            //Check if original album still has items
            if (originalAlbum.isEmpty()) {
                //Is empty -> Remove album completely
                albumsMap.remove(originalAlbum.imagesPath)
            }
        } else {
            //Update trash amount
            trashMap[originalAlbum] = newTrashAmount
        }
    }

    //Metadata
    private fun loadMetadataHelper(indicator: LoadingIndicator?, album: Album) {
        //Already loaded
        if (album.hasMetadata()) return

        //Check if images folder & metadata file exist
        if (!album.exists()) return

        //Update load indicator
        indicator?.load(album.name, "metadata")

        //Load metadata
        album.loadMetadata()
    }

    fun loadMetadata(indicator: LoadingIndicator?, album: Album) {
        //Check album
        if (album == all) {
            //All items album -> Load metadata from all albums
            for (a in albums) loadMetadataHelper(indicator, a)
        } else {
            //Normal album -> Load album metadata
            loadMetadataHelper(indicator, album)
        }
    }

    //Gallery
    private fun filterItem(item: CoonItem, filter: String): Boolean {
        //Check item name
        if (item.name.lowercase(Locale.getDefault()).contains(filter)) return true

        //Get metadata
        val metadata = item.getMetadata() ?: return false

        //Check caption
        if (metadata.has("caption")) {
            val caption = metadata.path("caption")
            if (caption.isTextual && caption.asText().lowercase(Locale.getDefault()) .contains(filter)) {
                return true
            }
        }

        //Check labels
        if (metadata.has("labels")) {
            val labels = metadata.path("labels")
            for (i in 0..<labels.size()) {
                if (labels.get(i).asText().lowercase(Locale.getDefault()).contains(filter)) {
                    return true
                }
            }
        }

        //Check text
        if (metadata.has("text")) {
            val text = metadata.path("text")
            for (i in 0..<text.size()) {
                if (text.get(i).asText().lowercase(Locale.getDefault()).contains(filter)) {
                    return true
                }
            }
        }

        //Not found
        return false
    }

    fun filterGallery(filter: String, album: Album) {
        //Check if filtering
        val isFiltering = !filter.isEmpty()

        //Clear items list
        gallery.clear()

        //Look for items that contain the filter
        for (item in album.items) {
            //No filter -> Skip check
            if (!isFiltering) {
                gallery.add(item)
                continue
            }

            //Check if json contains filter
            if (filterItem(item, filter)) gallery.add(item)
        }
    }

    //Actions (events)
    private fun invokeOnAction(action: Action) {
        for (listener in onAction) listener.invoke(action)
    }

    fun interface ActionEvent {
        fun invoke(action: Action)
    }

    fun addOnActionEvent(listener: ActionEvent) {
        onAction.add(listener)
    }

    fun removeOnActionEvent(listener: ActionEvent) {
        onAction.remove(listener)
    }

    //Actions (dialogs)
    fun showActionErrorsDialog(context: Context, action: Action) {
        //Create dialog
        DialogErrors(context, action.errors).buildAndShow()
    }

    fun showSelectAlbumDialog(context: Context, onSelect: Consumer<Album>) {
        //Create dialog
        DialogAlbums(
            context,
            albums,
            onSelect,
            { folder: File -> onSelect.accept(getOrCreateAlbumFromFolder(folder)) }
        ).buildAndShow()
    }

    fun showSelectFolderDialog(context: Context, onSelect: Consumer<File>) {
        //Create folders list
        val externalStorage = Environment.getExternalStorageDirectory()
        val imagesFolder = File(externalStorage, "Pictures")
        val folders = Orion.listFiles(imagesFolder)
        folders.sortBy { it.name.lowercase() }

        //Create dialog
        DialogFolders(context, externalStorage, imagesFolder, folders, onSelect).buildAndShow()
    }

    //Actions (base & util)
    private fun performRemoveFromAll(helper: ActionHelper) {
        //Not in all items list
        if (helper.indexInAll == -1) return

        //Remove item
        all.remove(helper.indexInAll)
    }

    private fun performRemoveFromGallery(helper: ActionHelper, action: Action) {
        //Not in gallery items list
        if (helper.indexInGallery == -1) return

        //Mark it as removed (will get removed in evaluateAction())
        action.removedIndexesInGallery.add(helper.indexInGallery)
    }

    private fun performRemoveFromAlbum(helper: ActionHelper, action: Action, album: Album) {
        //Not in album
        if (helper.indexInAlbum == -1) return

        //Remove item
        album.remove(helper.indexInAlbum)
        action.modifiedAlbums.add(album)

        //Check if album needs to be deleted or sorted
        if (album.isEmpty()) {
            //Album is empty -> Mark it as removed (will get removed in evaluateAction())
            action.removedIndexesInAlbums.add(helper.indexOfAlbum)
        } else if (helper.indexInAlbum == 0) {
            //Album isn't empty & first image was deleted -> Sort albums list in case the order changed
            action.hasSortedAlbumsList = true
        }
    }

    private fun performRemoveFromTrash(helper: ActionHelper, action: Action, originalAlbum: Album) {
        //Not in album
        if (helper.indexInAlbum == -1) return

        //Remove item
        removeItemFromTrash(helper.indexInAlbum, originalAlbum)
        action.trashAction = if (trash.isEmpty()) Action.TRASH_REMOVED else Action.TRASH_UPDATED
    }

    private fun performAction(context: Context, type: Int, items: Array<CoonItem>, onPerformAction: BiConsumer<Action, CoonItem>) {
        //Create action
        val action = Action(type, items)

        //Perform action for each item
        for (item in items) onPerformAction.accept(action, item)

        //Evaluate action
        evaluateAction(context, action)
    }

    private fun evaluateAction(context: Context, action: Action) {
        //Check if gallery items were marked as removed
        if (!action.removedIndexesInGallery.isEmpty()) {
            //Marked as removed -> Sort indexes
            action.removedIndexesInGallery.sortByDescending { it } //Sort from last to first to allow using a foreach

            //Remove items
            for (indexInGallery in action.removedIndexesInGallery) gallery.removeAt(indexInGallery)
        }

        //Check if albums were marked as removed
        if (!action.removedIndexesInAlbums.isEmpty()) {
            //Marked as removed -> Sort indexes
            action.removedIndexesInAlbums.sortByDescending { it } //Sort from last to first to allow using a foreach

            //Remove albums
            for (indexInAlbums in action.removedIndexesInAlbums) {
                //Remove album
                removeAlbum(indexInAlbums)

                //Album was the first -> Mark "all" as updated
                if (indexInAlbums == 0) action.modifiedAlbums.add(all)
            }
        }

        //Check if albums list was marked as sorted
        if (action.hasSortedAlbumsList) {
            //Marked as sorted -> Sort it
            sortAlbumsList()
        }

        //Check if actions failed
        if (!action.errors.isEmpty()) {
            //Actions failed -> Show dialog
            showActionErrorsDialog(context, action)
        }

        //Invoke action
        invokeOnAction(action)
    }

    //Actions (modify items)
    fun renameItem(context: Context, item: CoonItem) {
        //Check if item is in trash
        if (item.isTrashed) return

        //Get file info
        val oldName = item.name
        val extension = Orion.getExtension(oldName)
        val oldNameNoExtension = if (extension.isEmpty()) oldName else oldName.substring(0, oldName.length - (extension.length + 1))

        //Create action
        val action = Action(Action.TYPE_RENAME, arrayOf(item))

        //Ask for a new name
        val dialog = DialogInput(
            context,
            "Rename",
            "New name",
            label@{ newNameNoExtension: String? ->
                //Check if name changed
                if (newNameNoExtension == oldNameNoExtension) {
                    //Name didn't change -> Allow to rename
                    return@label true
                }

                //Check if name is valid
                if (newNameNoExtension!!.isEmpty()) {
                    //Name is empty -> Prevent from renaming
                    Toast.makeText(context, "New name can't be empty.", Toast.LENGTH_SHORT).show()
                    return@label false
                }

                //Get new name & file
                val newName = "$newNameNoExtension.$extension"
                val newFile = File(item.album.imagesFolder, newName)

                //Check if new file already exists
                if (newFile.exists()) {
                    //New file already exists -> Prevent from renaming
                    Toast.makeText(
                        context,
                        "A file named \"$newName\" already exists.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@label false
                }
                true
            },
            label@{ newNameNoExtension: String? ->
                //Check if name changed
                if (newNameNoExtension == oldNameNoExtension) {
                    //Name didn't change -> No need to rename
                    return@label
                }

                //Get new name & file
                val newName = "$newNameNoExtension.$extension"
                val newFile = File(item.album.imagesFolder, newName)

                //Rename file
                val renamed = item.file.renameTo(newFile)
                if (!renamed) {
                    //Failed to rename file
                    action.errors.add(
                        ActionError(
                            item,
                            "Failed to rename \"$oldName\" to \"$newName\"."
                        )
                    )
                    evaluateAction(context, action)
                    return@label
                }

                //Update item
                item.name = newName
                item.file = newFile

                //Copy item metadata to new key
                val album = item.album
                if (album.hasMetadataKey(oldName) && getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                    album.setMetadataKey(newName, album.getMetadataKey(oldName))
                    album.removeMetadataKey(oldName)
                    album.saveMetadata()
                }

                //Evaluate rename
                evaluateAction(context, action)
            }
        )
        dialog.buildAndShow()
        dialog.setText(oldNameNoExtension)
    }

    fun editItem(context: Context, item: CoonItem) {
        //Get URI & mime type
        val uri = Orion.getFileUriFromFilePath(context, item.file.absolutePath)
        val mimeType = item.mimeType

        //Edit
        val intent = Intent(Intent.ACTION_EDIT)
        intent.setDataAndType(uri, mimeType)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun shareItems(context: Context, items: Array<CoonItem>) {
        //No items
        if (items.isEmpty()) return

        //Share items
        val intent: Intent?
        if (items.size == 1) {
            //Share 1 item
            intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_STREAM, Orion.getFileUriFromFilePath(context, items[0].file.getAbsolutePath()))
            intent.type = items[0].mimeType
        } else {
            //Share multiple items
            intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            val URIs = ArrayList<Uri>()
            for (item in items) URIs.add(Orion.getFileUriFromFilePath(context, item.file.absolutePath))
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, URIs)
            intent.type = "*/*"
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    private fun moveItemsInternal(context: Context, items: Array<CoonItem>, newAlbum: Album) {
        performAction(context, Action.TYPE_MOVE, items) { action: Action, item: CoonItem ->
            //Check if item is in trash
            if (item.isTrashed) {
                //Item is in trash
                action.errors.add(ActionError(item, "Item is in the trash."))
                return@performAction
            }

            //Get old album
            val oldAlbum = item.album

            //Check if item is being moved to the same album
            if (newAlbum == oldAlbum) {
                //Already in the destination album
                action.errors.add(ActionError(item, "Item is already in the destination album."))
                return@performAction
            }

            //Move item file
            val newFile = File(newAlbum.imagesPath, item.name)
            if (!Orion.moveFile(item.file, newFile)) {
                //Failed to move file
                action.errors.add(ActionError(item, "Error while moving item file."))
                return@performAction
            }

            //Get action helper
            val helper = action.getHelper(item)

            //Remove item from gallery items list
            performRemoveFromGallery(helper, action)

            //Remove item from old album
            performRemoveFromAlbum(helper, action, oldAlbum)

            //Update item
            item.file = newFile
            item.album = newAlbum

            //Add item to new album
            helper.indexInAlbum = newAlbum.addSorted(item)
            action.modifiedAlbums.add(newAlbum)

            //Check if item was added as the album cover
            if (helper.indexInAlbum == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.hasSortedAlbumsList = true

                //Check if album is in albums list
                if (!albums.contains(newAlbum)) {
                    //Not in albums list -> Add it
                    albums.add(newAlbum)
                }
            }

            //Move item metadata from old album to new album
            if (oldAlbum.hasMetadataKey(item.name) && getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                //Copy metadata to new album
                newAlbum.setMetadataKey(item.name, oldAlbum.getMetadataKey(item.name))
                newAlbum.saveMetadata()

                //Remove metadata from old album
                oldAlbum.removeMetadataKey(item.name)
                oldAlbum.saveMetadata()
            }
        }
    }

    fun moveItems(context: Context, items: Array<CoonItem>) {
        //Show album selection dialog
        showSelectAlbumDialog(context, { newAlbum: Album -> moveItemsInternal(context, items, newAlbum) })
    }

    private fun copyItemsInternal(context: Context, items: Array<CoonItem>, newAlbum: Album) {
        performAction(context, Action.TYPE_COPY, items) { action: Action, item: CoonItem ->
            //Check if item is in trash
            if (item.isTrashed) {
                //Item is in trash
                action.errors.add(ActionError(item, "Item is in the trash."))
                return@performAction
            }

            //Get old album
            val oldAlbum = item.album

            //Check if item is being moved to the same album
            if (newAlbum == oldAlbum) {
                //Already in the destination album
                action.errors.add(ActionError(item, "Item is already in the destination album."))
                return@performAction
            }

            //Copy item file
            val newFile = File(newAlbum.imagesPath, item.name)
            if (!Orion.cloneFile(context, item.file, newFile)) {
                //Failed to copy file
                action.errors.add(ActionError(item, "Error while copying item file."))
                return@performAction
            }
            recentlyAddedFiles.add(newFile) //Add file to recently added to prevent duplicates

            //Create new item
            val newItem = CoonItem(
                newFile,
                newAlbum,
                item.lastModified,
                item.mimeType,
                item.size,
                item.isTrashed
            )

            //Add item to new album
            val indexInAlbum = newAlbum.addSorted(newItem)
            action.modifiedAlbums.add(newAlbum)

            //Check if item was added as the album cover
            if (indexInAlbum == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.hasSortedAlbumsList = true

                //Check if album is in albums list
                if (!albums.contains(newAlbum)) {
                    //Not in albums list -> Add it
                    albums.add(newAlbum)
                }
            }

            //Copy item metadata from old album to new album
            if (oldAlbum.hasMetadataKey(item.name) && getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                newAlbum.setMetadataKey(item.name, oldAlbum.getMetadataKey(item.name))
                newAlbum.saveMetadata()
            }
        }
    }

    fun copyItems(context: Context, items: Array<CoonItem>) {
        //Show album selection dialog
        showSelectAlbumDialog(context, { newAlbum: Album -> copyItemsInternal(context, items, newAlbum) })
    }

    private fun prepareItemURIs(context: Context?, items: Array<CoonItem>, action: Action): MutableMap<Uri, CoonItem> {
        val pendingItems: MutableMap<Uri, CoonItem> = HashMap()
        for (item in items) {
            //Check if item is already trashed
            when (action.type) {
                Action.TYPE_TRASH -> {
                    //Not trashed
                    if (!item.isTrashed) break

                    //Trashed
                    action.errors.add(ActionError(item, "Item is already in the trash."))
                    continue
                }

                Action.TYPE_RESTORE -> {
                    //Trashed
                    if (item.isTrashed) break

                    //Not trashed
                    action.errors.add(ActionError(item, "Item is not in the trash."))
                    continue
                }
            }

            //Get item URI
            val uri = Orion.getMediaUriFromFilePath(context, item.file.absolutePath)

            //Check if URI is valid
            if (uri == null) {
                //Not valid
                action.errors.add(ActionError(item, "Invalid item URI."))
                continue
            }

            //Add it to pending items
            pendingItems[uri] = item
        }
        return pendingItems
    }

    fun trashItems(activity: GalleryActivity, items: Array<CoonItem>, launcher: ActivityResultLauncher<IntentSenderRequest>): Action? {
        //Create action
        val action = Action(Action.TYPE_TRASH, items)

        //Get item URIs
        action.trashPending = prepareItemURIs(activity, items, action)

        //Check if there are any pending items
        if (action.trashPending.isEmpty()) {
            //No pending items -> Ignore action
            evaluateAction(activity, action)
            return null
        }

        //Create trash request
        val pendingIntent = MediaStore.createTrashRequest(activity.contentResolver, action.trashPending.keys, true) //True -> Move to trash
        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        launcher.launch(request)

        //Return action (the ones that will get trashed)
        return action
    }

    fun onTrashItemsResult(context: Context, action: Action) {
        //Update items
        for (entry in action.trashPending.entries) {
            //Get URI & item
            val uri: Uri = entry.key
            val item = entry.value

            //Get new item file
            val newFilePath = Orion.getFilePathFromMediaUri(context, uri)
            val newFile = File(newFilePath ?: "")
            if (!newFile.exists()) {
                //New file does not exist -> Something failed
                action.errors.add(ActionError(item, "New file does not exist."))
                continue
            }

            //Get original album & action helper
            val originalAlbum = item.album
            val helper = action.getHelper(item)

            //Update item
            item.file = newFile
            item.album = trash
            item.isTrashed = true

            //Add item to trash album
            addItemToTrash(item, originalAlbum)
            if (action.trashAction == Action.TRASH_NONE) {
                //First change to trash this action -> Check if trash was added to list or just updated
                action.trashAction = if (trash.size() == 1) Action.TRASH_ADDED else Action.TRASH_UPDATED
            }

            //Remove item from all
            performRemoveFromAll(helper)

            //Remove item from gallery items list
            performRemoveFromGallery(helper, action)

            //Remove item from album
            performRemoveFromAlbum(helper, action, originalAlbum) //Remove from album after adding to trash cause, if both trash & album are empty, the album gets removed from albumsMap
        }

        //Evaluate action
        evaluateAction(context, action)
    }

    fun restoreItems(activity: GalleryActivity, items: Array<CoonItem>, launcher: ActivityResultLauncher<IntentSenderRequest>): Action? {
        //Create action
        val action = Action(Action.TYPE_RESTORE, items)

        //Get item URIs
        action.trashPending = prepareItemURIs(activity, items, action)

        //Check if there are any pending items
        if (action.trashPending.isEmpty()) {
            //No pending items -> Ignore action
            evaluateAction(activity, action)
            return null
        }

        //Create restore request
        val pendingIntent = MediaStore.createTrashRequest(activity.contentResolver, action.trashPending.keys, false) //False -> Restore from trash
        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        launcher.launch(request)

        //Return action (the ones that will get restored)
        return action
    }

    fun onRestoreItemsResult(context: Context, action: Action) {
        //Update items
        for (entry in action.trashPending.entries) {
            //Get URI & item
            val uri: Uri = entry.key
            val item = entry.value

            //Get new item file
            val newFilePath = Orion.getFilePathFromMediaUri(context, uri)
            val newFile = File(newFilePath ?: "")
            if (!newFile.exists()) {
                //New file does not exist -> Something failed
                action.errors.add(ActionError(item, "New file does not exist."))
                continue
            }

            //Get original album & action helper
            val originalAlbum = getOrCreateAlbumFromItemFile(newFile)
            val helper = action.getHelper(item)

            //Update item
            item.file = newFile
            item.album = originalAlbum
            item.isTrashed = false

            //Add to all & original album
            helper.indexInAll = all.addSorted(item)
            action.modifiedAlbums.add(all)
            helper.indexInAlbum = originalAlbum.addSorted(item)
            action.modifiedAlbums.add(originalAlbum)

            //Check if item was added as the album cover
            if (helper.indexInAlbum == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.hasSortedAlbumsList = true

                //Check if album is in albums list
                if (!albums.contains(originalAlbum)) {
                    //Not in albums list -> Add it
                    albums.add(originalAlbum)
                }
            }

            //Remove item from gallery items list
            performRemoveFromGallery(helper, action)

            //Remove item from trash
            performRemoveFromTrash(helper, action, originalAlbum) //Remove from trash after adding to album cause, if both trash & album are empty, the album gets removed from albumsMap
        }

        //Evaluate action
        evaluateAction(context, action)
    }

    private fun deleteItemsInternal(context: Context, items: Array<CoonItem>) {
        performAction(context, Action.TYPE_DELETE, items) { action: Action, item: CoonItem ->
            //Delete item file
            if (!Orion.deleteFile(item.file)) {
                //Failed to delete file
                action.errors.add(ActionError(item, "Error while deleting item file."))
                return@performAction
            }

            //Get action helper
            val helper = action.getHelper(item)

            //Remove item from all
            performRemoveFromAll(helper)

            //Remove item from gallery items list
            performRemoveFromGallery(helper, action)

            //Check if item is in the trash to remove it & get its album
            val album: Album
            if (item.isTrashed) {
                //Get original album (before being trashed)
                album = getOrCreateAlbumFromItemFile(item.file)

                //Remove item from trash
                performRemoveFromTrash(helper, action, album)
            } else {
                //Get item album
                album = item.album

                //Remove item from album
                performRemoveFromAlbum(helper, action, album)
            }

            //Delete item metadata from album
            if (album.hasMetadataKey(item.name) && getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                album.removeMetadataKey(item.name)
                album.saveMetadata()
            }
        }
    }

    fun deleteItems(context: Context, items: Array<CoonItem>) {
        //Create message
        val message = StringBuilder()
        message.append("Are you sure you want to permanently delete ")
        if (items.size == 1) {
            message.append("\"")
            message.append(items[0].name)
            message.append("\"")
        } else {
            message.append(items.size)
            message.append(" items")
        }
        message.append("?")

        //Show confirmation dialog
        MaterialAlertDialogBuilder(context)
            .setMessage(message.toString())
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { dialog, whichButton -> deleteItemsInternal(context, items) }
            .show()
    }

}
