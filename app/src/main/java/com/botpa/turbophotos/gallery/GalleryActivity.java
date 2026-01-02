package com.botpa.turbophotos.gallery;

import android.app.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.botpa.turbophotos.gallery.actions.Action;
import com.botpa.turbophotos.gallery.actions.ActionError;
import com.botpa.turbophotos.util.Orion;

public class GalleryActivity extends AppCompatActivity {

    //Trash actions (trash/restore items)
    private Action trashAction; //The pending action

    private final ActivityResultLauncher<IntentSenderRequest> trashLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK) {
                    //Action failed or was cancelled -> Add errors
                    for (CoonItem item : trashAction.trashPending.values()) trashAction.errors.add(new ActionError(item, "Trash operation failed"));

                    //Empty pending items
                    trashAction.trashPending.clear();
                }

                //Action successful
                if (trashAction.isOfType(Action.TYPE_TRASH)) {
                    //Trash items
                    Library.onTrashItemsResult(this, trashAction);
                } else if (trashAction.isOfType(Action.TYPE_RESTORE)) {
                    //Restore items
                    Library.onRestoreItemsResult(this, trashAction);
                } else {
                    //Trash action type isn't valid
                    Orion.snack(this, "Trash action type isn't valid");
                }

                //Clear trash action
                trashAction = null;
            }
    );


    //Trash functions
    protected void trashItems(CoonItem[] itemsToTrash) {
        //Check if trash has a pending action
        if (trashAction != null) {
            //Trash has pending action -> Return
            Orion.snack(GalleryActivity.this, "Trash has a pending action");
            return;
        }

        //Trash items
        trashAction = Library.trashItems(this, itemsToTrash, trashLauncher);
    }

    protected void restoreItems(CoonItem[] itemsToRestore) {
        //Check if trash has a pending action
        if (trashAction != null) {
            //Trash has pending action -> Return
            Orion.snack(GalleryActivity.this, "Trash has a pending action");
            return;
        }

        //Restore items
        trashAction = Library.restoreItems(this, itemsToRestore, trashLauncher);
    }

}
