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

public class Library {

    //Album/Metadata links
    private static boolean linksLoaded = false;

    public static final ArrayList<Link> links = new ArrayList<>();
    public static final HashMap<String, Link> linksMap = new HashMap<>();

    //Albums
    private static long lastUpdate = 0;

    public static final ArrayList<Album> albums = new ArrayList<>();
    public static final HashMap<String, Album> albumsMap = new HashMap<>();

    //Files
    public static final ArrayList<TurboImage> allFiles = new ArrayList<>();


    //Album/Metadata links
    private static void loadLinks(boolean reset) {
        //Already loaded
        if (!reset && linksLoaded) return;
        linksLoaded = true;

        //Clear links
        links.clear();
        linksMap.clear();

        //Get links from storage (as strings)
        ArrayList<String> albumPairs = Storage.getStringList("Settings.albums");

        //Split album strings & create the links
        String imagesFolder, metadataFile;
        for (String albumString: albumPairs) {
            //Parse separator
            int index = albumString.indexOf("\n");
            if (index != -1) {
                imagesFolder = albumString.substring(0, index);
                metadataFile = albumString.substring(index + 1);
            } else {
                imagesFolder = albumString;
                metadataFile = "";
            }

            //Join as link
            addLink(new Link(imagesFolder, metadataFile));
        }
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


    //Albums
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
                MediaStore.Files.FileColumns.DATA,
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

    public static void sortAlbums() {
        albums.sort((a1, a2) -> Long.compare(a2.files.get(0).lastModified, a1.files.get(0).lastModified));
    }

    public static boolean loadAlbums(Context context, boolean reset) {
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
            int idxMediaType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int idxData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

            //Check files
            while (cursor.moveToNext()) {
                //Get bucket ID & check if it is already saved
                String bucketId = cursor.getString(idxBucketId);

                //Get other info
                String bucketName = cursor.getString(idxBucketDisplayName);
                long lastModified = cursor.getLong(idxLastModified);
                String mediaType = cursor.getString(idxMediaType);
                File file = new File(cursor.getString(idxData));

                //Get album
                Album album;
                if (!albumsMap.containsKey(bucketId)) {
                    //No album -> Create a new one

                    //Get images folder, album link & metadata file
                    File imagesFolder = file.getParentFile();
                    Link link = linksMap.getOrDefault(imagesFolder.getAbsolutePath(), null);
                    File metadataFile = link != null ? link.metadataFile : new File("");

                    //Create album
                    album = new Album(
                        bucketId,
                        imagesFolder,
                        metadataFile,
                        bucketName
                    );

                    //Save album & assign it to its metadata link
                    albumsMap.put(bucketId, album);
                    if (link != null) link.album = album;
                } else {
                    //Has album -> Get it
                    album = albumsMap.get(bucketId);
                }

                //Add file to album
                TurboImage image = new TurboImage(file, album, lastModified, mediaType);
                album.files.add(image);
                allFiles.add(image);
                updated = true;
            }
        } catch (Exception e) {
            Log.e("Library", "Error loading albums: " + e.getMessage());
        }

        //Update albums list with albums from map
        albums.clear();
        albums.addAll(albumsMap.values());

        //Sort all files & albums
        allFiles.sort((f1, f2) -> Long.compare(f2.lastModified, f1.lastModified));
        for (Album album: albums) album.sort();
        sortAlbums();

        //Save last update timestamp
        lastUpdate = System.currentTimeMillis();

        return updated;
    }

    public static void removeAlbum(int index) {
        Album album = albums.remove(index);
        albumsMap.remove(album.getId());
    }

    public static DeleteImageInfo deleteImage(TurboImage image) {
        //Create info
        DeleteImageInfo info = new DeleteImageInfo();

        //Get album
        Album album = image.album;

        //Get indexes
        info.indexInAll = Library.allFiles.indexOf(image);
        info.indexInAlbum = album.files.indexOf(image);
        info.indexOfAlbum = Library.albums.indexOf(album);

        //Delete image
        Orion.deleteFile(image.file);

        //Delete image metadata from album
        album.removeMetadataKey(image.getName());
        album.saveMetadata();

        //Check if image is in all files
        if (info.indexInAll != -1) {
            //Is present -> Remove it
            Library.allFiles.remove(info.indexInAll);
        }

        //Check if image is in album
        if (info.indexInAlbum != -1) {
            //Is present -> Remove it
            album.files.remove(info.indexInAlbum);

            //Check if album needs to be deleted or sorted
            if (album.files.isEmpty()) {
                //Album is empty -> Remove it from albums list
                Library.removeAlbum(info.indexOfAlbum);
                info.deletedAlbum = true;
            } else if (info.indexInAlbum == 0) {
                //Album isn't empty & deleted tge first image -> Sort albums in case the order changed
                Library.sortAlbums();
                info.sortedAlbums = true;
            }
        }

        //Return image deletion info
        return info;
    }

    public static class DeleteImageInfo {

        //All files
        public int indexInAll = -1;

        //Album files
        public int indexInAlbum = -1;

        //Albums
        public int indexOfAlbum = -1;
        public boolean deletedAlbum = false;
        public boolean sortedAlbums = false;

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

}
