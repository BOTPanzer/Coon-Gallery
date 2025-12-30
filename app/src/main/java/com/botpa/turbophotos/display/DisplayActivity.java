package com.botpa.turbophotos.display;

import android.content.Intent;
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
import com.botpa.turbophotos.util.Action;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.util.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.TurboItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

public class DisplayActivity extends AppCompatActivity {

    //Activity
    private BackManager backManager;

    //Events
    private final Library.ActionEvent onAction = this::manageAction;

    //List adapter
    private DisplayLayoutManager layoutManager;
    private DisplayAdapter adapter;

    //List items
    private final ArrayList<TurboItem> displayItems = new ArrayList<>();
    private int currentIndexInGallery = -1;
    private int currentIndexInDisplay = -1;
    private TurboItem currentItem = null;

    //Views (list)
    private RecyclerView list;

    //Views (overlay)
    private View overlayLayout;
    private TextView overlayName;
    private View overlayClose;
    private View overlayInfo;
    private View overlayOptions;

    //Views (info)
    private View infoLayout;
    private TextView infoName;
    private TextView infoCaption;
    private HorizontalScrollView infoLabelsScroll;
    private TextView infoLabels;
    private HorizontalScrollView infoTextScroll;
    private TextView infoText;
    private View infoEdit;

    //Views (edit)
    private View editLayout;
    private TextView editCaption;
    private TextView editLabels;
    private View editSave, editSpace;

    //Views (options)
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

        //Init back manager
        backManager = new BackManager(DisplayActivity.this, getOnBackPressedDispatcher());

        //Add events
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
        Library.removeOnActionEvent(onAction);
    }

    private void initActivity() {
        //Check if intent is valid
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        //Check if intent has item index
        int index = intent.getIntExtra("index", -1);
        if (index < 0) {
            finish();
            return;
        }
        selectItem(index);
    }

    //Views
    private void loadViews() {
        //Views (list)
        list = findViewById(R.id.list);

        //Views (overlay)
        overlayLayout = findViewById(R.id.overlayLayout);
        overlayName = findViewById(R.id.overlayName);
        overlayClose = findViewById(R.id.overlayClose);
        overlayInfo = findViewById(R.id.overlayInfo);
        overlayOptions = findViewById(R.id.overlayOptions);

        //Views (info)
        infoLayout = findViewById(R.id.infoLayout);
        infoName = findViewById(R.id.infoName);
        infoCaption = findViewById(R.id.infoCaption);
        infoLabelsScroll = findViewById(R.id.infoLabelsScroll);
        infoLabels = findViewById(R.id.infoLabels);
        infoTextScroll = findViewById(R.id.infoTextScroll);
        infoText = findViewById(R.id.infoText);
        infoEdit = findViewById(R.id.infoEdit);

        //Views (edit)
        editLayout = findViewById(R.id.editLayout);
        editCaption = findViewById(R.id.editCaption);
        editLabels = findViewById(R.id.editLabels);
        editSave = findViewById(R.id.editSave);
        editSpace = findViewById(R.id.editSpace);

        //Views (options)
        optionsLayout = findViewById(R.id.optionsLayout);
        optionsRestore = findViewById(R.id.optionsRestore);
        optionsDelete = findViewById(R.id.optionsDelete);
        optionsTrash = findViewById(R.id.optionsTrash);
        optionsShare = findViewById(R.id.optionsShare);
        optionsEdit = findViewById(R.id.optionsEdit);
        optionsOpenOutside = findViewById(R.id.optionsOpen);

        //Insets (overlay)
        Orion.addInsetsChangedListener(
                findViewById(R.id.overlayIndent),
                new int[] {
                        WindowInsetsCompat.Type.systemBars()
                },
                (view, insets, percent) -> {
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

        //Insets (options layout)
        Orion.addInsetsChangedListener(optionsLayout, new int[] { WindowInsetsCompat.Type.systemBars() });
    }

    private void addListeners() {
        //Main
        overlayClose.setOnClickListener(view -> finish());

        //Info & edit
        overlayInfo.setOnClickListener(view -> toggleInfo(true));

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
            String caption = editCaption.getText().toString();
            String labels = editLabels.getText().toString();
            String[] labelsArray = labels.split(",");
            for (int i = 0; i < labelsArray.length; i++) labelsArray[i] = labelsArray[i].trim();

            //Update info texts with new ones
            infoCaption.setText(caption);
            infoLabels.setText(labels);

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
        overlayOptions.setOnClickListener(view -> toggleOptions(true));

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

    //Events
    private void manageAction(Action action) {
        //No action
        if (action.isOfType(Action.TYPE_NONE)) return;

        //Failed actions
        if (!action.failed.isEmpty()) {
            if (action.failed.size() == 1) {
                //Only 1 failed -> Show error
                Orion.snack(DisplayActivity.this, action.failed.entrySet().iterator().next().getValue());
            } else if (!action.allFailed()) {
                //More than 1 failed -> Show general error
                Orion.snack(DisplayActivity.this, "Failed to perform " + action.failed.size() + " actions");
            } else {
                //All failed -> Show general error
                Orion.snack(DisplayActivity.this, "Failed to perform all actions");
                return;
            }
        }

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
        //Create layout manager
        layoutManager = new DisplayLayoutManager(DisplayActivity.this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        list.setLayoutManager(layoutManager);

        //Create adapter
        adapter = new DisplayAdapter(DisplayActivity.this, displayItems);
        list.setAdapter(adapter);

        //Add adapter listeners
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

        //Create snap helper
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(list);

        //Add snap helper listener
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
        overlayName.setText(currentItem.name);

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
        infoName.setText(currentItem.name);
        infoCaption.setText(caption);
        infoLabels.setText(labels);
        infoText.setText(text);
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
            editCaption.setText(infoCaption.getText());
            editLabels.setText(infoLabels.getText());
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
