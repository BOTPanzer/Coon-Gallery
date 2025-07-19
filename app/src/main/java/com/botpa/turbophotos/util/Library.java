package com.botpa.turbophotos.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import com.botpa.turbophotos.main.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class Library {

    //Album/Metadata links
    private static boolean linksLoaded = false;

    public static final ArrayList<Link> links = new ArrayList<>();
    public static final HashMap<String, Link> linksMap = new HashMap<>();

    //Trash
    private static boolean trashLoaded = false;

    public static final Album trash = new Album(Album.TRASH_ID, new File(""), new File(""), "Trash");
    public static final HashSet<TrashInfo> trashInfos = new HashSet<>();
    public static File trashFolder;

    //Albums
    private static long lastUpdate = 0;

    public static final ArrayList<Album> albums = new ArrayList<>();
    public static final HashMap<String, Album> albumsMap = new HashMap<>();     //Uses bucketId as key
    public static final HashMap<String, Album> albumsPathMap = new HashMap<>(); //Uses absolute path as key

    //Files
    public static final ArrayList<TurboFile> allFiles = new ArrayList<>();


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
                        MediaStore.Files.FileColumns.BUCKET_ID,
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
        //Load links
        loadLinks(reset);

        //Reset
        if (reset) {
            //Reset last update timestamp
            lastUpdate = 0;

            //Clear files from all files & albums
            allFiles.clear();
            for (Album album: albums) album.reset();
        }

        //Get album files
        boolean updated = false;
        try (Cursor cursor = getMediaCursor(context)) {
            //Get indexes for all variables
            int idxBucketId = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID);
            int idxBucketDisplayName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME);
            int idxLastModified = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int idxSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int idxMediaType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int idxData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

            //Check files
            while (cursor.moveToNext()) {
                //Get bucket ID & check if it is already saved
                String bucketId = cursor.getString(idxBucketId);

                //Get other info
                String bucketName = cursor.getString(idxBucketDisplayName);
                File file = new File(cursor.getString(idxData));
                long lastModified = cursor.getLong(idxLastModified);
                long size = cursor.getLong(idxSize);
                boolean isVideo = (Objects.equals(cursor.getString(idxMediaType), String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)));

                //Get album
                Album album;
                if (albumsMap.containsKey(bucketId)) {
                    //Has album -> Get it
                    album = albumsMap.get(bucketId);
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && albumsPathMap.containsKey(parent.getAbsolutePath())) {
                        //Has album -> Get it
                        album = albumsPathMap.get(bucketId);

                        //Update album id since it does not have one
                        album.setId(bucketId);
                        albumsMap.put(bucketId, album);
                    } else {
                        //No album -> Create a new one

                        //Get images folder, album link & metadata file
                        File imagesFolder = file.getParentFile();

                        //Create album
                        album = createAlbum(bucketId, imagesFolder, bucketName);
                    }
                }

                //Create file
                TurboFile turboFile = new TurboFile(file, album, lastModified, size, isVideo, null);
                Library.allFiles.add(turboFile);
                album.files.add(turboFile);

                //Mark library as updated
                updated = true;
            }
        } catch (Exception e) {
            Log.e("Library", "Error loading albums: " + e.getMessage());
        }

        //Load trash
        loadTrash(context, reset);

        //Update albums list with non empty albums from map
        albums.clear();
        for (Album album: albumsMap.values()) {
            if (album.files.isEmpty()) continue;
            albums.add(album);
        }

        //Sort all files & albums
        sort(allFiles);
        trash.sort();
        for (Album album: albums) album.sort();
        sortAlbumsList();

        //Save last update timestamp
        lastUpdate = System.currentTimeMillis();

        return updated;
    }

    //Album/Metadata links
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
        String key = link.getAbsoluteImagesPath();
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
        linksMap.remove(links.get(index).getAbsoluteImagesPath());
        links.remove(index);
        return true;
    }

    public static boolean updateLinkFolder(int index, File newFolder) {
        //Check if album is already in a link
        String keyNew = newFolder.getAbsolutePath();
        if (linksMap.containsKey(keyNew)) return false;

        //Update
        String keyOld = links.get(index).getAbsoluteImagesPath();
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
        boolean trashFolderCreated = trashFolder.mkdirs();

        //Clear trash
        trash.reset();
        trashInfos.clear();

        //Get trash files from storage (as strings)
        ArrayList<String> trashUnparsed = Storage.getStringList("Settings.trash");

        //Parse trash info strings
        boolean updated = false;
        for (int i = trashUnparsed.size() - 1; i >= 0; i--) {
            //Parse info
            String string = trashUnparsed.get(i);
            TrashInfo info = TrashInfo.Companion.parse(string);

            //Check if trashed file exists
            File trashedFile = new File(info.getTrashPath());
            if (!trashedFile.exists()) {
                //File does not exist -> Remove trash info
                trashUnparsed.remove(i);
                updated = true;
                continue;
            }

            //Save info
            trashInfos.add(info);

            //Get album
            Album album;
            File originalFile = new File(info.getOriginalPath());
            File originalFileParent = originalFile.getParentFile();
            String originalFileParentPath = originalFile.getParent();
            if (albumsPathMap.containsKey(originalFileParentPath)) {
                //Album exists -> Take it
                album = albumsPathMap.get(originalFileParentPath);
            } else {
                //Album does not exist -> Create it
                album = createAlbum("", originalFileParent, originalFileParent.getName());
            }

            //Create file & add it to trash album
            TurboFile turboFile = new TurboFile(trashedFile, album, trashedFile.lastModified(), trashedFile.length(), info.isVideo(), info);
            Library.trash.files.add(turboFile);
        }

        //Trash was updated while loading -> Save it
        if (updated) Storage.putStringList("Settings.trash", trashUnparsed);
    }

    private static void saveTrash() {
        //Save trash
        ArrayList<String> list = new ArrayList<>();
        for (TrashInfo info: trashInfos) list.add(info.toString());
        Storage.putStringList("Settings.trash", list);
    }

    //Albums
    public static void sortAlbumsList() {
        albums.sort((a1, a2) -> Long.compare(a2.files.get(0).lastModified, a1.files.get(0).lastModified));
    }

    public static Album createAlbum(String id, File imagesFolder, String name) {
        //Get folder path
        String imagesFolderPath = imagesFolder != null ? imagesFolder.getAbsolutePath() : null;

        //Get album link & metadata file
        Link link = linksMap.getOrDefault(imagesFolderPath, null);
        File metadataFile = link != null ? link.metadataFile : new File("");

        //Create new album
        Album album = new Album(id, imagesFolder, metadataFile, name);

        //Save album & assign it to its link
        albumsMap.put(album.getId(), album);
        albumsPathMap.put(album.getAbsoluteImagesPath(), album);
        if (link != null) link.album = album;

        //Return album
        return album;
    }

    public static void removeAlbum(int index) {
        Album album = albums.remove(index);
        albumsMap.remove(album.getId());
        albumsPathMap.remove(album.getAbsoluteImagesPath());
    }

    public static class FileActionInfo {

        //All files
        public int indexInAll = -1;

        //Album files
        public int indexInAlbum = -1;

        //Albums
        public int indexOfAlbum = -1;
        public boolean deletedAlbum = false;
        public boolean sortedAlbums = false;

        //Trash
        public boolean modifiedTrash = false;

    }

    public static FileActionInfo deleteFile(TurboFile file) {
        //Create info
        FileActionInfo info = new FileActionInfo();

        //Get album
        Album album = file.album;

        //Get indexes
        info.indexInAll = Library.allFiles.indexOf(file);
        info.indexInAlbum = album.files.indexOf(file);
        info.indexOfAlbum = Library.albums.indexOf(album);

        //Delete file
        Orion.deleteFile(file.file);

        //Delete file metadata from album
        album.removeMetadataKey(file.getName());
        album.saveMetadata();

        //Check if file is in all files
        if (info.indexInAll != -1) {
            //Is present -> Remove it
            Library.allFiles.remove(info.indexInAll);
        }

        //Check if file is in album
        if (info.indexInAlbum != -1) {
            //Is present -> Remove it
            album.files.remove(info.indexInAlbum);

            //Check if album needs to be deleted or sorted
            if (album.files.isEmpty()) {
                //Album is empty -> Remove it from albums list
                Library.removeAlbum(info.indexOfAlbum);
                info.deletedAlbum = true;
            } else if (info.indexInAlbum == 0) {
                //Album isn't empty & deleted the first image -> Sort albums in case the order changed
                Library.sortAlbumsList();
                info.sortedAlbums = true;
            }
        }

        //Return file action info
        return info;
    }

    public static FileActionInfo trashFile(TurboFile file) {
        //Create info
        FileActionInfo info = new FileActionInfo();

        //File already in trash -> Return
        if (file.isTrashed()) return info;

        //Move file
        File trashFile = new File(trashFolder.getAbsolutePath() + file.file.getPath());
        boolean success = Orion.cloneFile(file.file, trashFile);
        if (!success) return info;
        Orion.deleteFile(file.file);    //Delete original file
        file.file = trashFile;          //Update file to the new one

        //Get album
        Album album = file.album;

        //Get indexes
        info.indexInAll = Library.allFiles.indexOf(file);
        info.indexInAlbum = album.files.indexOf(file);
        info.indexOfAlbum = Library.albums.indexOf(album);

        //Create trash info
        TrashInfo trashInfo = new TrashInfo(file.file.getAbsolutePath(), trashFile.getAbsolutePath(), file.isVideo);
        file.trashInfo = trashInfo;
        trashInfos.add(trashInfo);
        saveTrash();

        //Add TurboFile to trash album
        trash.files.add(file);
        trash.sort();
        info.modifiedTrash = true;

        //Check if file is in all files
        if (info.indexInAll != -1) {
            //Is present -> Remove it
            Library.allFiles.remove(info.indexInAll);
        }

        //Check if file is in album
        if (info.indexInAlbum != -1) {
            //Is present -> Remove it
            album.files.remove(info.indexInAlbum);

            //Check if album needs to be sorted
            if (!album.files.isEmpty() && info.indexInAlbum == 0) {
                //Album isn't empty & trashed the first image -> Sort albums in case the order changed
                Library.sortAlbumsList();
                info.sortedAlbums = true;
            }
        }

        //Return file action info
        return info;
    }

    //Metadata
    public static void loadMetadata(LoadingIndicator indicator) {
        //Load files from all albums
        for (Album album: Library.albums) loadMetadata(indicator, album);
    }

    public static void loadMetadata(LoadingIndicator indicator, Album album) {
        //Already loaded
        if (album.hasMetadata()) return;

        //Check if images folder & metadata file exist
        if (!album.exists()) return;

        //Update load indicator
        if (indicator != null) indicator.load(album.getImagesFolder().getName(), "metadata");

        //Load metadata
        album.loadMetadata();
    }

    //Util
    public interface LoadingIndicator {
        void search();
        void load(String content);
        void load(String folder, String type);
        void hide();
    }

    public static void sort(ArrayList<TurboFile> files) {
        files.sort((f1, f2) -> Long.compare(f2.lastModified, f1.lastModified));
    }

}
