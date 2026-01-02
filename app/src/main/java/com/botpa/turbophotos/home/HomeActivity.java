package com.botpa.turbophotos.home;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.widget.LinearLayout;

import com.botpa.turbophotos.backup.BackupActivity;
import com.botpa.turbophotos.album.AlbumActivity;
import com.botpa.turbophotos.gallery.GalleryActivity;
import com.botpa.turbophotos.settings.SettingsActivity;
import com.botpa.turbophotos.gallery.actions.Action;
import com.botpa.turbophotos.gallery.Album;
import com.botpa.turbophotos.gallery.Library;
import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import org.jetbrains.annotations.NotNull;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class HomeActivity extends GalleryActivity {

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

    //Events
    private final Library.RefreshEvent onRefresh = this::manageRefresh;
    private final Library.ActionEvent onAction = this::manageAction;

    //Adapters
    private HomeAdapter adapter;
    private GridLayoutManager layoutManager;

    //Views (navbar)
    private View navbarBackup;
    private View navbarSettings;

    //Views (list)
    private SwipeRefreshLayout refreshLayout;
    private FastScrollRecyclerView list;

    //Views (loading indicator)
    private View loadingIndicator;

    //Views (system)
    private View systemNotificationsBar;
    private View systemNavigationBar;


    //Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.home_screen);

        //Enable HDR
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);

        //Load storage
        Storage.load(HomeActivity.this);

        //Load views & add listeners
        loadViews();
        addListeners();

        //Check permissions
        checkPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Remove events
        Library.removeOnRefreshEvent(onRefresh);
        Library.removeOnActionEvent(onAction);
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

        //Library not loaded -> Skip next
        if (!isLoaded) return;

        //Update horizontal item count
        updateHorizontalItemCount();

        //Refresh albums (check for new items)
        Library.loadLibrary(HomeActivity.this, false);
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //Update horizontal item count
        updateHorizontalItemCount();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }

    private void checkPermissions() {
        //Update permissions
        if (Environment.isExternalStorageManager()) {
            permissionWrite = true;
            findViewById(R.id.permissionWrite).setAlpha(0.5f);
        }
        if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
            permissionMedia = true;
            findViewById(R.id.permissionMedia).setAlpha(0.5f);
        }
        if (NotificationManagerCompat.from(HomeActivity.this).areNotificationsEnabled()) {
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

    private void initActivity() {
        //Init adapters
        initAdapters();

        //Hide list
        list.setVisibility(View.GONE);

        //Load albums
        new Thread(() -> {
            //Load albums
            Library.loadLibrary(HomeActivity.this, true);

            //Show list
            runOnUiThread(() -> {
                //Hide loading indicator
                loadingIndicator.setVisibility(View.GONE);

                //Show list
                Orion.showAnim(list);

                //Reload albums list
                adapter.notifyDataSetChanged();
            });

            //Add events
            Library.addOnRefreshEvent(onRefresh);
            Library.addOnActionEvent(onAction);

            //Mark as loaded
            isLoaded = true;
        }).start();
    }

    public static void reloadOnResume() {
        //Reload on resume
        shouldReloadOnResume = true;
    }

    //Views
    private void loadViews() {
        //Navbar
        navbarBackup = findViewById(R.id.navbarBackup);
        navbarSettings = findViewById(R.id.navbarSettings);

        //List
        refreshLayout = findViewById(R.id.refreshLayout);
        list = findViewById(R.id.list);

        //Loading indicator
        loadingIndicator = findViewById(R.id.loadingIndicator);

        //System
        systemNotificationsBar = findViewById(R.id.notificationsBar);
        systemNavigationBar = findViewById(R.id.navigationBar);


        //Insets (keyboard & system bars)
        Orion.addInsetsChangedListener(
                findViewById(R.id.content),
                new int[] {
                        WindowInsetsCompat.Type.systemBars()
                },
                (view, insets, duration) -> {
                    refreshLayout.setProgressViewOffset(false, 0, insets.top + 50);
                    list.setPadding(0, insets.top, 0, list.getPaddingBottom() + insets.bottom);
                }
        );
        Orion.addInsetsChangedListener(
                findViewById(R.id.layout),
                new int[] {
                        WindowInsetsCompat.Type.ime(),
                        WindowInsetsCompat.Type.systemBars()
                },
                200,
                (view, insets, percent) -> {
                    //Check if keyboard is open
                    WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(view);
                    if (windowInsets != null && windowInsets.isVisible(WindowInsetsCompat.Type.ime())) {
                        //Keyboard is open -> Only use keyboard insets
                        insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                    }

                    //Update insets
                    Orion.onInsetsChangedDefault.run(view, insets, percent);
                }
        );

        //Insets (system bars background)
        Orion.addInsetsChangedListener(
                systemNotificationsBar,
                new int[] {
                        WindowInsetsCompat.Type.systemBars()
                },
                (view, insets, duration) -> {
                    systemNotificationsBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, insets.top));
                    systemNavigationBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, insets.bottom));
                }
        );
    }

    private void addListeners() {
        //Navbar
        navbarSettings.setOnClickListener(view -> {
            //Open settings
            startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
        });

        navbarBackup.setOnClickListener(view -> {
            //Open backup
            startActivity(new Intent(HomeActivity.this, BackupActivity.class));
        });

        //List
        refreshLayout.setOnRefreshListener(() -> {
            //Reload library
            Library.loadLibrary(HomeActivity.this, true); //Fully refresh

            //Stop refreshing
            refreshLayout.setRefreshing(false);
        });
    }

    //Events
    private void manageRefresh(boolean updated) {
        //Refresh list
        if (updated) adapter.notifyDataSetChanged();
    }

    private void manageAction(Action action) {
        //No action
        if (action.isOfType(Action.TYPE_NONE)) return;

        //Check if albums list was changed
        if (action.hasSortedAlbumsList) {
            //Sorted albums list -> Notify all
            adapter.notifyDataSetChanged();
        } else {
            //Check if trash was added, removed or updated
            switch (action.trashAction) {
                case Action.TRASH_ADDED:
                    adapter.notifyItemInserted(0);
                    break;
                case Action.TRASH_REMOVED:
                    adapter.notifyItemRemoved(0);
                    break;
                case Action.TRASH_UPDATED:
                    adapter.notifyItemChanged(0);
                    break;
            }

            //Check if albums were deleted
            if (!action.removedIndexesInAlbums.isEmpty()) {
                //Albums were deleted -> Notify items removed
                for (int albumIndex : action.removedIndexesInAlbums) {
                    //Notify position removed
                    adapter.notifyItemRemoved(adapter.getPositionFromIndex(albumIndex));
                }
            }

            //Check if albums were sorted
            if (!action.modifiedAlbums.isEmpty()) {
                //Albums were sorted -> Notify items changed
                for (Album album : action.modifiedAlbums) {
                    //Get album index
                    int albumIndex = adapter.getIndexFromAlbum(album);
                    if (albumIndex < 0 && !album.isEspecial()) continue;

                    //Notify position changed
                    adapter.notifyItemChanged(adapter.getPositionFromIndex(albumIndex));
                }
            }
        }
    }

    //Home
    private int getHorizontalItemCount() {
        boolean isHorizontal = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float ratio = ((float) metrics.widthPixels / (float) metrics.heightPixels);

        //Get size for portrait
        int size = Storage.getInt("Settings.homeItemsPerRow", 2);

        //Return size for current orientation
        return isHorizontal ? (int) (size * ratio) : size;
    }

    private void updateHorizontalItemCount() {
        int newHorizontalItemCount = getHorizontalItemCount();
        if (layoutManager.getSpanCount() != newHorizontalItemCount) layoutManager.setSpanCount(newHorizontalItemCount);
    }

    private void initAdapters() {
        //Create layout manager
        layoutManager = new GridLayoutManager(HomeActivity.this, getHorizontalItemCount());
        list.setLayoutManager(layoutManager);

        //Init home adapter
        adapter = new HomeAdapter(HomeActivity.this, Library.albums);
        adapter.setOnClickListener((view, album) -> {
            //Create open animation
            int startX = view.getLeft() + (view.getWidth() / 2);
            int startY = view.getTop() + (view.getHeight() / 2);
            ActivityOptions options = ActivityOptions.makeScaleUpAnimation(
                    list, //The view to scale from
                    startX, startY, //Starting point
                    0, 0 //Starting size
            );

            //Open album
            Intent intent = new Intent(HomeActivity.this, AlbumActivity.class);
            if (album == Library.trash) {
                intent.putExtra("albumName", "trash");
            } else if (album == Library.all) {
                intent.putExtra("albumName", "all");
            } else {
                intent.putExtra("albumIndex", Library.albums.indexOf(album));
            }
            startActivity(intent, options.toBundle());
        });
        list.setAdapter(adapter);
    }

}