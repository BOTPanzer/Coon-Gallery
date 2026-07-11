package com.botpa.turbophotos.screens.video

import android.content.Context
import android.util.AttributeSet
import com.botpa.turbophotos.gallery.views.ZoomableLayout
import kotlin.math.max
import kotlin.math.min

class VideoZoomableLayout(context: Context, attrs: AttributeSet?) : ZoomableLayout(context, attrs) {

    //Skip
    private var skipDuration: Long = 0

    var skipBackwardsAmount: Long = 0
    var skipForwardAmount: Long = 0

    //Events
    var onBeforeSeek: ((amount: Long) -> Unit)? = null
    var onSeek: ((amount: Long) -> Unit)? = null


    //Register events
    init {
        onMultiClick = { x, y, count ->
            //Get seek areas size
            val maxArea = (150 * resources.displayMetrics.density).toInt()
            val doubleTapArea = min(width / 5, maxArea)

            //Check position to see if time should be skipped
            if (x <= doubleTapArea || x >= width - doubleTapArea) {
                //Skip time -> Check direction
                if (x < doubleTapArea) {
                    //Update indicators
                    skipDuration = min(skipDuration - skipBackwardsAmount, -skipBackwardsAmount)
                } else if (x > width - doubleTapArea) {
                    //Update indicators
                    skipDuration = max(skipDuration + skipForwardAmount, skipForwardAmount)
                }
                onBeforeSeek?.invoke(skipDuration)

                //Continue multi click
                true
            } else {
                //Don't skip time -> Finish multi click
                false
            }
        }

        onMultiClickFinished = { count ->
            //Check if seeking
            if (skipDuration != 0L) {
                //Seek & reset skip duration
                onSeek?.invoke(skipDuration)
                skipDuration = 0
            }
        }
    }

}