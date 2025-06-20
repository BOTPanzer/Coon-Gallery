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
import java.util.Map;

public class Library {

    //Albums
    public static final ArrayList<Album> albums = new ArrayList<>();
    public static final ArrayList<Link> links = new ArrayList<>();
    public static final HashMap<String, Link> linksMap = new HashMap<>();

    //Files
    public static final ArrayList<TurboImage> allFiles = new ArrayList<>();


    //Links
    private static void loadLinks() {
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
        MainActivity.shouldRestart();
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
    public static void loadAlbums(Context context) {
        //Load links
        loadLinks();

        //Get content resolver
        ContentResolver contentResolver = context.getContentResolver();

        //Create temp albums map
        Map<String, Album> albumMap = new HashMap<>();

        //Get album files
        try (Cursor cursor = contentResolver.query(
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
                MediaStore.Files.FileColumns.MEDIA_TYPE + "= ? OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "= ?",
                //Selection args
                new String[] {
                        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                },
                //Sorting order
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC" //Sort by date (newest first)
        )) {
            if (cursor != null) {
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
                    if (!albumMap.containsKey(bucketId)) {
                        //No album -> Create a new one

                        //Get images folder, album link & metadata file
                        File imagesFolder = file.getParentFile();
                        Link link = linksMap.getOrDefault(imagesFolder.getAbsolutePath(), null);
                        File metadataFile = link != null ? link.metadataFile : new File("");

                        //Create album
                        album = new Album(
                                imagesFolder,
                                metadataFile,
                                bucketId,
                                bucketName,
                                imagesFolder.lastModified()
                        );

                        //Save album & assign it to its metadata link
                        albumMap.put(bucketId, album);
                        if (link != null) link.album = album;
                    } else {
                        //Has album -> Get it
                        album = albumMap.get(bucketId);
                    }

                    //Add file to album
                    TurboImage image = new TurboImage(file, album, lastModified, mediaType);
                    album.files.add(image);
                    allFiles.add(image);
                }
            }
        } catch (Exception e) {
            Log.e("Library", "Error loading albums: " + e.getMessage());
        }

        //Add albums to list
        albums.clear();
        albums.addAll(albumMap.values());
        albums.sort((f1, f2) -> Long.compare(f2.getLastModified(), f1.getLastModified()));
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
