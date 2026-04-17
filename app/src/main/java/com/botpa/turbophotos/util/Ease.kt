package com.botpa.turbophotos.util

import kotlin.math.pow

object Ease {

    fun inCubic(x: Float): Float {
        return x.toDouble().pow(2.0).toFloat()
    }

    fun outCubic(x: Float): Float {
        return 1f - inCubic(1f - x)
    }

}
