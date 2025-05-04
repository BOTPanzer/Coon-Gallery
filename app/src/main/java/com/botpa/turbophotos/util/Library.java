package com.botpa.turbophotos.util;

import android.os.Environment;

import com.botpa.turbophotos.main.MainActivity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Library {

    //Albums
    public static final ArrayList<Album> albums = new ArrayList<>();

    //Files
    public static final ArrayList<TurboImage> files = new ArrayList<>();


    //Albums
    public static void loadAlbums() {
        //Clear previous albums
        albums.clear();

        //Get album pairs (strings)
        ArrayList<String> albumPairs = Storage.getStringList("Settings.albums");
        if (albumPairs.isEmpty()) albumPairs.add(Environment.getExternalStorageDirectory() + "/DCIM/Camera\n" + Environment.getExternalStorageDirectory() + "/DCIM/camera.metadata.json");

        //Split album strings & create the albums
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

            //Join in pair
            albums.add(new Album(imagesFolder, metadataFile));
        }
    }

    public static void saveAlbums() {
        //Save albums
        ArrayList<String> list = new ArrayList<>();
        for (Album album: albums) list.add(album.toString());
        Storage.putStringList("Settings.albums", list);

        //Make main activity restart on resume
        MainActivity.shouldRestart();
    }

    //Files
    public static void loadGallery() {
        //Load gallery without loading indicator
        loadGallery(null);
    }

    public static long loadGallery(MainActivity.LoadingIndicator indicator) {
        //Duration for testing which part is the slowest
        long duration = 0;
        long startTimestamp = new Date().toInstant().toEpochMilli();

        //Clear previous files
        files.clear();

        //Create filter
        FileFilter imageFileFilter = file -> {
            //Skip directories
            if (!file.isFile()) return false;

            //Skip hidden files (like .trashed)
            String fileName = file.getName().toLowerCase();
            if (fileName.startsWith(".")) return false;

            //Skip files without an extension
            int lastDotIndex = fileName.lastIndexOf(".");
            if (lastDotIndex == -1) return false;

            //Check if file is an image
            switch (fileName.substring(lastDotIndex + 1)) {
                case "png":
                case "jpg":
                case "jpeg":
                case "webp":
                    return true;
                default:
                    return false;
            }
        };

        //Load files from all albums
        for (Album album: Library.albums) {
            //Check if images folder & metadata file exist
            File imagesFolder = new File(album.getAbsoluteImagesPath());
            if (!imagesFolder.exists()) continue;
            File metadataFile = new File(album.getAbsoluteMetadataPath());
            if (!metadataFile.exists()) continue;

            //Update load indicator
            if (indicator != null) indicator.show(imagesFolder.getName(), "images");

            //Get folder files
            File[] folder = imagesFolder.listFiles(imageFileFilter);
            if (folder == null) continue;
            album.files.ensureCapacity(folder.length);

            //Save images with a key in metadata
            for (File file: folder) {
                //Create image container
                TurboImage image = new TurboImage(file, album, file.lastModified());

                //Add image to list
                album.files.add(image);
                files.add(image);
            }

            //Sort images by last modified
            album.files.sort((f1, f2) -> Long.compare(f2.lastModified, f1.lastModified));
        }

        //Sort images by last modified
        files.sort((f1, f2) -> Long.compare(f2.lastModified, f1.lastModified));

        duration += new Date().toInstant().toEpochMilli() - startTimestamp;
        return duration;
    }

    public static void loadMetadata(MainActivity.LoadingIndicator indicator) {
        //Load files from all albums
        for (Album album: Library.albums) loadMetadata(indicator, album);
    }

    public static void loadMetadata(MainActivity.LoadingIndicator indicator, Album album) {
        //Already loaded
        if (album.hasMetadata()) return;

        //Check if images folder & metadata file exist
        File imagesFolder = new File(album.getAbsoluteImagesPath());
        if (!imagesFolder.exists()) return;
        File metadataFile = new File(album.getAbsoluteMetadataPath());
        if (!metadataFile.exists()) return;

        //Update load indicator
        if (indicator != null) indicator.show(imagesFolder.getName(), "metadata");

        //Load metadata
        album.loadMetadata();
    }

}
