package com.botpa.turbophotos.util;

import android.os.Environment;

import com.botpa.turbophotos.main.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Library {

    //Albums
    public static final ArrayList<Album> albums = new ArrayList<>();

    //Files
    public static final ArrayList<TurboImage> files = new ArrayList<>();
    public static final Map<Album, ArrayList<TurboImage>> filesWithoutMetadata = new HashMap<>();
    public static int filesWithoutMetadataCount = 0;


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
    public static void loadMetadata() {
        //Load metadata without loading indicator
        loadMetadata(null);
    }

    public static void loadMetadata(MainActivity.LoadingIndicator indicator) {
        //Clear previous files
        files.clear();
        filesWithoutMetadata.clear();
        filesWithoutMetadataCount = 0;

        //Load files from all albums
        for (Album album: Library.albums) {
            //Create missing metadata files list for the album
            filesWithoutMetadata.put(album, new ArrayList<>());

            //Check if images folder & metadata file exist
            File imagesFolder = new File(album.getAbsoluteImagesPath());
            if (!imagesFolder.exists()) continue;
            File metadataFile = new File(album.getAbsoluteMetadataPath());
            if (!metadataFile.exists()) continue;

            //Update load indicator
            if (indicator != null) indicator.show(imagesFolder.getName());

            //Load metadata
            album.loadMetadata();

            //Save images with a key in metadata
            File[] folder = imagesFolder.listFiles();
            if (folder == null) continue;
            for (File file: folder) {
                //Get file extension
                String extension = file.getName().toLowerCase();
                if (extension.startsWith(".")) continue; //Skip .trashed files
                if (!extension.contains(".")) continue; //Skip files without a format
                extension = extension.substring(extension.lastIndexOf(".") + 1);

                //Check if file is an image
                switch (extension) {
                    case "png":
                    case "jpg":
                    case "jpeg":
                    case "webp":
                        break;
                    default:
                        continue;
                }

                //Create image container
                TurboImage image = new TurboImage(file, album);

                //Check if image appears in metadata
                if (!album.metadata.has(file.getName())) {
                    filesWithoutMetadata.get(album).add(image);
                    filesWithoutMetadataCount++;
                }

                //Add image to list
                files.add(image);
            }
        }

        //Sort images by last modified
        files.sort((f1, f2) -> Long.compare(f2.file.lastModified(), f1.file.lastModified()));
    }
}
