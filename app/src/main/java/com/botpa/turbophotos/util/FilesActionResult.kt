package com.botpa.turbophotos.util

class FilesActionResult(files: Array<TurboFile>) {

    //Static
    companion object {

        //Actions
        const val ACTION_NONE: String = "NONE"
        const val ACTION_DELETE: String = "DELETE"
        const val ACTION_TRASH: String = "TRASH"
        const val ACTION_RESTORE: String = "RESTORE"

        //Trash
        const val TRASH_NONE: String = "NONE"
        const val TRASH_UPDATED: String = "UPDATED"
        const val TRASH_CLEARED: String = "CLEARED"

    }

    //Action
    @JvmField var type: String = ACTION_NONE

    //Fail messages
    @JvmField var fails: HashMap<TurboFile, String> = HashMap()

    //Indexes
    @JvmField var indexInTrash: HashMap<TurboFile, Int> = HashMap()
    @JvmField var indexInAll: HashMap<TurboFile, Int> = HashMap()
    @JvmField var indexInAlbum: HashMap<TurboFile, Int> = HashMap()
    @JvmField var indexOfAlbum: HashMap<TurboFile, Int> = HashMap()

    //Results
    @JvmField var deletedAlbums: HashSet<Album> = HashSet()
    @JvmField var sortedAlbumsList: Boolean = false


    //Action
    fun isType(type: String): Boolean {
        return this.type == type
    }

}
