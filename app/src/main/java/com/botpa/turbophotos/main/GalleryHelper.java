package com.botpa.turbophotos.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.backup.BackupActivity;
import com.botpa.turbophotos.settings.SettingsActivity;
import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class GalleryHelper {

    //State
    private final MainActivity activity;

    public boolean inHome = true;
    public boolean isSearching = false;
    public boolean hasLoadedMetadata = false;

    //Loaded album
    private Album album = null;
    private ArrayList<TurboFile> filesUnfiltered = new ArrayList<>();

    public final ArrayList<TurboFile> files = new ArrayList<>();
    public final HashSet<Integer> selected = new HashSet<>();

    //Adapters
    private Parcelable listScroll;

    public GalleryHomeAdapter homeAdapter;
    public GalleryAlbumAdapter albumAdapter;
    public GridLayoutManager layoutManager;

    //Views
    private TextView title;
    private TextView subtitle;

    private View optionsButton;

    private View optionsLayout;
    private View optionsSettings;
    private View optionsBackup;
    private View optionsRestore;
    private View optionsTrash;
    private View optionsDelete;
    private View optionsShare;

    private SwipeRefreshLayout refreshLayout;
    private FastScrollRecyclerView list;

    private View searchClosedLayout;
    private View searchOpenButton;

    private View searchOpenLayout;
    private View searchCloseButton;
    private EditText searchInput;


    //Constructor
    public GalleryHelper(MainActivity activity) {
        this.activity = activity;
    }

    //Views
    public void loadViews() {
        //Navbar
        title = activity.findViewById(R.id.galleryTitle);
        subtitle = activity.findViewById(R.id.gallerySubtitle);
        optionsButton = activity.findViewById(R.id.galleryOptions);

        //Options
        optionsLayout = activity.findViewById(R.id.galleryOptionsLayout);
        optionsSettings = activity.findViewById(R.id.galleryOptionsSettings);
        optionsBackup = activity.findViewById(R.id.galleryOptionsBackup);
        optionsRestore = activity.findViewById(R.id.galleryOptionsRestore);
        optionsTrash = activity.findViewById(R.id.galleryOptionsTrash);
        optionsDelete = activity.findViewById(R.id.galleryOptionsDelete);
        optionsShare = activity.findViewById(R.id.galleryOptionsShare);

        //Gallery
        refreshLayout = activity.findViewById(R.id.galleryRefreshLayout);
        list = activity.findViewById(R.id.gallery);

        //Search
        searchClosedLayout = activity.findViewById(R.id.searchLayoutClosed);
        searchOpenButton = activity.findViewById(R.id.searchButtonOpen);

        searchOpenLayout = activity.findViewById(R.id.searchLayoutOpen);
        searchCloseButton = activity.findViewById(R.id.searchButtonClose);
        searchInput = activity.findViewById(R.id.searchInput);
    }

    public void addListeners() {
        //Options
        optionsButton.setOnClickListener(view -> showOptions(true));

        optionsButton.setOnLongClickListener(v -> {
            //Reload gallery
            activity.recreate();
            return true;
        });

        optionsLayout.setOnClickListener(view -> showOptions(false));

        optionsSettings.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Close search
            if (!inHome) showSearchLayout(false);

            //Open settings
            activity.startActivity(new Intent(activity, SettingsActivity.class));
        });

        optionsBackup.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Open backup
            activity.startActivity(new Intent(activity, BackupActivity.class));
        });

        optionsRestore.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Restore files from trash
            activity.restoreFiles(getSelectedFiles());
        });

        optionsTrash.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Move files to trash
            activity.trashFiles(getSelectedFiles());
        });

        optionsDelete.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Delete files
            activity.deleteFiles(getSelectedFiles());
        });

        optionsShare.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Share
            activity.shareFiles(getSelectedFiles());
        });

        //Gallery
        refreshLayout.setOnRefreshListener(() -> {
            //Refresh gallery
            refresh();

            //Stop refreshing
            refreshLayout.setRefreshing(false);
        });

        //Search
        searchOpenButton.setOnClickListener(view -> showSearchLayout(true));

        searchCloseButton.setOnClickListener(view -> showSearchLayout(false));

        searchInput.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                //Get search text
                String search = searchInput.getText().toString();

                //Filter gallery with search
                filterGallery(search, true);
            }
            return false;
        });
    }

    //Search
    private void showSearchOpenButton(boolean show) {
        if (show)
            Orion.showAnim(searchClosedLayout);
        else
            Orion.hideAnim(searchClosedLayout);
    }

    public void showSearchLayout(boolean show) {
        if (show) {
            //Loading or searching
            if (isSearching) return;

            //Open search layout
            searchOpenLayout.setVisibility(View.VISIBLE);
            searchClosedLayout.setVisibility(View.GONE);

            //Focus text & show keyboard
            searchInput.requestFocus();
            searchInput.selectAll();
            Orion.showKeyboard(activity);

            //Back button
            activity.backManager.register("searchMenu", () -> showSearchLayout(false));
        } else {
            //Close keyboard
            Orion.hideKeyboard(activity);
            Orion.clearFocus(activity);

            //Hide menu
            searchOpenLayout.setVisibility(View.GONE);
            searchClosedLayout.setVisibility(View.VISIBLE);

            //Back button
            activity.backManager.unregister("searchMenu");
        }
    }

    //Options
    private TurboFile[] getSelectedFiles() {
        ArrayList<TurboFile> selectedFiles = new ArrayList<>(selected.size());
        for (int index: selected) selectedFiles.add(files.get(index));
        return selectedFiles.toArray(new TurboFile[0]);
    }

    private void showOptions(boolean show) {
        if (show) {
            //Toggle buttons
            boolean isSelecting = !selected.isEmpty();
            boolean inTrash = album == Library.trash;
            optionsRestore.setVisibility(isSelecting && inTrash ? View.VISIBLE : View.GONE);
            optionsTrash.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);
            optionsDelete.setVisibility(isSelecting ? View.VISIBLE : View.GONE);
            optionsShare.setVisibility(isSelecting && !inTrash ? View.VISIBLE : View.GONE);

            //Show
            Orion.showAnim(optionsLayout);
            activity.backManager.register("galleryOptions", () -> showOptions(false));
        } else {
            Orion.hideAnim(optionsLayout);
            activity.backManager.unregister("galleryOptions");
        }
    }

    //Gallery
    private int getHorizontalItemCount() {
        boolean isHorizontal = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        float ratio = ((float) metrics.widthPixels / (float) metrics.heightPixels);

        //Get size for portrait
        int size = Storage.getInt(inHome ? "Settings.galleryAlbumsPerRow" : "Settings.galleryImagesPerRow", inHome ? 2 : 3);

        //Return size for current orientation
        return isHorizontal ? (int) (size * ratio) : size;
    }

    private void loadMetadata(Album album) {
        //Start loading
        hasLoadedMetadata = false;

        //Load metadata
        new Thread(() -> {
            //Load metadata
            Library.loadMetadata(activity.loadingIndicator, album);

            //Update gallery
            activity.runOnUiThread(() -> {
                //Update album list
                albumAdapter.notifyDataSetChanged();

                //Finish loading
                activity.loadingIndicator.hide();
                hasLoadedMetadata = true;
            });
        }).start();
    }

    private void toggleSelected(int index) {
        //Check if item is selected
        if (selected.contains(index)) {
            //Remove item
            selected.remove(index);

            //No more selected items -> Remove back event
            if (selected.isEmpty()) activity.backManager.unregister("selected");
        } else {
            //First item to be selected -> Add back event
            if (selected.isEmpty()) activity.backManager.register("selected", this::unselectAll);

            //Add item
            selected.add(index);
        }
        albumAdapter.notifyItemChanged(index);

        //Update gallery title
        title.setText(album.getName() + (selected.isEmpty() ? "" : " (" + selected.size() + " selected)"));
    }

    public void unselectAll() {
        //Remove back event
        activity.backManager.unregister("selected");

        //Unselect all
        if (!selected.isEmpty()) {
            HashSet<Integer> tmp = new HashSet<>(selected);
            selected.clear();
            for (Integer index: tmp) albumAdapter.notifyItemChanged(index);
        }

        //Update gallery title
        title.setText(album.getName());
    }

    public void initAdapters() {
        //Create gallery list viewer
        layoutManager = new GridLayoutManager(activity, getHorizontalItemCount());
        list.setLayoutManager(layoutManager);

        //Init home adapter
        homeAdapter = new GalleryHomeAdapter(activity, Library.albums);
        homeAdapter.setOnClickListener((view, album) -> {
            selectAlbum(album);
            showAlbumsList(false);
        });
        list.setAdapter(homeAdapter);

        //Init album adapter
        albumAdapter = new GalleryAlbumAdapter(activity, files, selected, Storage.getBool("Settings.showMissingMetadataIcon", false));
        albumAdapter.setOnClickListener((view, index) -> {
            //Loading metadata -> Return
            if (!hasLoadedMetadata) return;

            //Check if selecting
            if (!selected.isEmpty()) {
                //Selecting -> Toggle selected
                toggleSelected(index);
            } else {
                //Not selecting -> Open image
                activity.display.open(index);
            }
        });
        albumAdapter.setOnLongClickListener((view, index) -> {
            //Toggle item selected
            toggleSelected(index);

            //Consume click
            return true;
        });
    }

    public void refresh() {
        //Check folders for updates (could have new or deleted files)
        boolean albumsWereModified = false;
        for (Album album : Library.albums) {
            //Check if last modified date changed
            File imagesFolder = album.getImagesFolder();
            if (imagesFolder == null || imagesFolder.lastModified() == album.getLastModified()) continue;

            //An album was modified -> Reload activity
            albumsWereModified = true;
            break;
        }

        //Reload albums (reset if albums were modified)
        boolean albumsWereUpdated = Library.loadLibrary(activity, albumsWereModified);

        //Refresh lists
        if (albumsWereUpdated) {
            //Update albums list
            homeAdapter.notifyDataSetChanged();

            //Update gallery list
            if (!inHome) selectAlbum(album);

            //Close display menu
            activity.display.close();
        }
    }

    public void showList(boolean show) {
        list.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void updateHorizontalItemCount() {
        int newHorizontalItemCount = getHorizontalItemCount();
        if (layoutManager.getSpanCount() != newHorizontalItemCount) layoutManager.setSpanCount(newHorizontalItemCount);
    }

    //Toggle home & album list
    public void showAlbumsList(boolean show) {
        inHome = show;
        Orion.hideAnim(list, 150, () -> {
            //Change gallery
            if (inHome) {
                //Show home
                showSearchOpenButton(false);
                layoutManager.setSpanCount(getHorizontalItemCount());
                list.setAdapter(homeAdapter);
                activity.backManager.unregister("albums");

                //Scroll to saved scroll
                if (listScroll != null) layoutManager.onRestoreInstanceState(listScroll);

                //Reset gallery title
                title.setText("Coon Gallery");

                //Check albums for updates
                refresh();
            } else {
                //Save scroll
                listScroll = layoutManager.onSaveInstanceState();

                //Show album
                showSearchOpenButton(true);
                layoutManager.setSpanCount(getHorizontalItemCount());
                list.setAdapter(albumAdapter);
                activity.backManager.register("albums", () -> showAlbumsList(true));

                //Scroll to top
                list.scrollToPosition(0);
            }

            //Show list
            Orion.showAnim(list, 150);
        });
    }

    public void selectAlbum(Album album) {
        //Select album
        this.album = album;

        //Change gallery title
        title.setText(album.getName());

        //Load album
        filesUnfiltered = album.files;
        loadMetadata(album);
        filterGallery();
    }

    //Filter files
    private boolean filterImage(TurboFile image, String filter) {
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

    public void filterGallery(String filterText, boolean scrollToTop) {
        //Ignore case
        String filter = filterText.toLowerCase();
        boolean isFiltering = !filter.isEmpty();

        //Loading or searching
        if (isSearching || (!hasLoadedMetadata && isFiltering)) return;

        //Update subtitle
        subtitle.setText(isFiltering ? "Search: " + filterText : "");
        subtitle.setVisibility(isFiltering ? View.VISIBLE : View.GONE);

        //Start search
        isSearching = true;
        searchInput.setText(filterText);
        if (isFiltering) activity.loadingIndicator.search();
        showSearchLayout(false);

        //Clear files list
        files.clear();
        selected.clear();

        //Back button
        if (isFiltering)
            activity.backManager.register("search", this::filterGallery);
        else
            activity.backManager.unregister("search");

        //Search in metadata files
        new Thread(() -> {
            //Look for files that contain filter
            for (TurboFile image: filesUnfiltered) {
                //No filter -> Skip check
                if (!isFiltering) {
                    files.add(image);
                    continue;
                }

                //Check if json contains filter
                if (filterImage(image, filter)) files.add(image);
            }

            //Show gallery
            activity.runOnUiThread(() -> {
                //Show gallery
                albumAdapter.notifyDataSetChanged();
                list.stopScroll();
                if (scrollToTop) list.scrollToPosition(0);

                //Finish searching
                if (isFiltering) activity.loadingIndicator.hide();
                isSearching = false;
            });
        }).start();
    }

    public void filterGallery() { filterGallery("", false); }

}
