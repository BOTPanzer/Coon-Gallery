package com.botpa.turbophotos.gallery

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
import com.botpa.turbophotos.gallery.modals.DialogAlbums
import com.botpa.turbophotos.gallery.modals.DialogErrors
import com.botpa.turbophotos.gallery.modals.DialogInput
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage.getBool

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import java.io.File
import java.util.Locale

import kotlin.Array
import kotlin.Boolean
import kotlin.Exception
import kotlin.Int
import kotlin.String
import kotlin.arrayOf
import kotlin.checkNotNull

enum class SearchMethod { ContainsWords, ContainsText }

object Library {

    //Logging
    private const val LOGGING_TAG = "LIBRARY"

    //Events
    private val onRefresh = ArrayList<RefreshEvent>()
    private val onAction = ArrayList<ActionEvent>()

    //Library
    private val recentlyAddedFiles: MutableCollection<File> = HashSet() //Recently added items that should be ignored when refreshing to avoid duplicates
    private var lastUpdate: Long = 0

    var libraryFilter: String = "*/*" //Mime type used to filter the library
        private set

    //Albums
    private val _albumsMap: MutableMap<String, Album> = HashMap() //Uses album path as key for easy finding
    private val _albums: MutableList<Album> = ArrayList()

    val all: Album = Album("All")
    val favourites: Album = Album("Favourites")

    val albumsMap: Map<String, Album>
        get() = _albumsMap
    val albums: List<Album>
        get() = _albums

    //Trash
    private val trashMap: MutableMap<Album, Int> = HashMap() //Stores the amount of trashed items in each album

    val trash: Album = Album("Trash")

    //Gallery
    private var galleryAlbum: Album? = null
    private val _gallery: MutableList<CoonItem> = ArrayList() //Currently open album items (could be filtered)

    val gallery: List<CoonItem>
        get() = _gallery



    //Library (events)
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

    //Library (sort/load/refresh)
    private fun getMediaCursor(context: Context): Cursor {
        //Get content resolver
        val contentResolver = context.contentResolver

        //Create projection
        val projection = arrayOf<String?>(
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.IS_TRASHED,
            MediaStore.Files.FileColumns.IS_FAVORITE
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
        favourites.sort()
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
        libraryFilter = filterMimeType

        //Load links & trash
        loadLinks(reset)

        //Reset
        if (reset) {
            //Reset last update timestamp
            lastUpdate = 0

            //Clear items from albums
            all.reset()
            favourites.reset()
            trash.reset()
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
                val columnIsFavourite = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.IS_FAVORITE)

                //Check items
                while (cursor.moveToNext()) {
                    //Get file
                    val file = File(cursor.getString(columnData))

                    //Check if file is marked as recently added
                    if (recentlyAddedFiles.contains(file)) {
                        //Is recently added -> Unmark it & continue
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
                    val isFavourite = cursor.getInt(columnIsFavourite) == 1

                    //Check if is trashed
                    val album = getOrCreateAlbumFromItemFile(file)
                    if (isTrashed) {
                        //Trashed -> Create item with trash as album
                        val item = CoonItem(file, trash, lastModified, mimeType!!, size, true, isFavourite)

                        //Add to trash
                        addItemToTrash(item, album)
                    } else {
                        //Not trashed -> Create item with normal album
                        val item = CoonItem(file, album, lastModified, mimeType!!, size, false, isFavourite)

                        //Add to all items list & its album
                        all.add(item)
                        if (isFavourite) favourites.add(item)
                        item.album.add(item)
                    }

                    //Added an item
                    itemsAdded++
                }
            }
        } catch (e: Exception) {
            Log.e(LOGGING_TAG, "Error loading albums: ${e.message}")
        }

        //Remove unused albums & populate albums list
        _albums.clear()
        val iterator = _albumsMap.entries.iterator()
        while (iterator.hasNext()) {
            //Get album
            val album = iterator.next().value

            //Check what to do with album
            if (!album.isEmpty()) {
                //Not empty -> Add it to albums list
                _albums.add(album)
            } else if (!isAlbumInUse(album)) {
                //Not in use -> Remove it
                iterator.remove()
            }
        }

        //Sort albums
        sortLibrary(reset || itemsAdded > 0)
    }

