package com.botpa.turbophotos.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    //App
    private Slider galleryItemsPerRow;

    //Albums
    private AlbumAdapter albumsAdapter;
    private int albumsFilePickerIndex;
    private enum PickerAction { SelectFolder, SelectFile, CreateFile }
    private PickerAction albumsFilePickerAction;
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
            switch (albumsFilePickerAction) {
                case SelectFolder:
                    album.imagesFolder = file;
                    break;
                case SelectFile:
                    album.metadataFile = file;
                    break;
                case CreateFile:
                    File metadataFile;
                    String name;
                    int i = 0;
                    do {
                        name = album.imagesFolder.getName().toLowerCase().replace(" ", "-") + (i > 0 ? " (" + i + ")" : "") + ".metadata.json";
                        metadataFile =  new File(file.getAbsolutePath() + "/" + name);
                        i++;
                    } while (metadataFile.exists());
                    Orion.writeFile(metadataFile, "{}");
                    album.metadataFile = metadataFile;
                    break;
            }
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

        //Albums
        albumsFoldersList = findViewById(R.id.albumsFoldersList);
        albumsFoldersAdd = findViewById(R.id.albumsFoldersAdd);
    }

    private void loadSettings() {
        //App
        galleryItemsPerRow.setValue(Storage.getInt("Settings.galleryItemsPerRow", 3));
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
            //Save album index
            albumsFilePickerIndex = index;

            //Feedback toast
            Toast.makeText(SettingsActivity.this, "Select a folder to use as album", Toast.LENGTH_LONG).show();

            //Ask for a folder
            albumsFilePickerAction = PickerAction.SelectFolder;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            albumsFilePickerLauncher.launch(intent);
        });
        albumsAdapter.setOnChooseFileListener((view, index) -> {
            //Save album index
            albumsFilePickerIndex = index;

            //Check action
            Orion.snack2(SettingsActivity.this, "Do you have an already created metadata file?", "Select", () -> {
                //Feedback toast
                Toast.makeText(SettingsActivity.this, "Select a file to use as metadata", Toast.LENGTH_LONG).show();

                //Ask for a file
                albumsFilePickerAction = PickerAction.SelectFile;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                albumsFilePickerLauncher.launch(intent);
            }, "Create", () -> {
                //Album folder is needed to take the name
                if (!Library.albums.get(index).imagesFolder.exists()) {
                    Toast.makeText(SettingsActivity.this, "Please select an album folder first", Toast.LENGTH_LONG).show();
                    return;
                }

                //Feedback toast
                Toast.makeText(SettingsActivity.this, "Select a folder to create the album metadata", Toast.LENGTH_LONG).show();

                //Ask for a folder & create file inside
                albumsFilePickerAction = PickerAction.CreateFile;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                albumsFilePickerLauncher.launch(intent);
            }, Snackbar.LENGTH_INDEFINITE);
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

        //Albums
        albumsFoldersAdd.setOnClickListener(view -> {
            Library.albums.add(new Album("", ""));
            albumsAdapter.notifyItemInserted(Library.albums.size() - 1);
            albumsFoldersList.scrollToPosition(Library.albums.size() - 1);
            Library.saveAlbums();
        });
    }
}
