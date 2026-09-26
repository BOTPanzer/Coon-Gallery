package com.botpa.turbophotos.gallery.actions

import com.botpa.turbophotos.gallery.data.Album

open class ActionResult(val type: ActionType) {

    //Errors
    var errors: MutableList<ActionError> = ArrayList()

    //Albums
    var hasSortedAlbumsList: Boolean = false
    var modifiedAlbums: MutableSet<Album> = HashSet()
    var removedAlbumIndexes: MutableList<Int> = ArrayList()

    //Items
    var itemStepsInGallery: MutableList<ActionStep> = ArrayList()


    //Type
    fun isOfType(type: ActionType): Boolean {
        return this.type == type
    }

}