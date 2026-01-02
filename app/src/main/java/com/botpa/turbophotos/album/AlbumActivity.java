package com.botpa.turbophotos.album;

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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.display.DisplayActivity;
import com.botpa.turbophotos.gallery.Action;
import com.botpa.turbophotos.gallery.Album;
import com.botpa.turbophotos.gallery.GalleryActivity;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.gallery.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.gallery.CoonItem;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;

public class AlbumActivity extends GalleryActivity {

    //Activity
    private BackManager backManager;
    private boolean isSearching = false;
    private boolean hasLoadedMetadata = false;

    //Events
    private final Library.RefreshEvent onRefresh = this::manageRefresh;
    private final Library.ActionEvent onAction = this::manageAction;

    //List adapter
    private GridLayoutManager layoutManager;
    private AlbumAdapter adapter;

    //List album
    private boolean inTrash = false;
    private Album currentAlbum = null;
    private final HashSet<Integer> selectedItems = new HashSet<>();

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
    private View optionsEdit;
    private View optionsShare;
    private View optionsMove;
    private View optionsCopy;
    private View optionsTrash;
    private View optionsRestore;
    private View optionsRestoreAll;
    private View optionsDelete;
    private View optionsDeleteAll;

    //Views (list)
    private SwipeRefreshLayout refreshLayout;
    private FastScrollRecyclerView list;

    //Views (loading indicator)
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
            if (index < 0) {
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
        optionsRestore = findViewById(R.id.optionsRestore);
        optionsRestoreAll = findViewById(R.id.optionsRestoreAll);
        optionsDelete = findViewById(R.id.optionsDelete);
        optionsDeleteAll = findViewById(R.id.optionsDeleteAll);
        optionsTrash = findViewById(R.id.optionsTrash);
        optionsShare = findViewById(R.id.optionsShare);
        optionsEdit = findViewById(R.id.optionsEdit);
        optionsMove = findViewById(R.id.optionsMove);
        optionsCopy = findViewById(R.id.optionsCopy);

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

        optionsEdit.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Empty selection
            if (selectedItems.size() != 1) return;

            //Edit
            Library.editItem(AlbumActivity.this, Library.gallery.get(selectedItems.iterator().next()));
        });

        optionsShare.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Share
            Library.shareItems(AlbumActivity.this, getSelectedItems());
        });

        optionsMove.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Move items
            Library.moveItems(AlbumActivity.this, getSelectedItems());
        });

        optionsCopy.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Copy items
            Library.copyItems(AlbumActivity.this, getSelectedItems());
        });

        optionsTrash.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Move items to trash
            trashItems(getSelectedItems());
        });

        optionsRestore.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Restore items from trash
            restoreItems(getSelectedItems());
        });

        optionsRestoreAll.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Restore all items
            restoreItems(currentAlbum.items.toArray(new CoonItem[0]));
        });

        optionsDelete.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Delete items
            Library.deleteItems(AlbumActivity.this, getSelectedItems());
        });

        optionsDeleteAll.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Delete all items
            Library.deleteItems(AlbumActivity.this, currentAlbum.items.toArray(new CoonItem[0]));
        });

        //List
        refreshLayout.setOnRefreshListener(() -> {
            //Reload library
            Library.loadLibrary(AlbumActivity.this, false); //Soft refresh to look for new files

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
        //Refresh list
        if (updated) selectAlbum(currentAlbum);
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
        //Create layout manager
        layoutManager = new GridLayoutManager(
                AlbumActivity.this,
                getHorizontalItemCount()
        );
        list.setLayoutManager(layoutManager);

        //Create adapter
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
                //Not selecting -> Open display
                Intent intent = new Intent(AlbumActivity.this, DisplayActivity.class);
                intent.putExtra("index", index);
                startActivity(intent);
            }
        });
        adapter.setOnLongClickListener((view, index) -> {
            //Toggle item selected
            toggleSelected(index);

            //Consume click
            return true;
        });
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
            for (Integer index: temp) adapter.notifyItemChanged(index);
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

            //Toggle buttons
            optionsEdit.setVisibility(isSelectingSingle && !inTrash ? View.VISIBLE : View.GONE);
            optionsShare.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);
            optionsMove.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);
            optionsCopy.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);
            optionsTrash.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);
            optionsRestore.setVisibility(isSelecting && inTrash ? View.VISIBLE : View.GONE);
            optionsRestoreAll.setVisibility(!isSelecting && inTrash ? View.VISIBLE : View.GONE);
            optionsDelete.setVisibility(isSelecting ? View.VISIBLE : View.GONE);
            optionsDeleteAll.setVisibility(!isSelecting && inTrash ? View.VISIBLE : View.GONE);

            //Show
            Orion.showAnim(optionsLayout);
            backManager.register("options", () -> toggleOptions(false));
        } else {
            Orion.hideAnim(optionsLayout);
            backManager.unregister("options");
        }
    }

}