    fun loadLibrary(context: Context, reset: Boolean) {
        loadLibrary(context, reset, libraryFilter)
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
        _albumsMap[album.imagesPath] = album

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
        val album = _albums.removeAt(index)

        //Remove album completely if not being used
        removeAlbumFromMapIfSafe(album)
    }

    private fun sortAlbumsList() {
        _albums.sortByDescending { it.get(0).lastModified }
    }

    private fun isAlbumInUse(album: Album): Boolean {
        return !album.isEmpty() || trashMap.contains(album)
    }

    private fun removeAlbumFromMapIfSafe(album: Album) {
        //Album is in use
        if (isAlbumInUse(album)) return

        //Delete album
        _albumsMap.remove(album.imagesPath)
    }

    //Trash
    private fun addItemToTrash(item: CoonItem, originalAlbum: Album): Int {
        //Add to trash
        val index = trash.addSorted(item)
        trashMap[originalAlbum] = trashMap.getOrDefault(originalAlbum, 0) + 1
        return index
    }

    private fun removeItemFromTrash(itemIndex: Int, originalAlbum: Album) {
        //Remove from trash
        trash.remove(itemIndex)

        //Calculate new trash amount
        val newTrashAmount = trashMap.getOrDefault(originalAlbum, 0) - 1
        if (newTrashAmount <= 0) {
            //No more items from this album in trash -> Remove album from trash
            trashMap.remove(originalAlbum)

            //Remove album completely if not being used
            removeAlbumFromMapIfSafe(originalAlbum)
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
    private fun filterItemText(caption: String, labels: List<String>, texts: List<String>, filter: String): Boolean {
        //Check caption
        if (caption.contains(filter)) return true

        //Check labels
        for (label in labels) {
            if (label.contains(filter)) return true
        }

        //Check text
        for (text in texts) {
            if (text.contains(filter)) return true
        }

        //Not found
        return false
    }

    private fun filterItemWords(caption: String, labels: List<String>, texts: List<String>, filter: String): Boolean {
        //Get filter words
        val words = filter.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

        //Check if all words are contained
        return words.all { word ->
            //Check caption
            val inCaption = caption.split(" ").any { it.equals(word, ignoreCase = true) }

            //Check labels
            val inLabels = labels.any { it.equals(word, ignoreCase = true) }

            //Check text
            val inTexts = texts.any { it.equals(word, ignoreCase = true) }

            //Contained in any
            inCaption || inLabels || inTexts
        }
    }

    private fun filterItem(item: CoonItem, filter: String, method: SearchMethod): Boolean {
        //Check item name
        if (item.name.lowercase(Locale.getDefault()).contains(filter)) return true

        //Get metadata
        val metadata = item.getMetadata() ?: return false

        //Get caption
        val caption: String = (metadata.get("caption")?.asText() ?: "").lowercase(Locale.getDefault())

        //Get labels
        val labels: MutableList<String> = ArrayList()
        if (metadata.has("labels")) {
            val value = metadata.get("labels")
            for (i in 0..<value.size()) {
                labels.add(value.get(i).asText().lowercase(Locale.getDefault()))
            }
        }

        //Get text
        val text: MutableList<String> = ArrayList()
        if (metadata.has("text")) {
            val value = metadata.path("text")
            for (i in 0..<value.size()) {
                text.add(value.get(i).asText().lowercase(Locale.getDefault()))
            }
        }

        //Filter
        return if (method == SearchMethod.ContainsText) {
            //Contains whole text
            filterItemText(caption, labels, text, filter)
        } else {
            //Contains all words
            filterItemWords(caption, labels, text, filter)
        }
    }

    fun filterGallery(filter: String, album: Album, method: SearchMethod) {
        //Check if filtering
        val isFiltering = !filter.isEmpty()

        //Save album & reset gallery
        galleryAlbum = album
        _gallery.clear()

        //Look for items that contain the filter
        for (item in album.items) {
            //Filtering & not valid -> Skip item
            if (isFiltering && !filterItem(item, filter.lowercase(Locale.getDefault()), method)) continue

            //Add item
            _gallery.add(item)
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
    private fun showSelectAlbumDialog(context: Context, onSelect: (Album) -> Unit) {
        //Create dialog
        DialogAlbums(context, albums, onSelect, { folder: File -> onSelect(getOrCreateAlbumFromFolder(folder)) }).buildAndShow()
    }

    //Actions (base & util)
    private fun performRemoveFromAll(action: Action, indexInAll: Int) {
        //Not in all items list
        if (indexInAll == -1) return

        //Remove item
        all.remove(indexInAll)
        action.modifiedAlbums.add(all)
    }

    private fun performRemoveFromFavourites(action: Action, indexInFavourites: Int) {
        //Not in favourite items list
        if (indexInFavourites == -1) return

        //Remove item
        favourites.remove(indexInFavourites)
        action.modifiedAlbums.add(favourites)
    }

    private fun performRemoveFromGallery(action: Action, indexInGallery: Int) {
        //Not in gallery items list
        if (indexInGallery == -1) return

        //Mark it as removed (will get removed in evaluateAction())
        action.removedIndexesInGallery.add(indexInGallery)
    }

    private fun performRemoveFromAlbum(action: Action, indexInAlbum: Int, indexOfAlbum: Int, album: Album) {
        //Not in album
        if (indexInAlbum == -1) return

        //Remove item
        album.remove(indexInAlbum)
        action.modifiedAlbums.add(album)

        //Check if album needs to be deleted or sorted
        if (album.isEmpty()) {
            //Album is empty -> Mark it as removed (will get removed in evaluateAction())
            action.removedIndexesInAlbums.add(indexOfAlbum)
        } else if (indexInAlbum == 0) {
            //Album isn't empty & first image was deleted -> Sort albums list in case the order changed
            action.hasSortedAlbumsList = true
        }
    }

    private fun performRemoveFromTrash(action: Action, indexInTrash: Int, originalAlbum: Album) {
        //Not in album
        if (indexInTrash == -1) return

        //Remove item
        removeItemFromTrash(indexInTrash, originalAlbum)
        action.modifiedAlbums.add(trash)
    }

    private fun performCheckForAlbumChanges(action: Action, indexInAlbum: Int, album: Album) {
        //Check if item was added as the album cover
        if (indexInAlbum == 0) {
            //Added as the album cover -> Sort albums list in case the order changed
            action.hasSortedAlbumsList = true

            //Check if album is in albums list
            if (!albums.contains(album)) {
                //Not in albums list -> Add it
                _albums.add(album)
            }
        }
    }

    private fun performAddToAlbum(action: Action, item: CoonItem, album: Album): Int {
        val indexInAlbum = album.addSorted(item)
        action.modifiedAlbums.add(album)
        return indexInAlbum
    }

    private fun performAction(context: Context, type: Int, items: Array<CoonItem>, onPerformAction: (Action, CoonItem) -> Unit) {
        //Create action
        val action = Action(type, items)

        //Perform action for each item
        for (item in items) onPerformAction(action, item)

        //Evaluate action
        evaluateAction(context, action)
    }

    private fun evaluateAction(context: Context, action: Action) {
        //Check if gallery items were marked as removed
        if (!action.removedIndexesInGallery.isEmpty()) {
            //Marked as removed -> Sort indexes
            action.removedIndexesInGallery.sortByDescending { it } //Sort from last to first to allow using a foreach

            //Remove items
            for (indexInGallery in action.removedIndexesInGallery) _gallery.removeAt(indexInGallery)
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
            DialogErrors(context, action.errors).buildAndShow()
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
        val oldNameNoExtension = if (extension.isEmpty()) oldName else oldName.dropLast(extension.length + 1)

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
                if (album.hasMetadataKey(oldName) && getBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
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
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun shareItems(context: Context, items: Array<CoonItem>) {
        //No items
        if (items.isEmpty()) return

        //Share items
        val intent: Intent?
        if (items.size == 1) {
            //Share 1 item
            intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, Orion.getFileUriFromFilePath(context, items[0].file.absolutePath))
                type = items[0].mimeType
            }
        } else {
            //Share multiple items
            val uris = ArrayList<Uri?>()
            for (item in items) uris.add(Orion.getFileUriFromFilePath(context, item.file.absolutePath))
            intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                type = "*/*"
            }
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun setItemAs(context: Context, item: CoonItem) {
        //Get URI & mime type
        val uri = Orion.getFileUriFromFilePath(context, item.file.absolutePath)
        val mimeType = item.mimeType

        //Set as
        val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    private fun moveItemsInternal(context: Context, items: Array<CoonItem>, newAlbum: Album) {
        performAction(context, Action.TYPE_MOVE, items) { action: Action, item: CoonItem ->
            //Check if item is in trash
            if (item.isTrashed) {
                //Item is in trash -> Error
                action.errors.add(ActionError(item, "Item is in the trash."))
                return@performAction
            }

            //Get old album
            val oldAlbum = item.album

            //Check if item is being moved to the same album
            if (newAlbum == oldAlbum) {
                //Already in the destination album -> Error
                action.errors.add(ActionError(item, "Item is already in the destination album."))
                return@performAction
            }

            //Move item file
            val newFile = File(newAlbum.imagesPath, item.name)
            if (!item.file.renameTo(newFile)) {
                //Failed to move file -> Error
                action.errors.add(ActionError(item, "Error while moving item file."))
                return@performAction
            }

            //Get action helper
            val helper = action.getHelper(item)

            //Remove item from gallery & old album
            performRemoveFromGallery(action, helper.indexInGallery)
            performRemoveFromAlbum(action, helper.indexInAlbum, helper.indexOfAlbum, oldAlbum)

            //Update item
            item.file = newFile
            item.album = newAlbum

            //Add item to new album
            helper.indexInAlbum = performAddToAlbum(action, item, newAlbum)

            //Check for album changes
            performCheckForAlbumChanges(action, helper.indexInAlbum, newAlbum)

            //Move item metadata from old album to new album
            if (oldAlbum.hasMetadataKey(item.name) && getBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
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
                //Item is in trash -> Error
                action.errors.add(ActionError(item, "Item is in the trash."))
                return@performAction
            }

            //Get old album
            val oldAlbum = item.album

            //Check if item is being moved to the same album
            if (newAlbum == oldAlbum) {
                //Already in the destination album -> Error
                action.errors.add(ActionError(item, "Item is already in the destination album."))
                return@performAction
            }

            //Copy item file
            val newFile = File(newAlbum.imagesPath, item.name)
            if (!Orion.cloneFile(context, item.file, newFile)) {
                //Failed to copy file -> Error
                action.errors.add(ActionError(item, "Error while copying item file."))
                return@performAction
            }
            recentlyAddedFiles.add(newFile) //Mark file as recently added to prevent duplicates on refresh

            //Create new item
            val newItem = CoonItem(newFile, newAlbum, item.lastModified, item.mimeType, item.size, item.isTrashed, false)

            //Add item to new album
            val indexInAlbum = performAddToAlbum(action, newItem, newAlbum)

            //Check for album changes
            performCheckForAlbumChanges(action, indexInAlbum, newAlbum)

            //Copy item metadata from old album to new album
            if (oldAlbum.hasMetadataKey(item.name) && getBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                newAlbum.setMetadataKey(item.name, oldAlbum.getMetadataKey(item.name))
                newAlbum.saveMetadata()
            }
        }
    }

    fun copyItems(context: Context, items: Array<CoonItem>) {
        //Show album selection dialog
        showSelectAlbumDialog(context, { newAlbum: Album -> copyItemsInternal(context, items, newAlbum) })
    }

    private fun prepareItemURIs(context: Context, action: Action): MutableMap<Uri, CoonItem> {
        //Pending items map
        val pendingItems: MutableMap<Uri, CoonItem> = HashMap()

        //Get item URIs
        for (item in action.items) {
            //Check if item is valid for action
            when (action.type) {
                Action.TYPE_TRASH -> {
                    //Item is trashed
                    if (item.isTrashed) {
                        action.errors.add(ActionError(item, "Item is already in the trash."))
                        continue
                    }
                }
                Action.TYPE_RESTORE -> {
                    //Item is not trashed
                    if (!item.isTrashed) {
                        action.errors.add(ActionError(item, "Item is not in the trash."))
                        continue
                    }
                }
                Action.TYPE_FAVOURITE -> {
                    //Item is favourited
                    if (item.isFavourite) {
                        action.errors.add(ActionError(item, "Item is already favourited."))
                        continue
                    }
                }
                Action.TYPE_UNFAVOURITE -> {
                    //Item is not favourited
                    if (!item.isFavourite) {
                        action.errors.add(ActionError(item, "Item is not favourited."))
                        continue
                    }
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

    fun favouriteItems(activity: BaseActivity, items: Array<CoonItem>, launcher: ActivityResultLauncher<IntentSenderRequest>): Action? {
        //Create action
        val action = Action(Action.TYPE_FAVOURITE, items)

        //Get item URIs
        action.pending = prepareItemURIs(activity, action)

        //Check if there are any pending items
        if (action.pending.isEmpty()) {
            //No pending items -> Ignore action
            evaluateAction(activity, action)
            return null
        }

        //Create favourite request
        val pendingIntent = MediaStore.createFavoriteRequest(activity.contentResolver, action.pending.keys, true) //True -> Favourite
        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        launcher.launch(request)

        //Return action (the ones that will get favourited)
        return action
    }

    fun onFavouriteItemsResult(context: Context, action: Action) {
        //Update items
        for (entry in action.pending.entries) {
            //Get item & action helper
            val item = entry.value
            val helper = action.getHelper(item)

            //Update item
            item.isFavourite = true

            //Add item to favourites
            helper.indexInFavourites = performAddToAlbum(action, item, favourites)

            //Mark as modified to update star icon
            action.modifiedIndexesInGallery.add(helper.indexInGallery)
        }

        //Evaluate action
        evaluateAction(context, action)
    }

    fun unfavouriteItems(activity: BaseActivity, items: Array<CoonItem>, launcher: ActivityResultLauncher<IntentSenderRequest>): Action? {
        //Create action
        val action = Action(Action.TYPE_UNFAVOURITE, items)

        //Get item URIs
        action.pending = prepareItemURIs(activity, action)

        //Check if there are any pending items
        if (action.pending.isEmpty()) {
            //No pending items -> Ignore action
            evaluateAction(activity, action)
            return null
        }

        //Create unfavourite request
        val pendingIntent = MediaStore.createFavoriteRequest(activity.contentResolver, action.pending.keys, false) //False -> Unfavourite
        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        launcher.launch(request)

        //Return action (the ones that will get unfavourited)
        return action
    }

    fun onUnfavouriteItemsResult(context: Context, action: Action) {
        //Update items
        for (entry in action.pending.entries) {
            //Get item & action helper
            val item = entry.value
            val helper = action.getHelper(item)

            //Update item
            item.isFavourite = false

            //Remove item from favourites
            performRemoveFromFavourites(action, helper.indexInFavourites)

            //Check gallery album
            if (galleryAlbum == favourites) {
                //Favourites album only shows favourites
                performRemoveFromGallery(action, helper.indexInGallery)
            } else {
                //Mark as modified to update star icon
                action.modifiedIndexesInGallery.add(helper.indexInGallery)
            }
        }

        //Evaluate action
        evaluateAction(context, action)
    }

    fun trashItems(activity: BaseActivity, items: Array<CoonItem>, launcher: ActivityResultLauncher<IntentSenderRequest>): Action? {
        //Create action
        val action = Action(Action.TYPE_TRASH, items)

        //Get item URIs
        action.pending = prepareItemURIs(activity, action)

        //Check if there are any pending items
        if (action.pending.isEmpty()) {
            //No pending items -> Ignore action
            evaluateAction(activity, action)
            return null
        }

        //Create trash request
        val pendingIntent = MediaStore.createTrashRequest(activity.contentResolver, action.pending.keys, true) //True -> Move to trash
        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        launcher.launch(request)

        //Return action (the ones that will get trashed)
        return action
    }

    fun onTrashItemsResult(context: Context, action: Action) {
        //Update items
        for (entry in action.pending.entries) {
            //Get URI & item
            val uri: Uri = entry.key
            val item = entry.value

            //Get new item file
            val newFilePath = Orion.getFilePathFromMediaUri(context, uri)
            if (newFilePath == null) {
                //New file path is invalid -> Error
                action.errors.add(ActionError(item, "Could not resolve file path."))
                continue
            }
            val newFile = File(newFilePath)
            if (!newFile.exists()) {
                //New file does not exist -> Error
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

            //Add item to trash
            helper.indexInTrash = addItemToTrash(item, originalAlbum)
            action.modifiedAlbums.add(trash)

            //Remove item from all, favourites, gallery & album
            performRemoveFromAll(action, helper.indexInAll)
            performRemoveFromFavourites(action, helper.indexInFavourites)
            performRemoveFromGallery(action, helper.indexInGallery)
            performRemoveFromAlbum(action, helper.indexInAlbum, helper.indexOfAlbum, originalAlbum) //Remove from album after adding to trash cause, if both trash & album are empty, the album gets removed from albumsMap
        }

        //Evaluate action
        evaluateAction(context, action)
    }

    fun restoreItems(activity: BaseActivity, items: Array<CoonItem>, launcher: ActivityResultLauncher<IntentSenderRequest>): Action? {
        //Create action
        val action = Action(Action.TYPE_RESTORE, items)

        //Get item URIs
        action.pending = prepareItemURIs(activity, action)

        //Check if there are any pending items
        if (action.pending.isEmpty()) {
            //No pending items -> Ignore action
            evaluateAction(activity, action)
            return null
        }

        //Create restore request
        val pendingIntent = MediaStore.createTrashRequest(activity.contentResolver, action.pending.keys, false) //False -> Restore from trash
        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        launcher.launch(request)

        //Return action (the ones that will get restored)
        return action
    }

    fun onRestoreItemsResult(context: Context, action: Action) {
        //Update items
        for (entry in action.pending.entries) {
            //Get URI & item
            val uri: Uri = entry.key
            val item = entry.value

            //Get new item file
            val newFilePath = Orion.getFilePathFromMediaUri(context, uri)
            if (newFilePath == null) {
                //New file path is invalid -> Error
                action.errors.add(ActionError(item, "Could not resolve file path."))
                continue
            }
            val newFile = File(newFilePath)
            if (!newFile.exists()) {
                //New file does not exist -> Error
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
            helper.indexInAll = performAddToAlbum(action, item, all)
            helper.indexInFavourites = performAddToAlbum(action, item, favourites)
            helper.indexInAlbum = performAddToAlbum(action, item, originalAlbum)

            //Check for album changes
            performCheckForAlbumChanges(action, helper.indexInAlbum, originalAlbum)

            //Remove item from gallery & trash
            performRemoveFromGallery(action, helper.indexInGallery)
            performRemoveFromTrash(action, helper.indexInTrash, originalAlbum) //Remove from trash after adding to album cause, if both trash & album are empty, the album gets removed from albumsMap
        }

        //Evaluate action
        evaluateAction(context, action)
    }

    private fun deleteItemsInternal(context: Context, items: Array<CoonItem>) {
        performAction(context, Action.TYPE_DELETE, items) { action: Action, item: CoonItem ->
            //Delete item file
            if (!item.file.delete()) {
                //Failed to delete file -> Error
                action.errors.add(ActionError(item, "Error while deleting item file."))
                return@performAction
            }

            //Get action helper
            val helper = action.getHelper(item)

            //Remove item from all, favourites & gallery
            performRemoveFromAll(action, helper.indexInAll)
            performRemoveFromFavourites(action, helper.indexInFavourites)
            performRemoveFromGallery(action, helper.indexInGallery)

            //Get item album & remove item from it
            val album: Album
            if (item.isTrashed) {
                album = getOrCreateAlbumFromItemFile(item.file) //Get original album (before getting trashed)
                performRemoveFromTrash(action, helper.indexInTrash, album)
            } else {
                album = item.album
                performRemoveFromAlbum(action, helper.indexInAlbum, helper.indexOfAlbum, album)
            }

            //Delete item metadata from album
            if (album.hasMetadataKey(item.name) && getBool(StoragePairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
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
