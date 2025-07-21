package com.botpa.turbophotos.util

class Action(private val type: Int, @JvmField val files: Array<TurboFile>) {

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
    @JvmField var failed: HashMap<TurboFile, String> = HashMap()

    //Results
    @JvmField var sortedAlbumsList: Boolean = false
    @JvmField var trashChanged: Int = TRASH_NONE
    @JvmField var deletedAlbums: ArrayList<Int> = ArrayList()


    //Action
    fun getHelper(file: TurboFile): ActionHelper {
        return ActionHelper(file)
    }

    fun isOfType(type: Int): Boolean {
        return this.type == type
    }

    fun allFailed(): Boolean {
        return failed.size == files.size
    }

}
