package com.botpa.turbophotos.gallery.modals

import android.content.Context
import android.content.Intent
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.StoragePairs
import com.botpa.turbophotos.util.Storage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.net.toUri

class UpdateDialog(val context: Context, val newestVersion: String) {

    fun buildAndShow() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.app_update_dialog_title)
            .setMessage(context.getString(R.string.app_update_dialog_content, newestVersion))
            .setPositiveButton(R.string.app_update_dialog_action_download) { dialog, which ->
                //Open release page
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/BOTPanzer/Coon-Gallery/releases/latest".toUri()))
            }
            .setNegativeButton(R.string.app_update_dialog_action_ignore) { dialog, which ->
                //Mark version as skipped to prevent showing dialog again
                Storage.putString(StoragePairs.APP_UPDATE_SKIPPED, newestVersion)
            }
            .show()
    }

}