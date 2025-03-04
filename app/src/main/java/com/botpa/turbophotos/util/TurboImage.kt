package com.botpa.turbophotos.util

import java.io.File

class TurboImage(@JvmField var file: File, @JvmField var album: Album, @JvmField var lastModified: Long) {

    fun hasMetadata(): Boolean {
        return album.metadata.has(file.name)
    }
}
