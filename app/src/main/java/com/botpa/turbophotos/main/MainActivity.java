package com.botpa.turbophotos.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.Manifest;
import com.botpa.turbophotos.backup.BackupActivity;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboImage;
import com.botpa.turbophotos.settings.SettingsActivity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class MainActivity extends AppCompatActivity {

    //Permissions
    private boolean permissionCheck = false;
    private boolean permissionWrite = false;
    private boolean permissionNotifications = false;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        permissionNotifications = isGranted;
        checkPermissions();
    });

    //App
    private BackManager backManager;
    private static boolean shouldRestart = false;

    //Files
    private boolean isLoading = false;

    //Settings
    private CardView backup;
    private CardView settings;

    //Gallery
    private final ArrayList<TurboImage> galleryFiles = new ArrayList<>();

    private GridLayoutManager galleryLayoutManager;
    private AlbumsAdapter albumsAdapter;
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
    private final ArrayList<TurboImage> displayFiles = new ArrayList<>();
    private DisplayLayoutManager displayLayoutManager;
    private DisplayAdapter displayAdapter;
    private int displayCurrentIndex = -1;
    private int displayCurrentRelativeIndex = -1;
    private TurboImage displayCurrent = null;

    private View displayLayout;
    private TextView displayNameText;
    private View displayClose;
    private View displayInfo;
    private View displayEdit;
    private View displayOptions;
    private RecyclerView displayList;
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
        if (galleryList != null && galleryLayoutManager != null) {
            int newHorizontalItemCount = Storage.getInt("Settings.galleryItemsPerRow", 3);
            if (galleryLayoutManager.getSpanCount() != newHorizontalItemCount) {
                galleryLayoutManager.setSpanCount(newHorizontalItemCount);
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

        findViewById(R.id.permissionNotifications).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        //Check for permissions
        if (Environment.isExternalStorageManager()) {
            permissionWrite = true;
            findViewById(R.id.permissionWrite).setAlpha(0.5f);
        }
        if (NotificationManagerCompat.from(MainActivity.this).areNotificationsEnabled()) {
            permissionNotifications = true;
            findViewById(R.id.permissionNotifications).setAlpha(0.5f);
        }

        //Has permissions
        if (permissionWrite && permissionNotifications) {
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


        //Load gallery (images & metadata)
        loadGallery();


        //Init gallery & display lists
        initGallery();


        //Add listeners
        addListeners();
    }

    private void getViews() {
        //Activities
        backup = findViewById(R.id.backup);
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
        displayList = findViewById(R.id.displayList);

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
        //Activities
        backup.setOnClickListener(view -> {
            //Loading
            if (isLoading) return;

            //Open backup
            startActivity(new Intent(MainActivity.this, BackupActivity.class));
        });

        settings.setOnClickListener(view -> {
            //Loading
            if (isLoading) return;

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
            backManager.register("searchMenu", () -> searchClose.performClick());
        });

        searchClose.setOnClickListener(view -> {
            Orion.hideAnim(searchLayoutOpen, () -> Orion.showAnim(searchLayoutClosed));
            Orion.hideKeyboard(MainActivity.this);
            Orion.clearFocus(MainActivity.this);

            //Back button
            backManager.unregister("searchMenu");
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
            selectImage(-1);

            //Hide display
            Orion.hideAnim(displayLayout);
            Orion.hideAnim(displayInfoLayout);

            //Back button
            backManager.unregister("display");
        });

        displayEdit.setOnClickListener(view -> {
            //No metadata
            if (!displayCurrent.hasMetadata()) {
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
            String key = displayCurrent.file.getName();
            ObjectNode metadata = displayCurrent.album.getMetadataKey(key);
            if (metadata == null) {
                metadata = Orion.getEmptyJson();
                displayCurrent.album.metadata.set(key, metadata);
            }
            metadata.put("caption", caption);
            metadata.set("labels", Orion.arrayToJson(labelsArray));

            //Save
            boolean saved = displayCurrent.album.saveMetadata();
            Toast.makeText(MainActivity.this, saved ? "Saved successfully" : "An error occurred while saving", Toast.LENGTH_SHORT).show();

            //Close menu
            displayEditLayout.performClick();
        });

        displayInfo.setOnClickListener(view -> {
            //No metadata
            if (!displayCurrent.hasMetadata()) {
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

    //Gallery & Display
    private void initGallery() {
        //Create gallery
        galleryLayoutManager = new GridLayoutManager(this, Storage.getInt("Settings.galleryItemsPerRow", 3));
        galleryList.setLayoutManager(galleryLayoutManager);

        albumsAdapter = new AlbumsAdapter(this, Library.albums);
        //galleryList.setAdapter(albumsAdapter);

        galleryAdapter = new GalleryAdapter(this, galleryFiles);
        galleryAdapter.setOnItemClickListener((view, index) -> {
            if (!isLoading) selectImage(index);
        });
        galleryList.setAdapter(galleryAdapter);

        //Create display
        displayLayoutManager = new DisplayLayoutManager(this);
        displayLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        displayList.setLayoutManager(displayLayoutManager);

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(displayList);

        displayAdapter = new DisplayAdapter(this, displayFiles);
        displayAdapter.setOnClickListener((view, index) -> {
            if (displayOverlayLayout.getVisibility() == View.VISIBLE)
                Orion.hideAnim(displayOverlayLayout);
            else
                Orion.showAnim(displayOverlayLayout);
        });
        displayAdapter.setOnZoomListener((view, index) -> {
            //Enable scrolling only if not zoomed and one finger is over
            displayLayoutManager.setScrollEnabled(view.getZoom() <= 1 && view.getPointers() <= 1);
        });
        displayList.setAdapter(displayAdapter);

        displayList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && displayLayoutManager.canScrollHorizontally()) {
                    //Get position
                    View view = snapHelper.findSnapView(displayLayoutManager);
                    int position = (view != null) ? displayLayoutManager.getPosition(view) : -1;
                    if (position == -1) return;

                    //Check what to do
                    if (position < displayCurrentRelativeIndex) {
                        //Previous
                        displayLayoutManager.setScrollEnabled(false);
                        selectImage(displayCurrentIndex - 1);
                    } else if (position > displayCurrentRelativeIndex) {
                        //Next
                        displayLayoutManager.setScrollEnabled(false);
                        selectImage(displayCurrentIndex + 1);
                    }
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public interface LoadingIndicator { void show(String folderName, String type); }
    public LoadingIndicator showLoadingIndicator = (folderName, type) -> {
        runOnUiThread(() -> {
            searchIndicatorText.setText("Loading \"" + folderName + "\" " + type + "...");
            searchIndicator.setVisibility(View.VISIBLE);
        });
    };

    private void loadGallery() {
        //Loading or searching
        if (isLoading || isSearching) return;

        //Start loading
        isLoading = true;
        galleryFiles.clear();
        galleryList.setVisibility(View.GONE);

        //Read metadata files
        new Thread(() -> {
            //Load metadata
            long loadGalleryDuration = Library.loadGallery(showLoadingIndicator);

            //Add all files to gallery list
            galleryFiles.addAll(Library.files);

            //Show gallery
            runOnUiThread(() -> {
                //Toast.makeText(MainActivity.this, "" + loadGalleryDuration, Toast.LENGTH_SHORT).show();

                //Show gallery (without loaded metadata)
                galleryAdapter.notifyDataSetChanged();
                galleryList.stopScroll();
                galleryList.scrollToPosition(0);
                galleryList.setVisibility(View.VISIBLE);
            });

            //Load metadata after gallery is loaded
            long loadMetadataDuration = Library.loadMetadata(showLoadingIndicator);

            //Update gallery
            runOnUiThread(() -> {
                //Toast.makeText(MainActivity.this, "" + loadMetadataDuration, Toast.LENGTH_SHORT).show();

                //Update gallery
                galleryAdapter.notifyDataSetChanged();

                //Finish loading
                searchIndicator.setVisibility(View.GONE);
                Orion.showAnim(searchLayoutClosed);
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

        //Back button
        if (filter.isEmpty())
            backManager.unregister("search");
        else
            backManager.register("search", () -> filterGallery(""));

        //Search in metadata files
        new Thread(() -> {
            //Look for files that contain filter
            for (TurboImage image: Library.files) {
                //No filter -> Skip check
                if (filter.isEmpty()) {
                    galleryFiles.add(image);
                    continue;
                }

                //Check if json contains filter
                if (filterImage(image.album.getMetadataKey(image.file.getName()), filter)) galleryFiles.add(image);
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

    private boolean filterImage(ObjectNode metadata, String filter) {
        //No metadata
        if (metadata == null) return false;

        //Check caption
        if (metadata.has("caption")) {
            JsonNode caption = metadata.path("caption");
            if (caption.isTextual() && caption.asText().toLowerCase().contains(filter)) {
                return true;
            }
        }

        //Check labels
        if (metadata.has("labels")) {
            JsonNode labels = metadata.path("labels");
            for (int i = 0; i < labels.size(); i++) {
                if (labels.get(i).asText().toLowerCase().contains(filter)) {
                    return true;
                }
            }
        }

        //Check text
        if (metadata.has("text")) {
            JsonNode text = metadata.path("text");
            for (int i = 0; i < text.size(); i++) {
                if (text.get(i).asText().toLowerCase().contains(filter)) {
                    return true;
                }
            }
        }

        //Not found
        return false;
    }

    private void selectImage(int index) {
        //Deselect
        if (index == -1) {
            displayCurrentRelativeIndex = -1;
            displayCurrentIndex = -1;
            displayCurrent = null;
            return;
        }

        //Fill display files
        displayFiles.clear();
        displayCurrentIndex = index;
        displayCurrentRelativeIndex = 0;

        //Add files to list
        if (index > 0) {
            //Has file before
            displayFiles.add(galleryFiles.get(index - 1));
            displayCurrentRelativeIndex++;
        }
        displayFiles.add(galleryFiles.get(index));
        if (index < galleryFiles.size() - 1) {
            //Has file after
            displayFiles.add(galleryFiles.get(index + 1));
        }

        //Get current image, update adapter & select it
        displayCurrent = displayFiles.get(displayCurrentRelativeIndex);
        displayAdapter.notifyDataSetChanged();
        displayList.scrollToPosition(displayCurrentRelativeIndex);
        displayLayoutManager.setScrollEnabled(true);

        //Change image name
        displayNameText.setText(displayCurrent.file.getName());

        //Prepare options menu
        findViewById(R.id.displayOptionsDelete).setOnClickListener(view2 -> {
            //Delete metadata key
            displayCurrent.album.removeMetadataKey(displayCurrent.file.getName());
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
            JsonNode metadata = displayCurrent.album.getMetadataKey(displayCurrent.file.getName());
            if (metadata == null) throw new Exception();

            //Load caption
            caption = metadata.path("caption").asText();

            //Add labels
            StringBuilder info = new StringBuilder();
            if (metadata.has("labels")) {
                //Get labels array
                JsonNode array = metadata.path("labels");

                //Get array max & append all labels to info
                int arrayMax = array.size() - 1;
                if (arrayMax >= 0 && info.length() > 0) info.append("\n\n");
                for (int i = 0; i <= arrayMax; i++) {
                    info.append(array.get(i).asText());
                    if (i != arrayMax) info.append(", ");
                }
            }

            //Update info text
            labels = info.toString();
        } catch (Exception ignored) {
            //Error while parsing JSON
        }
        displayInfoCaptionText.setText(caption);
        displayInfoLabelsText.setText(labels);

        //Close search & show display
        searchClose.performClick();
        Orion.showAnim(displayLayout);

        //Back button
        backManager.register("display", () -> displayClose.performClick());
    }
}