package com.botpa.turbophotos.gallery.actions

import android.net.Uri
import com.botpa.turbophotos.gallery.data.Album
import com.botpa.turbophotos.gallery.data.Item

class Action(val type: Int, @JvmField val items: Array<Item>) {

    //Errors
    var errors: MutableList<ActionError> = ArrayList()

    //Results (async actions)
    var pending: MutableMap<Uri, Item> = HashMap()

    //Results (albums & gallery)
    var hasSortedAlbumsList: Boolean = false
    var modifiedAlbums: MutableSet<Album> = HashSet()
    var removedIndexesInAlbums: MutableList<Int> = ArrayList()
    var removedIndexesInGallery: MutableList<Int> = ArrayList()
    var modifiedIndexesInGallery: MutableList<Int> = ArrayList()


    //Action
    fun getHelper(file: Item): ActionHelper {
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
        const val TYPE_RENAME:      Int = 6
        const val TYPE_FAVOURITE:   Int = 7
        const val TYPE_UNFAVOURITE: Int = 8

    }

}
