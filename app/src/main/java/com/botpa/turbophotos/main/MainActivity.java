package com.botpa.turbophotos.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import com.botpa.turbophotos.backup.BackupActivity;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.util.FileActionResult;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboFile;
import com.botpa.turbophotos.settings.SettingsActivity;

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
    private static boolean shouldReload = false;
    private boolean isLoaded = false;
    private boolean skipResume = true;

    public BackManager backManager;

    //Navbar
    private View backup;
    private View settings;

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

    //Gallery
    public final GalleryHelper gallery = new GalleryHelper(this);

    //Display
    public final DisplayHelper display = new DisplayHelper(this);


    //App
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Load storage
        Storage.load(MainActivity.this);

        //Check permissions
        checkPermissions();
    }

    private void loadApp() {
        //App
        backManager = new BackManager(MainActivity.this, getOnBackPressedDispatcher());
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);

        //Load views
        loadViews();

        //Add listeners
        addListeners();

        //Init list adapters (gallery & display)
        gallery.initAdapters();
        display.initAdapters();

        //Load albums
        new Thread(() -> {
            //Load albums
            Library.loadLibrary(MainActivity.this, true);

            //Show gallery
            runOnUiThread(() -> {
                //Show list
                gallery.showList(true);

                //Reload albums list
                gallery.homeAdapter.notifyDataSetChanged();
            });

            //Gallery loaded
            isLoaded = true;
        }).start();
    }

    private void loadViews() {
        //Load indicator
        loadIndicator = findViewById(R.id.loadIndicator);
        loadIndicatorText = findViewById(R.id.loadIndicatorText);

        //Gallery & Search
        gallery.loadViews();

        //Display
        display.loadViews();
    }

    private void addListeners() {
        //Gallery & Search
        gallery.addListeners();

        //Display
        display.addListeners();
    }

    public static void shouldReload() {
        //Reload on resume
        shouldReload = true;
    }

    //Permission
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
    public void onResume() {
        super.onResume();

        //Skip first resume (called after onCreate)
        if (skipResume) {
            skipResume = false;
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
            return;
        }

        //Gallery not loaded -> Skip next
        if (!isLoaded) return;

        //Update horizontal item count
        int newHorizontalItemCount = Storage.getInt(
                gallery.inHome ? "Settings.galleryAlbumsPerRow" : "Settings.galleryImagesPerRow",
                gallery.inHome ? 2 : 3);
        if (gallery.layoutManager.getSpanCount() != newHorizontalItemCount) gallery.layoutManager.setSpanCount(newHorizontalItemCount);

        //Update show missing metadata icon
        boolean showMissingMetadataIcon = Storage.getBool("Settings.showMissingMetadataIcon", false);
        if (showMissingMetadataIcon != gallery.albumAdapter.getShowMissingMetadataIcon()) {
            gallery.albumAdapter.setShowMissingMetadataIcon(showMissingMetadataIcon);
            gallery.albumAdapter.notifyDataSetChanged();
        }

        //Check albums for updates
        gallery.refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }

    //Actions
    public void shareFiles(TurboFile[] files) {
        //No files
        if (files.length == 0) return;

        //Share files
        Intent intent;
        if (files.length == 1) {
            //Share 1 file
            intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, Orion.getUriFromFile(MainActivity.this, files[0].file));
            intent.setType(files[0].getMimeType());
        } else {
            //Share multiple files
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Uri> URIs = new ArrayList<>();
            for (TurboFile file : files) URIs.add(Orion.getUriFromFile(MainActivity.this, file.file));
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, URIs);
            intent.setType("*/*");
        }
        startActivity(Intent.createChooser(intent, null));
    }

    public void deleteFile(TurboFile file) {
        //Delete file & consume action result
        manageActionResult(file, Library.deleteFile(file));
    }

    public void trashFile(TurboFile file) {
        //Trash file & consume action result
        manageActionResult(file, Library.trashFile(MainActivity.this, file));
    }

    public void restoreFile(TurboFile file) {
        //Restore file & manage action result
        manageActionResult(file, Library.restoreFile(MainActivity.this, file));
    }

    private void manageActionResult(TurboFile file, FileActionResult result) {
        //Action was not performed
        if (result.action.equals(FileActionResult.ACTION_NONE)) {
            Orion.snack(MainActivity.this, result.info);
            return;
        }

        //Check if albums list was changed
        if (result.sortedAlbumsList) {
            //Sorted albums list -> Notify all
            gallery.homeAdapter.notifyDataSetChanged();
        } else if (!result.deletedAlbum) {
            //Deleted album -> Notify item removed
            gallery.homeAdapter.notifyItemRemoved(result.indexOfAlbum);
        } else if (result.acted(FileActionResult.ACTION_TRASH) || result.acted(FileActionResult.ACTION_RESTORE)) {
            //Trashed/restored file -> Notify all
            gallery.homeAdapter.notifyDataSetChanged(); //Should update this to change only trash index instead of all
        }

        //Check if image is in gallery list
        int indexInGallery = gallery.files.indexOf(file);
        if (indexInGallery == -1) return;

        //Is present -> Remove it & update adapter
        gallery.files.remove(indexInGallery);
        gallery.selected.remove(indexInGallery);
        gallery.albumAdapter.notifyItemRemoved(indexInGallery);

        //Check gallery needs to be closed or select a new image
        if (gallery.files.isEmpty()) {
            //Gallery is empty -> Close display list & return to albums
            display.close();
            gallery.showAlbumsList(true);
        } else if (display.isOpen && display.current == file) {
            //Display list is visible -> Check if a new image can be selected
            if (display.currentRelativeIndex != display.files.size() - 1) {
                //An image is available next -> Select it
                display.open(indexInGallery);   //Next image would be the same index since this image was deleted
            } else if (display.currentRelativeIndex != 0) {
                //An image is available before -> Select it
                display.open(indexInGallery - 1);
            }
        }
    }

}