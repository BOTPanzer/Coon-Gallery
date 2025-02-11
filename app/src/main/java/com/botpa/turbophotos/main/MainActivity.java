package com.botpa.turbophotos.main;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboImage;
import com.botpa.turbophotos.settings.SettingsActivity;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //Permissions
    private boolean permissionWrite = false;
    private boolean permissionCheck = false;

    //App
    private BackManager backManager;
    private static boolean shouldRestart = false;

    //Files
    private boolean isLoading = false;

    //Settings
    private CardView settings;

    //Gallery
    private final ArrayList<TurboImage> galleryFiles = new ArrayList<>();

    private GalleryAdapter galleryAdapter;
    private RecyclerView galleryList;

    //Search
    private boolean isSearching = false;

    private TextView searchFilterText;

    private View searchLayoutClosed;
    private View searchOpen;

    private View searchLayoutOpen;
    private CardView searchClose;
    private EditText searchText;

    private View searchIndicator;
    private TextView searchIndicatorText;

    //Display
    private TurboImage displayCurrent;

    private View displayLayout;
    private TextView displayNameText;
    private View displayClose;
    private View displayInfo;
    private View displayEdit;
    private View displayOptions;
    private ZoomableImageView displayImage;
    private View displayOverlayLayout;

    private View displayEditLayout;
    private TextView displayEditCaptionText;
    private TextView displayEditLabelsText;
    private View displayEditSave;

    private View displayInfoLayout;
    private TextView displayInfoCaptionText;
    private HorizontalScrollView displayInfoLabelsScroll;
    private TextView displayInfoLabelsText;

    private View displayOptionsLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //EdgeToEdge.enable(this); //Enable edge to edge (fullscreen)

        //Load storage
        Storage.load(MainActivity.this);

        //Check permissions
        checkPermissions();
    }

    //Permission
    @Override
    public void onResume() {
        super.onResume();

        //Restart
        if (shouldRestart) {
            shouldRestart = false;
            recreate();
            return;
        }

        //Check for permissions
        if (permissionCheck) {
            permissionCheck = false;
            checkPermissions();
        }

        //Update gallery horizontal item count
        if (galleryList != null) {
            GridLayoutManager gridLayoutManager = ((GridLayoutManager) galleryList.getLayoutManager());
            if (gridLayoutManager != null) {
                int newHorizontalItemCount = Storage.getInt("Settings.galleryItemsPerRow", 3);
                if (gridLayoutManager.getSpanCount() != newHorizontalItemCount) {
                    gridLayoutManager.setSpanCount(newHorizontalItemCount);
                    galleryAdapter.notifyItemRangeChanged(0, galleryAdapter.getItemCount());
                }
            }
        }
    }

    private void checkPermissions() {
        //Show permission layout
        findViewById(R.id.permissionLayout).setVisibility(View.VISIBLE);

        //Button listeners
        findViewById(R.id.permissionWrite).setOnClickListener(v -> {
            permissionCheck = true;
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        });

        //Check for permissions
        if (Environment.isExternalStorageManager()) permissionWrite = true;

        //Has permissions
        if (permissionWrite) {
            findViewById(R.id.permissionLayout).setVisibility(View.GONE);

            //Start
            loadApp();
        }
    }

    //App
    public static void shouldRestart() {
        //Enable restart on resume
        shouldRestart = true;
    }

    private void loadApp() {
        //App
        backManager = new BackManager(MainActivity.this, getOnBackPressedDispatcher());
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);


        //Get views
        getViews();


        //Load settings
        Library.loadAlbums();


        //Load metadata
        loadMetadata();


        //Create gallery
        galleryAdapter = new GalleryAdapter(this, galleryFiles);
        galleryAdapter.setOnItemClickListener((view, index) -> {
            //Get file & metadata from list
            displayCurrent = galleryFiles.get(index);

            //Load image in display
            Glide.with(MainActivity.this).asBitmap().load(displayCurrent.file).into(displayImage);
            displayImage.fit(); //In case the image is the same as before, make it fit
            displayNameText.setText(displayCurrent.file.getName());

            //Prepare options menu
            findViewById(R.id.displayOptionsDelete).setOnClickListener(view2 -> {
                //Delete metadata key
                displayCurrent.album.metadata.remove(displayCurrent.file.getName());
                displayCurrent.album.saveMetadata();

                //Delete image
                Orion.deleteFile(displayCurrent.file);
                Library.files.remove(displayCurrent);
                galleryFiles.remove(index);
                galleryAdapter.notifyItemRemoved(index);
                displayClose.performClick();

                //Close menu
                displayOptionsLayout.performClick();
            });

            findViewById(R.id.displayOptionsShare).setOnClickListener(view2 -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, Orion.getUriFromFile(MainActivity.this, displayCurrent.file));
                intent.setType("image/*");
                startActivity(Intent.createChooser(intent, null));

                //Close menu
                displayOptionsLayout.performClick();
            });

            findViewById(R.id.displayOptionsOpen).setOnClickListener(view2 -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(displayCurrent.file.getAbsolutePath()), "image/*");
                startActivity(intent);

                //Close menu
                displayOptionsLayout.performClick();
            });

            //Load image info (caption & labels)
            String caption = "";
            String labels = "";
            try {
                JSONObject metadata = displayCurrent.album.metadata.getJSONObject(displayCurrent.file.getName());

                //Load caption
                try { caption = metadata.getString("caption"); } catch (JSONException ignored) {}

                //Load labels
                try {
                    //Add labels
                    StringBuilder info = new StringBuilder();
                    if (metadata.has("labels")) {
                        //Get labels array
                        JSONArray array = metadata.getJSONArray("labels");

                        //Get array max & append all labels to info
                        int arrayMax = array.length() - 1;
                        if (arrayMax >= 0 && info.length() > 0) info.append("\n\n");
                        for (int i = 0; i <= arrayMax; i++) {
                            info.append(array.getString(i));
                            if (i != arrayMax) info.append(", ");
                        }
                    }

                    //Update info text
                    labels = info.toString();
                } catch (JSONException ignored) {}
            } catch (JSONException ignored) {}
            displayInfoCaptionText.setText(caption);
            displayInfoLabelsText.setText(labels);

            //Close search & show display
            searchClose.performClick();
            Orion.showAnim(displayLayout);

            //Back button
            backManager.register("display", () -> displayClose.performClick());
        });
        galleryList.setAdapter(galleryAdapter);
        galleryList.setLayoutManager(new GridLayoutManager(this, Storage.getInt("Settings.galleryItemsPerRow", 3)));


        //Add listeners
        addListeners();
    }

    private void getViews() {
        //Settings
        settings = findViewById(R.id.settings);

        //Gallery
        galleryList = findViewById(R.id.gallery);

        //Search
        searchFilterText = findViewById(R.id.searchFilterText);

        searchLayoutClosed = findViewById(R.id.searchLayoutClosed);
        searchOpen = findViewById(R.id.searchOpen);

        searchLayoutOpen = findViewById(R.id.searchLayoutOpen);
        searchClose = findViewById(R.id.searchClose);
        searchText = findViewById(R.id.searchText);

        searchIndicator = findViewById(R.id.searchIndicator);
        searchIndicatorText = findViewById(R.id.searchIndicatorText);

        //Display
        displayLayout = findViewById(R.id.displayLayout);
        displayNameText = findViewById(R.id.displayNameText);
        displayClose = findViewById(R.id.displayClose);
        displayOptions = findViewById(R.id.displayOptions);
        displayInfo = findViewById(R.id.displayInfo);
        displayEdit = findViewById(R.id.displayEdit);
        displayImage = findViewById(R.id.displayImage);

        displayOverlayLayout = findViewById(R.id.displayOverlayLayout);

        displayInfoLayout = findViewById(R.id.displayInfoLayout);
        displayInfoCaptionText = findViewById(R.id.displayInfoCaptionText);
        displayInfoLabelsScroll = findViewById(R.id.displayInfoLabelsScroll);
        displayInfoLabelsText = findViewById(R.id.displayInfoLabelsText);

        displayEditLayout = findViewById(R.id.displayEditLayout);
        displayEditCaptionText = findViewById(R.id.displayEditCaptionText);
        displayEditLabelsText = findViewById(R.id.displayEditLabelsText);
        displayEditSave = findViewById(R.id.displayEditSave);

        //Options
        displayOptionsLayout = findViewById(R.id.displayOptionsLayout);
    }

    private void addListeners() {
        //Settings
        settings.setOnClickListener(view -> {
            //Close search
            searchClose.performClick();

            //Open settings
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        //Search
        searchOpen.setOnClickListener(view -> {
            //Loading or searching
            if (isLoading || isSearching) return;

            //Open search layout
            Orion.showAnim(searchLayoutOpen);
            Orion.hideAnim(searchLayoutClosed);
            searchText.requestFocus();
            searchText.selectAll();
            Orion.showKeyboard(MainActivity.this);

            //Back button
            backManager.register("search", () -> searchClose.performClick());
        });

        searchClose.setOnClickListener(view -> {
            Orion.hideAnim(searchLayoutOpen, () -> Orion.showAnim(searchLayoutClosed));
            Orion.hideKeyboard(MainActivity.this);
            Orion.clearFocus(MainActivity.this);

            //Back button
            backManager.unregister("search");
        });

        searchText.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                //Loading or searching
                if (isLoading || isSearching) return false;

                //Get search text
                String search = searchText.getText().toString();

                //Filter gallery with search
                filterGallery(search);
            }
            return false;
        });

        //Display
        displayClose.setOnClickListener(view -> {
            //Reset display current
            displayCurrent = null;

            //Hide display
            Orion.hideAnim(displayLayout);
            Orion.hideAnim(displayInfoLayout);

            //Back button
            backManager.unregister("display");
        });

        displayImage.setOnClick(() -> {
            if (displayOverlayLayout.getVisibility() == View.VISIBLE) {
                Orion.hideAnim(displayOverlayLayout);
            } else {
                Orion.showAnim(displayOverlayLayout);
            }
        });

        displayEdit.setOnClickListener(view -> {
            //No metadata
            if (!displayCurrent.album.metadata.has(displayCurrent.file.getName())) {
                Toast.makeText(this, "This file does not contain metadata", Toast.LENGTH_SHORT).show();
                return;
            }

            //Toggle
            if (displayEditLayout.getVisibility() == View.VISIBLE) {
                displayEditLayout.performClick();
            } else {
                displayEditCaptionText.setText(displayInfoCaptionText.getText());
                displayEditLabelsText.setText(displayInfoLabelsText.getText());
                Orion.showAnim(displayEditLayout);
                backManager.register("displayEdit", () -> displayEdit.performClick());
            }
        });

        displayEditLayout.setOnClickListener(view -> {
            Orion.hideKeyboard(MainActivity.this);
            Orion.clearFocus(MainActivity.this);
            Orion.hideAnim(displayEditLayout);
            backManager.unregister("displayEdit");
        });

        displayEditSave.setOnClickListener(view -> {
            //Get new caption & labels
            String caption = displayEditCaptionText.getText().toString();
            String labels = displayEditLabelsText.getText().toString();
            String[] labelsArray = labels.split(",");
            for (int i = 0; i < labelsArray.length; i++) labelsArray[i] = labelsArray[i].trim();

            //Update info texts with new ones
            displayInfoCaptionText.setText(caption);
            displayInfoLabelsText.setText(labels);

            //Update metadata
            try {
                JSONObject metadata = displayCurrent.album.metadata.getJSONObject(displayCurrent.file.getName());
                metadata.put("caption", caption);
                metadata.put("labels", new JSONArray(labelsArray));
                boolean saved = displayCurrent.album.saveMetadata();
                Toast.makeText(MainActivity.this, saved ? "Saved successfully" : "An error occurred while saving", Toast.LENGTH_SHORT).show();
            } catch (JSONException ignored) {}

            //Close menu
            displayEditLayout.performClick();
        });

        displayInfo.setOnClickListener(view -> {
            //No metadata
            if (!displayCurrent.album.metadata.has(displayCurrent.file.getName())) {
                Toast.makeText(this, "This file does not contain metadata", Toast.LENGTH_SHORT).show();
                return;
            }

            //Toggle
            if (displayInfoLayout.getVisibility() == View.VISIBLE) {
                displayInfoLayout.performClick();
            } else {
                displayInfoLabelsScroll.scrollTo(0, 0);
                Orion.showAnim(displayInfoLayout);
                backManager.register("displayInfo", () -> displayInfo.performClick());
            }
        });

        displayInfoLayout.setOnClickListener(view -> {
            Orion.hideAnim(displayInfoLayout);
            backManager.unregister("displayInfo");
        });

        displayOptions.setOnClickListener(view -> {
            if (displayOptionsLayout.getVisibility() == View.VISIBLE) {
                displayOptionsLayout.performClick();
            } else {
                Orion.showAnim(displayOptionsLayout);
                backManager.register("displayOptions", () -> displayOptions.performClick());
            }
        });

        displayOptionsLayout.setOnClickListener(view -> {
            Orion.hideAnim(displayOptionsLayout);
            backManager.unregister("displayOptions");
        });
    }

    //Gallery
    public interface LoadingIndicator { void show(String folderName); }
    public LoadingIndicator showLoadingIndicator = (folderName) -> {
        runOnUiThread(() -> {
            searchIndicatorText.setText("Loading \"" + folderName + "\" metadata...");
            searchIndicator.setVisibility(View.VISIBLE);
        });
    };

    private void loadMetadata() {
        //Loading or searching
        if (isLoading || isSearching) return;

        //Start loading
        isLoading = true;
        galleryFiles.clear();
        galleryList.setVisibility(View.GONE);

        //Read metadata files
        new Thread(() -> {
            //Load metadata
            Library.loadMetadata(showLoadingIndicator);

            //Add all files to gallery list
            galleryFiles.addAll(Library.files);

            //Show gallery
            runOnUiThread(() -> {
                //Show gallery
                galleryAdapter.notifyDataSetChanged();
                galleryList.stopScroll();
                galleryList.scrollToPosition(0);
                galleryList.setVisibility(View.VISIBLE);

                //Finish loading
                searchIndicator.setVisibility(View.GONE);
                galleryList.setVisibility(View.VISIBLE);
                isLoading = false;

                //Log amount of images with missing metadata
                if (Library.filesWithoutMetadataCount > 0 && Storage.getBool("Settings.showMissingMetadataMessage", true))
                    Toast.makeText(MainActivity.this, "Found " + Library.filesWithoutMetadataCount + " images without metadata", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void filterGallery(String _filter) {
        //Loading or searching
        if (isLoading || isSearching) return;

        //Ignore case
        String filter = _filter.toLowerCase();

        //Start search
        isSearching = true;
        searchFilterText.setText(filter.isEmpty() ? "" : "Search: " + filter);
        searchFilterText.setVisibility(filter.isEmpty() ? View.GONE : View.VISIBLE);
        searchIndicatorText.setText("Searching...");
        searchIndicator.setVisibility(View.VISIBLE);
        searchClose.performClick();

        //Clear files list
        galleryFiles.clear();

        //Search in metadata files
        new Thread(() -> {
            //Temp variables
            boolean addToList;
            JSONObject metadata;

            //Look for files that contain filter
            for (TurboImage image: Library.files) {
                //No filter -> Skip check
                if (filter.isEmpty()) {
                    galleryFiles.add(image);
                    continue;
                }

                //Check if file contains filter
                addToList = false;

                //Check JSON contains filter
                try {
                    metadata = image.album.metadata.getJSONObject(image.file.getName());

                    //Check caption
                    if (metadata.has("caption") && metadata.getString("caption").toLowerCase().contains(filter)) {
                        addToList = true;
                    }

                    //Check labels
                    if (!addToList && metadata.has("labels")) {
                        JSONArray labels = metadata.getJSONArray("labels");
                        for (int i = 0; i < labels.length(); i++) {
                            if (!labels.getString(i).toLowerCase().contains(filter)) continue;
                            addToList = true;
                            break;
                        }
                    }
                } catch (JSONException e) {
                    //Error while checking if metadata contains filter
                    continue;
                }

                //Add to list
                if (addToList) galleryFiles.add(image);
            }

            //Show gallery
            runOnUiThread(() -> {
                //Show gallery
                galleryAdapter.notifyDataSetChanged();
                galleryList.stopScroll();
                galleryList.scrollToPosition(0);

                //Finish searching
                searchIndicator.setVisibility(View.GONE);
                isSearching = false;
            });
        }).start();
    }
}