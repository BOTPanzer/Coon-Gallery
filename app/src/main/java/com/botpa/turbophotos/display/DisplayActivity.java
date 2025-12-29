package com.botpa.turbophotos.display;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.main.DisplayLayoutManager;
import com.botpa.turbophotos.util.Action;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.util.TurboItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

public class DisplayActivity extends AppCompatActivity {

    //Activity
    private BackManager backManager;

    //Actions
    private final Library.ActionEvent onAction = this::manageAction;

    //Display
    private DisplayLayoutManager layoutManager;
    private DisplayAdapter adapter;

    public final ArrayList<TurboItem> displayItems = new ArrayList<>();
    public int currentIndexInGallery = -1;
    public int currentIndexInDisplay = -1;
    public TurboItem currentItem = null;

    //Views
    private TextView nameText;
    private View closeButton;
    private View infoButton;
    private View optionsButton;
    private RecyclerView list;
    private View overlayLayout;

    private View infoLayout;
    private TextView infoNameText;
    private TextView infoCaptionText;
    private HorizontalScrollView infoLabelsScroll;
    private TextView infoLabelsText;
    private HorizontalScrollView infoTextScroll;
    private TextView infoTextText;
    private View infoEdit;

    private View editLayout;
    private TextView editCaptionText;
    private TextView editLabelsText;
    private View editSave, editSpace;

    private View optionsLayout;
    private View optionsRestore;
    private View optionsDelete;
    private View optionsTrash;
    private View optionsShare;
    private View optionsEdit;
    private View optionsOpenOutside;


    //Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.display);

        //Load storage
        Storage.load(DisplayActivity.this);

        //Add on action listener
        Library.addOnActionEvent(onAction);

        //Load views & add listeners
        loadViews();
        addListeners();

