package com.botpa.turbophotos.gallery.modals

import android.content.Context
import java.io.File

class FileExplorerDialog(
    context: Context,
    onSelect: (File) -> Unit,
    startingFolder: File? = null,
    fileExtension: String = "",
    onCreate: ((File) -> Boolean)? = null,
) : ExplorerDialog(
    context,
    true,
    onSelect,
    startingFolder,
    fileExtension,
    onCreate
)