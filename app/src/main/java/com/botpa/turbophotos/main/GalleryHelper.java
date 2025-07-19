package com.botpa.turbophotos.main;

import android.annotation.SuppressLint;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Album;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;

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

    //Adapters
    private Parcelable listScroll;

    public GalleryHomeAdapter homeAdapter;
    public GalleryAlbumAdapter albumAdapter;
    public GridLayoutManager layoutManager;

    //Views
    private TextView title;
    private TextView subtitle;

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
        //Gallery
        title = activity.findViewById(R.id.galleryTitle);
        subtitle = activity.findViewById(R.id.gallerySubtitle);
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

    //Gallery
    private void loadMetadata(Album album) {
        //Start loading
        hasLoadedMetadata = false;

        //Load metadata
        new Thread(() -> {
            //Load metadata
            if (album == null) {
                Library.loadMetadata(activity.loadingIndicator);
            } else {
                Library.loadMetadata(activity.loadingIndicator, album);
            }

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

    public void initAdapters() {
        //Create gallery list viewer
        layoutManager = new GridLayoutManager(activity, Storage.getInt("Settings.galleryAlbumsPerRow", 3));
        list.setLayoutManager(layoutManager);

        //Init home adapter
        homeAdapter = new GalleryHomeAdapter(activity, Library.albums);
        homeAdapter.setOnItemClickListener((view, album) -> {
            selectAlbum(album);
            showAlbumsList(false);
        });
        list.setAdapter(homeAdapter);

        //Init album adapter
        albumAdapter = new GalleryAlbumAdapter(activity, files, Storage.getBool("Settings.showMissingMetadataIcon", false));
        albumAdapter.setOnItemClickListener((view, index) -> {
            if (!hasLoadedMetadata) return;
            activity.display.open(index);
        });
    }

    public void refresh() {
        //Check folders for updates (could have new or deleted files)
        boolean albumsUpdated = false;
        for (Album album : Library.albums) {
            if (album.getImagesFolder().lastModified() == album.getLastModified()) continue;

            //An album was modified -> Reload activity
            albumsUpdated = true;
            break;
        }

        //Reload albums (reset if albums were updated)
        albumsUpdated = Library.loadLibrary(activity, albumsUpdated);

        //Refresh lists
        if (albumsUpdated) {
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

    //Toggle home & album list
    public void showAlbumsList(boolean show) {
        inHome = show;
        Orion.hideAnim(list, 150, () -> {
            //Change gallery
            if (inHome) {
                //Show home
                showSearchOpenButton(false);
                layoutManager.setSpanCount(Storage.getInt("Settings.galleryAlbumsPerRow", 2));
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
                layoutManager.setSpanCount(Storage.getInt("Settings.galleryImagesPerRow", 3));
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
        //Save album
        this.album = album;

        //Select album
        if (album == null) {
            //Load
            filesUnfiltered = Library.allFiles;
            loadMetadata(null);

            //Change gallery title
            title.setText("All");
        } else {
            //Load
            filesUnfiltered = album.files;
            loadMetadata(album);

            //Change gallery title
            title.setText(album.getName());
        }

        //Reset search
        searchInput.setText("");
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

    public void filterGallery() { filterGallery("", false); }

    public void filterGallery(String filterText, boolean scrollToTop) {
        //Ignore case
        String filter = filterText.toLowerCase();
        boolean isFiltering = !filter.isEmpty();

        //Loading or searching
        if (isSearching || (!hasLoadedMetadata && isFiltering)) return;

        //Update subtitle
        subtitle.setText(isFiltering ? "Search: " + filter : "");
        subtitle.setVisibility(isFiltering ? View.VISIBLE : View.GONE);

        //Start search
        isSearching = true;
        if (isFiltering) activity.loadingIndicator.search();
        showSearchLayout(false);

        //Clear files list
        files.clear();

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

}
