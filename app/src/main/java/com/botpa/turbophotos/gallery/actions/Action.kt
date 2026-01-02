package com.botpa.turbophotos.gallery.actions

import android.net.Uri
import com.botpa.turbophotos.gallery.Album
import com.botpa.turbophotos.gallery.CoonItem

class Action(val type: Int, @JvmField val items: Array<CoonItem>) {

    //Errors
    @JvmField var errors: ArrayList<ActionError> = ArrayList()

    //Results (trash)
    @JvmField var trashAction: Int = TRASH_NONE
    @JvmField var trashPending: Map<Uri, CoonItem> = HashMap()

    //Results (albums & gallery)
    @JvmField var sortedAlbumsList: Boolean = false
    @JvmField var updatedAlbums: HashSet<Album> = HashSet()
    @JvmField var removedIndexesInAlbums: ArrayList<Int> = ArrayList()
    @JvmField var removedIndexesInGallery: ArrayList<Int> = ArrayList()


    //Action
    fun getHelper(file: CoonItem): ActionHelper {
        return ActionHelper(file)
    }

    fun isOfType(type: Int): Boolean {
        return this.type == type
    }


    //Static
    companion object {

        //Normal actions
        const val TYPE_NONE:        Int = 0
        const val TYPE_DELETE:      Int = 1
        const val TYPE_TRASH:       Int = 2
        const val TYPE_RESTORE:     Int = 3
        const val TYPE_MOVE:        Int = 4
        const val TYPE_COPY:        Int = 5

        //Trash action
        const val TRASH_NONE:       Int = 0
        const val TRASH_ADDED:      Int = 1
        const val TRASH_REMOVED:    Int = 2
        const val TRASH_UPDATED:    Int = 3

    }

}
