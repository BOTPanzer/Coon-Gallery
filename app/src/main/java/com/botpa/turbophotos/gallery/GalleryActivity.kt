package com.botpa.turbophotos.gallery

import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.actions.ActionError
import com.botpa.turbophotos.util.Orion

open class GalleryActivity : AppCompatActivity() {

    //Trash actions (trash/restore items)
    private var trashAction: Action? = null //The pending action
    private val trashLauncher = registerForActivityResult(StartIntentSenderForResult()) { result: ActivityResult ->
        //Get action
        val action = trashAction ?: return@registerForActivityResult //Return if its null

        //Check if action failed or was cancelled
        if (result.resultCode != RESULT_OK) {
            //Action failed or was cancelled -> Add errors
            for (item in action.trashPending.values) {
                action.errors.add(ActionError(item, "Trash operation failed"))
            }

            //Empty pending items
            action.trashPending.clear()
        }

        //Action successful
        if (action.isOfType(Action.TYPE_TRASH)) {
            //Trash items
            Library.onTrashItemsResult(this, trashAction)
        } else if (action.isOfType(Action.TYPE_RESTORE)) {
            //Restore items
            Library.onRestoreItemsResult(this, trashAction)
        } else {
            //Trash action type isn't valid
            Orion.snack(this, "Trash action type isn't valid")
        }

        //Clear trash action
        trashAction = null
    }


    //Trash functions
    protected fun trashItems(itemsToTrash: Array<CoonItem?>?) {
        //Check if trash has a pending action
        if (trashAction != null) {
            //Trash has pending action -> Return
            Orion.snack(this@GalleryActivity, "Trash has a pending action")
        } else {
            //Trash items
            trashAction = Library.trashItems(this, itemsToTrash, trashLauncher)
        }
    }

    protected fun restoreItems(itemsToRestore: Array<CoonItem?>?) {
        //Check if trash has a pending action
        if (trashAction != null) {
            //Trash has pending action -> Return
            Orion.snack(this@GalleryActivity, "Trash has a pending action")
        } else {
            //Restore items
            trashAction = Library.restoreItems(this, itemsToRestore, trashLauncher)
        }
    }

}
