package com.botpa.turbophotos.album;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.display.DisplayActivity;
import com.botpa.turbophotos.gallery.LoadingIndicator;
import com.botpa.turbophotos.gallery.actions.Action;
import com.botpa.turbophotos.gallery.Album;
import com.botpa.turbophotos.gallery.GalleryActivity;
import com.botpa.turbophotos.gallery.options.OptionsAdapter;
import com.botpa.turbophotos.gallery.options.OptionsItem;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.gallery.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.gallery.CoonItem;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class AlbumActivity extends GalleryActivity {

    //Activity
    private BackManager backManager;
    private boolean isSearching = false;
    private boolean hasLoadedMetadata = false;

    //Activity (external item picker)
    private boolean isPicking = false; //An app requested to select an item

    //Events
    private final Library.RefreshEvent onRefresh = this::manageRefresh;
    private final Library.ActionEvent onAction = this::manageAction;

    //List
    private GridLayoutManager layoutManager;
    private AlbumAdapter adapter;

    //Options
    private final Map<String, OptionsItem> options = new HashMap<>();
    private final List<OptionsItem> optionsItems = new ArrayList<>();
    private static final String OPTIONS_SEPARATOR = "separator";
    private static final String OPTIONS_EDIT = "edit";
    private static final String OPTIONS_SHARE = "share";
    private static final String OPTIONS_MOVE = "move";
    private static final String OPTIONS_COPY = "copy";
    private static final String OPTIONS_TRASH = "trash";
    private static final String OPTIONS_RESTORE = "restore";
    private static final String OPTIONS_RESTORE_ALL = "restore_all";
    private static final String OPTIONS_DELETE = "delete";
    private static final String OPTIONS_DELETE_ALL = "delete_all";

    private OptionsAdapter optionsAdapter;

    //List album
    private boolean inTrash = false;
    private Album currentAlbum = null;
    private final Set<Integer> selectedItems = new HashSet<>();

    //Views (navbar)
    private View navbarLayout;
    private TextView navbarTitle;
    private TextView navbarSubtitle;
    private View navbarSearch;
    private View navbarOptions;

    //Views (search)
    private View searchLayout;
    private EditText searchInput;
    private View searchClose;

    //Views (options)
    private View optionsLayout;
    private RecyclerView optionsList;

    //Views (list)
    private SwipeRefreshLayout refreshLayout;
    private FastScrollRecyclerView list;

    //Views (loading indicator)
    private View loadIndicator;
    private TextView loadIndicatorText;

    public LoadingIndicator loadingIndicator = new LoadingIndicator() {
        @Override
        public void search() {
            loadIndicatorText.setText("Searching...");
            loadIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        public void load(@NonNull String content) {
            runOnUiThread(() -> {
                loadIndicatorText.setText("Loading " + content + "...");
                loadIndicator.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void load(@NonNull String folder, @NonNull String type) {
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

    //Views (system)
    private View systemNotificationsBar;
    private View systemNavigationBar;


    //Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.album_screen);

        //Enable HDR
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);

        //Load storage
        Storage.load(AlbumActivity.this);

        //Init back manager
        backManager = new BackManager(AlbumActivity.this, getOnBackPressedDispatcher());

        //Add events
        Library.addOnRefreshEvent(onRefresh);
        Library.addOnActionEvent(onAction);

        //Load views & add listeners
        loadViews();
        addListeners();

        //Init adapters
        initAdapters();

        //Init activity
        initActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Remove events
        Library.removeOnRefreshEvent(onRefresh);
        Library.removeOnActionEvent(onAction);
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //Update horizontal item count
        updateHorizontalItemCount();
    }

    private void initActivity() {
        //Check if intent is valid
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        //Update is picking
        isPicking = intent.getBooleanExtra("isPicking", false);

        //Check if intent has album name or index
        if (intent.hasExtra("albumName")) {
            //Has name -> Check it
            switch (intent.getStringExtra("albumName")) {
                case "trash":
                    selectAlbum(Library.trash);
                    break;
                case "all":
                    selectAlbum(Library.all);
                    break;
                default:
                    finish();
            }
        } else {
            //No name -> Check index
            int index = intent.getIntExtra("albumIndex", -1);
            if (index < 0 || index >= Library.albums.size()) {
                finish();
                return;
            }
            selectAlbum(Library.albums.get(index));
        }
    }

    //Views
    private void loadViews() {
        //Navbar
        navbarLayout = findViewById(R.id.navbarLayout);
        navbarTitle = findViewById(R.id.navbarTitle);
        navbarSubtitle = findViewById(R.id.navbarSubtitle);
        navbarSearch = findViewById(R.id.navbarSearch);
        navbarOptions = findViewById(R.id.navbarOptions);

        //Search
        searchLayout = findViewById(R.id.searchLayout);
        searchInput = findViewById(R.id.searchInput);
        searchClose = findViewById(R.id.searchClose);

        //Options
        optionsLayout = findViewById(R.id.optionsLayout);
        optionsList = findViewById(R.id.optionsList);

        //List
        refreshLayout = findViewById(R.id.refreshLayout);
        list = findViewById(R.id.list);

        //Loading indicator
        loadIndicator = findViewById(R.id.loadIndicator);
        loadIndicatorText = findViewById(R.id.loadIndicatorText);

        //System
        systemNotificationsBar = findViewById(R.id.notificationsBar);
        systemNavigationBar = findViewById(R.id.navigationBar);


        //Insets (content)
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

        //Insets (layout)
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

        //Insets (options layout)
        Orion.addInsetsChangedListener(optionsLayout, new int[] { WindowInsetsCompat.Type.systemBars() });

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
        navbarSearch.setOnClickListener(view -> showSearchLayout(true));

        navbarOptions.setOnClickListener(view -> toggleOptions(true));

        //Options
        optionsLayout.setOnClickListener(view -> toggleOptions(false));

        options.put(OPTIONS_SEPARATOR, new OptionsItem());

        options.put(OPTIONS_EDIT, new OptionsItem(R.drawable.edit, "Edit", () -> {
            //Empty selection
            if (selectedItems.size() != 1) return;

            //Edit
            Library.editItem(AlbumActivity.this, Library.gallery.get(selectedItems.iterator().next()));
        }));

        options.put(OPTIONS_SHARE, new OptionsItem(R.drawable.share, "Share", () -> {
            //Share
            Library.shareItems(AlbumActivity.this, getSelectedItems());
        }));

        options.put(OPTIONS_MOVE, new OptionsItem(R.drawable.move, "Move to album", () -> {
            //Move items
            Library.moveItems(AlbumActivity.this, getSelectedItems());
        }));

        options.put(OPTIONS_COPY, new OptionsItem(R.drawable.copy, "Copy to album", () -> {
            //Copy items
            Library.copyItems(AlbumActivity.this, getSelectedItems());
        }));

        options.put(OPTIONS_TRASH, new OptionsItem(R.drawable.delete, "Move to trash", () -> {
            //Move items to trash
            trashItems(getSelectedItems());
        }));

        options.put(OPTIONS_RESTORE, new OptionsItem(R.drawable.restore, "Restore", () -> {
            //Restore items from trash
            restoreItems(getSelectedItems());
        }));

        options.put(OPTIONS_RESTORE_ALL, new OptionsItem(R.drawable.restore, "Restore all", () -> {
            //Restore all items from trash
            restoreItems(currentAlbum.items.toArray(new CoonItem[0]));
        }));

        options.put(OPTIONS_DELETE, new OptionsItem(R.drawable.delete, "Delete", () -> {
            //Delete item
            Library.deleteItems(AlbumActivity.this, getSelectedItems());
        }));

        options.put(OPTIONS_DELETE_ALL, new OptionsItem(R.drawable.delete, "Delete all", () -> {
            //Delete all items
            Library.deleteItems(AlbumActivity.this, currentAlbum.items.toArray(new CoonItem[0]));
        }));

        //List
        refreshLayout.setOnRefreshListener(() -> {
            //Reload library
            Library.loadLibrary(AlbumActivity.this, false); //Soft refresh to ONLY look for new files

            //Stop refreshing
            refreshLayout.setRefreshing(false);
        });

        //Search
        searchInput.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                //Get search text
                String search = searchInput.getText().toString();

                //Filter items with search
                filterItems(search, true);
            }
            return false;
        });

        searchClose.setOnClickListener(view -> showSearchLayout(false));
    }

    //Events
    private void manageRefresh(boolean updated) {
        runOnUiThread(() -> {
            //Nothing updated
            if (!updated) return;

            //Unselect all
            unselectAll();

            //Refresh list
            selectAlbum(currentAlbum);
        });
    }

    private void manageAction(Action action) {
        //No action
        if (action.isOfType(Action.TYPE_NONE)) return;

        //Check if gallery is empty
        if (Library.gallery.isEmpty()) {
            //Is empty -> Close display
            finish();
            return;
        }

        //Remove selected items & update adapter
        for (int indexInGallery : action.removedIndexesInGallery) {
            selectedItems.remove(indexInGallery);
            adapter.notifyItemRemoved(indexInGallery);
        }

        //Remove select back callback if no more items are selected
        if (selectedItems.isEmpty()) unselectAll();
    }

    //Album
    private int getHorizontalItemCount() {
        boolean isHorizontal = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float ratio = ((float) metrics.widthPixels / (float) metrics.heightPixels);

        //Get size for portrait
        int size = Storage.getInt("Settings.albumItemsPerRow", 3);

        //Return size for current orientation
        return isHorizontal ? (int) (size * ratio) : size;
    }

    private void updateHorizontalItemCount() {
        int newHorizontalItemCount = getHorizontalItemCount();
        if (layoutManager.getSpanCount() != newHorizontalItemCount) layoutManager.setSpanCount(newHorizontalItemCount);
    }

    private void initAdapters() {
        //Init album layout manager
        layoutManager = new GridLayoutManager(AlbumActivity.this, getHorizontalItemCount());
        list.setLayoutManager(layoutManager);

        //Init album adapter
        adapter = new AlbumAdapter(
                AlbumActivity.this,
                Library.gallery,
                selectedItems,
                Storage.getBool("Settings.albumShowMissingMetadataIcon", false)
        );
        list.setAdapter(adapter);

        //Add adapter listeners
        adapter.setOnClickListener((view, index) -> {
            //Loading metadata -> Return
            if (!hasLoadedMetadata) return;

            //Check if selecting
            if (!selectedItems.isEmpty()) {
                //Selecting -> Toggle selected
                toggleSelected(index);
            } else {
                //Not selecting -> Check action
                if (isPicking) {
                    //Pick item
                    Intent resultIntent = new Intent();
                    resultIntent.setData(Orion.getFileUriFromFilePath(AlbumActivity.this, Library.gallery.get(index).file.getAbsolutePath()));
                    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    //Open display
                    Intent intent = new Intent(AlbumActivity.this, DisplayActivity.class);
                    intent.putExtra("index", index);
                    startActivity(intent);
                }
            }
        });
        adapter.setOnLongClickListener((view, index) -> {
            //Toggle item selected
            toggleSelected(index);

            //Consume click
            return true;
        });

        //Init options layout manager
        optionsList.setLayoutManager(new LinearLayoutManager(AlbumActivity.this));

        //Init options adapter
        optionsAdapter = new OptionsAdapter(AlbumActivity.this, optionsItems);
        optionsAdapter.setOnClickListener((view, index) -> {
            //Get option
            OptionsItem option = optionsItems.get(index);
            if (option == null) return;

            //Get action
            Runnable action = option.getAction();
            if (action == null) return;

            //Invoke action
            action.run();
            toggleOptions(false);
        });
        optionsList.setAdapter(optionsAdapter);
    }

    private void loadMetadata(Album album) {
        //Start loading
        hasLoadedMetadata = false;

        //Load metadata
        new Thread(() -> {
            //Load metadata
            Library.loadMetadata(loadingIndicator, album);

            //Update items
            runOnUiThread(() -> {
                //Update album list
                adapter.notifyDataSetChanged();

                //Finish loading
                loadingIndicator.hide();
                hasLoadedMetadata = true;
            });
        }).start();
    }

    private void filterItems(String filterText, boolean scrollToTop) {
        //Ignore case
        String filter = filterText.toLowerCase();

        //Check if filtering
        boolean isFiltering = !filter.isEmpty();

        //Loading or searching
        if (isSearching || (!hasLoadedMetadata && isFiltering)) return;

        //Update subtitle
        navbarSubtitle.setText(isFiltering ? "Search: " + filterText : "");
        navbarSubtitle.setVisibility(isFiltering ? View.VISIBLE : View.GONE);

        //Start search
        isSearching = true;
        searchInput.setText(filterText);
        if (isFiltering) loadingIndicator.search();
        showSearchLayout(false);

        //Update back manager
        if (isFiltering)
            backManager.register("search", this::filterItems);
        else
            backManager.unregister("search");

        //Clear selected items
        selectedItems.clear();

        //Filter items
        new Thread(() -> {
            //Filter library gallery list
            Library.filterGallery(filter, currentAlbum);

            //Update items
            runOnUiThread(() -> {
                //Update adapter
                adapter.notifyDataSetChanged();

                //Scroll to top
                list.stopScroll();
                if (scrollToTop) list.scrollToPosition(0);

                //Finish searching
                if (isFiltering) loadingIndicator.hide();
                isSearching = false;
            });
        }).start();
    }

    private void filterItems() { filterItems("", false); }

    private void selectAlbum(Album album) {
        //Select album
        this.currentAlbum = album;

        //Check if in trash (trash always shows options cause of "Empty trash" action)
        inTrash = (album == Library.trash);
        navbarOptions.setVisibility(inTrash ? View.VISIBLE : View.GONE);

        //Change navbar title
        navbarTitle.setText(album.getName());

        //Load album
        loadMetadata(album);
        filterItems();
    }

    //Selections
    private void toggleSelected(int index) {
        //Check if item is selected
        if (selectedItems.contains(index)) {
            //Remove item
            selectedItems.remove(index);

            //No more selected items
            if (selectedItems.isEmpty()) {
                //Remove back event
                backManager.unregister("selected");

                //Hide options button
                if (!inTrash) navbarOptions.setVisibility(View.GONE);
            }
        } else {
            //First item to be selected
            if (selectedItems.isEmpty()) {
                //Add back event
                backManager.register("selected", this::unselectAll);

                //Show options button
                if (!inTrash) navbarOptions.setVisibility(View.VISIBLE);
            }

            //Add item
            selectedItems.add(index);
        }
        adapter.notifyItemChanged(index);

        //Update navbar title
        navbarTitle.setText(currentAlbum.getName() + (selectedItems.isEmpty() ? "" : " (" + selectedItems.size() + " selected)"));
    }

    private void unselectAll() {
        //Remove back event
        backManager.unregister("selected");

        //Hide options
        if (!inTrash) navbarOptions.setVisibility(View.GONE);

        //Unselect all
        if (!selectedItems.isEmpty()) {
            HashSet<Integer> temp = new HashSet<>(selectedItems);
            selectedItems.clear();
            for (int index : temp) adapter.notifyItemChanged(index);
        }

        //Update navbar title
        navbarTitle.setText(currentAlbum.getName());
    }

    //Search
    private void showSearchLayout(boolean show) {
        if (show) {
            //Loading or searching
            if (isSearching) return;

            //Toggle search
            Orion.hideAnim(navbarLayout, 50, () -> Orion.showAnim(searchLayout, 50));

            //Focus text & show keyboard
            searchInput.requestFocus();
            searchInput.selectAll();
            Orion.showKeyboard(AlbumActivity.this);

            //Back button
            backManager.register("searchMenu", () -> showSearchLayout(false));
        } else {
            //Close keyboard
            Orion.hideKeyboard(AlbumActivity.this);
            Orion.clearFocus(AlbumActivity.this);

            //Toggle search
            Orion.hideAnim(searchLayout, 50, () -> Orion.showAnim(navbarLayout, 50));

            //Back button
            backManager.unregister("searchMenu");
        }
    }

    //Options
    private CoonItem[] getSelectedItems() {
        ArrayList<CoonItem> selectedFiles = new ArrayList<>(selectedItems.size());
        for (int index: selectedItems) selectedFiles.add(Library.gallery.get(index));
        return selectedFiles.toArray(new CoonItem[0]);
    }

    private void toggleOptions(boolean show) {
        if (show) {
            //Get state info
            boolean isSelecting = !selectedItems.isEmpty();
            boolean isSelectingSingle = selectedItems.size() == 1;

            //Update options list
            optionsItems.clear();
            if (!inTrash && isSelectingSingle)
                optionsItems.add(options.get(OPTIONS_EDIT));
            if (!inTrash && isSelecting)
                optionsItems.add(options.get(OPTIONS_SHARE));
            if (!inTrash && isSelecting)
                optionsItems.add(options.get(OPTIONS_MOVE));
            if (!inTrash && isSelecting)
                optionsItems.add(options.get(OPTIONS_COPY));
            if (!inTrash)
                optionsItems.add(options.get(OPTIONS_SEPARATOR));
            if (!inTrash && isSelecting)
                optionsItems.add(options.get(OPTIONS_TRASH));
            if (inTrash && isSelecting)
                optionsItems.add(options.get(OPTIONS_RESTORE));
            if (inTrash && !isSelecting)
                optionsItems.add(options.get(OPTIONS_RESTORE_ALL));
            if (isSelecting)
                optionsItems.add(options.get(OPTIONS_DELETE));
            if (inTrash && !isSelecting)
                optionsItems.add(options.get(OPTIONS_DELETE_ALL));
            optionsAdapter.notifyDataSetChanged();

            //Show
            Orion.showAnim(optionsLayout);
            backManager.register("options", () -> toggleOptions(false));
        } else {
            //Hide
            Orion.hideAnim(optionsLayout);
            backManager.unregister("options");
        }
    }

}
