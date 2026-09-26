package com.botpa.turbophotos.gallery.actions

import android.net.Uri
import com.botpa.turbophotos.gallery.data.Item

class Action(type: ActionType, val items: Array<Item>) : ActionResult(type) {

    //Results (async actions)
    var pending: MutableMap<Uri, Item> = HashMap()


    //Util
    fun getHelper(item: Item): ActionHelper {
        return ActionHelper(item)
    }

}
