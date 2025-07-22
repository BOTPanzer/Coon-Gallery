package com.botpa.turbophotos.util

class ActionHelper(file: TurboFile) {

    @JvmField var indexInTrash = Library.trash.indexOf(file)
    @JvmField var indexInAll = Library.all.indexOf(file)
    @JvmField var indexInAlbum = file.album.indexOf(file)
    @JvmField var indexOfAlbum = Library.albums.indexOf(file.album)

}