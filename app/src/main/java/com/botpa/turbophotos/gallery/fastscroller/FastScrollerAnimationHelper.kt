package com.botpa.turbophotos.gallery.fastscroller

import android.view.View
import android.view.animation.Interpolator
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.botpa.turbophotos.gallery.fastscroller.FastScroller.AnimationHelper
import kotlin.math.max

class FastScrollerAnimationHelper(private val mView: View) : AnimationHelper {

    override val isScrollbarAutoHideEnabled: Boolean = true
    override val scrollbarAutoHideDelayMillis: Int = AUTO_HIDE_SCROLLBAR_DELAY_MILLIS

    private var isShowingScrollbar = true
    private var isShowingPopup = false

    override fun showScrollbar(trackView: View, thumbView: View) {
        if (isShowingScrollbar)  return
        isShowingScrollbar = true

        trackView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(SHOW_DURATION_MILLIS.toLong())
            .setInterpolator(SHOW_SCROLLBAR_INTERPOLATOR)
            .start()
        thumbView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(SHOW_DURATION_MILLIS.toLong())
            .setInterpolator(SHOW_SCROLLBAR_INTERPOLATOR)
            .start()
    }

    override fun hideScrollbar(trackView: View, thumbView: View) {
        if (!isShowingScrollbar) return
        isShowingScrollbar = false

        val isLayoutRtl = mView.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val width = max(trackView.width, thumbView.width)
        val translationX: Float = if (isLayoutRtl) {
            (if (trackView.left == 0) -width else 0).toFloat()
        } else {
            (if (trackView.right == mView.width) width else 0).toFloat()
        }
        trackView.animate()
            .alpha(0f)
            .translationX(translationX)
            .setDuration(HIDE_DURATION_MILLIS.toLong())
            .setInterpolator(HIDE_SCROLLBAR_INTERPOLATOR)
            .start()
        thumbView.animate()
            .alpha(0f)
            .translationX(translationX)
            .setDuration(HIDE_DURATION_MILLIS.toLong())
            .setInterpolator(HIDE_SCROLLBAR_INTERPOLATOR)
            .start()
    }

    override fun showPopup(popupView: View) {
        if (isShowingPopup) {
            return
        }
        isShowingPopup = true

        popupView.animate()
            .alpha(1f)
            .setDuration(SHOW_DURATION_MILLIS.toLong())
            .start()
    }

    override fun hidePopup(popupView: View) {
        if (!isShowingPopup) {
            return
        }
        isShowingPopup = false

        popupView.animate()
            .alpha(0f)
            .setDuration(HIDE_DURATION_MILLIS.toLong())
            .start()
    }

    companion object {
        private const val SHOW_DURATION_MILLIS = 150
        private const val HIDE_DURATION_MILLIS = 200
        private val SHOW_SCROLLBAR_INTERPOLATOR: Interpolator = LinearOutSlowInInterpolator()
        private val HIDE_SCROLLBAR_INTERPOLATOR: Interpolator = FastOutLinearInInterpolator()
        private const val AUTO_HIDE_SCROLLBAR_DELAY_MILLIS = 1500
    }

}