        //Init activity
        initActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Remove on action listener
        Library.removeOnActionEvent(onAction);
    }

    private void initActivity() {
        //Init back manager
        backManager = new BackManager(DisplayActivity.this, getOnBackPressedDispatcher());

        //Enable HDR
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);

        //Init adapters
        initAdapters();

        //Check intent
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        int index = intent.getIntExtra("index", -1);
        if (index < 0) {
            finish();
            return;
        }
        selectItem(index);
    }

    //Views
    private void loadViews() {
        //Find views
        nameText = findViewById(R.id.displayNameText);
        closeButton = findViewById(R.id.displayCloseButton);
        infoButton = findViewById(R.id.displayInfoButton);
        infoEdit = findViewById(R.id.displayInfoEdit);
        optionsButton = findViewById(R.id.displayOptionsButton);
        list = findViewById(R.id.displayList);

        overlayLayout = findViewById(R.id.displayOverlayLayout);

        infoLayout = findViewById(R.id.displayInfoLayout);
        infoNameText = findViewById(R.id.displayInfoNameText);
        infoCaptionText = findViewById(R.id.displayInfoCaptionText);
        infoLabelsScroll = findViewById(R.id.displayInfoLabelsScroll);
        infoLabelsText = findViewById(R.id.displayInfoLabelsText);
        infoTextScroll = findViewById(R.id.displayInfoTextScroll);
        infoTextText = findViewById(R.id.displayInfoTextText);

        editLayout = findViewById(R.id.displayEditLayout);
        editCaptionText = findViewById(R.id.displayEditCaptionText);
        editLabelsText = findViewById(R.id.displayEditLabelsText);
        editSave = findViewById(R.id.displayEditSave);
        editSpace = findViewById(R.id.displayEditSpace);

        //Options
        optionsLayout = findViewById(R.id.displayOptionsLayout);
        optionsRestore = findViewById(R.id.displayOptionsRestore);
        optionsDelete = findViewById(R.id.displayOptionsDelete);
        optionsTrash = findViewById(R.id.displayOptionsTrash);
        optionsShare = findViewById(R.id.displayOptionsShare);
        optionsEdit = findViewById(R.id.displayOptionsEdit);
        optionsOpenOutside = findViewById(R.id.displayOptionsOpen);

        //Insets
        Orion.addInsetsChangedListener(
                findViewById(R.id.displayOverlayIndent),
                new int[]{ WindowInsetsCompat.Type.systemBars() },
                0,
                (view, insets, duration) -> {
                    //Ignore if no margins
                    if (insets.top <= 0 && insets.bottom <= 0) return;

                    //Update margins
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                    params.setMargins(insets.left, insets.top, insets.right, insets.bottom);
                    view.setLayoutParams(params);
                }
        );

        //Insets (edit info layout)
        int defaultEditSpaceHeight = editSpace.getMinimumHeight(); //getHeight() returns 0 when view is not rendered, so store height on minHeight too :D
        ViewCompat.setOnApplyWindowInsetsListener(editSpace, (view, windowInsets) -> {
            //Get new bottom space height
            Insets insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets insetsKeyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            boolean keyboardOpen = insetsKeyboard.bottom != 0;
            int height = keyboardOpen ? insetsKeyboard.bottom : insetsSystemBars.bottom + defaultEditSpaceHeight;

            //Update bottom space height
            if (view.getHeight() == 0) {
                //Not rendered yet -> Don't animate
                view.getLayoutParams().height = height;
                view.requestLayout();
            } else {
                //Has height -> Animate
                Orion.ResizeHeightAnimation resize = new Orion.ResizeHeightAnimation(editSpace, height);
                resize.setDuration(100L);
                view.startAnimation(resize);
            }

            //Done
            return windowInsets;
        });
    }

    private void addListeners() {
        //Main
        closeButton.setOnClickListener(view -> finish());

        //Info & edit
        infoButton.setOnClickListener(view -> toggleInfo(true));

        infoLayout.setOnClickListener(view -> toggleInfo(false));

        infoEdit.setOnClickListener(view -> {
            //No metadata file
            if (!currentItem.album.hasMetadata()) {
                Toast.makeText(DisplayActivity.this, "This item's album does not have a metadata file linked to it", Toast.LENGTH_SHORT).show();
                return;
            }

            //No metadata
            if (!currentItem.hasMetadata()) {
                Toast.makeText(DisplayActivity.this, "This item does not have a key in its album metadata", Toast.LENGTH_SHORT).show();
                return;
            }

            //Hide display info
            toggleInfo(false);

            //Toggle display edit
            toggleEdit(editLayout.getVisibility() != View.VISIBLE);
        });

        editLayout.setOnClickListener(view -> toggleEdit(false));

        editSave.setOnClickListener(view -> {
            //Get new caption & labels
            String caption = editCaptionText.getText().toString();
            String labels = editLabelsText.getText().toString();
            String[] labelsArray = labels.split(",");
            for (int i = 0; i < labelsArray.length; i++) labelsArray[i] = labelsArray[i].trim();

            //Update info texts with new ones
            infoCaptionText.setText(caption);
            infoLabelsText.setText(labels);

            //Update metadata
            String key = currentItem.name;
            ObjectNode metadata = currentItem.album.getMetadataKey(key);
            if (metadata == null) {
                metadata = Orion.getEmptyJson();
                currentItem.album.metadata.set(key, metadata);
            }
            metadata.put("caption", caption);
            metadata.set("labels", Orion.arrayToJson(labelsArray));

            //Save
            boolean saved = currentItem.album.saveMetadata();
            Toast.makeText(DisplayActivity.this, saved ? "Saved successfully" : "An error occurred while saving", Toast.LENGTH_SHORT).show();

            //Close menu
            toggleEdit(false);
        });

        //Options
        optionsButton.setOnClickListener(view -> toggleOptions(true));

        optionsLayout.setOnClickListener(view -> toggleOptions(false));

        optionsRestore.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Restore from trash
            Library.restoreItems(DisplayActivity.this, new TurboItem[] { currentItem });
        });

        optionsDelete.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Delete items
            Library.deleteItems(DisplayActivity.this, new TurboItem[] { currentItem });
        });

        optionsTrash.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Move to trash
            Library.trashItems(DisplayActivity.this, new TurboItem[] { currentItem });
        });

        optionsShare.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Share
            Library.shareItems(DisplayActivity.this, new TurboItem[]{ currentItem });
        });

        optionsEdit.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Get mime type and URI
            String mimeType = currentItem.mimeType;
            Uri uri = Orion.getMediaStoreUriFromFile(DisplayActivity.this, currentItem.file, mimeType);

            //Edit
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setDataAndType(uri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, null));
        });

        optionsOpenOutside.setOnClickListener(view -> {
            //Close options menu
            toggleOptions(false);

            //Show open with menu
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(currentItem.file.getAbsolutePath()), currentItem.mimeType);
            startActivity(intent);
        });
    }

    //Actions
    private void manageAction(Action action) {
        //Check if gallery is empty
        if (Library.gallery.isEmpty()) {
            //Is empty -> Close display
            finish();
            return;
        }

        //Update selected item
        int originalCurrentIndex = currentIndexInGallery;
        for (int indexInGallery : action.removedIndexesInGallery) {
            //Check if current item index changed (an item before it was removed)
            if (indexInGallery < originalCurrentIndex) currentIndexInGallery--;
        }
        selectItem(currentIndexInGallery);
    }

    //Display
    private void initAdapters() {
        //Create display list viewer
        layoutManager = new DisplayLayoutManager(DisplayActivity.this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        list.setLayoutManager(layoutManager);

        //Create display snap helper
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(list);

        //Create display adapter
        adapter = new DisplayAdapter(DisplayActivity.this, displayItems);
        adapter.setOnClickListener((view, index) -> {
            if (overlayLayout.getVisibility() == View.VISIBLE) {
                Orion.hideAnim(overlayLayout);
                toggleSystemUI(false);
            } else {
                Orion.showAnim(overlayLayout);
                toggleSystemUI(true);
            }
        });
        adapter.setOnZoomChangedListener((view, index) -> {
            //Enable scrolling only if not zoomed and one finger is over
            layoutManager.setScrollEnabled(view.getZoom() <= 1 && view.getPointers() <= 1);
        });
        adapter.setOnPointersChangedListener((view, index) -> {
            //Enable scrolling only if not zoomed and one finger is over
            layoutManager.setScrollEnabled(view.getZoom() <= 1 && view.getPointers() <= 1);
        });
        adapter.setOnPlayListener((view, index) -> {
            //Play video outside
            optionsOpenOutside.performClick();
        });
        list.setAdapter(adapter);

        //Add snap listener
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && layoutManager.canScrollHorizontally()) {
                    //Get position
                    View view = snapHelper.findSnapView(layoutManager);
                    int position = (view != null) ? layoutManager.getPosition(view) : -1;
                    if (position == -1) return;

                    //Check what to do
                    if (position < currentIndexInDisplay) {
                        //Previous
                        layoutManager.setScrollEnabled(false);
                        selectItem(currentIndexInGallery - 1);
                    } else if (position > currentIndexInDisplay) {
                        //Next
                        layoutManager.setScrollEnabled(false);
                        selectItem(currentIndexInGallery + 1);
                    }
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    private void selectItem(int index) {
        //Fix index overflow
        index = Math.clamp(index, 0, Library.gallery.size() - 1);

        //Reset display items
        displayItems.clear();
        currentIndexInGallery = index;
        currentIndexInDisplay = 0;

        //Add items to display list
        if (index > 0) {
            //Has item before -> Add it
            displayItems.add(Library.gallery.get(index - 1));
            currentIndexInDisplay++;
        }
        displayItems.add(Library.gallery.get(index));
        if (index < Library.gallery.size() - 1) {
            //Has item after -> Add it
            displayItems.add(Library.gallery.get(index + 1));
        }

        //Get current image, update adapter & select it
        currentItem = displayItems.get(currentIndexInDisplay);
        adapter.notifyDataSetChanged();
        list.scrollToPosition(currentIndexInDisplay);
        layoutManager.setScrollEnabled(true);

        //Change image name
        nameText.setText(currentItem.name);

        //Prepare options menu
        boolean isTrashed = currentItem.isTrashed();
        optionsRestore.setVisibility(isTrashed ? View.VISIBLE : View.GONE);
        optionsShare.setVisibility(isTrashed ? View.GONE : View.VISIBLE);
        optionsTrash.setVisibility(isTrashed ? View.GONE : View.VISIBLE);
        optionsEdit.setVisibility(isTrashed ? View.GONE : View.VISIBLE);
        optionsOpenOutside.setVisibility(isTrashed ? View.GONE : View.VISIBLE);

        //Load image info (caption & labels)
        String caption = "";
        String labels = "";
        String text = "";
        try {
            JsonNode metadata = currentItem.album.getMetadataKey(currentItem.name);
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
        infoNameText.setText(currentItem.name);
        infoCaptionText.setText(caption);
        infoLabelsText.setText(labels);
        infoTextText.setText(text);
    }

    //Menus
    private void toggleInfo(boolean show) {
        if (show) {
            infoLabelsScroll.scrollTo(0, 0);
            infoTextScroll.scrollTo(0, 0);
            Orion.showAnim(infoLayout);
            backManager.register("displayInfo", () -> toggleInfo(false));
        } else {
            Orion.hideAnim(infoLayout);
            backManager.unregister("displayInfo");
        }
    }

    private void toggleEdit(boolean show) {
        if (show) {
            editCaptionText.setText(infoCaptionText.getText());
            editLabelsText.setText(infoLabelsText.getText());
            Orion.showAnim(editLayout);
            backManager.register("displayEdit", () -> toggleEdit(false));
        } else {
            Orion.hideKeyboard(DisplayActivity.this);
            Orion.clearFocus(DisplayActivity.this);
            Orion.hideAnim(editLayout);
            backManager.unregister("displayEdit");
        }
    }

    private void toggleOptions(boolean show) {
        if (show) {
            Orion.showAnim(optionsLayout);
            backManager.register("displayOptions", () -> toggleOptions(false));
        } else {
            Orion.hideAnim(optionsLayout);
            backManager.unregister("displayOptions");
        }
    }

    //Util
    private void toggleSystemUI(boolean show) {
        //Get controller
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), overlayLayout);

        //Toggle system UI
        if (show) {
            controller.show(WindowInsetsCompat.Type.systemBars());
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars());
        }
    }

}
