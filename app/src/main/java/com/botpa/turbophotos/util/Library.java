package com.botpa.turbophotos.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.botpa.turbophotos.main.MainActivity;
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

    //Links
    private static boolean linksLoaded = false;

    public static final ArrayList<Link> links = new ArrayList<>();
    public static final HashMap<String, Link> linksMap = new HashMap<>();

    //Trash
    private static boolean trashLoaded = false;

    public static final Album trash = new Album("Trash");
    public static final HashMap<Album, ArrayList<TurboItem>> trashAlbumsMap = new HashMap<>(); //Albums of files in trash
    public static File trashFolder;

    //Albums
    private static long lastUpdate = 0;

    public static final Album all = new Album("All");
    public static final ArrayList<Album> albums = new ArrayList<>();
    public static final HashMap<String, Album> albumsMap = new HashMap<>(); //Uses album path as key

    //Gallery
    public static final ArrayList<TurboItem> gallery = new ArrayList<>();

    //Actions
    private static final ArrayList<ActionEvent> onAction = new ArrayList<>();


    //Library
    private static Cursor getMediaCursor(Context context) {
        //Get content resolver
        ContentResolver contentResolver = context.getContentResolver();

        //Get cursor resolver
        return contentResolver.query(
                //Collection
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                //Projection
                new String[] {
                        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
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
                        String.valueOf(lastUpdate),
                        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                },
                //Sorting order
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC" //Sort by date (newest first)
        );
    }

    public static boolean loadLibrary(Context context, boolean reset) {
        //Load links & trash
        loadLinks(reset);
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
            int idxBucketDisplayName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME);
            int idxLastModified = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int idxSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int idxMediaType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int idxData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

            //Check itemss
            while (cursor.moveToNext()) {
                //Get other info
                String bucketName = cursor.getString(idxBucketDisplayName);
                File file = new File(cursor.getString(idxData));
                long lastModified = cursor.getLong(idxLastModified);
                long size = cursor.getLong(idxSize);
                boolean isVideo = (Objects.equals(cursor.getString(idxMediaType), String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)));

                //Get album
                Album album;
                File parent = file.getParentFile();
                String parentPath = parent != null ? parent.getAbsolutePath() : null;
                if (albumsMap.containsKey(parentPath)) {
                    //Has album -> Get it
                    album = albumsMap.get(parentPath);
                } else {
                    //No album -> Create a new one
                    album = createAlbum(parent, bucketName);
                }
                assert album != null;

                //Create item
                TurboItem item = new TurboItem(file, album, lastModified, size, isVideo, null);
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

        //Sort albums & albums list
        all.sort();
        for (Album album: albums) album.sort();
        sortAlbumsList();

        //Return true if the albums were updated
        return updated;
    }

    //Links
    private static void loadLinks(boolean reset) {
        //Already loaded
        if (!reset && linksLoaded) return;
        linksLoaded = true;

        //Clear links
        links.clear();
        linksMap.clear();

        //Get links from storage (as strings)
        ArrayList<String> linksUnparsed = Storage.getStringList("Settings.albums");

        //Parse links
        for (String string: linksUnparsed) addLink(Link.Companion.parse(string));
    }

    public static void saveLinks() {
        //Save links
        ArrayList<String> list = new ArrayList<>();
        for (Link link: links) list.add(link.toString());
        Storage.putStringList("Settings.albums", list);

        //Restart main activity on resume
        MainActivity.reloadOnResume();
    }

    public static boolean addLink(Link link) {
        //Check if link exists
        String key = link.getAlbumPath();
        if (linksMap.containsKey(key)) return false;

        //Add link
        links.add(link);
        linksMap.put(key, link);

        //Relink with album if it exists
        link.album = albumsMap.getOrDefault(link.getAlbumPath(), null);
        if (link.album != null) link.album.updateMetadataFile(link.metadataFile);
        return true;
    }

    public static boolean removeLink(int index) {
        //Check if link exists
        if (index < 0 || index >= links.size()) return false;

        //Remove link
        Link link = links.remove(index);
        linksMap.remove(link.getAlbumPath());
        return true;
    }

    public static boolean updateLinkFolder(int index, File newFolder) {
        //Check if album is already in a link
        String keyNew = newFolder.getAbsolutePath();
        if (linksMap.containsKey(keyNew)) return false;

        //Update
        String keyOld = links.get(index).getAlbumPath();
        links.get(index).albumFolder = newFolder;
        linksMap.put(keyNew, linksMap.get(keyOld));
        linksMap.remove(keyOld);
        return true;
    }

    public static void updateLinkFile(int index, File newFile) {
        //Update
        links.get(index).metadataFile = newFile;
    }

    //Trash
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

            //Get album
            Album album;
            File originalFileParent = info.originalFile.getParentFile();
            assert originalFileParent != null;
            String originalFileParentPath = originalFileParent.getAbsolutePath();
            if (albumsMap.containsKey(originalFileParentPath)) {
                //Album exists -> Take it
                album = albumsMap.get(originalFileParentPath);
            } else {
                //Album does not exist -> Create it
                album = createAlbum(originalFileParent, originalFileParent.getName());
            }
            assert album != null;

            //Create item & add it to trash album
            addTrashItem(new TurboItem(info.trashFile, album, info.trashFile.lastModified(), info.trashFile.length(), info.isVideo(), info));
        }
        trash.sort();

        //Trash was updated while loading -> Save it
        if (updated) Storage.putStringList("Settings.trash", trashUnparsed);
    }

    private static void saveTrash() {
        //Save trash
        ArrayList<String> list = new ArrayList<>();
        for (ArrayList<TurboItem> items: trashAlbumsMap.values()) {
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

    private static void addTrashItem(TurboItem item) {
        //Add item to trash album
        trash.addSorted(item);

        //Add item to trash albums map
        ArrayList<TurboItem> list = trashAlbumsMap.computeIfAbsent(item.album, key -> new ArrayList<>());   //Get list of items, create and save a new one if missing
        list.add(item);
    }

    private static void Item(int itemIndex) {
        //Remove item from trash album
        TurboItem item = trash.remove(itemIndex);

        //Remove item from trash albums map
        ArrayList<TurboItem> list = trashAlbumsMap.computeIfAbsent(item.album, key -> new ArrayList<>());   //Get list of items, create and save a new one if missing
        list.remove(item);
        if (list.isEmpty()) trashAlbumsMap.remove(item.album);
    }

    //Albums
    private static Album createAlbum(File imagesFolder, String name) {
        //Get folder path
        String folderPath = imagesFolder != null ? imagesFolder.getAbsolutePath() : null;

        //Get album link
        Link link = linksMap.getOrDefault(folderPath, null);
        boolean hasLink = link != null;

        //Get metadata file
        File metadataFile = hasLink ? link.metadataFile : null;

        //Create new album
        Album album = new Album(name, imagesFolder, metadataFile);

        //Save album & assign it to its link
        albumsMap.put(album.getImagesPath(), album);
        if (hasLink) link.album = album;

        //Return album
        return album;
    }

    private static void addAlbum(Album album) {
        //Album is already in list
        if (albums.contains(album)) return;

        //Add album to list
        albums.add(album);
        albumsMap.put(album.getImagesPath(), album);
        sortAlbumsList();
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
    private static boolean filterFile(TurboItem item, String filter) {
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
        for (TurboItem item: album.items) {
            //No filter -> Skip check
            if (!isFiltering) {
                gallery.add(item);
                continue;
            }

            //Check if json contains filter
            if (filterFile(item, filter)) Library.gallery.add(item);
        }
    }

    //Action (events)
    public interface ActionEvent {

        void onAction(Action action);

    }

    private static void invokeAction(Action action) {
        for (ActionEvent listener: onAction) listener.onAction(action);
    }

    public static void addOnActionEvent(ActionEvent listener) {
        onAction.add(listener);
    }

    public static void removeOnActionEvent(ActionEvent listener) {
        onAction.remove(listener);
    }

    //Actions (modify items)
    private static void performAction(int type, TurboItem[] items, BiConsumer<Action, TurboItem> onPerformAction) {
        //Create action
        Action action = new Action(type, items);

        //Perform action for each item
        for (TurboItem item : items) onPerformAction.accept(action, item);

        //Trash was modified
        if (action.trashChanges != Action.TRASH_NONE) {
            //No more items -> Empty trash folder
            if (action.trashChanges == Action.TRASH_REMOVED) Orion.emptyFolder(trashFolder, false);

            //Save trash
            saveTrash();
        }

        //Albums list was marked as sorted
        if (action.sortedAlbumsList) {
            //Sort albums list
            sortAlbumsList();
        }

        //Invoke action
        invokeAction(action);
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

        //Remove item
        gallery.remove(helper.indexInGallery);

        //Add shifted index to gallery
        action.removedIndexesInGallery.add(helper.indexInGallery);
    }

    private static void performRemoveFromAlbum(ActionHelper helper, Action action) {
        //Not in album items list
        if (helper.indexInAlbum == -1) return;

        //Get album
        Album album = helper.item.album;

        //Remove item
        album.remove(helper.indexInAlbum);

        //Check if album needs to be deleted or sorted
        if (album.isEmpty()) {
            //Album is empty -> Remove it from list & mark it as deleted
            removeAlbum(helper.indexOfAlbum);
            action.deletedAlbums.add(album);
        } else if (helper.indexInAlbum == 0) {
            //Album isn't empty & first image was deleted -> Sort albums list in case the order changed
            action.sortedAlbumsList = true;
        }
    }

    private static void deleteItems(TurboItem[] items) {
        performAction(Action.TYPE_DELETE, items, (action, item) -> {
            //Delete item file
            if (!Orion.deleteFile(item.file)) {
                //Failed to delete file
                action.failed.put(item, "Error while deleting a file");
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

            //Check if item is in trash
            if (helper.indexInTrash != -1) {
                //Is present -> Remove it
                Item(helper.indexInTrash);
                action.trashChanges = trash.isEmpty() ? Action.TRASH_REMOVED : Action.TRASH_UPDATED;

                //Check to finish removing album
                if (helper.indexOfAlbum == -1 && !trashAlbumsMap.containsKey(item.album)) {
                    //Album not used in albums list nor in trash items -> Finish removing it completely
                    albumsMap.remove(album.getImagesPath());
                }
            }

            //Remove from all items list
            performRemoveFromAll(helper);

            //Remove from gallery items list
            performRemoveFromGallery(helper, action);

            //Remove from album items list
            performRemoveFromAlbum(helper, action);
        });
    }

    public static void deleteItems(Context context, TurboItem[] items) {
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

        //Delete item & manage action
        new MaterialAlertDialogBuilder(context)
                .setMessage(message.toString())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, whichButton) -> deleteItems(items))
                .show();
    }

    public static void trashItems(Context context, TurboItem[] items) {
        performAction(Action.TYPE_TRASH, items, (action, item) -> {
            //Already in trash
            if (item.isTrashed()) {
                action.failed.put(item, "Item is already trashed");
                return;
            }

            //Create trash info
            TrashInfo trashInfo = new TrashInfo(item.file.getAbsolutePath(), trashFolder.getAbsolutePath() + item.file.getPath(), item.isVideo);

            //Move item file to trash folder
            if (!Orion.cloneFile(context, item.file, trashInfo.trashFile)) {
                action.failed.put(item, "Could not clone original file to trash path");
                return;
            }
            Orion.deleteFile(item.file);

            //Update item
            item.file = trashInfo.trashFile;
            item.trashInfo = trashInfo;

            //Add item to trash album
            addTrashItem(item);
            if (action.trashChanges == Action.TRASH_NONE) {
                //First change to trash this action -> Check if trash was added to list or just updated
                action.trashChanges = trash.size() == 1 ? Action.TRASH_ADDED : Action.TRASH_UPDATED;
            }

            //Get action helper
            ActionHelper helper = action.getHelper(item);

            //Remove from all items list
            performRemoveFromAll(helper);

            //Remove from gallery items list
            performRemoveFromGallery(helper, action);

            //Remove from album items list
            performRemoveFromAlbum(helper, action);
        });
    }

    public static void restoreItems(Context context, TurboItem[] items) {
        performAction(Action.TYPE_RESTORE, items, (action, item) -> {
            //Not in trash
            if (!item.isTrashed()) {
                action.failed.put(item, "Item is not trashed");
                return;
            }

            //Get trash info
            TrashInfo trashInfo = item.trashInfo;
            if (trashInfo == null) {
                action.failed.put(item, "Invalid trash info");
                return;
            }

            //Move item file to original folder
            if (!Orion.cloneFile(context, item.file, trashInfo.originalFile)) {
                action.failed.put(item, "Could not clone trash file to original path");
                return;
            }
            Orion.deleteFile(item.file);

            //Update item
            item.file = trashInfo.originalFile;
            item.trashInfo = null;

            //Get action helper
            ActionHelper helper = action.getHelper(item);

            //Check if item is in trash
            if (helper.indexInTrash != -1) {
                //Is present -> Remove it
                Item(helper.indexInTrash);
                action.trashChanges = trash.isEmpty() ? Action.TRASH_REMOVED : Action.TRASH_UPDATED;
            }

            //Add item to all items
            all.addSorted(item);
            action.sortedAlbums.add(all);

            //Remove from gallery items list
            performRemoveFromGallery(helper, action);

            //Add item to album
            item.album.addSorted(item);
            action.sortedAlbums.add(item.album);
            helper.indexInAlbum = item.album.indexOf(item);

            //Check album state
            if (helper.indexInAlbum == 0) {
                //Item is the first in the album -> Check if the album is in albums list
                if (helper.indexOfAlbum == -1) {
                    //Album is missing -> Add it to albums list
                    addAlbum(item.album);
                }

                //Sort albums list in case the order changed
                action.sortedAlbumsList = true;
            }
        });
    }

    public static void shareItems(Context context, TurboItem[] items) {
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
            for (TurboItem item : items) URIs.add(Orion.getUriFromFile(context, item.file));
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, URIs);
            intent.setType("*/*");
        }
        context.startActivity(Intent.createChooser(intent, null));
    }

    public static void moveItems(TurboItem[] items, Album destination) {
        performAction(Action.TYPE_MOVE, items, (action, item) -> {
            //Check destination
            if (destination == null) {
                //Invalid destination
                action.failed.put(item, "Invalid destination");
                return;
            }

            //Check if in trash
            if (item.isTrashed()) {
                //Item is in trash
                action.failed.put(item, "File is in the trash");
                return;
            }

            //Check if can move
            if (item.album == destination) {
                //Already in the destination album
                action.failed.put(item, "File does not need to be moved");
                return;
            }

            //Move item file
            File newFile = new File(destination.getImagesPath(), item.name);
            if (!Orion.moveFile(item.file, newFile)) {
                //Failed to move file
                action.failed.put(item, "Error while moving a file");
                return;
            }

            //Get album & action helper
            Album album = item.album;
            ActionHelper helper = action.getHelper(item);

            //Remove from gallery items list
            performRemoveFromGallery(helper, action);

            //Move item metadata from old album to destination
            if (album.hasMetadataKey(item.name)) {
                destination.setMetadataKey(item.name, album.getMetadataKey(item.name));
                destination.saveMetadata();
                album.removeMetadataKey(item.name);
                album.saveMetadata();
            }

            //Remove from album items list
            performRemoveFromAlbum(helper, action);

            //Add to destination album items list
            int destinationIndex = destination.addSorted(item);
            if (destinationIndex == 0) {
                //Added as the album cover -> Sort albums list in case the order changed
                action.sortedAlbumsList = true;
            }
            item.album = destination;
        });
    }

    //Util
    public interface LoadingIndicator {

        void search();
        void load(String content);
        void load(String folder, String type);
        void hide();

    }

}
