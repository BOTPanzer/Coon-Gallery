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

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Link;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    //App
    private Slider galleryAlbumsPerRow;
    private Slider galleryImagesPerRow;

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
            Link album = Library.links.get(albumsFilePickerIndex);
            switch (albumsFilePickerAction) {
                case SelectFolder: {
                    boolean updated = Library.updateLinkFolder(albumsFilePickerIndex, file);
                    if (!updated) {
                        Orion.snack(SettingsActivity.this, "Album already exists");
                        return;
                    }
                    break;
                }
                case SelectFile: {
                    Library.updateLinkFile(albumsFilePickerIndex, file);
                    break;
                }
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
            Library.saveLinks();
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
        galleryAlbumsPerRow = findViewById(R.id.galleryAlbumsPerRow);
        galleryImagesPerRow = findViewById(R.id.galleryImagesPerRow);

        //Albums
        albumsFoldersList = findViewById(R.id.albumsFoldersList);
        albumsFoldersAdd = findViewById(R.id.albumsFoldersAdd);
    }

    private void loadSettings() {
        //App
        galleryAlbumsPerRow.setValue(Storage.getInt("Settings.galleryAlbumsPerRow", 2));
        galleryImagesPerRow.setValue(Storage.getInt("Settings.galleryImagesPerRow", 3));
    }

    private void initAlbumsList() {
        //Create album folders adapter
        albumsAdapter = new AlbumAdapter(this, Library.links);
        albumsAdapter.setOnDeleteListener((view, index) -> {
            //Remove album
            boolean removed = Library.removeLink(index);
            if (!removed) return;
            albumsAdapter.notifyItemRemoved(index);

            //Notify all albums starting from the removed one (to update number)
            if (index < Library.links.size()) albumsAdapter.notifyItemRangeChanged(index, Library.links.size() - index);

            //Save
            Library.saveLinks();
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
                if (!Library.links.get(index).imagesFolder.exists()) {
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
        galleryAlbumsPerRow.addOnChangeListener((slider, value, fromUser) -> {
            Storage.putInt("Settings.galleryAlbumsPerRow", (int) value);
        });
        galleryImagesPerRow.addOnChangeListener((slider, value, fromUser) -> {
            Storage.putInt("Settings.galleryImagesPerRow", (int) value);
        });

        //Albums
        albumsFoldersAdd.setOnClickListener(view -> {
            boolean added = Library.addLink(new Link("", ""));
            if (added) {
                albumsAdapter.notifyItemInserted(Library.links.size() - 1);
                albumsFoldersList.scrollToPosition(Library.links.size() - 1);
                Library.saveLinks();
            } else {
                Orion.snack(SettingsActivity.this, "Can't have duplicate albums");
            }
        });
    }

}
