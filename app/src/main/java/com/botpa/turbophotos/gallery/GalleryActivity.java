package com.botpa.turbophotos.gallery;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.display.DisplayActivity;
import com.botpa.turbophotos.home.HomeActivity;
import com.botpa.turbophotos.util.Action;
import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboItem;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class GalleryActivity extends AppCompatActivity {

    //Activity
    private BackManager backManager;
    private boolean isSearching = false;
    private boolean hasLoadedMetadata = false;

    //Events
    private final Library.RefreshEvent onRefresh = this::manageRefresh;
    private final Library.ActionEvent onAction = this::manageAction;

    //List adapter
    private GridLayoutManager layoutManager;
    private GalleryAdapter adapter;

    //List album
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
    private View optionsRestore;
    private View optionsDelete;
    private View optionsTrash;
    private View optionsTrashEmpty;
    private View optionsMove;
    private View optionsShare;

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


    //Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.gallery);

        //Init back manager
        backManager = new BackManager(GalleryActivity.this, getOnBackPressedDispatcher());

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
        optionsDelete = findViewById(R.id.optionsDelete);
        optionsTrash = findViewById(R.id.optionsTrash);
        optionsTrashEmpty = findViewById(R.id.optionsTrashEmpty);
        optionsShare = findViewById(R.id.optionsShare);
        optionsMove = findViewById(R.id.optionsMove);

        //List
        refreshLayout = findViewById(R.id.refreshLayout);
        list = findViewById(R.id.list);

        //Loading indicator
        loadIndicator = findViewById(R.id.loadIndicator);
        loadIndicatorText = findViewById(R.id.loadIndicatorText);

        //Insets (keyboard & system bars)
        Orion.addInsetsChangedListener(
                findViewById(R.id.galleryContent),
                new int[] {
                        WindowInsetsCompat.Type.systemBars()
                },
                (view, insets, duration) -> list.setPadding(0, insets.top, 0, list.getPaddingBottom() + insets.bottom)
        );
        Orion.addInsetsChangedListener(
                findViewById(R.id.galleryLayout),
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
    }

    private void addListeners() {
        //Navbar
        navbarSearch.setOnClickListener(view -> showSearchLayout(true));

        navbarOptions.setOnClickListener(view -> toggleOptions(true));

        //Options
        optionsLayout.setOnClickListener(view -> toggleOptions(false));

        optionsRestore.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Restore items from trash
            Library.restoreItems(GalleryActivity.this, getSelectedItems());
        });

        optionsDelete.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Delete items
            Library.deleteItems(GalleryActivity.this, getSelectedItems());
        });

        optionsTrash.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Move items to trash
            Library.trashItems(GalleryActivity.this, getSelectedItems());
        });

        optionsTrashEmpty.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Delete items
            Library.deleteItems(GalleryActivity.this, currentAlbum.items.toArray(new TurboItem[0]));
        });

        optionsShare.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Share
            Library.shareItems(GalleryActivity.this, getSelectedItems());
        });

        optionsMove.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Move items
            Library.moveItems(getSelectedItems(), null);
        });

        //Gallery
        refreshLayout.setOnRefreshListener(() -> {
            //Reload library
            Library.loadLibrary(GalleryActivity.this, true);

            //Stop refreshing
            refreshLayout.setRefreshing(false);
        });

        //Search
        searchInput.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                //Get search text
                String search = searchInput.getText().toString();

                //Filter gallery with search
                filterGallery(search, true);
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

        //Failed actions
        if (!action.failed.isEmpty()) {
            if (action.failed.size() == 1) {
                //Only 1 failed -> Show error
                Orion.snack(GalleryActivity.this, action.failed.entrySet().iterator().next().getValue());
            } else if (!action.allFailed()) {
                //More than 1 failed -> Show general error
                Orion.snack(GalleryActivity.this, "Failed to perform " + action.failed.size() + " actions");
            } else {
                //All failed -> Show general error
                Orion.snack(GalleryActivity.this, "Failed to perform all actions");
                return;
            }
        }

        //Check if gallery is empty
        if (Library.gallery.isEmpty()) {
            //Is empty -> Close display
            finish();
            return;
        }

        //Not empty -> Remove selected items & update adapter
        for (int indexInGallery : action.removedIndexesInGallery) {
            selectedItems.remove(indexInGallery);
            adapter.notifyItemRemoved(indexInGallery);
        }

        //Remove select back callback if no more items are selected
        if (selectedItems.isEmpty()) unselectAll();
    }

    //Gallery
    private int getHorizontalItemCount() {
        boolean isHorizontal = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float ratio = ((float) metrics.widthPixels / (float) metrics.heightPixels);

        //Get size for portrait
        int size = Storage.getInt("Settings.galleryImagesPerRow", 3);

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
                GalleryActivity.this,
                getHorizontalItemCount()
        );
        list.setLayoutManager(layoutManager);

        //Create adapter
        adapter = new GalleryAdapter(
                GalleryActivity.this,
                Library.gallery,
                selectedItems,
                Storage.getBool("Settings.showMissingMetadataIcon", false)
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
                Intent intent = new Intent(GalleryActivity.this, DisplayActivity.class);
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

            //Update gallery
            runOnUiThread(() -> {
                //Update album list
                adapter.notifyDataSetChanged();

                //Finish loading
                loadingIndicator.hide();
                hasLoadedMetadata = true;
            });
        }).start();
    }

    private void filterGallery(String filterText, boolean scrollToTop) {
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
            backManager.register("search", this::filterGallery);
        else
            backManager.unregister("search");

        //Clear selected items
        selectedItems.clear();

        //Filter items
        new Thread(() -> {
            //Filter library gallery list
            Library.filterGallery(filter, currentAlbum);

            //Update gallery
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

    private void filterGallery() { filterGallery("", false); }

    private void selectAlbum(Album album) {
        //Select album
        this.currentAlbum = album;

        //Change gallery title
        navbarTitle.setText(album.getName());

        //Load album
        loadMetadata(album);
        filterGallery();
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
                navbarOptions.setVisibility(View.GONE);
            }
        } else {
            //First item to be selected
            if (selectedItems.isEmpty()) {
                //Add back event
                backManager.register("selected", this::unselectAll);

                //Show options button
                navbarOptions.setVisibility(View.VISIBLE);
            }

            //Add item
            selectedItems.add(index);
        }
        adapter.notifyItemChanged(index);

        //Update gallery title
        navbarTitle.setText(currentAlbum.getName() + (selectedItems.isEmpty() ? "" : " (" + selectedItems.size() + " selected)"));
    }

    private void unselectAll() {
        //Remove back event
        backManager.unregister("selected");

        //Hide options
        navbarOptions.setVisibility(View.GONE);

        //Unselect all
        if (!selectedItems.isEmpty()) {
            HashSet<Integer> temp = new HashSet<>(selectedItems);
            selectedItems.clear();
            for (Integer index: temp) adapter.notifyItemChanged(index);
        }

        //Update gallery title
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
            Orion.showKeyboard(GalleryActivity.this);

            //Back button
            backManager.register("searchMenu", () -> showSearchLayout(false));
        } else {
            //Close keyboard
            Orion.hideKeyboard(GalleryActivity.this);
            Orion.clearFocus(GalleryActivity.this);

            //Toggle search
            Orion.hideAnim(searchLayout, 50, () -> Orion.showAnim(navbarLayout, 50));

            //Back button
            backManager.unregister("searchMenu");
        }
    }

    //Options
    private TurboItem[] getSelectedItems() {
        ArrayList<TurboItem> selectedFiles = new ArrayList<>(selectedItems.size());
        for (int index: selectedItems) selectedFiles.add(Library.gallery.get(index));
        return selectedFiles.toArray(new TurboItem[0]);
    }

    private void toggleOptions(boolean show) {
        if (show) {
            //Get state info
            boolean isSelecting = !selectedItems.isEmpty();
            boolean inTrash = (currentAlbum == Library.trash);

            //Toggle buttons
            optionsRestore.setVisibility(isSelecting && inTrash ? View.VISIBLE : View.GONE);
            optionsDelete.setVisibility(isSelecting ? View.VISIBLE : View.GONE);
            optionsTrash.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);
            optionsTrashEmpty.setVisibility(inTrash ? View.VISIBLE : View.GONE);
            optionsShare.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);
            optionsMove.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);

            //Show
            Orion.showAnim(optionsLayout);
            backManager.register("galleryOptions", () -> toggleOptions(false));
        } else {
            Orion.hideAnim(optionsLayout);
            backManager.unregister("galleryOptions");
        }
    }

}
