package com.botpa.turbophotos.util

class ActionHelper(@JvmField val item: TurboItem) {

    @JvmField var indexInTrash = Library.trash.indexOf(item)
    @JvmField var indexInAll = Library.all.indexOf(item)
    @JvmField var indexInGallery = Library.gallery.indexOf(item)
    @JvmField var indexInAlbum = item.album.indexOf(item)
    @JvmField var indexOfAlbum = Library.albums.indexOf(item.album)

}