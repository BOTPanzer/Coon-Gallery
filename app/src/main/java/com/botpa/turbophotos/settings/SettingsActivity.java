package com.botpa.turbophotos.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    //App
    private Slider galleryItemsPerRow;
    private MaterialSwitch showMissingMetadata;

    //Albums
    private AlbumAdapter albumsAdapter;
    private int albumsFilePickerIndex;
    private boolean albumsFilePickerIsFolder;
    private final ActivityResultLauncher<Intent> albumsFilePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        //Bad result
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;

        //parse file path from URI
        try {
            String path = Orion.convertUriToFilePath(SettingsActivity.this, result.getData().getData());
            if (path == null) throw new Exception("Path was null");
            File file = new File(path);

            //Update
            Album album = Library.albums.get(albumsFilePickerIndex);
            if (albumsFilePickerIsFolder)
                album.imagesFolder = file;
            else
                album.metadataFile = file;
            albumsAdapter.notifyItemChanged(albumsFilePickerIndex);

            //Save albums
            Library.saveAlbums();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    });

    private RecyclerView albumsFoldersList;
    private View albumsFoldersAdd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        //Load storage
        Storage.load(SettingsActivity.this);


        //Get views
        getViews();


        //Load settings
        loadSettings();


        //Init albums list
        initAlbumsList();


        //Add listeners
        addListeners();
    }

    //App
    private void getViews() {
        //App
        galleryItemsPerRow = findViewById(R.id.galleryItemsPerRow);
        showMissingMetadata = findViewById(R.id.showMissingMetadata);

        //Albums
        albumsFoldersList = findViewById(R.id.albumsFoldersList);
        albumsFoldersAdd = findViewById(R.id.albumsFoldersAdd);
    }

    private void loadSettings() {
        //App
        galleryItemsPerRow.setValue(Storage.getInt("Settings.galleryItemsPerRow", 3));
        showMissingMetadata.setChecked(Storage.getBool("Settings.showMissingMetadataMessage", true));
    }

    private void initAlbumsList() {
        //Create album folders adapter
        albumsAdapter = new AlbumAdapter(this, Library.albums);
        albumsAdapter.setOnDeleteListener((view, index) -> {
            if (Library.albums.size() <= 1) return;

            //Remove album
            Library.albums.remove(index);
            albumsAdapter.notifyItemRemoved(index);

            //Notify all albums starting from the removed one (to update number)
            if (index < Library.albums.size()) albumsAdapter.notifyItemRangeChanged(index, Library.albums.size() - index);

            //Save
            Library.saveAlbums();
        });
        albumsAdapter.setOnChooseFolderListener((view, index) -> {
            //Save info
            albumsFilePickerIndex = index;
            albumsFilePickerIsFolder = true;

            //Get start folder
            //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DocumentFile.fromTreeUri(MainActivity.this, Uri.parse(albums.get(index).imagesFolder.getPath())).getUri());

            //Ask for a folder
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            albumsFilePickerLauncher.launch(intent);
        });
        albumsAdapter.setOnChooseFileListener((view, index) -> {
            //Save info
            albumsFilePickerIndex = index;
            albumsFilePickerIsFolder = false;

            //Get start folder
            //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DocumentFile.fromTreeUri(MainActivity.this, Uri.parse(albums.get(index).metadataFile.getParent())).getUri());

            //Ask for a file
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            albumsFilePickerLauncher.launch(intent);
        });

        //Add adapter to list
        albumsFoldersList.setAdapter(albumsAdapter);
        albumsFoldersList.setLayoutManager(new LinearLayoutManager(this));
        albumsFoldersList.setItemAnimator(null);
    }

    private void addListeners() {
        //App
        galleryItemsPerRow.addOnChangeListener((slider, value, fromUser) -> {
            Storage.putInt("Settings.galleryItemsPerRow", (int) value);
        });

        showMissingMetadata.setOnCheckedChangeListener((compoundButton, b) -> {
            Storage.putBool("Settings.showMissingMetadataMessage", b);
        });

        //Albums
        albumsFoldersAdd.setOnClickListener(view -> {
            Library.albums.add(new Album("", ""));
            albumsAdapter.notifyItemInserted(Library.albums.size() - 1);
            albumsFoldersList.scrollToPosition(Library.albums.size() - 1);
            Library.saveAlbums();
        });
    }
}
