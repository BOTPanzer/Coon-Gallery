package com.botpa.turbophotos.gallery;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class Library {

    //Logging
    private static final String LOG_LIBRARY = "LIBRARY";
    private static final String LOG_TRASH = "TRASH";

    //Events
    private static final ArrayList<RefreshEvent> onRefresh = new ArrayList<>();
    private static final ArrayList<ActionEvent> onAction = new ArrayList<>();

    //Trash
    private static boolean trashLoaded = false;
    private static final HashMap<Album, ArrayList<CoonItem>> trashAlbumsMap = new HashMap<>(); //Albums of files in trash
    private static File trashFolder;

    public static final Album trash = new Album("Trash");

    //Albums
    private static long lastUpdate = 0;

    public static final HashMap<String, Album> albumsMap = new HashMap<>(); //Uses album path as key
    public static final ArrayList<Album> albums = new ArrayList<>();
    public static final Album all = new Album("All");                 //Album with all files

    //Gallery
    public static final ArrayList<CoonItem> gallery = new ArrayList<>();    //Currently open album items (can be filtered)


    //Library
    public interface RefreshEvent {
        void invoke(boolean updated);
    }

    private static void invokeOnRefresh(boolean updated) {
        for (RefreshEvent listener: onRefresh) listener.invoke(updated);
    }

    public static void addOnRefreshEvent(RefreshEvent listener) {
        onRefresh.add(listener);
    }

    public static void removeOnRefreshEvent(RefreshEvent listener) {
        onRefresh.remove(listener);
    }

    private static Cursor getMediaCursor(Context context) {
        //Get content resolver
        ContentResolver contentResolver = context.getContentResolver();

        //Get cursor resolver
        return contentResolver.query(
                //Collection
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                //Projection
                new String[] {
                        //MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.Files.FileColumns.DATE_MODIFIED,
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Files.FileColumns.SIZE,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.Files.FileColumns.IS_TRASHED,
                },
                //Selection
                MediaStore.Files.FileColumns.DATE_ADDED + " > ? AND (" + MediaStore.Files.FileColumns.MEDIA_TYPE + "= ? OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "= ?)",
                //Selection args
                new String[] {
                        String.valueOf(lastUpdate), //To check recently added items
                        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                },
                //Sorting order
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC" //Sort by date (newest first)
        );
    }

    public static void loadLibrary(Context context, boolean reset) {
        //Load links & trash
        Link.Companion.loadLinks(reset);
        loadTrash(context, reset);

        //Reset
        if (reset) {
            //Reset last update timestamp
            lastUpdate = 0;

            //Clear items from albums
            all.reset();
            for (Album album: albums) album.reset();

            //Albums map doesn't get cleared so that the gallery can stay on the selected album on reload :D
        }

        //Get album items
        boolean updated = false;
        try (Cursor cursor = getMediaCursor(context)) {
            //Save last update timestamp
            lastUpdate = System.currentTimeMillis() / 1000L;

            //Get indexes for all columns
            int idxLastModified = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int idxSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int idxMediaType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int idxData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

            //Check items
            while (cursor.moveToNext()) {
                //Get other info
                File file = new File(cursor.getString(idxData));
                long lastModified = cursor.getLong(idxLastModified);
                long size = cursor.getLong(idxSize);
                boolean isVideo = (Objects.equals(cursor.getString(idxMediaType), String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)));

                //Get album
                Album album = getOrCreateAlbumFromItemFile(file);

                //Create item
                CoonItem item = new CoonItem(file, album, lastModified, size, isVideo, null);
                all.add(item);
                album.add(item);

                //Mark library as updated
                updated = true;
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
            if (album.isNotEmpty()) {
                //Not empty -> Add it to albums list
                albums.add(album);
                continue;
            }

            //Check any trash items are from this album
            if (trashAlbumsMap.containsKey(album)) continue;

            //Empty album & not used in trash -> Delete
            iterator.remove();
        }

        //Sort albums
        all.sort();
        for (Album album: albums) album.sort();
        sortAlbumsList();

        //Return true if the albums were updated
        invokeOnRefresh(updated);
    }

    //Trash
    private static void addTrashItem(CoonItem item) {
        //Add item to trash album
        trash.addSorted(item);

        //Add item to trash albums map
        ArrayList<CoonItem> list = trashAlbumsMap.computeIfAbsent(item.album, key -> new ArrayList<>());   //Get list of items, create and save a new one if missing
        list.add(item);
    }

    private static void removeTrashItem(int itemIndex) {
        //Remove item from trash album
        CoonItem item = trash.remove(itemIndex);

        //Remove item from trash albums map
        ArrayList<CoonItem> list = trashAlbumsMap.computeIfAbsent(item.album, key -> new ArrayList<>());   //Get list of items, create and save a new one if missing
        list.remove(item);
        if (list.isEmpty()) trashAlbumsMap.remove(item.album);
    }

    private static void loadTrash(Context context, boolean reset) {
        //Already loaded
        if (!reset && trashLoaded) return;
        trashLoaded = true;

        //Init trash folder
        trashFolder = new File(context.getFilesDir().getAbsolutePath() + "/trash/");
        if (!trashFolder.exists() && trashFolder.mkdirs()) Log.e(LOG_TRASH, "Could not create trash folder");

        //Clear trash
        trash.reset();
        trashAlbumsMap.clear();

        //Get trash items from storage (as strings)
        ArrayList<String> trashUnparsed = Storage.getStringList("Settings.trash");

        //Parse trash info strings
        boolean updated = false;
        for (int i = trashUnparsed.size() - 1; i >= 0; i--) {
            //Parse info
            String string = trashUnparsed.get(i);
            TrashInfo info = TrashInfo.Companion.parse(string);

            //Check if trashed file exists
            if (!info.trashFile.exists()) {
                //File does not exist -> Remove trash info
                trashUnparsed.remove(i);
                updated = true;
                continue;
            }

            //Create item & add it to trash album
            addTrashItem(new CoonItem(info.trashFile, trash, info.trashFile.lastModified(), info.trashFile.length(), info.isVideo(), info));
        }
        trash.sort();

        //Trash was updated while loading -> Save it
        if (updated) Storage.putStringList("Settings.trash", trashUnparsed);
    }

    private static void saveTrash() {
        //Save trash
        ArrayList<String> list = new ArrayList<>();
        for (ArrayList<CoonItem> items: trashAlbumsMap.values()) {
            for (int i = items.size() - 1; i >= 0; i--) {
                //Get item trash info & remove item if not in trash
                TrashInfo trashInfo = items.get(i).trashInfo;
                if (trashInfo == null) {
                    items.remove(i);
                    continue;
                }

                //Add trash info to list
                list.add(trashInfo.toString());
            }
        }
        Storage.putStringList("Settings.trash", list);
    }

    //Albums
    private static Album createAlbum(File imagesFolder, String name) {
        //Get folder path
        String folderPath = imagesFolder != null ? imagesFolder.getAbsolutePath() : null;

        //Get album link
        Link link = Link.linksMap.getOrDefault(folderPath, null);
        boolean hasLink = link != null;

        //Get metadata file
        File metadataFile = hasLink ? link.metadataFile : null;

        //Create new album
        Album album = new Album(name, imagesFolder, metadataFile);

        //Add album to albums map
        albumsMap.put(album.getImagesPath(), album);

        //Assign album to its link
        if (hasLink) link.album = album;

        //Return album
        return album;
    }

    private static Album getOrCreateAlbumFromItemFile(File file) {
        //Temp album var
        Album album;

        //Get file parent path
        File fileParent = file.getParentFile();
        assert fileParent != null;
        String fileParentPath = fileParent.getAbsolutePath();

        //Check if an album exists for the path
        if (albumsMap.containsKey(fileParentPath)) {
            //Album exists -> Take it
            album = albumsMap.get(fileParentPath);
        } else {
            //Album does not exist -> Create it
            album = createAlbum(fileParent, fileParent.getName());
        }
        assert album != null;

        //Return album
        return album;
    }

    private static void removeAlbum(int index) {
        //Remove album from list
        Album album = albums.remove(index);

        //Check if any items in trash are from this album
        if (!trashAlbumsMap.containsKey(album)) {
            //Album isn't in trash albums -> Remove it completely
            albumsMap.remove(album.getImagesPath());
        }
    }

    private static void sortAlbumsList() {
        albums.sort((a1, a2) -> Long.compare(a2.get(0).lastModified, a1.get(0).lastModified));
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
            for (Album _album: albums) loadMetadataHelper(indicator, _album);
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
        ObjectNode metadata = item.album.getMetadataKey(item.name);
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
        for (CoonItem item: album.items) {
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
        for (ActionEvent listener: onAction) listener.invoke(action);
    }

    public static void addOnActionEvent(ActionEvent listener) {
        onAction.add(listener);
    }

    public static void removeOnActionEvent(ActionEvent listener) {
        onAction.remove(listener);
    }

    //Actions (base)
    private static void showActionErrorsDialog(Context context, Action action) {
        //Create albums adapter
        DialogErrorsAdapter adapter = new DialogErrorsAdapter(context, action.errors);

        //Show confirmation dialog
        new MaterialAlertDialogBuilder(context)
                .setTitle("Errors")
                .setAdapter(adapter, (dialog, which) -> {})
                .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static void performAction(Context context, int type, CoonItem[] items, BiConsumer<Action, CoonItem> onPerformAction) {
        //Create action
        Action action = new Action(type, items);

        //Perform action for each item
        for (CoonItem item : items) onPerformAction.accept(action, item);

        //Check if trash was modified
        if (action.trashAction != Action.TRASH_NONE) {
            //Trash was emptied -> Clear leftover empty folders in trash folder
            if (action.trashAction == Action.TRASH_REMOVED) Orion.emptyFolder(trashFolder, false);

            //Save trash
            saveTrash();
        }

        //Check if gallery items were marked as removed
        if (!action.removedIndexesInGallery.isEmpty()) {
            //Marked as removed -> Sort indexes
            action.removedIndexesInGallery.sort((a, b) -> b - a); //Sort from last to first to allow using a foreach

            //Remove items
            for (int indexInGallery: action.removedIndexesInGallery) gallery.remove(indexInGallery);
        }

        //Check if albums were marked as removed
        if (!action.removedIndexesInAlbums.isEmpty()) {
            //Marked as removed -> Sort indexes
            action.removedIndexesInAlbums.sort((a, b) -> b - a); //Sort from last to first to allow using a foreach

            //Remove albums
            for (int indexInAlbums: action.removedIndexesInAlbums) {
                //Remove album
                removeAlbum(indexInAlbums);

                //Album was the first -> Mark "all" as updated
                if (indexInAlbums == 0) action.updatedAlbums.add(all);
            }
        }

        //Check if albums list was marked as sorted
        if (action.sortedAlbumsList) {
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

    private static void performRemoveFromAll(ActionHelper helper) {
        //Not in all items list
        if (helper.indexInAll == -1) return;

        //Remove item
        all.remove(helper.indexInAll);
    }

    private static void performRemoveFromGallery(ActionHelper helper, Action action) {
        //Not in gallery items list
        if (helper.indexInGallery == -1) return;

        //Mark it as removed (will get removed in performAction())
        action.removedIndexesInGallery.add(helper.indexInGallery);
    }

    private static void performRemoveFromAlbum(ActionHelper helper, Action action) {
        //Not in album
        if (helper.indexInAlbum == -1) return;

        //Get album
        Album album = helper.item.album;

        //Remove item
        album.remove(helper.indexInAlbum);

        //Check if album needs to be deleted or sorted
        if (album.isEmpty()) {
            //Album is empty -> Mark it as removed (will get removed in performAction())
            action.removedIndexesInAlbums.add(helper.indexOfAlbum);
        } else if (helper.indexInAlbum == 0) {
            //Album isn't empty & first image was deleted -> Sort albums list in case the order changed
            action.sortedAlbumsList = true;
        }
    }

    //Actions (modify items)
    public static void editItem(Context context, CoonItem item) {
        //Get mime type and URI
        String mimeType = item.mimeType;
        Uri uri = Orion.getMediaStoreUriFromFile(context, item.file, mimeType);

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
            intent.putExtra(Intent.EXTRA_STREAM, Orion.getUriFromFile(context, items[0].file));
            intent.setType(items[0].mimeType);
        } else {
            //Share multiple items
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Uri> URIs = new ArrayList<>();
            for (CoonItem item : items) URIs.add(Orion.getUriFromFile(context, item.file));
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, URIs);
            intent.setType("*/*");
        }
        context.startActivity(Intent.createChooser(intent, null));
    }

    private static void moveItemsInternal(Context context, CoonItem[] items, Album newAlbum) {
        performAction(context, Action.TYPE_MOVE, items, (action, item) -> {
            //Check new album
            if (newAlbum == null) {
                //Invalid destination
                action.errors.add(new ActionError(item, "Invalid destination"));
                return;
            }

            //Check if in trash
            if (item.hasTrashInfo()) {
                //Item is in trash
                action.errors.add(new ActionError(item, "Item is in the trash"));
                return;
            }

            //Get album
            Album oldAlbum = item.album;

            //Check if can move
            if (oldAlbum == newAlbum) {
                //Already in the destination album
                action.errors.add(new ActionError(item, "Item is already in the destination album"));
                return;
            }

            //Move item file
            File newFile = new File(newAlbum.getImagesPath(), item.name);
            if (!Orion.moveFile(item.file, newFile)) {
                //Failed to move file
                action.errors.add(new ActionError(item, "Error while moving item file"));
                return;
            }

            //Get action helper
            ActionHelper helper = action.getHelper(item);

            //Remove from gallery items list
            performRemoveFromGallery(helper, action);

            //Move item metadata from old album to new album
            if (oldAlbum.hasMetadataKey(item.name)) {
                //Copy metadata to new album
                newAlbum.setMetadataKey(item.name, oldAlbum.getMetadataKey(item.name));
                newAlbum.saveMetadata();

                //Remove metadata from old album
                oldAlbum.removeMetadataKey(item.name);
                oldAlbum.saveMetadata();
            }

            //Remove from old album
            performRemoveFromAlbum(helper, action);

            //Add to new album
            int newAlbumIndex = newAlbum.addSorted(item);
            item.album = newAlbum;

            //Check if item was added as the album cover
            if (newAlbumIndex == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.sortedAlbumsList = true;
            }
        });
    }

    public static void moveItems(Context context, CoonItem[] items) {
        //Create albums adapter
        DialogAlbumsAdapter adapter = new DialogAlbumsAdapter(context, albums, R.layout.library_move_item);

        //Show confirmation dialog
        new MaterialAlertDialogBuilder(context)
                .setTitle("Select a destination")
                .setAdapter(adapter, (dialog, which) -> {
                    //Move items to selected album
                    Album newAlbum = albums.get(which);
                    moveItemsInternal(context, items, newAlbum);
                })
                .setNeutralButton("Select External", (dialog, which) -> {
                    //Select an external folder
                    Toast.makeText(context, "Not implemented", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static void copyItemsInternal(Context context, CoonItem[] items, Album newAlbum) {
        performAction(context, Action.TYPE_COPY, items, (action, item) -> {
            //Check new album
            if (newAlbum == null) {
                //Invalid destination
                action.errors.add(new ActionError(item, "Invalid destination"));
                return;
            }

            //Check if in trash
            if (item.hasTrashInfo()) {
                //Item is in trash
                action.errors.add(new ActionError(item, "Item is in the trash"));
                return;
            }

            //Get old album
            Album oldAlbum = item.album;

            //Check if can move
            if (oldAlbum == newAlbum) {
                //Already in the destination album
                action.errors.add(new ActionError(item, "Item is already in the destination album"));
                return;
            }

            //Copy item file
            File newFile = new File(newAlbum.getImagesPath(), item.name);
            if (!Orion.cloneFile(context, item.file, newFile)) {
                //Failed to copy file
                action.errors.add(new ActionError(item, "Error while copying item file"));
                return;
            }

            //Copy item metadata from old album to new album
            if (oldAlbum.hasMetadataKey(item.name)) {
                newAlbum.setMetadataKey(item.name, oldAlbum.getMetadataKey(item.name));
                newAlbum.saveMetadata();
            }

            //Create new item
            CoonItem newItem = new CoonItem(newFile, newAlbum, item.lastModified, item.size, item.isVideo, null);

            //Add to new album
            int newAlbumIndex = newAlbum.addSorted(newItem);

            //Check if item was added as the album cover
            if (newAlbumIndex == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.sortedAlbumsList = true;
            }
        });
    }

    public static void copyItems(Context context, CoonItem[] items) {
        //Create albums adapter
        DialogAlbumsAdapter adapter = new DialogAlbumsAdapter(context, albums, R.layout.library_copy_item);

        //Show confirmation dialog
        new MaterialAlertDialogBuilder(context)
                .setTitle("Select a destination")
                .setAdapter(adapter, (dialog, which) -> {
                    //Copy items to selected album
                    Album newAlbum = albums.get(which);
                    copyItemsInternal(context, items, newAlbum);
                })
                .setNeutralButton("Select External", (dialog, which) -> {
                    //Select an external folder
                    Toast.makeText(context, "Not implemented", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void trashItems(Context context, CoonItem[] items) {
        performAction(context, Action.TYPE_TRASH, items, (action, item) -> {
            //Already has in trash
            if (item.album == trash || item.hasTrashInfo()) {
                action.errors.add(new ActionError(item, "Item is already in the trash"));
                return;
            }

            //Create trash info
            TrashInfo trashInfo = new TrashInfo(item.file.getAbsolutePath(), trashFolder.getAbsolutePath() + item.file.getPath(), item.isVideo);

            //Move item file to trash folder
            if (!Orion.cloneFile(context, item.file, trashInfo.trashFile)) {
                //Failed to copy file
                action.errors.add(new ActionError(item, "Error while copying item file"));
                return;
            }
            Orion.deleteFile(item.file);

            //Get action helper
            ActionHelper helper = action.getHelper(item);

            //Remove from all items list
            performRemoveFromAll(helper);

            //Remove from gallery items list
            performRemoveFromGallery(helper, action);

            //Remove from album
            performRemoveFromAlbum(helper, action);

            //Update item
            item.file = trashInfo.trashFile;
            item.album = trash;
            item.trashInfo = trashInfo;

            //Add item to trash album
            addTrashItem(item);
            if (action.trashAction == Action.TRASH_NONE) {
                //First change to trash this action -> Check if trash was added to list or just updated
                action.trashAction = trash.size() == 1 ? Action.TRASH_ADDED : Action.TRASH_UPDATED;
            }
        });
    }

    public static void restoreItems(Context context, CoonItem[] items) {
        performAction(context, Action.TYPE_RESTORE, items, (action, item) -> {
            //Not in trash
            if (item.album != trash || !item.hasTrashInfo()) {
                action.errors.add(new ActionError(item, "Item is not in the trash"));
                return;
            }

            //Get trash info
            TrashInfo trashInfo = item.trashInfo;
            if (trashInfo == null) {
                action.errors.add(new ActionError(item, "Invalid trash info"));
                return;
            }

            //Move item file to original folder
            if (!Orion.cloneFile(context, item.file, trashInfo.originalFile)) {
                //Failed to copy file
                action.errors.add(new ActionError(item, "Error while copying item file"));
                return;
            }
            Orion.deleteFile(item.file);

            //Get action helper
            ActionHelper helper = action.getHelper(item);

            //Check if item is in trash
            if (helper.indexInAlbum != -1) {
                //Is present -> Remove it
                removeTrashItem(helper.indexInAlbum);
                action.trashAction = trash.isEmpty() ? Action.TRASH_REMOVED : Action.TRASH_UPDATED;
            }

            //Remove from gallery items list
            performRemoveFromGallery(helper, action);

            //Get original album
            Album album = getOrCreateAlbumFromItemFile(trashInfo.originalFile);

            //Update item
            item.file = trashInfo.originalFile;
            item.album = album;
            item.trashInfo = null;

            //Add to all items list
            helper.indexInAll = all.addSorted(item);
            action.updatedAlbums.add(all);

            //Add to album
            helper.indexInAlbum = item.album.addSorted(item);
            action.updatedAlbums.add(item.album);

            //Check if item was added as the album cover
            if (helper.indexInAlbum == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.sortedAlbumsList = true;

                //Check if album is in albums list
                if (!albums.contains(album)) {
                    //Not in albums list -> Add it
                    albums.add(album);
                }
            }
        });
    }

    private static void deleteItemsInternal(Context context, CoonItem[] items) {
        performAction(context, Action.TYPE_DELETE, items, (action, item) -> {
            //Delete item file
            if (!Orion.deleteFile(item.file)) {
                //Failed to delete file
                action.errors.add(new ActionError(item, "Error while deleting item file"));
                return;
            }

            //Get album & action helper
            Album album = item.album;
            ActionHelper helper = action.getHelper(item);

            //Delete item metadata from album
            if (album.hasMetadataKey(item.name)) {
                album.removeMetadataKey(item.name);
                album.saveMetadata();
            }

            //Remove from all items list
            performRemoveFromAll(helper);

            //Remove from gallery items list
            performRemoveFromGallery(helper, action);

            //Check if item is in the trash
            if (item.hasTrashInfo()) {
                //In the trash -> Remove it
                removeTrashItem(helper.indexInAlbum);
                action.trashAction = trash.isEmpty() ? Action.TRASH_REMOVED : Action.TRASH_UPDATED;
            } else {
                //Not in the trash -> Remove from album
                performRemoveFromAlbum(helper, action);
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

    //Util
    public interface LoadingIndicator {

        void search();
        void load(String content);
        void load(String folder, String type);
        void hide();

    }

}
