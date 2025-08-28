package com.botpa.turbophotos.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.TurboItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class DisplayHelper {

    //State
    private final MainActivity activity;

    public boolean isOpen = false;

    //Display
    private DisplayLayoutManager layoutManager;
    private DisplayAdapter adapter;

    public final ArrayList<TurboItem> items = new ArrayList<>();
    public TurboItem currentItem = null;
    public int currentIndex = -1;
    public int currentRelativeIndex = -1;

    //Views
    private View layout;
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
    private View editSave;

    private View optionsLayout;
    private View optionsRestore;
    private View optionsTrash;
    private View optionsDelete;
    private View optionsShare;
    private View optionsEdit;
    private View optionsOpenOutside;


    //Constructor
    public DisplayHelper(MainActivity activity) {
        this.activity = activity;
    }

    //Views
    public void loadViews() {
        layout = activity.findViewById(R.id.displayLayout);
        nameText = activity.findViewById(R.id.displayNameText);
        closeButton = activity.findViewById(R.id.displayCloseButton);
        infoButton = activity.findViewById(R.id.displayInfoButton);
        infoEdit = activity.findViewById(R.id.displayInfoEdit);
        optionsButton = activity.findViewById(R.id.displayOptionsButton);
        list = activity.findViewById(R.id.displayList);

        overlayLayout = activity.findViewById(R.id.displayOverlayLayout);

        infoLayout = activity.findViewById(R.id.displayInfoLayout);
        infoNameText = activity.findViewById(R.id.displayInfoNameText);
        infoCaptionText = activity.findViewById(R.id.displayInfoCaptionText);
        infoLabelsScroll = activity.findViewById(R.id.displayInfoLabelsScroll);
        infoLabelsText = activity.findViewById(R.id.displayInfoLabelsText);
        infoTextScroll = activity.findViewById(R.id.displayInfoTextScroll);
        infoTextText = activity.findViewById(R.id.displayInfoTextText);

        editLayout = activity.findViewById(R.id.displayEditLayout);
        editCaptionText = activity.findViewById(R.id.displayEditCaptionText);
        editLabelsText = activity.findViewById(R.id.displayEditLabelsText);
        editSave = activity.findViewById(R.id.displayEditSave);

        //Options
        optionsLayout = activity.findViewById(R.id.displayOptionsLayout);
        optionsRestore = activity.findViewById(R.id.displayOptionsRestore);
        optionsTrash = activity.findViewById(R.id.displayOptionsTrash);
        optionsDelete = activity.findViewById(R.id.displayOptionsDelete);
        optionsShare = activity.findViewById(R.id.displayOptionsShare);
        optionsEdit = activity.findViewById(R.id.displayOptionsEdit);
        optionsOpenOutside = activity.findViewById(R.id.displayOptionsOpen);

        //Insets
        Orion.addInsetsChangedListener(overlayLayout, 150);
    }

    public void addListeners() {
        //Main
        closeButton.setOnClickListener(view -> {
            //Reset display current
            open(-1, false);

            //Hide display
            Orion.hideAnim(layout);
            Orion.hideAnim(infoLayout);
            isOpen = false;

            //Back button
            activity.backManager.unregister("display");
        });

        //Info & edit
        infoButton.setOnClickListener(view -> showInfo(true));

        infoLayout.setOnClickListener(view -> showInfo(false));

        infoEdit.setOnClickListener(view -> {
            //No metadata file
            if (!currentItem.album.hasMetadata()) {
                Toast.makeText(activity, "This item's album does not have a metadata file linked to it", Toast.LENGTH_SHORT).show();
                return;
            }

            //No metadata
            if (!currentItem.hasMetadata()) {
                Toast.makeText(activity, "This item does not have a key in its album metadata", Toast.LENGTH_SHORT).show();
                return;
            }

            //Hide display info
            showInfo(false);

            //Toggle display edit
            showEdit(editLayout.getVisibility() != View.VISIBLE);
        });

        editLayout.setOnClickListener(view -> showEdit(false));

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
            Toast.makeText(activity, saved ? "Saved successfully" : "An error occurred while saving", Toast.LENGTH_SHORT).show();

            //Close menu
            showEdit(false);
        });

        //Options
        optionsButton.setOnClickListener(view -> showOptions(true));

        optionsLayout.setOnClickListener(view -> showOptions(false));

        optionsRestore.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Restore from trash
            activity.restoreFiles(new TurboItem[] {currentItem});
        });

        optionsTrash.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Move to trash
            activity.trashFiles(new TurboItem[] {currentItem});
        });

        optionsDelete.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Delete items
            activity.deleteFiles(new TurboItem[] {currentItem});
        });

        optionsShare.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Share
            activity.shareFiles(new TurboItem[]{currentItem});
        });

        optionsEdit.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Get mime type and URI
            String mimeType = currentItem.mimeType;
            Uri uri = Orion.getMediaStoreUriFromFile(activity, currentItem.file, mimeType);

            //Edit
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setDataAndType(uri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, null));
        });

        optionsOpenOutside.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Show open with menu
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(currentItem.file.getAbsolutePath()), currentItem.mimeType);
            activity.startActivity(intent);
        });
    }

    //Display
    private void showInfo(boolean show) {
        if (show) {
            infoLabelsScroll.scrollTo(0, 0);
            infoTextScroll.scrollTo(0, 0);
            Orion.showAnim(infoLayout);
            activity.backManager.register("displayInfo", () -> showInfo(false));
        } else {
            Orion.hideAnim(infoLayout);
            activity.backManager.unregister("displayInfo");
        }
    }

    private void showEdit(boolean show) {
        if (show) {
            editCaptionText.setText(infoCaptionText.getText());
            editLabelsText.setText(infoLabelsText.getText());
            Orion.showAnim(editLayout);
            activity.backManager.register("displayEdit", () -> showEdit(false));
        } else {
            Orion.hideKeyboard(activity);
            Orion.clearFocus(activity);
            Orion.hideAnim(editLayout);
            activity.backManager.unregister("displayEdit");
        }
    }

    private void showOptions(boolean show) {
        if (show) {
            Orion.showAnim(optionsLayout);
            activity.backManager.register("displayOptions", () -> showOptions(false));
        } else {
            Orion.hideAnim(optionsLayout);
            activity.backManager.unregister("displayOptions");
        }
    }

    public void initAdapters() {
        //Create display list viewer
        layoutManager = new DisplayLayoutManager(activity);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        list.setLayoutManager(layoutManager);

        //Create display snap helper
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(list);

        //Create display adapter
        adapter = new DisplayAdapter(activity, items);
        adapter.setOnClickListener((view, index) -> {
            if (overlayLayout.getVisibility() == View.VISIBLE) {
                Orion.hideAnim(overlayLayout);
                showBars(false);
            } else {
                Orion.showAnim(overlayLayout);
                showBars(true);
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
                    if (position < currentRelativeIndex) {
                        //Previous
                        layoutManager.setScrollEnabled(false);
                        open(currentIndex - 1, false);
                    } else if (position > currentRelativeIndex) {
                        //Next
                        layoutManager.setScrollEnabled(false);
                        open(currentIndex + 1, false);
                    }
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public void open(int index, boolean showOverlay) {
        //Deselect
        if (index == -1) {
            currentRelativeIndex = -1;
            currentIndex = -1;
            currentItem = null;
            return;
        }

        //Reset display items
        items.clear();
        currentIndex = index;
        currentRelativeIndex = 0;

        //Add items to display list
        if (index > 0) {
            //Has item before -> Add it
            items.add(activity.gallery.items.get(index - 1));
            currentRelativeIndex++;
        }
        items.add(activity.gallery.items.get(index));
        if (index < activity.gallery.items.size() - 1) {
            //Has item after -> Add it
            items.add(activity.gallery.items.get(index + 1));
        }

        //Get current image, update adapter & select it
        currentItem = items.get(currentRelativeIndex);
        adapter.notifyDataSetChanged();
        list.scrollToPosition(currentRelativeIndex);
        layoutManager.setScrollEnabled(true);

        //Change image name
        nameText.setText(currentItem.name);

        //Prepare options menu
        optionsRestore.setVisibility(currentItem.isTrashed() ? View.VISIBLE : View.GONE);
        optionsTrash.setVisibility(currentItem.isTrashed() ? View.GONE : View.VISIBLE);
        optionsShare.setVisibility(currentItem.isTrashed() ? View.GONE : View.VISIBLE);
        optionsEdit.setVisibility(currentItem.isTrashed() ? View.GONE : View.VISIBLE);
        optionsOpenOutside.setVisibility(currentItem.isTrashed() ? View.GONE : View.VISIBLE);

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

        //Close search & show display
        if (showOverlay) {
            overlayLayout.setVisibility(View.VISIBLE);
            showBars(false);    //Hide & show to trigger overlay animation
            showBars(true);
        }
        activity.gallery.showSearchLayout(false);
        Orion.showAnim(layout);
        isOpen = true;

        //Back button
        activity.backManager.register("display", () -> closeButton.performClick());
    }

    public void close() {
        showBars(true);
        showInfo(false);
        showEdit(false);
        showOptions(false);
        closeButton.performClick();
    }

    private void showBars(boolean show) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), overlayLayout);

        if (show) {
            controller.show(WindowInsetsCompat.Type.systemBars());
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars());
        }
    }

}
