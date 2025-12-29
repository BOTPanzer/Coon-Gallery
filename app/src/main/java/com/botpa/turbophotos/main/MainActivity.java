package com.botpa.turbophotos.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import com.botpa.turbophotos.util.Action;
import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;

import org.jetbrains.annotations.NotNull;

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

    //Activity
    private static boolean shouldReloadOnResume = false;
    private boolean isLoaded = false;
    private boolean skipResume = true;

    public BackManager backManager;

    //Actions
    private final Library.ActionEvent onAction = this::manageAction;

    //Gallery
    public final GalleryHelper gallery = new GalleryHelper(this);

    //Loading indicator
    private View loadIndicator;
    private TextView loadIndicatorText;

    public Library.LoadingIndicator loadingIndicator = new Library.LoadingIndicator() {
        @Override
        public void search() {
            loadIndicatorText.setText("Searching...");
            loadIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        public void load(String content) {
            runOnUiThread(() -> {
                loadIndicatorText.setText("Loading " + content + "...");
                loadIndicator.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void load(String folder, String type) {
            runOnUiThread(() -> {
                loadIndicatorText.setText("Loading \"" + folder + "\" " + type + "...");
                loadIndicator.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void hide() {
            runOnUiThread(() -> loadIndicator.setVisibility(View.GONE));
        }
    };


    //Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main);

        //Load storage
        Storage.load(MainActivity.this);

        //Add on action listener
        Library.addOnActionEvent(onAction);

        //Load views & add listeners
        loadViews();
        addListeners();

        //Check permissions
        checkPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Remove on action listener
        Library.removeOnActionEvent(onAction);
    }

    private void initActivity() {
        //Init back manager
        backManager = new BackManager(MainActivity.this, getOnBackPressedDispatcher());

        //Enable HDR
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);

        //Init list adapters (gallery)
        gallery.initAdapters();

        //Load albums
        new Thread(() -> {
            //Load albums
            Library.loadLibrary(MainActivity.this, true);

            //Show gallery
            runOnUiThread(() -> {
                //Show list
                gallery.loaded();

                //Reload albums list
                gallery.homeAdapter.notifyDataSetChanged();
            });

            //Gallery loaded
            isLoaded = true;
        }).start();
    }

    public static void reloadOnResume() {
        //Reload on resume
        shouldReloadOnResume = true;
    }

    //Views
    private void loadViews() {
        //Load indicator
        loadIndicator = findViewById(R.id.loadIndicator);
        loadIndicatorText = findViewById(R.id.loadIndicatorText);

        //Gallery & Search
        gallery.loadViews();
    }

    private void addListeners() {
        //Gallery & Search
        gallery.addListeners();
    }

    //Permission & Settings
    private void checkPermissions() {
        //Update permissions
        if (Environment.isExternalStorageManager()) {
            permissionWrite = true;
            findViewById(R.id.permissionWrite).setAlpha(0.5f);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
            permissionMedia = true;
            findViewById(R.id.permissionMedia).setAlpha(0.5f);
        }
        if (NotificationManagerCompat.from(MainActivity.this).areNotificationsEnabled()) {
            permissionNotifications = true;
            findViewById(R.id.permissionNotifications).setAlpha(0.5f);
        }

        //Check if permissions are granted
        if (permissionWrite && permissionMedia && permissionNotifications) {
            //Hide permission layout
            findViewById(R.id.permissionLayout).setVisibility(View.GONE);

            //Init activity
            initActivity();
        } else {
            //Show permission layout
            findViewById(R.id.permissionLayout).setVisibility(View.VISIBLE);

            //Add request permission button listeners
            findViewById(R.id.permissionWrite).setOnClickListener(view -> {
                //Already has permission
                if (permissionWrite) return;

                //Ask for permission
                permissionCheck = true;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            });

            findViewById(R.id.permissionMedia).setOnClickListener(view -> {
                //Already has permission
                if (permissionMedia) return;

                //Ask for permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO }, 0);
                } else {
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 0);
                }
            });

            findViewById(R.id.permissionNotifications).setOnClickListener(view -> {
                //Already has permission
                if (permissionNotifications) return;

                //Ask for permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Skip first resume (called after onCreate)
        if (skipResume) {
            skipResume = false;
            return;
        }

        //Reload
        if (shouldReloadOnResume) {
            shouldReloadOnResume = false;
            recreate();
            return;
        }

        //Check for permissions
        if (permissionCheck) {
            permissionCheck = false;
            checkPermissions();
            return;
        }

        //Gallery not loaded -> Skip next
        if (!isLoaded) return;

        //Update gallery horizontal item count
        gallery.updateHorizontalItemCount();

        //Update show missing metadata icon
        boolean showMissingMetadataIcon = Storage.getBool("Settings.showMissingMetadataIcon", false);
        if (showMissingMetadataIcon != gallery.albumAdapter.getShowMissingMetadataIcon()) {
            gallery.albumAdapter.setShowMissingMetadataIcon(showMissingMetadataIcon);
            gallery.albumAdapter.notifyDataSetChanged();
        }

        //Check albums for updates
        if (gallery.inHome) gallery.refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //Update gallery horizontal item count
        gallery.updateHorizontalItemCount();
    }

    //Actions
    private void manageAction(Action action) {
        //No action
        if (action.isOfType(Action.TYPE_NONE)) return;

        //Failed actions
        if (!action.failed.isEmpty()) {
            if (action.failed.size() == 1) {
                //Only 1 failed -> Show error
                Orion.snack(MainActivity.this, action.failed.entrySet().iterator().next().getValue());
            } else if (!action.allFailed()) {
                //More than 1 failed -> Show general error
                Orion.snack(MainActivity.this, "Failed to perform " + action.failed.size() + " actions");
            } else {
                //All failed -> Show general error
                Orion.snack(MainActivity.this, "Failed to perform all actions");
                return;
            }
        }

        //Check if albums list was changed
        if (action.sortedAlbumsList) {
            //Sorted albums list -> Notify all
            gallery.homeAdapter.notifyDataSetChanged();
        } else {
            //Check if trash was added, removed or updated
            switch (action.trashChanges) {
                case Action.TRASH_ADDED:
                    gallery.homeAdapter.notifyItemInserted(0);
                    break;
                case Action.TRASH_REMOVED:
                    gallery.homeAdapter.notifyItemRemoved(0);
                    break;
                case Action.TRASH_UPDATED:
                    gallery.homeAdapter.notifyItemChanged(0);
                    break;
            }

            //Check if albums were deleted or sorted
            if (!action.deletedAlbums.isEmpty()) {
                //Albums were deleted -> Notify items removed
                for (Album album : action.deletedAlbums) {
                    int position = gallery.homeAdapter.getIndexFromAlbum(album) + gallery.homeAdapter.getIndexOffset(); //albumIndex + adapterIndexOffset
                    gallery.homeAdapter.notifyItemRemoved(position);
                }
            }

            //Check if albums were sorted
            if (!action.sortedAlbums.isEmpty()) {
                //Sorted albums -> Notify items removed
                for (Album album : action.sortedAlbums) {
                    int position = gallery.homeAdapter.getIndexFromAlbum(album) + gallery.homeAdapter.getIndexOffset(); //albumIndex + adapterIndexOffset
                    gallery.homeAdapter.notifyItemChanged(position);
                }
            }
        }

        //Check if gallery is empty
        if (Library.gallery.isEmpty()) {
            //Is empty -> Return to albums
            gallery.showAlbumsList(true);
        } else {
            //Not empty -> Remove selected items & update adapter
            for (int indexInGallery : action.removedIndexesInGallery) {
                gallery.itemsSelected.remove(indexInGallery);
                gallery.albumAdapter.notifyItemRemoved(indexInGallery);
            }
        }

        //Remove select back callback if no more items are selected
        if (gallery.itemsSelected.isEmpty()) gallery.unselectAll();
    }

}