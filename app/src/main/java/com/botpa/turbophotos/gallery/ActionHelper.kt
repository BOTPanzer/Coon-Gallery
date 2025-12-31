package com.botpa.turbophotos.gallery

class ActionHelper(@JvmField val item: CoonItem) {

    @JvmField var indexInTrash = Library.trash.indexOf(item)
    @JvmField var indexInAll = Library.all.indexOf(item)
    @JvmField var indexInGallery = Library.gallery.indexOf(item)
    @JvmField var indexInAlbum = item.album.indexOf(item)
    @JvmField var indexOfAlbum = Library.albums.indexOf(item.album)

}