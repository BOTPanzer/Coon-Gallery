package com.botpa.turbophotos.util;

import android.os.Environment;
import android.util.Log;

import com.botpa.turbophotos.main.MainActivity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;

/** @noinspection ResultOfMethodCallIgnored*/
public class Library {

    //Albums
    public static final ArrayList<Album> albums = new ArrayList<>();

    //Files
    public static final ArrayList<TurboImage> allFiles = new ArrayList<>();
    public static boolean allFilesUpToDate = false;

    //Cache
    private static ObjectNode cache;


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
    private static void sortFiles(ArrayList<TurboImage> files) {
        files.sort((f1, f2) -> Long.compare(f2.lastModified, f1.lastModified));
    }

    public static void loadGalleryFromCache(LoadingIndicator indicator) {
        //Update load indicator
        indicator.load("gallery");

        //No albums
        if (albums.isEmpty()) loadAlbums();

        //Clear previous files
        allFiles.clear();

        //Load cache
        loadCache();

        //Load albums files from cache
        allFilesUpToDate = true;
        for (Album album: Library.albums) {
            //Check if images folder & metadata file exist
            if (!album.exists()) continue;

            //Clear album files
            album.clearFiles();

            //Try to load images from cache
            String albumImagesPath = album.imagesFolder.getAbsolutePath();
            try {
                if (cache.has(albumImagesPath)) {
                    //Cache exists -> Get album cache
                    JsonNode albumCache = cache.get(albumImagesPath);

                    //Check if cache has last modified timestamp & files list
                    if (albumCache.has("lastModified") && albumCache.has("files")) {
                        //Get cached last modified timestamp & files list
                        long cachedLastModified = albumCache.get("lastModified").asLong();
                        ArrayNode cachedFiles = (ArrayNode) albumCache.get("files");

                        //Check timestamps
                        album.isUpToDate = cachedLastModified == album.imagesFolder.lastModified();
                        if (!album.isUpToDate) allFilesUpToDate = false;

                        //Load images from cache
                        album.files.ensureCapacity(cachedFiles.size());

                        //Get files
                        for (int i = 0; i < cachedFiles.size(); i++) {
                            //Get file info
                            JsonNode node = cachedFiles.get(i);
                            String name = node.get("name").asText();
                            long lastModified = node.get("lastModified").asLong();

                            //Create image container
                            TurboImage image = new TurboImage(new File(albumImagesPath + "/" + name), album, lastModified);

                            //Add image to list
                            album.files.add(image);
                            if (album.isUpToDate) allFiles.add(image); //Add to all files only if up to date
                        }

                        //Successfully loaded album from cache -> Skip to next
                        Log.i("Library", "Loaded cache load for \"" + album.getName() + "\"");
                        continue;
                    }
                    Log.i("Library", "Skipping cache load for \"" + album.getName() + "\"");
                }
            } catch (Exception e){
                //Error loading cache -> Clear files & load from disk
                Log.i("Library", "Couldn't load cache for \"" + album.getName() + "\". Reason: " + e.getMessage());

                //Clear album files
                album.clearFiles();

                //Mark album & all files as not up to date
                album.isUpToDate = false;
                allFilesUpToDate = false;
            }
        }

        //Sort all files
        sortFiles(allFiles);
    }

    public static long loadGalleryFromDisk(LoadingIndicator indicator) {
        //Duration for testing which part is the slowest
        long startTimestamp = new Date().toInstant().toEpochMilli();

        //Create images filter
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
            if (!album.exists()) continue;

            //Check if album is up to date
            if (album.isUpToDate) continue;

            //Create new album files
            ArrayList<TurboImage> newFiles = new ArrayList<>();

            //Update load indicator & clear album files
            indicator.load(album.imagesFolder.getName(), "images");

            //Get files from folder
            File[] files = album.imagesFolder.listFiles(imageFileFilter);
            if (files == null) continue;

            //Resize lists
            album.files.ensureCapacity(files.length);
            allFiles.ensureCapacity(allFiles.size() + files.length);

            //Save images
            for (File file: files) {
                //Create image container
                TurboImage image = new TurboImage(file, album, file.lastModified());

                //Add image to lists
                newFiles.add(image);
                allFiles.add(image);
            }
            album.files = newFiles;

            //Sort album & mark as up to date
            sortFiles(album.files);
            album.isUpToDate = true;

            //Remake cache for this album
            remakeCacheForAlbum(album, false);
        }

        //Sort all files
        sortFiles(allFiles);

        //Mark all files as up to date
        allFilesUpToDate = true;

        //Save cache
        saveCache();

        //Return the time it took to load gallery
        return new Date().toInstant().toEpochMilli() - startTimestamp;
    }

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
        if (indicator != null) indicator.load(album.imagesFolder.getName(), "metadata");

        //Load metadata
        album.loadMetadata();
    }

    //Cache
    private static void loadCache() {
        //Already loaded
        if (cache != null) return;

        //Get cache file
        File cacheFile = new File(Environment.getExternalStorageDirectory() + "/CoonGallery/cache.json");

        //Load cache
        cache = Orion.loadJson(cacheFile);
    }

    private static void saveCache() {
        //No cache
        if (cache == null) return;

        //Get cache file
        File cacheFile = new File(Environment.getExternalStorageDirectory() + "/CoonGallery/cache.json");

        //Crate parent folder in case it does not exist
        String parentPath = cacheFile.getParent();
        if (parentPath != null) new File(parentPath).mkdir();

        //Save cache file
        Orion.writeJson(cacheFile, cache);
    }

    public static void remakeCacheForAlbum(Album album, boolean save) {
        //Update cache for this album
        ObjectNode albumCache = Orion.getEmptyJson();

        //Set last modified date
        albumCache.put("lastModified", album.imagesFolder.lastModified());

        //Set files list
        ArrayNode albumCacheFiles = Orion.getEmptyJsonArray();
        for (TurboImage image: album.files) {
            ObjectNode file = Orion.getEmptyJson();
            file.put("name", image.file.getName());
            file.put("lastModified", image.lastModified);
            albumCacheFiles.add(file);
        }
        albumCache.set("files", albumCacheFiles);

        //Update cache
        cache.set(album.imagesFolder.getAbsolutePath(), albumCache);
        if (save) saveCache();
    }

    //Util
    public interface LoadingIndicator {
        void search();
        void load(String content);
        void load(String folder, String type);
        void hide();
    }

}
