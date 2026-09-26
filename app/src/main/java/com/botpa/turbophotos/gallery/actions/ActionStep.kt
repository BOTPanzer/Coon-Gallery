package com.botpa.turbophotos.gallery.actions

class ActionStep(val type: ActionStepType, val index: Int, val index2: Int = -1) {

    //Type
    fun isOfType(type: ActionStepType): Boolean {
        return this.type == type
    }

}
