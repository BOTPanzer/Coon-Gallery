package com.botpa.turbophotos.gallery.modals

import android.content.Context
import java.io.File

class FolderExplorerDialog(
    context: Context,
    allowCreation: Boolean,
    onSelect: (File) -> Unit,
    startingFolder: File? = null,
) : ExplorerDialog(
    context,
    false,
    onSelect,
    startingFolder,
    onCreate = if (allowCreation) { folder -> folder.mkdir() } else null
)