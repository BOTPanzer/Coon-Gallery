package com.botpa.turbophotos.gallery

import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.actions.ActionError
import com.botpa.turbophotos.util.Orion

open class BaseActivity : AppCompatActivity() {

    //Trash actions (trash/restore items)
    private var pendingAction: Action? = null //The pending action
    private val actionLauncher = registerForActivityResult(StartIntentSenderForResult()) { result: ActivityResult ->
        //Get action
        val action = pendingAction ?: return@registerForActivityResult //Return if its null

        //Check if action failed or was cancelled
        if (result.resultCode != RESULT_OK) {
            //Action failed or was cancelled -> Add errors
            for (item in action.pending.values) {
                action.errors.add(ActionError(item, "Operation failed."))
            }

            //Empty pending items
            action.pending.clear()
        }

        //Check action type
        when {
            action.isOfType(Action.TYPE_TRASH) -> Library.onTrashItemsResult(this, action)
            action.isOfType(Action.TYPE_RESTORE) -> Library.onRestoreItemsResult(this, action)
            action.isOfType(Action.TYPE_FAVOURITE) -> Library.onFavouriteItemsResult(this, action)
            action.isOfType(Action.TYPE_UNFAVOURITE) -> Library.onUnfavouriteItemsResult(this, action)
            else -> Orion.snack(this, "Action type isn't valid.")
        }

        //Clear pending action
        pendingAction = null
    }


    //Trash functions
    protected fun trashItems(itemsToTrash: Array<CoonItem>) {
        //Check if there is a pending action
        if (pendingAction != null) {
            Orion.snack(this, "There is a pending action.")
        } else {
            //Trash items
            pendingAction = Library.trashItems(this, itemsToTrash, actionLauncher)
        }
    }

    protected fun restoreItems(itemsToRestore: Array<CoonItem>) {
        //Check if there is a pending action
        if (pendingAction != null) {
            Orion.snack(this, "There is a pending action.")
        } else {
            //Restore items
            pendingAction = Library.restoreItems(this, itemsToRestore, actionLauncher)
        }
    }

    //Favourite functions
    protected fun favouriteItems(itemsToFavourite: Array<CoonItem>) {
        //Check if there is a pending action
        if (pendingAction != null) {
            Orion.snack(this, "There is a pending action.")
        } else {
            //Favourite items
            pendingAction = Library.favouriteItems(this, itemsToFavourite, actionLauncher)
        }
    }

    protected fun unfavouriteItems(itemsToUnfavourite: Array<CoonItem>) {
        //Check if there is a pending action
        if (pendingAction != null) {
            Orion.snack(this, "There is a pending action.")
        } else {
            //Unfavourite items
            pendingAction = Library.unfavouriteItems(this, itemsToUnfavourite, actionLauncher)
        }
    }

}
