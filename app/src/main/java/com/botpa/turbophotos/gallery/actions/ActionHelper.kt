package com.botpa.turbophotos.gallery.actions

import com.botpa.turbophotos.gallery.CoonItem
import com.botpa.turbophotos.gallery.Library

class ActionHelper(item: CoonItem) {

    @JvmField var indexInAll = Library.all.indexOf(item)
    @JvmField var indexInGallery = Library.gallery.indexOf(item)
    @JvmField var indexInAlbum = item.album.indexOf(item)
    @JvmField var indexOfAlbum = Library.albums.indexOf(item.album)

}