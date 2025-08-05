package com.botpa.turbophotos.util

class Action(private val type: Int, @JvmField val items: Array<TurboItem>) {

    //Static
    companion object {

        //Actions
        const val TYPE_NONE: Int = 0
        const val TYPE_DELETE: Int = 1
        const val TYPE_TRASH: Int = 2
        const val TYPE_RESTORE: Int = 3

        //Trash changes
        const val TRASH_NONE: Int = 0
        const val TRASH_ADDED: Int = 1
        const val TRASH_REMOVED: Int = 2
        const val TRASH_UPDATED: Int = 3

    }

    //Failed messages
    @JvmField var failed: HashMap<TurboItem, String> = HashMap()

    //Results
    @JvmField var trashChanged: Int = TRASH_NONE
    @JvmField var deletedAlbums: HashSet<Album> = HashSet()
    @JvmField var sortedAlbums: HashSet<Album> = HashSet()
    @JvmField var sortedAlbumsList: Boolean = false


    //Action
    fun getHelper(file: TurboItem): ActionHelper {
        return ActionHelper(file)
    }

    fun isOfType(type: Int): Boolean {
        return this.type == type
    }

    fun allFailed(): Boolean {
        return failed.size == items.size
    }

}
