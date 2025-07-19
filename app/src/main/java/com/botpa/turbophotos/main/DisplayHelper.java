package com.botpa.turbophotos.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.botpa.turbophotos.R;
import com.botpa.turbophotos.util.Orion;
import com.botpa.turbophotos.util.TurboFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class DisplayHelper {

    //State
    private final MainActivity activity;

    public boolean isOpen = false;

    //Display
    private DisplayLayoutManager layoutManager;
    private DisplayAdapter adapter;

    public final ArrayList<TurboFile> files = new ArrayList<>();
    public TurboFile current = null;
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
        optionsTrash = activity.findViewById(R.id.displayOptionsTrash);
        optionsDelete = activity.findViewById(R.id.displayOptionsDelete);
        optionsShare = activity.findViewById(R.id.displayOptionsShare);
        optionsEdit = activity.findViewById(R.id.displayOptionsEdit);
        optionsOpenOutside = activity.findViewById(R.id.displayOptionsOpen);
    }

    public void addListeners() {
        //Main
        closeButton.setOnClickListener(view -> {
            //Reset display current
            open(-1);

            //Hide display
            Orion.hideAnim(layout);
            Orion.hideAnim(infoLayout);
            isOpen = false;

            //Back button
            activity.backManager.unregister("display");
        });

        //Info & edit
        infoButton.setOnClickListener(view -> showInfo(infoLayout.getVisibility() != View.VISIBLE));

        infoLayout.setOnClickListener(view -> showInfo(false));

        infoEdit.setOnClickListener(view -> {
            //No metadata file
            if (!current.album.hasMetadata()) {
                Toast.makeText(activity, "This file does not have metadata file linked to its album", Toast.LENGTH_SHORT).show();
                return;
            }

            //No metadata
            if (!current.hasMetadata()) {
                Toast.makeText(activity, "This file does not have a key in its album metadata", Toast.LENGTH_SHORT).show();
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
            String key = current.getName();
            ObjectNode metadata = current.album.getMetadataKey(key);
            if (metadata == null) {
                metadata = Orion.getEmptyJson();
                current.album.metadata.set(key, metadata);
            }
            metadata.put("caption", caption);
            metadata.set("labels", Orion.arrayToJson(labelsArray));

            //Save
            boolean saved = current.album.saveMetadata();
            Toast.makeText(activity, saved ? "Saved successfully" : "An error occurred while saving", Toast.LENGTH_SHORT).show();

            //Close menu
            showEdit(false);
        });

        //Options
        optionsButton.setOnClickListener(view -> {
            //File is trashed -> Do not allow options yet
            if (current.isTrashed()) return;

            //Show options menu
            showOptions(optionsLayout.getVisibility() != View.VISIBLE);
        });

        optionsLayout.setOnClickListener(view -> showOptions(false));
    }

    //Display
    private void showInfo(boolean show) {
        if (show) {
            infoLabelsScroll.scrollTo(0, 0);
            infoTextScroll.scrollTo(0, 0);
            Orion.showAnim(infoLayout);
            activity.backManager.register("displayInfo", () -> infoButton.performClick());
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
            activity.backManager.register("displayEdit", () -> infoEdit.performClick());
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
            activity.backManager.register("displayOptions", () -> optionsButton.performClick());
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
        adapter = new DisplayAdapter(activity, files);
        adapter.setOnClickListener((view, index) -> {
            if (overlayLayout.getVisibility() == View.VISIBLE)
                Orion.hideAnim(overlayLayout);
            else
                Orion.showAnim(overlayLayout);
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
                        open(currentIndex - 1);
                    } else if (position > currentRelativeIndex) {
                        //Next
                        layoutManager.setScrollEnabled(false);
                        open(currentIndex + 1);
                    }
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public void open(int index) {
        //Deselect
        if (index == -1) {
            currentRelativeIndex = -1;
            currentIndex = -1;
            current = null;
            return;
        }

        //Fill display files
        files.clear();
        currentIndex = index;
        currentRelativeIndex = 0;

        //Add files to display list
        if (index > 0) {
            //Has file before
            files.add(activity.gallery.files.get(index - 1));
            currentRelativeIndex++;
        }
        files.add(activity.gallery.files.get(index));
        if (index < activity.gallery.files.size() - 1) {
            //Has file after
            files.add(activity.gallery.files.get(index + 1));
        }

        //Get current image, update adapter & select it
        current = files.get(currentRelativeIndex);
        adapter.notifyDataSetChanged();
        list.scrollToPosition(currentRelativeIndex);
        layoutManager.setScrollEnabled(true);

        //Change image name
        nameText.setText(current.getName());

        //Prepare options menu
        optionsTrash.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Move to trash
            activity.trashFile(current);
        });

        optionsDelete.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Show delete confirmation dialog
            new MaterialAlertDialogBuilder(activity)
                    .setMessage("Are you sure you want to permanently delete \"" + current.getName() + "\"?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, whichButton) -> {
                        activity.deleteFile(current);
                    })
                    .show();
        });

        optionsShare.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Share
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, Orion.getUriFromFile(activity, current.file));
            intent.setType(current.getMimeType());
            activity.startActivity(Intent.createChooser(intent, null));
        });

        optionsEdit.setOnClickListener(view -> {
            //Close options menu
            showOptions(false);

            //Get mime type and URI
            String mimeType = current.getMimeType();
            Uri uri = Orion.getMediaStoreUriFromFile(activity, current.file, mimeType);

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
            intent.setDataAndType(Uri.parse(current.file.getAbsolutePath()), current.getMimeType());
            activity.startActivity(intent);
        });

        //Load image info (caption & labels)
        String caption = "";
        String labels = "";
        String text = "";
        try {
            JsonNode metadata = current.album.getMetadataKey(current.getName());
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
        infoNameText.setText(current.getName());
        infoCaptionText.setText(caption);
        infoLabelsText.setText(labels);
        infoTextText.setText(text);

        //Close search & show display
        activity.gallery.showSearchLayout(false);
        Orion.showAnim(layout);
        isOpen = true;

        //Back button
        activity.backManager.register("display", () -> closeButton.performClick());
    }

    public void close() {
        showInfo(false);
        showEdit(false);
        showOptions(false);
        closeButton.performClick();
    }

}
