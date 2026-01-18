package com.botpa.turbophotos.gallery;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.core.content.MimeTypeFilter;

import com.botpa.turbophotos.gallery.actions.Action;
import com.botpa.turbophotos.gallery.actions.ActionError;
import com.botpa.turbophotos.gallery.actions.ActionHelper;
import com.botpa.turbophotos.gallery.dialogs.DialogAlbums;
import com.botpa.turbophotos.gallery.dialogs.DialogErrors;
import com.botpa.turbophotos.gallery.dialogs.DialogFolders;
import com.botpa.turbophotos.gallery.dialogs.DialogInput;
import com.botpa.turbophotos.settings.SettingsPairs;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Library {

    //Logging
    private static final String LOG_LIBRARY = "LIBRARY";

    //Events
    private static final ArrayList<RefreshEvent> onRefresh = new ArrayList<>();
    private static final ArrayList<ActionEvent> onAction = new ArrayList<>();

    //Albums
    private static long lastUpdate = 0;

    public static final HashMap<String, Album> albumsMap = new HashMap<>(); //Uses album path as key for easy finding
    public static final ArrayList<Album> albums = new ArrayList<>();
    public static final Map<Album, Integer> trashMap = new HashMap<>(); //A map to store the amount of trashed items in each album
    public static final Album trash = new Album("Trash");
    public static final Album all = new Album("All");

    //Gallery
    private static final Collection<File> recentlyAddedFiles = new HashSet<>(); //List of items recently added that should be ignored when refreshing to avoid duplicates

    public static final ArrayList<CoonItem> gallery = new ArrayList<>(); //Currently open album items (could be filtered or the same items)
    public static String galleryFilter = "*/*"; //Mime type


    //Gallery (events)
    public interface RefreshEvent {
        void invoke(boolean updated);
    }

    private static void invokeOnRefresh(boolean updated) {
        for (RefreshEvent listener : onRefresh) listener.invoke(updated);
    }

    public static void addOnRefreshEvent(RefreshEvent listener) {
        onRefresh.add(listener);
    }

    public static void removeOnRefreshEvent(RefreshEvent listener) {
        onRefresh.remove(listener);
    }

    //Gallery (sort/load/refresh)
    private static Cursor getMediaCursor(Context context) {
        //Get content resolver
        ContentResolver contentResolver = context.getContentResolver();

        //Create projection
        String[] projection = {
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.IS_TRASHED
        };

        //Create selection
        String selection =
                MediaStore.Files.FileColumns.DATE_ADDED + " > ? AND (" +
                        MediaStore.Files.FileColumns.MEDIA_TYPE + "= ? OR " +
                        MediaStore.Files.FileColumns.MEDIA_TYPE + "= ?)";

        //Create selection args
        String[] selectionArgs = {
                String.valueOf(lastUpdate),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        };

        //Create bundle with selection, sorting order & include trashed items
        Bundle queryArgs = new Bundle();
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);

        //Get cursor resolver
        return contentResolver.query(
                //Collection
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                //Projection
                projection,
                //Query
                queryArgs,
                //Cancellation signal
                null
        );
    }

    public static void sortLibrary() {
        //Sort albums
        sortLibrary(true);
    }

    private static void sortLibrary(boolean refresh) {
        //Sort albums
        trash.sort();
        all.sort();
        for (Album album : albums) album.sort();
        sortAlbumsList();

        //Invoke on refresh
        invokeOnRefresh(refresh);
    }

    public static void loadLibrary(Context context, boolean reset) {
        loadLibrary(context, reset, galleryFilter);
    }

    public static void loadLibrary(Context context, String filterType) {
        //Filtering requires a reset of albums
        loadLibrary(context, true, filterType);
    }

    private static void loadLibrary(Context context, boolean reset, String filterMimeType) {
        //Save filter
        galleryFilter = filterMimeType;

        //Load links & trash
        Link.Companion.loadLinks(reset);

        //Reset
        if (reset) {
            //Reset last update timestamp
            lastUpdate = 0;

            //Clear items from albums
            trash.reset();
            all.reset();
            for (Album album : albums) album.reset();

            //Albums map doesn't get cleared so that the gallery can stay on the selected album on reload :D
        }

        //Get album items
        int itemsAdded = 0;
        try (Cursor cursor = getMediaCursor(context)) {
            //Save last update timestamp
            lastUpdate = System.currentTimeMillis() / 1000L;

            //Get columns for query
            int columnLastModified = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int columnMimeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
            int columnSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int columnData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            int columnIsTrashed = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.IS_TRASHED);

            //Check items
            while (cursor.moveToNext()) {
                //Get file
                File file = new File(cursor.getString(columnData));

                //Check if file is in recently added items
                if (recentlyAddedFiles.contains(file)) {
                    //Is in recently added items -> Remove it & continue
                    recentlyAddedFiles.remove(file);
                    continue;
                }

                //Get type & apply filter
                String mimeType = cursor.getString(columnMimeType);
                if (!MimeTypeFilter.matches(mimeType, filterMimeType)) continue;

                //Get other info
                long lastModified = cursor.getLong(columnLastModified);
                long size = cursor.getLong(columnSize);
                boolean isTrashed = cursor.getInt(columnIsTrashed) == 1;

                //Check if is trashed
                Album album = getOrCreateAlbumFromItemFile(file);
                if (isTrashed) {
                    //Trashed -> Create item with trash as album
                    CoonItem item = new CoonItem(file, trash, lastModified, mimeType, size, true);

                    //Add to trash
                    addItemToTrash(item, album);
                } else {
                    //Not trashed -> Create item with normal album
                    CoonItem item = new CoonItem(file, album, lastModified, mimeType, size, false);

                    //Add to all items list & its album
                    all.add(item);
                    item.album.add(item);
                }

                //Added an item
                itemsAdded++;
            }
        } catch (Exception e) {
            Log.e(LOG_LIBRARY, "Error loading albums: " + e.getMessage());
        }

        //Remove unused albums & fill albums list
        albums.clear();
        Iterator<Map.Entry<String, Album>> iterator = albumsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            //Get album
            Album album = iterator.next().getValue();

            //Check if album is empty
            if (album.isEmpty()) {
                //Is empty -> Delete it
                iterator.remove();
            } else {
                //Not empty -> Add it to albums list
                albums.add(album);
            }
        }

        //Sort albums
        sortLibrary(reset || itemsAdded > 0);
    }

    //Albums
    private static Album createAlbum(File imagesFolder, String name) {
        //Get folder path
        String folderPath = imagesFolder != null ? imagesFolder.getAbsolutePath() : null;

        //Create new album
        Album album = new Album(name, imagesFolder, null);

        //Add album to albums map
        albumsMap.put(album.getImagesPath(), album);

        //Get album link
        Link link = Link.linksMap.getOrDefault(folderPath, null);
        if (link != null) Link.Companion.relinkWithAlbum(link);

        //Return album
        return album;
    }

    private static Album getOrCreateAlbumFromFolder(File folder) {
        //Temp album var
        Album album;

        //Get folder path
        String folderPath = folder.getAbsolutePath();

        //Check if an album exists for the path
        if (albumsMap.containsKey(folderPath)) {
            //Album exists -> Take it
            album = albumsMap.get(folderPath);
        } else {
            //Album does not exist -> Create it
            album = createAlbum(folder, folder.getName());
        }
        assert album != null;

        //Return album
        return album;
    }

    private static Album getOrCreateAlbumFromItemFile(File file) {
        File fileParent = file.getParentFile();
        assert fileParent != null;
        return getOrCreateAlbumFromFolder(fileParent);
    }

    private static void removeAlbum(int index) {
        //Remove album from albums list
        Album album = albums.remove(index);

        //Check if trash contains items from this album
        if (!trashMap.containsKey(album)) {
            //No items from this album in trash -> Remove album completely
            albumsMap.remove(album.getImagesPath());
        }
    }

    private static void sortAlbumsList() {
        albums.sort((a1, a2) -> Long.compare(a2.get(0).lastModified, a1.get(0).lastModified));
    }

    //Trash
    private static void addItemToTrash(CoonItem item, Album originalAlbum) {
        //Add to trash
        trash.addSorted(item);
        trashMap.put(originalAlbum, trashMap.getOrDefault(originalAlbum, 0) + 1);
    }

    private static void removeItemFromTrash(int itemIndex, Album originalAlbum) {
        //Remove from trash
        trash.remove(itemIndex);

        //Calculate new trash amount
        int newTrashAmount = trashMap.getOrDefault(originalAlbum, 0) - 1;
        if (newTrashAmount <= 0) {
            //No more items from this album in trash -> Remove album from trash
            trashMap.remove(originalAlbum);

            //Check if original album still has items
            if (originalAlbum.isEmpty()) {
                //Is empty -> Remove album completely
                albumsMap.remove(originalAlbum.getImagesPath());
            }
        } else {
            //Update trash amount
            trashMap.put(originalAlbum, newTrashAmount);
        }
    }

    //Metadata
    private static void loadMetadataHelper(LoadingIndicator indicator, Album album) {
        //Already loaded
        if (album.hasMetadata()) return;

        //Check if images folder & metadata file exist
        if (!album.exists()) return;

        //Update load indicator
        if (indicator != null) indicator.load(album.getName(), "metadata");

        //Load metadata
        album.loadMetadata();
    }

    public static void loadMetadata(LoadingIndicator indicator, Album album) {
        //Check album
        if (album == all) {
            //All items album -> Load metadata from all albums
            for (Album _album : albums) loadMetadataHelper(indicator, _album);
        } else {
            //Normal album -> Load album metadata
            loadMetadataHelper(indicator, album);
        }
    }

    //Gallery
    private static boolean filterItem(CoonItem item, String filter) {
        //Check item name
        if (item.name.toLowerCase().contains(filter)) return true;

        //Get metadata
        ObjectNode metadata = item.getMetadata();
        if (metadata == null) return false;

        //Check caption
        if (metadata.has("caption")) {
            JsonNode caption = metadata.path("caption");
            if (caption.isTextual() && caption.asText().toLowerCase().contains(filter)) {
                return true;
            }
        }

        //Check labels
        if (metadata.has("labels")) {
            JsonNode labels = metadata.path("labels");
            for (int i = 0; i < labels.size(); i++) {
                if (labels.get(i).asText().toLowerCase().contains(filter)) {
                    return true;
                }
            }
        }

        //Check text
        if (metadata.has("text")) {
            JsonNode text = metadata.path("text");
            for (int i = 0; i < text.size(); i++) {
                if (text.get(i).asText().toLowerCase().contains(filter)) {
                    return true;
                }
            }
        }

        //Not found
        return false;
    }

    public static void filterGallery(String filter, Album album) {
        //Check if filtering
        boolean isFiltering = !filter.isEmpty();

        //Clear items list
        gallery.clear();

        //Look for items that contain the filter
        for (CoonItem item : album.items) {
            //No filter -> Skip check
            if (!isFiltering) {
                gallery.add(item);
                continue;
            }

            //Check if json contains filter
            if (filterItem(item, filter)) Library.gallery.add(item);
        }
    }

    //Actions (events)
    public interface ActionEvent {
        void invoke(Action action);
    }

    private static void invokeOnAction(Action action) {
        for (ActionEvent listener : onAction) listener.invoke(action);
    }

    public static void addOnActionEvent(ActionEvent listener) {
        onAction.add(listener);
    }

    public static void removeOnActionEvent(ActionEvent listener) {
        onAction.remove(listener);
    }

    //Actions (dialogs)
    public static void showActionErrorsDialog(Context context, Action action) {
        //Create dialog
        new DialogErrors(context, action.errors).buildAndShow();
    }

    public static void showSelectAlbumDialog(Context context, Consumer<Album> onSelect) {
        //Create dialog
        new DialogAlbums(context, albums, onSelect, (folder) -> onSelect.accept(getOrCreateAlbumFromFolder(folder))).buildAndShow();
    }

    public static void showSelectFolderDialog(Context context, Consumer<File> onSelect) {
        //Create folders list
        File externalStorage = Environment.getExternalStorageDirectory();
        File imagesFolder = new File(externalStorage, "Pictures");
        List<File> folders = Orion.listFiles(imagesFolder);
        folders.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        //Create dialog
        new DialogFolders(context, externalStorage, imagesFolder, folders, onSelect).buildAndShow();
    }

    //Actions (base & util)
    private static void performRemoveFromAll(ActionHelper helper) {
        //Not in all items list
        if (helper.indexInAll == -1) return;

        //Remove item
        all.remove(helper.indexInAll);
    }

    private static void performRemoveFromGallery(ActionHelper helper, Action action) {
        //Not in gallery items list
        if (helper.indexInGallery == -1) return;

        //Mark it as removed (will get removed in evaluateAction())
        action.removedIndexesInGallery.add(helper.indexInGallery);
    }

    private static void performRemoveFromAlbum(ActionHelper helper, Action action, Album album) {
        //Not in album
        if (helper.indexInAlbum == -1) return;

        //Remove item
        album.remove(helper.indexInAlbum);
        action.modifiedAlbums.add(album);

        //Check if album needs to be deleted or sorted
        if (album.isEmpty()) {
            //Album is empty -> Mark it as removed (will get removed in evaluateAction())
            action.removedIndexesInAlbums.add(helper.indexOfAlbum);
        } else if (helper.indexInAlbum == 0) {
            //Album isn't empty & first image was deleted -> Sort albums list in case the order changed
            action.hasSortedAlbumsList = true;
        }
    }

    private static void performRemoveFromTrash(ActionHelper helper, Action action, Album originalAlbum) {
        //Not in album
        if (helper.indexInAlbum == -1) return;

        //Remove item
        removeItemFromTrash(helper.indexInAlbum, originalAlbum);
        action.trashAction = trash.isEmpty() ? Action.TRASH_REMOVED : Action.TRASH_UPDATED;
    }

    private static void performAction(Context context, int type, CoonItem[] items, BiConsumer<Action, CoonItem> onPerformAction) {
        //Create action
        Action action = new Action(type, items);

        //Perform action for each item
        for (CoonItem item : items) onPerformAction.accept(action, item);

        //Evaluate action
        evaluateAction(context, action);
    }

    private static void evaluateAction(Context context, Action action) {
        //Check if gallery items were marked as removed
        if (!action.removedIndexesInGallery.isEmpty()) {
            //Marked as removed -> Sort indexes
            action.removedIndexesInGallery.sort((a, b) -> b - a); //Sort from last to first to allow using a foreach

            //Remove items
            for (int indexInGallery : action.removedIndexesInGallery) gallery.remove(indexInGallery);
        }

        //Check if albums were marked as removed
        if (!action.removedIndexesInAlbums.isEmpty()) {
            //Marked as removed -> Sort indexes
            action.removedIndexesInAlbums.sort((a, b) -> b - a); //Sort from last to first to allow using a foreach

            //Remove albums
            for (int indexInAlbums : action.removedIndexesInAlbums) {
                //Remove album
                removeAlbum(indexInAlbums);

                //Album was the first -> Mark "all" as updated
                if (indexInAlbums == 0) action.modifiedAlbums.add(all);
            }
        }

        //Check if albums list was marked as sorted
        if (action.hasSortedAlbumsList) {
            //Marked as sorted -> Sort it
            sortAlbumsList();
        }

        //Check if actions failed
        if (!action.errors.isEmpty()) {
            //Actions failed -> Show dialog
            showActionErrorsDialog(context, action);
        }

        //Invoke action
        invokeOnAction(action);
    }

    //Actions (modify items)
    public static void renameItem(Context context, CoonItem item) {
        //Check if item is in trash
        if (item.isTrashed) return;

        //Get file info
        String oldName = item.name;
        String extension = Orion.getExtension(oldName);
        String oldNameNoExtension = extension.isEmpty() ? oldName : oldName.substring(0, oldName.length() - (extension.length() + 1));

        //Create action
        Action action = new Action(Action.TYPE_RENAME, new CoonItem[] { item });

        //Ask for a new name
        DialogInput dialog = new DialogInput(
                context,
                "Rename",
                "New name",
                (newNameNoExtension) -> {
                    //Check if name changed
                    if (newNameNoExtension.equals(oldNameNoExtension)) {
                        //Name didn't change -> Allow to rename
                        return true;
                    }

                    //Check if name is valid
                    if (newNameNoExtension.isEmpty()) {
                        //Name is empty -> Prevent from renaming
                        Toast.makeText(context, "New name can't be empty.", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    //Get new name & file
                    String newName = newNameNoExtension + "." + extension;
                    File newFile = new File(item.album.getImagesFolder(),  newName);

                    //Check if new file already exists
                    if (newFile.exists()) {
                        //New file already exists -> Prevent from renaming
                        Toast.makeText(context, "A file named \"" + newFile.getName() + "\" already exists.", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    //All good
                    return true;
                },
                (newNameNoExtension) -> {
                    //Check if name changed
                    if (newNameNoExtension.equals(oldNameNoExtension)) {
                        //Name didn't change -> No need to rename
                        return null;
                    }

                    //Get new name & file
                    String newName = newNameNoExtension + "." + extension;
                    File newFile = new File(item.album.getImagesFolder(),  newName);

                    //Rename file
                    boolean renamed = item.file.renameTo(newFile);
                    if (!renamed) {
                        //Failed to rename file
                        action.errors.add(new ActionError(item, "Failed to rename \"" + oldName + "\" to \"" + newName + "\"."));
                        evaluateAction(context, action);
                        return null;
                    }

                    //Update item
                    item.name = newName;
                    item.file = newFile;

                    //Copy item metadata to new key
                    Album album = item.album;
                    if (album.hasMetadataKey(oldName) && Storage.getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                        album.setMetadataKey(newName, album.getMetadataKey(oldName));
                        album.removeMetadataKey(oldName);
                        album.saveMetadata();
                    }

                    //Evaluate rename
                    evaluateAction(context, action);
                    return null;
                }
        );
        dialog.buildAndShow();
        dialog.setText(oldNameNoExtension);
    }

    public static void editItem(Context context, CoonItem item) {
        //Get URI & mime type
        Uri uri = Orion.getFileUriFromFilePath(context, item.file.getAbsolutePath());
        String mimeType = item.mimeType;

        //Edit
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, null));
    }

    public static void shareItems(Context context, CoonItem[] items) {
        //No items
        if (items.length == 0) return;

        //Share items
        Intent intent;
        if (items.length == 1) {
            //Share 1 item
            intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, Orion.getFileUriFromFilePath(context, items[0].file.getAbsolutePath()));
            intent.setType(items[0].mimeType);
        } else {
            //Share multiple items
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Uri> URIs = new ArrayList<>();
            for (CoonItem item : items) URIs.add(Orion.getFileUriFromFilePath(context, item.file.getAbsolutePath()));
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, URIs);
            intent.setType("*/*");
        }
        context.startActivity(Intent.createChooser(intent, null));
    }

    private static void moveItemsInternal(Context context, CoonItem[] items, Album newAlbum) {
        performAction(context, Action.TYPE_MOVE, items, (action, item) -> {
            //Check if new album is valid
            if (newAlbum == null) {
                //Invalid destination
                action.errors.add(new ActionError(item, "Invalid destination."));
                return;
            }

            //Check if item is in trash
            if (item.isTrashed) {
                //Item is in trash
                action.errors.add(new ActionError(item, "Item is in the trash."));
                return;
            }

            //Get old album
            Album oldAlbum = item.album;

            //Check if item is being moved to the same album
            if (newAlbum == oldAlbum) {
                //Already in the destination album
                action.errors.add(new ActionError(item, "Item is already in the destination album."));
                return;
            }

            //Move item file
            File newFile = new File(newAlbum.getImagesPath(), item.name);
            if (!Orion.moveFile(item.file, newFile)) {
                //Failed to move file
                action.errors.add(new ActionError(item, "Error while moving item file."));
                return;
            }

            //Get action helper
            ActionHelper helper = action.getHelper(item);

            //Remove item from gallery items list
            performRemoveFromGallery(helper, action);

            //Remove item from old album
            performRemoveFromAlbum(helper, action, oldAlbum);

            //Update item
            item.file = newFile;
            item.album = newAlbum;

            //Add item to new album
            helper.indexInAlbum = newAlbum.addSorted(item);
            action.modifiedAlbums.add(newAlbum);

            //Check if item was added as the album cover
            if (helper.indexInAlbum == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.hasSortedAlbumsList = true;

                //Check if album is in albums list
                if (!albums.contains(newAlbum)) {
                    //Not in albums list -> Add it
                    albums.add(newAlbum);
                }
            }

            //Move item metadata from old album to new album
            if (oldAlbum.hasMetadataKey(item.name) && Storage.getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                //Copy metadata to new album
                newAlbum.setMetadataKey(item.name, oldAlbum.getMetadataKey(item.name));
                newAlbum.saveMetadata();

                //Remove metadata from old album
                oldAlbum.removeMetadataKey(item.name);
                oldAlbum.saveMetadata();
            }
        });
    }

    public static void moveItems(Context context, CoonItem[] items) {
        //Show album selection dialog
        showSelectAlbumDialog(context, newAlbum -> moveItemsInternal(context, items, newAlbum));
    }

    private static void copyItemsInternal(Context context, CoonItem[] items, Album newAlbum) {
        performAction(context, Action.TYPE_COPY, items, (action, item) -> {
            //Check if new album is valid
            if (newAlbum == null) {
                //Invalid destination
                action.errors.add(new ActionError(item, "Invalid destination."));
                return;
            }

            //Check if item is in trash
            if (item.isTrashed) {
                //Item is in trash
                action.errors.add(new ActionError(item, "Item is in the trash."));
                return;
            }

            //Get old album
            Album oldAlbum = item.album;

            //Check if item is being moved to the same album
            if (newAlbum == oldAlbum) {
                //Already in the destination album
                action.errors.add(new ActionError(item, "Item is already in the destination album."));
                return;
            }

            //Copy item file
            File newFile = new File(newAlbum.getImagesPath(), item.name);
            if (!Orion.cloneFile(context, item.file, newFile)) {
                //Failed to copy file
                action.errors.add(new ActionError(item, "Error while copying item file."));
                return;
            }
            recentlyAddedFiles.add(newFile); //Add file to recently added to prevent duplicates

            //Create new item
            CoonItem newItem = new CoonItem(newFile, newAlbum, item.lastModified, item.mimeType, item.size, item.isTrashed);

            //Add item to new album
            int indexInAlbum = newAlbum.addSorted(newItem);
            action.modifiedAlbums.add(newAlbum);

            //Check if item was added as the album cover
            if (indexInAlbum == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.hasSortedAlbumsList = true;

                //Check if album is in albums list
                if (!albums.contains(newAlbum)) {
                    //Not in albums list -> Add it
                    albums.add(newAlbum);
                }
            }

            //Copy item metadata from old album to new album
            if (oldAlbum.hasMetadataKey(item.name) && Storage.getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                newAlbum.setMetadataKey(item.name, oldAlbum.getMetadataKey(item.name));
                newAlbum.saveMetadata();
            }
        });
    }

    public static void copyItems(Context context, CoonItem[] items) {
        //Show album selection dialog
        showSelectAlbumDialog(context, newAlbum -> copyItemsInternal(context, items, newAlbum));
    }

    private static Map<Uri, CoonItem> prepareItemURIs(Context context, CoonItem[] items, Action action) {
        Map<Uri, CoonItem> pendingItems = new HashMap<>();
        for (CoonItem item : items) {
            //Check if item is already trashed
            switch (action.getType()) {
                //Check if item is already trashed
                case Action.TYPE_TRASH:
                    //Not trashed
                    if (!item.isTrashed) break;

                    //Trashed
                    action.errors.add(new ActionError(item, "Item is already in the trash."));
                    continue;

                //Check if item is not trashed
                case Action.TYPE_RESTORE:
                    //Trashed
                    if (item.isTrashed) break;

                    //Not trashed
                    action.errors.add(new ActionError(item, "Item is not in the trash."));
                    continue;
            }

            //Get item URI
            Uri uri = Orion.getMediaUriFromFilePath(context, item.file.getAbsolutePath());

            //Check if URI is valid
            if (uri == null) {
                //Not valid
                action.errors.add(new ActionError(item, "Invalid item URI."));
                continue;
            }

            //Add it to pending items
            pendingItems.put(uri, item);
        }
        return pendingItems;
    }

    public static Action trashItems(GalleryActivity activity, CoonItem[] items, ActivityResultLauncher<IntentSenderRequest> launcher) {
        //Create action
        Action action = new Action(Action.TYPE_TRASH, items);

        //Get item URIs
        action.trashPending = prepareItemURIs(activity, items, action);

        //Check if there are any pending items
        if (action.trashPending.isEmpty()) {
            //No pending items -> Ignore action
            evaluateAction(activity, action);
            return null;
        }

        //Create trash request
        PendingIntent pendingIntent = MediaStore.createTrashRequest(activity.getContentResolver(), action.trashPending.keySet(), true); //True -> Move to trash
        IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
        launcher.launch(request);

        //Return action (the ones that will get trashed)
        return action;
    }

    public static void onTrashItemsResult(Context context, Action action) {
        //Update items
        for (Map.Entry<Uri, CoonItem> entry : action.trashPending.entrySet()) {
            //Get URI & item
            Uri uri = entry.getKey();
            CoonItem item = entry.getValue();

            //Get new item file
            String newFilePath = Orion.getFilePathFromMediaUri(context, uri);
            File newFile = new File(newFilePath == null ? "" : newFilePath);
            if (!newFile.exists()) {
                //New file does not exist -> Something failed
                action.errors.add(new ActionError(item, "New file does not exist."));
                continue;
            }

            //Get original album & action helper
            Album originalAlbum = item.album;
            ActionHelper helper = action.getHelper(item);

            //Update item
            item.file = newFile;
            item.album = trash;
            item.isTrashed = true;

            //Add item to trash album
            addItemToTrash(item, originalAlbum);
            if (action.trashAction == Action.TRASH_NONE) {
                //First change to trash this action -> Check if trash was added to list or just updated
                action.trashAction = trash.size() == 1 ? Action.TRASH_ADDED : Action.TRASH_UPDATED;
            }

            //Remove item from all
            performRemoveFromAll(helper);

            //Remove item from gallery items list
            performRemoveFromGallery(helper, action);

            //Remove item from album
            performRemoveFromAlbum(helper, action, originalAlbum); //Remove from album after adding to trash cause, if both trash & album are empty, the album gets removed from albumsMap
        }

        //Evaluate action
        evaluateAction(context, action);
    }

    public static Action restoreItems(GalleryActivity activity, CoonItem[] items, ActivityResultLauncher<IntentSenderRequest> launcher) {
        //Create action
        Action action = new Action(Action.TYPE_RESTORE, items);

        //Get item URIs
        action.trashPending = prepareItemURIs(activity, items, action);

        //Check if there are any pending items
        if (action.trashPending.isEmpty()) {
            //No pending items -> Ignore action
            evaluateAction(activity, action);
            return null;
        }

        //Create restore request
        PendingIntent pendingIntent = MediaStore.createTrashRequest(activity.getContentResolver(), action.trashPending.keySet(), false); //False -> Restore from trash
        IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
        launcher.launch(request);

        //Return action (the ones that will get restored)
        return action;
    }

    public static void onRestoreItemsResult(Context context, Action action) {
        //Update items
        for (Map.Entry<Uri, CoonItem> entry : action.trashPending.entrySet()) {
            //Get URI & item
            Uri uri = entry.getKey();
            CoonItem item = entry.getValue();

            //Get new item file
            String newFilePath = Orion.getFilePathFromMediaUri(context, uri);
            File newFile = new File(newFilePath == null ? "" : newFilePath);
            if (!newFile.exists()) {
                //New file does not exist -> Something failed
                action.errors.add(new ActionError(item, "New file does not exist."));
                continue;
            }

            //Get original album & action helper
            Album originalAlbum = getOrCreateAlbumFromItemFile(newFile);
            ActionHelper helper = action.getHelper(item);

            //Update item
            item.file = newFile;
            item.album = originalAlbum;
            item.isTrashed = false;

            //Add to all & original album
            helper.indexInAll = all.addSorted(item);
            action.modifiedAlbums.add(all);
            helper.indexInAlbum = originalAlbum.addSorted(item);
            action.modifiedAlbums.add(originalAlbum);

            //Check if item was added as the album cover
            if (helper.indexInAlbum == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.hasSortedAlbumsList = true;

                //Check if album is in albums list
                if (!albums.contains(originalAlbum)) {
                    //Not in albums list -> Add it
                    albums.add(originalAlbum);
                }
            }

            //Remove item from gallery items list
            performRemoveFromGallery(helper, action);

            //Remove item from trash
            performRemoveFromTrash(helper, action, originalAlbum); //Remove from trash after adding to album cause, if both trash & album are empty, the album gets removed from albumsMap
        }

        //Evaluate action
        evaluateAction(context, action);
    }

    private static void deleteItemsInternal(Context context, CoonItem[] items) {
        performAction(context, Action.TYPE_DELETE, items, (action, item) -> {
            //Delete item file
            if (!Orion.deleteFile(item.file)) {
                //Failed to delete file
                action.errors.add(new ActionError(item, "Error while deleting item file."));
                return;
            }

            //Get action helper
            ActionHelper helper = action.getHelper(item);

            //Remove item from all
            performRemoveFromAll(helper);

            //Remove item from gallery items list
            performRemoveFromGallery(helper, action);

            //Check if item is in the trash to remove it & get its album
            Album album;
            if (item.isTrashed) {
                //Get original album (before being trashed)
                album = getOrCreateAlbumFromItemFile(item.file);

                //Remove item from trash
                performRemoveFromTrash(helper, action, album);
            } else {
                //Get item album
                album = item.album;

                //Remove item from album
                performRemoveFromAlbum(helper, action, album);
            }

            //Delete item metadata from album
            if (album.hasMetadataKey(item.name) && Storage.getBool(SettingsPairs.APP_AUTOMATIC_METADATA_MODIFICATION)) {
                album.removeMetadataKey(item.name);
                album.saveMetadata();
            }
        });
    }

    public static void deleteItems(Context context, CoonItem[] items) {
        //Create message
        StringBuilder message = new StringBuilder();
        message.append("Are you sure you want to permanently delete ");
        if (items.length == 1) {
            message.append("\"");
            message.append(items[0].name);
            message.append("\"");
        } else {
            message.append(items.length);
            message.append(" items");
        }
        message.append("?");

        //Show confirmation dialog
        new MaterialAlertDialogBuilder(context)
                .setMessage(message.toString())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, whichButton) -> deleteItemsInternal(context, items))
                .show();
    }

}
