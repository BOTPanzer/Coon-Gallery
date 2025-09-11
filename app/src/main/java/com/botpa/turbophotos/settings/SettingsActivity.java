package com.botpa.turbophotos.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Link;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    //App
    private Slider galleryAlbumsPerRow;
    private Slider galleryImagesPerRow;
    private MaterialSwitch galleryMissingMetadataIcon;

    //Links
    private LinksAdapter linksAdapter;
    private int linksFilePickerIndex;
    private enum PickerAction { SelectFolder, SelectFile, CreateFile }
    private PickerAction linksFilePickerAction;
    private final ActivityResultLauncher<Intent> linksFilePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        //Bad result
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;

        //Parse file path from URI
        try {
            String path = Orion.convertUriToFilePath(SettingsActivity.this, result.getData().getData());
            if (path == null) throw new Exception("Path was null");
            File file = new File(path);

            //Update
            Link album = Library.links.get(linksFilePickerIndex);
            switch (linksFilePickerAction) {
                case SelectFolder: {
                    boolean updated = Library.updateLinkFolder(linksFilePickerIndex, file);
                    if (!updated) {
                        Orion.snack(SettingsActivity.this, "Album already exists");
                        return;
                    }
                    break;
                }
                case SelectFile: {
                    Library.updateLinkFile(linksFilePickerIndex, file);
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
            linksAdapter.notifyItemChanged(linksFilePickerIndex);

            //Save albums
            Library.saveLinks();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    });

    private RecyclerView linksFoldersList;
    private View linksFoldersAdd;


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

        //Init links list
        initLinksList();

        //Add listeners
        addListeners();
    }

    //App
    private void getViews() {
        //App
        galleryAlbumsPerRow = findViewById(R.id.galleryAlbumsPerRow);
        galleryImagesPerRow = findViewById(R.id.galleryImagesPerRow);
        galleryMissingMetadataIcon = findViewById(R.id.galleryMissingMetadataIcon);

        //Links
        linksFoldersList = findViewById(R.id.linksList);
        linksFoldersAdd = findViewById(R.id.linksAdd);

        //Insets
        Orion.addInsetsChangedListener(getWindow().getDecorView(), new int[] { WindowInsetsCompat.Type.systemBars(), WindowInsetsCompat.Type.ime() });
    }

    private void loadSettings() {
        //App
        galleryAlbumsPerRow.setValue(Storage.getInt("Settings.galleryAlbumsPerRow", 2));
        galleryImagesPerRow.setValue(Storage.getInt("Settings.galleryImagesPerRow", 3));
        galleryMissingMetadataIcon.setChecked(Storage.getBool("Settings.showMissingMetadataIcon", false));
    }

    private void initLinksList() {
        //Create links adapter
        linksAdapter = new LinksAdapter(this, Library.links);
        linksAdapter.setOnDeleteListener((view, index) -> {
            //Remove link
            boolean removed = Library.removeLink(index);
            if (!removed) return;
            linksAdapter.notifyItemRemoved(index);

            //Notify all links starting from the removed one (to update their number)
            if (index < Library.links.size()) linksAdapter.notifyItemRangeChanged(index, Library.links.size() - index);

            //Save
            Library.saveLinks();
        });
        linksAdapter.setOnChooseFolderListener((view, index) -> {
            //Save link index
            linksFilePickerIndex = index;

            //Feedback toast
            Toast.makeText(SettingsActivity.this, "Select a folder to use as album", Toast.LENGTH_LONG).show();

            //Ask for a folder
            linksFilePickerAction = PickerAction.SelectFolder;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            linksFilePickerLauncher.launch(intent);
        });
        linksAdapter.setOnChooseFileListener((view, index) -> {
            //Save link index
            linksFilePickerIndex = index;

            //Check action
            Orion.snack2(SettingsActivity.this, "Do you have an already created metadata file?", "Select", () -> {
                //Feedback toast
                Toast.makeText(SettingsActivity.this, "Select a file to use as metadata", Toast.LENGTH_LONG).show();

                //Ask for a file
                linksFilePickerAction = PickerAction.SelectFile;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                linksFilePickerLauncher.launch(intent);
            }, "Create", () -> {
                //Album folder is needed to take the name
                if (!Library.links.get(index).imagesFolder.exists()) {
                    Toast.makeText(SettingsActivity.this, "Please select an album folder first", Toast.LENGTH_LONG).show();
                    return;
                }

                //Feedback toast
                Toast.makeText(SettingsActivity.this, "Select a folder to create the album metadata file", Toast.LENGTH_LONG).show();

                //Ask for a folder & create file inside
                linksFilePickerAction = PickerAction.CreateFile;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                linksFilePickerLauncher.launch(intent);
            }, Snackbar.LENGTH_INDEFINITE);
        });

        //Add adapter to list
        linksFoldersList.setAdapter(linksAdapter);
        linksFoldersList.setLayoutManager(new LinearLayoutManager(this));
        linksFoldersList.setItemAnimator(null);
    }

    private void addListeners() {
        //App
        galleryAlbumsPerRow.addOnChangeListener((slider, value, fromUser) -> {
            Storage.putInt("Settings.galleryAlbumsPerRow", (int) value);
        });
        galleryImagesPerRow.addOnChangeListener((slider, value, fromUser) -> {
            Storage.putInt("Settings.galleryImagesPerRow", (int) value);
        });
        galleryMissingMetadataIcon.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Storage.putBool("Settings.showMissingMetadataIcon", isChecked);
        });

        //Links
        linksFoldersAdd.setOnClickListener(view -> {
            boolean added = Library.addLink(new Link("", ""));
            if (added) {
                linksAdapter.notifyItemInserted(Library.links.size() - 1);
                linksFoldersList.scrollToPosition(Library.links.size() - 1);
                Library.saveLinks();
            } else {
                Orion.snack(SettingsActivity.this, "Can't have duplicate links");
            }
        });
    }

}
