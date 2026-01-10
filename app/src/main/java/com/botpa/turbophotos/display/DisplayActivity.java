package com.botpa.turbophotos.display;

import android.annotation.SuppressLint;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.gallery.actions.Action;
import com.botpa.turbophotos.gallery.GalleryActivity;
import com.botpa.turbophotos.gallery.options.OptionsAdapter;
import com.botpa.turbophotos.gallery.options.OptionsItem;
import com.botpa.turbophotos.util.BackManager;
import com.botpa.turbophotos.gallery.Library;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.Storage;
import com.botpa.turbophotos.gallery.CoonItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class DisplayActivity extends GalleryActivity {

    //Activity
    private BackManager backManager;

    //Events
    private final Library.ActionEvent onAction = this::manageAction;

    //List
    private DisplayLayoutManager layoutManager;
    private DisplayAdapter adapter;

    //List items
    private final ArrayList<CoonItem> displayItems = new ArrayList<>();
    private int currentIndexInGallery = -1;
    private int currentIndexInDisplay = -1;
    private CoonItem currentItem = null;

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
    private static final String OPTIONS_DELETE = "delete";

    private OptionsAdapter optionsAdapter;

    //Views (list)
    private RecyclerView list;

    //Views (overlay)
    private View overlayLayout;
    private TextView overlayName;
    private View overlayInfo;
    private View overlayOptions;

    //Views (info)
    private View infoLayout;
    private TextView infoName;
    private TextView infoDate;
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
    private RecyclerView optionsList;


    //Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.display_screen);

        //Enable HDR
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR);

        //Load storage
        Storage.load(DisplayActivity.this);

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
        overlayInfo = findViewById(R.id.overlayInfo);
        overlayOptions = findViewById(R.id.overlayOptions);

        //Views (info)
        infoLayout = findViewById(R.id.infoLayout);
        infoName = findViewById(R.id.infoName);
        infoDate = findViewById(R.id.infoDate);
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
        optionsList = findViewById(R.id.optionsList);


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
        //Overlay
        overlayInfo.setOnClickListener(view -> toggleInfo(true));

        overlayOptions.setOnClickListener(view -> toggleOptions(true));

        //Info
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

        //Edit
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
        optionsLayout.setOnClickListener(view -> toggleOptions(false));

        options.put(OPTIONS_SEPARATOR, new OptionsItem());

        options.put(OPTIONS_EDIT, new OptionsItem(R.drawable.edit, "Edit", () -> {
            //Edit
            Library.editItem(DisplayActivity.this, currentItem);
            return null;
        }));

        options.put(OPTIONS_SHARE, new OptionsItem(R.drawable.share, "Share", () -> {
            //Share
            Library.shareItems(DisplayActivity.this, new CoonItem[] { currentItem });
            return null;
        }));

        options.put(OPTIONS_MOVE, new OptionsItem(R.drawable.move, "Move to album", () -> {
            //Move items
            Library.moveItems(DisplayActivity.this, new CoonItem[]{ currentItem });
            return null;
        }));

        options.put(OPTIONS_COPY, new OptionsItem(R.drawable.copy, "Copy to album", () -> {
            //Copy items
            Library.copyItems(DisplayActivity.this, new CoonItem[]{ currentItem });
            return null;
        }));

        options.put(OPTIONS_TRASH, new OptionsItem(R.drawable.delete, "Move to trash", () -> {
            //Move to trash
            trashItems(new CoonItem[] { currentItem });
            return null;
        }));

        options.put(OPTIONS_RESTORE, new OptionsItem(R.drawable.restore, "Restore", () -> {
            //Restore from trash
            restoreItems(new CoonItem[] { currentItem });
            return null;
        }));

        options.put(OPTIONS_DELETE, new OptionsItem(R.drawable.delete, "Delete", () -> {
            //Delete item
            Library.deleteItems(DisplayActivity.this, new CoonItem[] { currentItem });
            return null;
        }));
    }

    //Events
    private void manageAction(Action action) {
        //No action
        if (action.isOfType(Action.TYPE_NONE)) return;

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
        //Init display layout manager
        layoutManager = new DisplayLayoutManager(DisplayActivity.this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        list.setLayoutManager(layoutManager);

        //Init display adapter
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
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(currentItem.file.getAbsolutePath()), currentItem.mimeType);
            startActivity(intent);
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

        //Init options layout manager
        optionsList.setLayoutManager(new LinearLayoutManager(DisplayActivity.this));

        //Init options adapter
        optionsAdapter = new OptionsAdapter(DisplayActivity.this, optionsItems);
        optionsAdapter.setOnClickListener((view, index) -> {
            //Get option
            OptionsItem option = optionsItems.get(index);
            if (option == null) return;

            //Get action
            Function0<Unit> action = option.getAction();
            if (action == null) return;

            //Invoke action
            action.invoke();
            toggleOptions(false);
        });
        optionsList.setAdapter(optionsAdapter);
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

        //Create date text
        Date date = new Date(currentItem.lastModified * 1000);
        SimpleDateFormat formatter1 = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
        SimpleDateFormat formatter2 = new SimpleDateFormat("hh:mm.ss a", Locale.ENGLISH);

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

        //Update text
        infoName.setText(currentItem.name);
        infoDate.setText(formatter1.format(date) + ", " + formatter2.format(date));
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
            //Get state info
            boolean isTrashed = currentItem.isTrashed;

            //Update options list
            optionsItems.clear();
            if (!isTrashed)
                optionsItems.add(options.get(OPTIONS_EDIT));
            if (!isTrashed)
                optionsItems.add(options.get(OPTIONS_SHARE));
            if (!isTrashed)
                optionsItems.add(options.get(OPTIONS_MOVE));
            if (!isTrashed)
                optionsItems.add(options.get(OPTIONS_COPY));
            if (!isTrashed)
                optionsItems.add(options.get(OPTIONS_SEPARATOR));
            if (!isTrashed)
                optionsItems.add(options.get(OPTIONS_TRASH));
            if (isTrashed)
                optionsItems.add(options.get(OPTIONS_RESTORE));
            optionsItems.add(options.get(OPTIONS_DELETE));
            optionsAdapter.notifyDataSetChanged();

            //Show
            Orion.showAnim(optionsLayout);
            backManager.register("displayOptions", () -> toggleOptions(false));
        } else {
            //Hide
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
