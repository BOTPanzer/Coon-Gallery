package com.botpa.turbophotos.util

import java.util.Arrays

class Action(
    private val type: Int,
    @JvmField val items: Array<TurboItem>
) {

    //Static
    companion object {

        //Actions
        const val TYPE_NONE: Int = 0
        const val TYPE_DELETE: Int = 1
        const val TYPE_TRASH: Int = 2
        const val TYPE_RESTORE: Int = 3
        const val TYPE_MOVE: Int = 4

        //Trash changes
        const val TRASH_NONE: Int = 0
        const val TRASH_ADDED: Int = 1
        const val TRASH_REMOVED: Int = 2
        const val TRASH_UPDATED: Int = 3

    }

    //Failed messages
    @JvmField var failed: HashMap<TurboItem, String> = HashMap()

    //Results
    @JvmField var trashChanges: Int = TRASH_NONE
    @JvmField var deletedAlbums: HashSet<Album> = HashSet()
    @JvmField var sortedAlbums: HashSet<Album> = HashSet()
    @JvmField var sortedAlbumsList: Boolean = false
    @JvmField var removedIndexesInGallery: ArrayList<Int> = ArrayList() //In the order of removal, if index 1 gets removed before 2, both will appear as 1


    //Constructor
    init {
        //Get item gallery indexes
        val indexesInGallery: MutableMap<TurboItem?, Int> = HashMap()
        for (item in items) indexesInGallery[item] = Library.gallery.indexOf(item)

        //Sort items based on their indexes
        Arrays.sort<TurboItem?>(items, Comparator { a: TurboItem?, b: TurboItem? ->
            indexesInGallery.getOrDefault(b, -1).compareTo(indexesInGallery.getOrDefault(a, -1))
        })
    }

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
