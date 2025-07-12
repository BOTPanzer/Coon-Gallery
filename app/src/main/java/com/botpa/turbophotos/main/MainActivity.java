package com.botpa.turbophotos.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import com.botpa.turbophotos.backup.BackupActivity;
import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboImage;
import com.botpa.turbophotos.settings.SettingsActivity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class MainActivity extends AppCompatActivity {

    //Permissions
    private boolean permissionCheck = false;
    private boolean permissionWrite = false;
    private boolean permissionMedia = false;
    private boolean permissionNotifications = false;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        permissionNotifications = isGranted;
        checkPermissions();
    });

    //App
    private BackManager backManager;
    private boolean firstResume = true;
    private boolean galleryLoaded = false;
    private static boolean shouldReload = false;

    //Files
    private boolean hasLoadedMetadata = false;

    //Settings
    private CardView backup;
    private CardView settings;

    //Gallery
    private boolean galleryInHome = true;
    private Album galleryAlbum = null;
    private ArrayList<TurboImage> galleryFilesUnfiltered = new ArrayList<>();
    private final ArrayList<TurboImage> galleryFiles = new ArrayList<>();

    private Parcelable galleryListScroll;
    private GridLayoutManager galleryLayoutManager;
    private AlbumsAdapter albumsAdapter;
    private GalleryAdapter galleryAdapter;
    private SwipeRefreshLayout galleryRefreshLayout;
    private RecyclerView galleryList;
    private TextView galleryTitle;

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
    private View displayOptions;
    private RecyclerView displayList;
    private View displayOverlayLayout;

    private View displayInfoLayout;
    private TextView displayInfoNameText;
    private TextView displayInfoCaptionText;
    private HorizontalScrollView displayInfoLabelsScroll;
    private TextView displayInfoLabelsText;
    private HorizontalScrollView displayInfoTextScroll;
    private TextView displayInfoTextText;
    private View displayInfoEdit;

    private View displayEditLayout;
    private TextView displayEditCaptionText;
    private TextView displayEditLabelsText;
    private View displayEditSave;

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

        //Skip first resume (called after onCreate)
        if (firstResume) {
            firstResume = false;
            return;
        }

        //Reload
        if (shouldReload) {
            shouldReload = false;
            recreate();
            return;
        }

        //Check for permissions
        if (permissionCheck) {
            permissionCheck = false;
            checkPermissions();
        }

        //Gallery not loaded -> Skip next
        if (!galleryLoaded) return;

        //Update settings
        if (galleryList != null && galleryLayoutManager != null) {
            //Get horizontal item count & update it
            int newHorizontalItemCount = Storage.getInt(
                    galleryInHome ? "Settings.galleryAlbumsPerRow" : "Settings.galleryImagesPerRow",
                    galleryInHome ? 2 : 3);
            if (galleryLayoutManager.getSpanCount() != newHorizontalItemCount) galleryLayoutManager.setSpanCount(newHorizontalItemCount);

            //Get show missing metadata icon & update it
            boolean showMissingMetadataIcon = Storage.getBool("Settings.showMissingMetadataIcon", false);
            if (showMissingMetadataIcon != galleryAdapter.getShowMissingMetadataIcon()) {
                galleryAdapter.setShowMissingMetadataIcon(showMissingMetadataIcon);
                galleryAdapter.notifyDataSetChanged();
            }
        }

        //Check albums for updates (only works when in home, aka albums list)
        refreshGallery(false);
    }

    private void checkPermissions() {
        //Show permission layout
        findViewById(R.id.permissionLayout).setVisibility(View.VISIBLE);

        //Button listeners
        findViewById(R.id.permissionWrite).setOnClickListener(v -> {
            if (permissionWrite) return;
            
            //Ask for permission
            permissionCheck = true;
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        });

        findViewById(R.id.permissionMedia).setOnClickListener(v -> {
            if (permissionMedia) return;

            //Ask for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO }, 0);
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 0);
            }
        });

        findViewById(R.id.permissionNotifications).setOnClickListener(v -> {
            if (permissionNotifications) return;

            //Ask for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        //Check for permissions
        if (Environment.isExternalStorageManager()) {
            permissionWrite = true;
            findViewById(R.id.permissionWrite).setAlpha(0.5f);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
            permissionMedia = true;
            findViewById(R.id.permissionMedia).setAlpha(0.5f);
        }
        if (NotificationManagerCompat.from(MainActivity.this).areNotificationsEnabled()) {
            permissionNotifications = true;
            findViewById(R.id.permissionNotifications).setAlpha(0.5f);
        }

        //Has permissions
        if (permissionWrite && permissionMedia && permissionNotifications) {
            findViewById(R.id.permissionLayout).setVisibility(View.GONE);

            //Start
            loadApp();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }

    //App
    public static void shouldReload() {
        //Reload on resume
        shouldReload = true;
    }

    private void loadApp() {
        //App
        backManager = new BackManager(MainActivity.this, getOnBackPressedDispatcher());
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);

        //Load views
        loadViews();

        //Load gallery (images & metadata)
        loadGalleryImages();

        //Init lists (gallery & display)
        initGalleryAdapters();

        //Add listeners
        addListeners();
    }

    private void loadViews() {
        //Activities
        backup = findViewById(R.id.backup);
        settings = findViewById(R.id.settings);

        //Gallery
        galleryRefreshLayout = findViewById(R.id.galleryRefreshLayout);
        galleryList = findViewById(R.id.gallery);
        galleryTitle = findViewById(R.id.galleryTitle);

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
        displayInfo = findViewById(R.id.displayInfo);
        displayInfoEdit = findViewById(R.id.displayInfoEdit);
        displayOptions = findViewById(R.id.displayOptions);
        displayList = findViewById(R.id.displayList);

        displayOverlayLayout = findViewById(R.id.displayOverlayLayout);

        displayInfoLayout = findViewById(R.id.displayInfoLayout);
        displayInfoNameText = findViewById(R.id.displayInfoNameText);
        displayInfoCaptionText = findViewById(R.id.displayInfoCaptionText);
        displayInfoLabelsScroll = findViewById(R.id.displayInfoLabelsScroll);
        displayInfoLabelsText = findViewById(R.id.displayInfoLabelsText);
        displayInfoTextScroll = findViewById(R.id.displayInfoTextScroll);
        displayInfoTextText = findViewById(R.id.displayInfoTextText);

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
            //Open backup
            startActivity(new Intent(MainActivity.this, BackupActivity.class));
        });

        settings.setOnClickListener(view -> {
            //Close search
            if (!galleryInHome) searchClose.performClick();

            //Open settings
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        settings.setOnLongClickListener(v -> {
            //Reload gallery
            recreate();
            return true;
        });

        //Gallery
        galleryRefreshLayout.setOnRefreshListener(() -> {
            //Refresh gallery
            refreshGallery(true);

            //Stop refreshing
            galleryRefreshLayout.setRefreshing(false);
        });

        //Search
        searchOpen.setOnClickListener(view -> {
            //Loading or searching
            if (isSearching) return;

            //Open search layout
            searchLayoutOpen.setVisibility(View.VISIBLE);
            searchLayoutClosed.setVisibility(View.GONE);

            //Focus text & show keyboard
            searchText.requestFocus();
            searchText.selectAll();
            Orion.showKeyboard(MainActivity.this);

            //Back button
            backManager.register("searchMenu", () -> searchClose.performClick());
        });

        searchClose.setOnClickListener(view -> {
            //Close keyboard
            Orion.hideKeyboard(MainActivity.this);
            Orion.clearFocus(MainActivity.this);

            //Hide menu
            searchLayoutOpen.setVisibility(View.GONE);
            searchLayoutClosed.setVisibility(View.VISIBLE);
            //Orion.hideAnim(searchLayoutOpen, () -> Orion.showAnim(searchLayoutClosed));

            //Back button
            backManager.unregister("searchMenu");
        });

        searchText.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                //Get search text
                String search = searchText.getText().toString();

                //Filter gallery with search
                filterGallery(search, true);
            }
            return false;
        });

        //Display
        displayInfo.setOnClickListener(view -> {
            //Toggle
            if (displayInfoLayout.getVisibility() == View.VISIBLE) {
                displayInfoLayout.performClick();
            } else {
                displayInfoLabelsScroll.scrollTo(0, 0);
                displayInfoTextScroll.scrollTo(0, 0);
                Orion.showAnim(displayInfoLayout);
                backManager.register("displayInfo", () -> displayInfo.performClick());
            }
        });

        displayInfoLayout.setOnClickListener(view -> {
            Orion.hideAnim(displayInfoLayout);
            backManager.unregister("displayInfo");
        });

        displayClose.setOnClickListener(view -> {
            //Reset display current
            selectImage(-1);

            //Hide display
            Orion.hideAnim(displayLayout);
            Orion.hideAnim(displayInfoLayout);

            //Back button
            backManager.unregister("display");
        });

        displayInfoEdit.setOnClickListener(view -> {
            //No metadata file
            if (!displayCurrent.album.hasMetadata()) {
                Toast.makeText(this, "This file does not have metadata file linked to its album", Toast.LENGTH_SHORT).show();
                return;
            }

            //No metadata
            if (!displayCurrent.hasMetadata()) {
                Toast.makeText(this, "This file does not have a key in its album metadata", Toast.LENGTH_SHORT).show();
                return;
            }

            //Hide display info
            displayInfoLayout.performClick();

            //Toggle display edit
            if (displayEditLayout.getVisibility() == View.VISIBLE) {
                displayEditLayout.performClick();
            } else {
                displayEditCaptionText.setText(displayInfoCaptionText.getText());
                displayEditLabelsText.setText(displayInfoLabelsText.getText());
                Orion.showAnim(displayEditLayout);
                backManager.register("displayEdit", () -> displayInfoEdit.performClick());
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
            String key = displayCurrent.getName();
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
    public Library.LoadingIndicator loadingIndicator = new Library.LoadingIndicator() {
        @Override
        public void search() {
            searchIndicatorText.setText("Searching...");
            searchIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        public void load(String content) {
            runOnUiThread(() -> {
                searchIndicatorText.setText("Loading " + content + "...");
                searchIndicator.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void load(String folder, String type) {
            runOnUiThread(() -> {
                searchIndicatorText.setText("Loading \"" + folder + "\" " + type + "...");
                searchIndicator.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void hide() {
            runOnUiThread(() -> {
                searchIndicator.setVisibility(View.GONE);
            });
        }
    };

    private void initGalleryAdapters() {
        //Create gallery list viewer
        galleryLayoutManager = new GridLayoutManager(this, Storage.getInt("Settings.galleryAlbumsPerRow", 3));
        galleryList.setLayoutManager(galleryLayoutManager);

        //Create gallery albums adapter
        albumsAdapter = new AlbumsAdapter(this, Library.albums);
        albumsAdapter.setOnItemClickListener((view, index) -> {
            selectAlbum(index);
            showAlbumsList(false);
        });
        galleryList.setAdapter(albumsAdapter);

        //Create gallery selected album adapter
        galleryAdapter = new GalleryAdapter(this, galleryFiles, Storage.getBool("Settings.showMissingMetadataIcon", false));
        galleryAdapter.setOnItemClickListener((view, index) -> {
            if (!hasLoadedMetadata) return;
            selectImage(index);
        });


        //Create display list viewer
        displayLayoutManager = new DisplayLayoutManager(this);
        displayLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        displayList.setLayoutManager(displayLayoutManager);

        //Create display snap helper
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(displayList);

        //Create display adapter
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
        displayAdapter.setOnPlayListener((view, index) -> {
            //Play video outside
            findViewById(R.id.displayOptionsOpen).performClick();
        });
        displayList.setAdapter(displayAdapter);

        //Add snap listener
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

    private void loadGalleryImages() {
        //Start loading
        galleryList.setVisibility(View.GONE);

        //Load images
        new Thread(() -> {
            //Load albums
            Library.loadAlbums(MainActivity.this, true);

            //Show gallery
            runOnUiThread(() -> {
                //Show gallery list (albums)
                galleryList.setVisibility(View.VISIBLE);

                //Reload albums list
                albumsAdapter.notifyDataSetChanged();
            });

            //Gallery loaded
            galleryLoaded = true;
        }).start();
    }

    private void loadGalleryMetadata(Album album) {
        //Start loading
        hasLoadedMetadata = false;

        //Load metadata
        new Thread(() -> {
            //Load metadata
            if (album == null) {
                Library.loadMetadata(loadingIndicator);
            } else {
                Library.loadMetadata(loadingIndicator, album);
            }

            //Update gallery
            runOnUiThread(() -> {
                //Update gallery
                galleryAdapter.notifyDataSetChanged();

                //Finish loading
                loadingIndicator.hide();
                hasLoadedMetadata = true;
            });
        }).start();
    }

    private void filterGallery() { filterGallery("", false); }

    private void filterGallery(String filterText, boolean scrollToTop) {
        //Ignore case
        String filter = filterText.toLowerCase();
        boolean isFiltering = !filter.isEmpty();

        //Loading or searching
        if (isSearching || (!hasLoadedMetadata && isFiltering)) return;

        //Start search
        isSearching = true;
        searchFilterText.setText(isFiltering ? "Search: " + filter : "");
        searchFilterText.setVisibility(isFiltering ? View.VISIBLE : View.GONE);
        if (isFiltering) loadingIndicator.search();
        searchClose.performClick();

        //Clear files list
        galleryFiles.clear();

        //Back button
        if (isFiltering)
            backManager.register("search", this::filterGallery);
        else
            backManager.unregister("search");

        //Search in metadata files
        new Thread(() -> {
            //Look for files that contain filter
            for (TurboImage image: galleryFilesUnfiltered) {
                //No filter -> Skip check
                if (!isFiltering) {
                    galleryFiles.add(image);
                    continue;
                }

                //Check if json contains filter
                if (filterImage(image, filter)) galleryFiles.add(image);
            }

            //Show gallery
            runOnUiThread(() -> {
                //Show gallery
                galleryAdapter.notifyDataSetChanged();
                galleryList.stopScroll();
                if (scrollToTop) galleryList.scrollToPosition(0);

                //Finish searching
                if (isFiltering) loadingIndicator.hide();
                isSearching = false;
            });
        }).start();
    }

    private boolean filterImage(TurboImage image, String filter) {
        //Check file name
        if (image.getName().contains(filter)) return true;

        //Get metadata
        ObjectNode metadata = image.album.getMetadataKey(image.getName());
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

    private void showAlbumsList(boolean show) {
        galleryInHome = show;
        Orion.hideAnim(galleryList, 150, () -> {
            //Change gallery
            if (galleryInHome) {
                //Show home
                Orion.hideAnim(searchLayoutClosed);
                galleryLayoutManager.setSpanCount(Storage.getInt("Settings.galleryAlbumsPerRow", 2));
                galleryList.setAdapter(albumsAdapter);
                backManager.unregister("albums");

                //Scroll to saved scroll
                if (galleryListScroll != null) galleryLayoutManager.onRestoreInstanceState(galleryListScroll);

                //Reset gallery title
                galleryTitle.setText("Coon Gallery");

                //Check albums for updates
                refreshGallery(false);
            } else {
                //Save scroll
                galleryListScroll = galleryLayoutManager.onSaveInstanceState();

                //Show album
                Orion.showAnim(searchLayoutClosed);
                galleryLayoutManager.setSpanCount(Storage.getInt("Settings.galleryImagesPerRow", 3));
                galleryList.setAdapter(galleryAdapter);
                backManager.register("albums", () -> showAlbumsList(true));

                //Scroll to top
                galleryList.scrollToPosition(0);
            }

            //Show list
            Orion.showAnim(galleryList, 150);
        });
    }

    private void selectAlbum(int albumIndex) {
        selectAlbum(albumIndex < 0 || albumIndex >= Library.albums.size() ? null : Library.albums.get(albumIndex));
    }

    private void selectAlbum(Album album) {
        //Save album
        galleryAlbum = album;

        //Select album
        if (album == null) {
            //Load
            galleryFilesUnfiltered = Library.allFiles;
            loadGalleryMetadata(null);

            //Change gallery title
            galleryTitle.setText("All");
        } else {
            //Load
            galleryFilesUnfiltered = album.files;
            loadGalleryMetadata(album);

            //Change gallery title
            galleryTitle.setText(album.getName());
        }
        searchText.setText("");
        filterGallery();
    }

    private void refreshGallery(boolean softRefresh) {
        //Check albums for updates
        if (Library.loadAlbums(MainActivity.this, false)) {
            //Don't reload on resume
            shouldReload = false;

            //Update albums list
            albumsAdapter.notifyDataSetChanged();

            //Update gallery list
            if (!galleryInHome) selectAlbum(galleryAlbum);

            //Close display menu
            hideDisplay();
            return;
        }

        //Soft refresh -> Stop here
        if (softRefresh) return;

        //No new images -> Check current albums for updates
        for (Album album : Library.albums) {
            if (album.getImagesFolder().lastModified() == album.getLastModified()) continue;

            //An album was modified -> Reload activity
            recreate();
            return;
        }
    }

    private void deleteImage(TurboImage image) {
        //Get gallery index
        int indexInGallery = galleryFiles.indexOf(image);

        //Delete image
        Library.DeleteImageInfo info = Library.deleteImage(image);

        //Check if album was deleted
        if (info.deletedAlbum) {
            //Deleted -> Notify adapter
            albumsAdapter.notifyItemRemoved(info.indexOfAlbum);
        }

        //Check if albums were sorted
        if (info.sortedAlbums) {
            //Sorted -> Notify adapter
            albumsAdapter.notifyDataSetChanged();
        }

        //Check if image is in gallery list
        if (indexInGallery != -1) {
            //Is present -> Remove it & update adapter
            galleryFiles.remove(indexInGallery);
            galleryAdapter.notifyItemRemoved(indexInGallery);

            //Check gallery needs to be closed or select a new image
            if (galleryFiles.isEmpty()) {
                //Gallery is empty -> Close display list & return to albums
                hideDisplay();
                showAlbumsList(true);
            } else if (displayLayout.getVisibility() == View.VISIBLE && displayCurrent == image) {
                //Display list is visible -> Check if a new image can be selected
                if (displayCurrentRelativeIndex != displayFiles.size() - 1) {
                    //An image is available next -> Select it
                    selectImage(indexInGallery);   //Next image would be the same index since this image was deleted
                } else if (displayCurrentRelativeIndex != 0) {
                    //An image is available before -> Select it
                    selectImage(indexInGallery - 1);
                }
            }
        }
    }

    //Display
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

        //Add files to display list
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
        displayNameText.setText(displayCurrent.getName());

        //Prepare options menu
        findViewById(R.id.displayOptionsDelete).setOnClickListener(view -> {
            //Close options menu
            displayOptionsLayout.performClick();

            //Show delete confirmation dialog
            new MaterialAlertDialogBuilder(this)
                .setMessage("Are you sure you want to permanently delete \"" + displayCurrent.getName() + "\"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, whichButton) -> {
                    deleteImage(displayCurrent);
                })
                .show();
        });

        findViewById(R.id.displayOptionsShare).setOnClickListener(view -> {
            //Share
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, Orion.getUriFromFile(MainActivity.this, displayCurrent.file));
            intent.setType(displayCurrent.getMimeType());
            startActivity(Intent.createChooser(intent, null));

            //Close menu
            displayOptionsLayout.performClick();
        });

        findViewById(R.id.displayOptionsEdit).setOnClickListener(view -> {
            //Get mime type and URI
            String mimeType = displayCurrent.getMimeType();
            Uri uri = Orion.getMediaStoreUriFromFile(MainActivity.this, displayCurrent.file, mimeType);

            //Edit
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setDataAndType(uri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, null));

            //Close menu
            displayOptionsLayout.performClick();
        });

        findViewById(R.id.displayOptionsOpen).setOnClickListener(view -> {
            //Close options menu
            displayOptionsLayout.performClick();

            //Show open with menu
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(displayCurrent.file.getAbsolutePath()), displayCurrent.getMimeType());
            startActivity(intent);
        });

        //Load image info (caption & labels)
        String caption = "";
        String labels = "";
        String text = "";
        try {
            JsonNode metadata = displayCurrent.album.getMetadataKey(displayCurrent.getName());
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
            labels = info.toString();

            //Add text
            info = new StringBuilder();
            if (metadata.has("text")) {
                //Get labels array
                JsonNode array = metadata.path("text");

                //Get array max & append all labels to info
                int arrayMax = array.size() - 1;
                if (arrayMax >= 0 && info.length() > 0) info.append("\n\n");
                for (int i = 0; i <= arrayMax; i++) {
                    info.append(array.get(i).asText());
                    if (i != arrayMax) info.append(", ");
                }
            }
            text = info.toString();
        } catch (Exception ignored) {
            //Error while parsing JSON
        }
        displayInfoNameText.setText(displayCurrent.getName());
        displayInfoCaptionText.setText(caption);
        displayInfoLabelsText.setText(labels);
        displayInfoTextText.setText(text);

        //Close search & show display
        searchClose.performClick();
        Orion.showAnim(displayLayout);

        //Back button
        backManager.register("display", () -> displayClose.performClick());
    }

    private void hideDisplay() {
        displayOptionsLayout.performClick();
        displayEditLayout.performClick();
        displayClose.performClick();
    }

}