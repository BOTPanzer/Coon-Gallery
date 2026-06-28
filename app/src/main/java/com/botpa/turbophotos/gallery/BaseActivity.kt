package com.botpa.turbophotos.gallery

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import com.botpa.turbophotos.gallery.actions.Action
import com.botpa.turbophotos.gallery.actions.ActionError
import com.botpa.turbophotos.gallery.permissions.PermissionManager
import com.botpa.turbophotos.gallery.permissions.PermissionType
import com.botpa.turbophotos.util.BackManager
import com.botpa.turbophotos.util.Orion
import com.botpa.turbophotos.util.Storage

open class BaseActivity : AppCompatActivity() {

    //Components
    protected lateinit var backManager: BackManager
    protected lateinit var permissionManager: PermissionManager

    protected open val permissions: List<PermissionType> = emptyList()
    protected open val contentViewResource: Int = 0

    //Gallery actions
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


    //Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(contentViewResource)

        //Enable HDR
        window.colorMode = ActivityInfo.COLOR_MODE_HDR

        //Init components
        backManager = BackManager(this, onBackPressedDispatcher)
        permissionManager = PermissionManager(this, permissions)
        Storage.init(this)

        //Init views
        onBeforeInitViews()
        onInitViews()
        onInitListeners()
        onAfterInitViews()

        //Check permissions
        checkPermissions()
    }

    protected open fun onBeforeInitViews() {}

    protected open fun onInitViews() {}

    protected open fun onInitListeners() {}

    protected open fun onAfterInitViews() {}

    protected fun checkPermissions() {
        //Check if permissions are granted
        if (permissionManager.hasAllPermissions) {
            //Granted
            onPermissionsGranted()
        } else {
            //Denied
            onPermissionsDenied()
        }
    }

    protected open fun onPermissionsGranted() {}

    protected open fun onPermissionsDenied() {}

    //Trash functions
    protected fun trashItems(itemsToTrash: Array<Item>) {
        //Check if there is a pending action
        if (pendingAction != null) {
            Orion.snack(this, "There is a pending action.")
        } else {
            //Trash items
            pendingAction = Library.trashItems(this, itemsToTrash, actionLauncher)
        }
    }

    protected fun restoreItems(itemsToRestore: Array<Item>) {
        //Check if there is a pending action
        if (pendingAction != null) {
            Orion.snack(this, "There is a pending action.")
        } else {
            //Restore items
            pendingAction = Library.restoreItems(this, itemsToRestore, actionLauncher)
        }
    }

    //Favourite functions
    protected fun favouriteItems(itemsToFavourite: Array<Item>) {
        //Check if there is a pending action
        if (pendingAction != null) {
            Orion.snack(this, "There is a pending action.")
        } else {
            //Favourite items
            pendingAction = Library.favouriteItems(this, itemsToFavourite, actionLauncher)
        }
    }

    protected fun unfavouriteItems(itemsToUnfavourite: Array<Item>) {
        //Check if there is a pending action
        if (pendingAction != null) {
            Orion.snack(this, "There is a pending action.")
        } else {
            //Unfavourite items
            pendingAction = Library.unfavouriteItems(this, itemsToUnfavourite, actionLauncher)
        }
    }

}
