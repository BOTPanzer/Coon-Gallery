package com.botpa.turbophotos.gallery.actions

import com.botpa.turbophotos.gallery.CoonItem
import com.botpa.turbophotos.gallery.Library

class ActionHelper(item: CoonItem) {

    @JvmField var indexInGallery =  Library.gallery.indexOf(item)
    @JvmField var indexInTrash =    if (item.isTrashed) Library.trash.indexOf(item) else -1
    @JvmField var indexInAll =      if (!item.isTrashed) Library.all.indexOf(item) else -1
    @JvmField var indexInAlbum =    if (!item.isTrashed) item.album.indexOf(item) else -1
    @JvmField var indexOfAlbum =    if (!item.isTrashed) Library.albums.indexOf(item.album) else -1

}