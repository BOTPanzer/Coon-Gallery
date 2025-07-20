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

public class Library {

    //Links
    private static boolean linksLoaded = false;

    public static final ArrayList<Link> links = new ArrayList<>();
    public static final HashMap<String, Link> linksMap = new HashMap<>();

    //Trash
    private static boolean trashLoaded = false;

    public static final Album trash = new Album("Trash");
    public static final HashMap<Album, ArrayList<TurboFile>> trashAlbumsMap = new HashMap<>(); //Albums of files in trash
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
                MediaStore.Files.FileColumns.DATE_MODIFIED + " > ? AND (" + MediaStore.Files.FileColumns.MEDIA_TYPE + "= ? OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "= ?)",
                //Selection args
                new String[] {
                        String.valueOf(lastUpdate / 1000),
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

            //Clear files from albums
            all.reset();
            for (Album album: albums) album.reset();

            //Albums map doesn't get cleared so that the gallery can stay on the selected album on reload :D
        }

        //Get album files
        boolean updated = false;
        try (Cursor cursor = getMediaCursor(context)) {
            //Save last update timestamp
            lastUpdate = System.currentTimeMillis();

            //Get indexes for all columns
            int idxBucketDisplayName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME);
            int idxLastModified = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int idxSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int idxMediaType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int idxData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

            //Check files
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

                //Create file
                TurboFile turboFile = new TurboFile(file, album, lastModified, size, isVideo, null);
                all.add(turboFile);
                album.add(turboFile);

                //Mark library as updated
                updated = true;
            }
        } catch (Exception e) {
            Log.e("Library", "Error loading albums: " + e.getMessage());
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

            //Check if album is used by trash files
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

        //Make main activity restart on resume
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
        if (!trashFolder.exists() && trashFolder.mkdirs()) Log.e("TRASH", "Could not create trash folder");

        //Clear trash
        trash.reset();
        trashAlbumsMap.clear();

        //Get trash files from storage (as strings)
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

            //Create file & add it to trash album
            addTrashFile(new TurboFile(info.trashFile, album, info.trashFile.lastModified(), info.trashFile.length(), info.isVideo(), info));
        }
        trash.sort();

        //Trash was updated while loading -> Save it
        if (updated) Storage.putStringList("Settings.trash", trashUnparsed);
    }

    private static void saveTrash() {
        //Save trash
        ArrayList<String> list = new ArrayList<>();
        for (ArrayList<TurboFile> files: trashAlbumsMap.values()) {
            for (int i = files.size() - 1; i >= 0; i--) {
                //Get file trash info & remove file if not in trash
                TrashInfo trashInfo = files.get(i).trashInfo;
                if (trashInfo == null) {
                    files.remove(i);
                    continue;
                }

                //Add trash info to list
                list.add(trashInfo.toString());
            }
        }
        Storage.putStringList("Settings.trash", list);
    }

    private static void addTrashFile(TurboFile file) {
        //Add file to trash album
        trash.add(file);

        //Add file to trash albums map
        ArrayList<TurboFile> list = trashAlbumsMap.computeIfAbsent(file.album, key -> new ArrayList<>());   //Get list of files, create and save a new one if missing
        list.add(file);
    }

    private static void removeTrashFile(int fileIndex) {
        //Remove file from trash album
        TurboFile file = trash.remove(fileIndex);

        //Remove file to trash albums map
        ArrayList<TurboFile> list = trashAlbumsMap.computeIfAbsent(file.album, key -> new ArrayList<>());   //Get list of files, create and save a new one if missing
        list.remove(file);
        if (list.isEmpty()) trashAlbumsMap.remove(file.album);
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

        //Remove from maps if no files in trash are from this album
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
            //All files album -> Load metadata from all albums
            for (Album _album: albums) loadMetadataHelper(indicator, _album);
        } else {
            //Normal album -> Load album metadata
            loadMetadataHelper(indicator, album);
        }
    }

    //Actions
    public static FileActionResult deleteFile(TurboFile file) {
        //Create action result
        FileActionResult result = new FileActionResult(file);

        //Delete file
        Orion.deleteFile(file.file);

        //Delete file metadata from album
        file.album.removeMetadataKey(file.getName());
        file.album.saveMetadata();

        //Check if file is in trash
        if (result.indexInTrash != -1) {
            //Is present -> Remove it
            removeTrashFile(result.indexInTrash);
            saveTrash();

            //Check if album was only used in trash and is now empty
            if (result.indexOfAlbum == -1 && !trashAlbumsMap.containsKey(file.album)) {
                //Album is empty and no trash files are part of it -> Finish removing it it completely
                albumsMap.remove(file.album.getImagesPath());
            }
        }

        //Check if file is in all files
        if (result.indexInAll != -1) {
            //Is present -> Remove it
            all.remove(result.indexInAll);
        }

        //Check if file is in album
        if (result.indexInAlbum != -1) {
            //Is present -> Remove it
            file.album.remove(result.indexInAlbum);

            //Check if album needs to be deleted or sorted
            if (file.album.isEmpty()) {
                //Album is empty -> Remove it from albums list
                removeAlbum(result.indexOfAlbum);
                result.deletedAlbum = true;
            } else if (result.indexInAlbum == 0) {
                //Album isn't empty & deleted the first image -> Sort albums in case the order changed
                sortAlbumsList();
                result.sortedAlbumsList = true;
            }
        }

        //Return action result
        result.action = FileActionResult.ACTION_DELETE;
        return result;
    }

    public static FileActionResult trashFile(Context context, TurboFile file) {
        //Create action result
        FileActionResult result = new FileActionResult(file);

        //File already in trash -> Return
        if (file.isTrashed()) {
            result.info = "File is already trashed";
            return result;
        }

        //Create trash info
        TrashInfo trashInfo = new TrashInfo(file.file.getAbsolutePath(), trashFolder.getAbsolutePath() + file.file.getPath(), file.isVideo);

        //Move file to trash folder
        if (!Orion.cloneFile(context, file.file, trashInfo.trashFile)) {
            result.info = "Could not clone original file to trash path";
            return result;
        }
        Orion.deleteFile(file.file);

        //Update file
        file.file = trashInfo.trashFile;
        file.trashInfo = trashInfo;

        //Add file to trash album
        addTrashFile(file);
        trash.sort();
        saveTrash();

        //Check if file is in all files
        if (result.indexInAll != -1) {
            //Is present -> Remove it
            all.remove(result.indexInAll);
        }

        //Check if file is in album
        if (result.indexInAlbum != -1) {
            //Is present -> Remove it
            file.album.remove(result.indexInAlbum);

            //Check if album needs to be deleted or sorted
            if (file.album.isEmpty()) {
                //Album is empty -> Remove it from albums list
                removeAlbum(result.indexOfAlbum);
                result.deletedAlbum = true;
            } else if (result.indexInAlbum == 0) {
                //Album isn't empty & deleted the first image -> Sort albums in case the order changed
                sortAlbumsList();
                result.sortedAlbumsList = true;
            }
        }

        //Return action result
        result.action = FileActionResult.ACTION_TRASH;
        return result;
    }

    public static FileActionResult restoreFile(Context context, TurboFile file) {
        //Create action result
        FileActionResult result = new FileActionResult(file);

        //File not in trash -> Return
        if (!file.isTrashed()) {
            result.info = "File is not trashed";
            return result;
        }

        //Get trash info
        TrashInfo trashInfo = file.trashInfo;
        if (trashInfo == null) {
            result.info = "Invalid trash info";
            return result;
        }

        //Move file to original folder
        if (!Orion.cloneFile(context, file.file, trashInfo.originalFile)) {
            result.info = "Could not clone trash file to original path";
            return result;
        }
        Orion.deleteFile(file.file);

        //Update file
        file.file = trashInfo.originalFile;
        file.trashInfo = null;

        //Remove file from trash
        if (result.indexInTrash != -1) {
            //Is present -> Remove it
            removeTrashFile(result.indexInTrash);
            saveTrash();
        }

        //Add file to all files
        all.add(file);
        all.sort();
        result.indexInAll = all.indexOf(file);

        //Add file to album
        file.album.add(file);
        file.album.sort();
        result.indexInAlbum = file.album.indexOf(file);

        //Check album state
        if (result.indexInAlbum == 0) {
            //File is the first in the album -> Check if the album is in albums list
            if (result.indexOfAlbum == -1) {
                //Album is missing -> Add it to albums list
                addAlbum(file.album);
            }

            //Sort albums in case the order changed
            sortAlbumsList();
            result.sortedAlbumsList = true;
        }

        //Return action result
        result.action = FileActionResult.ACTION_RESTORE;
        return result;
    }

    //Util
    public interface LoadingIndicator {
        void search();
        void load(String content);
        void load(String folder, String type);
        void hide();
    }

}
