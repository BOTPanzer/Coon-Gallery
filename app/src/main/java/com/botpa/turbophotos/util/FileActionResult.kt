package com.botpa.turbophotos.util

class FileActionResult(file: TurboFile) {

    //Actions
    companion object {

        const val ACTION_NONE: String = "NONE"
        const val ACTION_DELETE: String = "DELETE"
        const val ACTION_TRASH: String = "TRASH"
        const val ACTION_RESTORE: String = "RESTORE"

    }

    //Action
    @JvmField var action: String = ACTION_NONE
    @JvmField var info: String = ""

    fun acted(action: String): Boolean {
        return this.action == action
    }

    //Indexes
    @JvmField var indexInTrash: Int = Library.trash.indexOf(file)
    @JvmField var indexInAll: Int = Library.allFiles.indexOf(file)
    @JvmField var indexInAlbum: Int = file.album.indexOf(file)
    @JvmField var indexOfAlbum: Int = Library.albums.indexOf(file.album)

    //Results
    @JvmField var deletedAlbum: Boolean = false
    @JvmField var sortedAlbumsList: Boolean = false

}
