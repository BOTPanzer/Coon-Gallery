package com.botpa.turbophotos.util

class FileActionResult(file: TurboFile) {

    //Actions
    companion object {

        const val ACTION_NONE: String = "NONE"
        const val ACTION_DELETE: String = "DELETE"
        const val ACTION_TRASH: String = "TRASH"
        const val ACTION_RESTORE: String = "RESTORE"

        //Trash
        const val TRASH_NONE: String = "NONE"
        const val TRASH_ADDED: String = "ADDED"
        const val TRASH_REMOVED: String = "REMOVED"
        const val TRASH_UPDATED: String = "UPDATED"

    }

    //Action
    @JvmField var type: String = ACTION_NONE

    //Fail message
    @JvmField var fail: String = ""

    //Indexes
    @JvmField var indexInTrash: Int = Library.trash.indexOf(file)
    @JvmField var indexInAll: Int = Library.all.indexOf(file)
    @JvmField var indexInAlbum: Int = file.album.indexOf(file)
    @JvmField var indexOfAlbum: Int = Library.albums.indexOf(file.album)

    //Results
    @JvmField var albumDeleted: Boolean = false
    @JvmField var sortedAlbumsList: Boolean = false
    @JvmField var trashState: String = TRASH_NONE


    //Action
    fun isType(type: String): Boolean {
        return this.type == type
    }

}
