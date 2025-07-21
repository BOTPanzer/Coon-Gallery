package com.botpa.turbophotos.util

class ActionHelper(file: TurboFile) {

    @JvmField val indexInTrash = Library.trash.indexOf(file)
    @JvmField val indexInAll = Library.all.indexOf(file)
    @JvmField val indexInAlbum = file.album.indexOf(file)
    @JvmField val indexOfAlbum = Library.albums.indexOf(file.album)

}