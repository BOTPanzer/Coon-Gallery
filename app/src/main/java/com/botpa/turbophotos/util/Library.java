package com.botpa.turbophotos.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import com.botpa.turbophotos.main.MainActivity;

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
        MainActivity.shouldReload();
    }

    public static boolean addLink(Link link) {
        //Check if link exists
        String key = link.getImagesPath();
        if (linksMap.containsKey(key)) return false;

        //Add link
        links.add(link);
        linksMap.put(key, link);
        return true;
    }

    public static boolean removeLink(int index) {
        //Check if link exists
        if (index < 0 || index >= links.size()) return false;

        //Remove link
        Link link = links.remove(index);
        linksMap.remove(link.getImagesPath());
        return true;
    }

    public static boolean updateLinkFolder(int index, File newFolder) {
        //Check if album is already in a link
        String keyNew = newFolder.getAbsolutePath();
        if (linksMap.containsKey(keyNew)) return false;

        //Update
        String keyOld = links.get(index).getImagesPath();
        links.get(index).imagesFolder = newFolder;
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

    //Actions
    private static Action performAction(int actionType, TurboItem[] items, BiConsumer<Action, TurboItem> onPerformAction) {
        //Create action
        Action action = new Action(actionType, items);

        //Perform action for each item
        for (TurboItem item : items) onPerformAction.accept(action, item);

        //Trash was modified
        if (action.trashChanged != Action.TRASH_NONE) {
            //No more items -> Empty trash folder
            if (action.trashChanged == Action.TRASH_REMOVED) Orion.emptyFolder(trashFolder, false);

            //Save trash
            saveTrash();
        }

        //Albums list was marked as sorted
        if (action.sortedAlbumsList) {
            //Sort albums list
            sortAlbumsList();
        }

        //Return action
        return action;
    }

    public static Action restoreItems(Context context, TurboItem[] items) {
        return performAction(Action.TYPE_RESTORE, items, (action, item) -> {
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
                action.trashChanged = trash.isEmpty() ? Action.TRASH_REMOVED : Action.TRASH_UPDATED;
            }

            //Add item to all items
            all.addSorted(item);
            action.sortedAlbums.add(all);

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

    public static Action trashItems(Context context, TurboItem[] items) {
        return performAction(Action.TYPE_TRASH, items, (action, item) -> {
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
            if (action.trashChanged == Action.TRASH_NONE) {
                //First change to trash this action -> Check if trash was added to list or just updated
                action.trashChanged = trash.size() == 1 ? Action.TRASH_ADDED : Action.TRASH_UPDATED;
            }

            //Get album & action helper
            Album album = item.album;
            ActionHelper helper = action.getHelper(item);

            //Check if item is in all items
            if (helper.indexInAll != -1) {
                //Is present -> Remove it
                all.remove(helper.indexInAll);
            }

            //Check if item is in album
            if (helper.indexInAlbum != -1) {
                //Is present -> Remove it
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
        });
    }

    public static Action deleteItems(TurboItem[] items) {
        return performAction(Action.TYPE_DELETE, items, (action, item) -> {
            //Delete item file
            boolean deleted = Orion.deleteFile(item.file);
            if (!deleted) {
                //Failed to delete file
                action.failed.put(item, "Error while deleting a file");
                return;
            }

            //Get album & action helper
            Album album = item.album;
            ActionHelper helper = action.getHelper(item);

            //Delete item metadata from album
            album.removeMetadataKey(item.getName());
            album.saveMetadata();

            //Check if item is in trash
            if (helper.indexInTrash != -1) {
                //Is present -> Remove it
                Item(helper.indexInTrash);
                action.trashChanged = trash.isEmpty() ? Action.TRASH_REMOVED : Action.TRASH_UPDATED;

                //Check to finish removing album
                if (helper.indexOfAlbum == -1 && !trashAlbumsMap.containsKey(item.album)) {
                    //Album not used in albums list nor in trash items -> Finish removing it completely
                    albumsMap.remove(album.getImagesPath());
                }
            }

            //Check if item is in all items
            if (helper.indexInAll != -1) {
                //Is present -> Remove it
                all.remove(helper.indexInAll);
            }

            //Check if item is in album
            if (helper.indexInAlbum != -1) {
                //Is present -> Remove it
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
