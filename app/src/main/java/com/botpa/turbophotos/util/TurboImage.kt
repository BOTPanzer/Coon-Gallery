package com.botpa.turbophotos.util

import android.provider.MediaStore
import java.io.File

class TurboImage(@JvmField var file: File, @JvmField var album: Album, @JvmField var lastModified: Long, @JvmField var mediaType: String) {

    var name: String = file.name
    var isVideo: Boolean = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())

    fun hasMetadata(): Boolean {
        return album.hasMetadataKey(name)
    }

}
