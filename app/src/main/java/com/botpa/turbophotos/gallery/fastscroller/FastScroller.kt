package com.botpa.turbophotos.gallery.fastscroller

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.math.MathUtils
import kotlin.math.abs
import kotlin.math.max

class FastScroller(
    private val view: ViewGroup,
    private val viewHelper: ViewHelper,
    padding: Rect?,
    trackDrawable: Drawable,
    thumbDrawable: Drawable,
    private val animationHelper: AnimationHelper
) {

    private val minTouchTargetSize = (20 * view.resources.displayMetrics.density).toInt()
    private val touchSlop: Int

    private var userPadding: Rect? = padding

    private val trackWidth: Int
    private val thumbWidth: Int
    private val thumbHeight: Int

    private val trackView: View
    private val thumbView: View
    private val popupView: TextView

    private var scrollbarEnabled = false
    private var thumbOffset = 0

    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var dragStartY = 0f
    private var dragStartThumbOffset = 0
    private var isDragging = false

    private val autoHideScrollbarRunnable = Runnable { this.autoHideScrollbar() }

    private val tempRect = Rect()


    init {
        val context = view.context
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop

        trackWidth = requireNonNegative(trackDrawable.intrinsicWidth, "trackDrawable.intrinsicWidth < 0")
        thumbWidth = requireNonNegative(thumbDrawable.intrinsicWidth, "thumbDrawable.intrinsicWidth < 0")
        thumbHeight = requireNonNegative(thumbDrawable.intrinsicHeight, "thumbDrawable.intrinsicHeight < 0")

        trackView = View(context)
        trackView.background = trackDrawable
        thumbView = View(context)
        thumbView.background = thumbDrawable
        popupView = AppCompatTextView(context)
        popupView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val overlay = view.overlay
        overlay.add(trackView)
        overlay.add(thumbView)
        overlay.add(popupView)

        postAutoHideScrollbar()
        popupView.alpha = 0f

        viewHelper.addOnPreDrawListener { this.onPreDraw() }
        viewHelper.addOnScrollChangedListener { this.onScrollChanged() }
        viewHelper.addOnTouchEventListener { event -> this.onTouchEvent(event) }
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        if (userPadding != null && userPadding!!.left == left && userPadding!!.top == top && userPadding!!.right == right && userPadding!!.bottom == bottom) {
            return
        }
        if (userPadding == null) {
            userPadding = Rect()
        }
        userPadding!!.set(left, top, right, bottom)
        view.invalidate()
    }

    private var padding: Rect?
        get() {
            if (userPadding != null) {
                tempRect.set(userPadding!!)
            } else {
                tempRect.set(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
            }
            return tempRect
        }
        set(padding) {
            if (userPadding == padding) {
                return
            }
            if (padding != null) {
                if (userPadding == null) {
                    userPadding = Rect()
                }
                userPadding!!.set(padding)
            } else {
                userPadding = null
            }
            view.invalidate()
        }

    private fun onPreDraw() {
        updateScrollbarState()
        trackView.visibility = if (scrollbarEnabled) View.VISIBLE else View.INVISIBLE
        thumbView.visibility = if (scrollbarEnabled) View.VISIBLE else View.INVISIBLE
        if (!scrollbarEnabled) {
            popupView.visibility = View.INVISIBLE
            return
        }

        val layoutDirection = view.layoutDirection
        trackView.layoutDirection = layoutDirection
        thumbView.layoutDirection = layoutDirection
        popupView.layoutDirection = layoutDirection

        val isLayoutRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
        val viewWidth = view.width
        val viewHeight = view.height

        val padding = this.padding!!
        val trackLeft = if (isLayoutRtl) padding.left else viewWidth - padding.right - trackWidth
        layoutView(trackView, trackLeft, padding.top, trackLeft + trackWidth, max(viewHeight - padding.bottom, padding.top))
        val thumbLeft = if (isLayoutRtl) padding.left else viewWidth - padding.right - thumbWidth
        val thumbTop = padding.top + thumbOffset
        layoutView(thumbView, thumbLeft, thumbTop, thumbLeft + thumbWidth, thumbTop + thumbHeight)

        val popupText = viewHelper.popupText
        val hasPopup = !TextUtils.isEmpty(popupText)
        popupView.visibility = if (hasPopup) View.VISIBLE else View.INVISIBLE
        if (hasPopup) {
            val popupLayoutParams = popupView.layoutParams as FrameLayout.LayoutParams
            if (popupView.text != popupText) {
                popupView.text = popupText
                val widthMeasureSpec = ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY), (padding.left + padding.right + thumbWidth + popupLayoutParams.leftMargin + popupLayoutParams.rightMargin), popupLayoutParams.width)
                val heightMeasureSpec = ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(viewHeight, View.MeasureSpec.EXACTLY), (padding.top + padding.bottom + popupLayoutParams.topMargin + popupLayoutParams.bottomMargin), popupLayoutParams.height)
                popupView.measure(widthMeasureSpec, heightMeasureSpec)
            }
            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight
            val popupLeft = if (isLayoutRtl)
                padding.left + thumbWidth + popupLayoutParams.leftMargin
            else
                (viewWidth - padding.right - thumbWidth - popupLayoutParams.rightMargin - popupWidth)
            val popupAnchorY: Int = when (popupLayoutParams.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.LEFT -> 0
                Gravity.CENTER_HORIZONTAL -> popupHeight / 2
                Gravity.RIGHT -> popupHeight
                else -> 0
            }
            val thumbAnchorY: Int
            when (popupLayoutParams.gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                Gravity.TOP -> thumbAnchorY = thumbView.paddingTop
                Gravity.CENTER_VERTICAL -> {
                    val thumbPaddingTop = thumbView.paddingTop
                    thumbAnchorY = thumbPaddingTop + ((thumbHeight - thumbPaddingTop - thumbView.paddingBottom)) / 2
                }

                Gravity.BOTTOM -> thumbAnchorY = thumbHeight - thumbView.paddingBottom
                else -> thumbAnchorY = thumbView.paddingTop
            }
            val popupTop = MathUtils.clamp(
                thumbTop + thumbAnchorY - popupAnchorY,
                padding.top + popupLayoutParams.topMargin,
                viewHeight - padding.bottom - popupLayoutParams.bottomMargin - popupHeight
            )
            layoutView(
                popupView, popupLeft, popupTop, popupLeft + popupWidth,
                popupTop + popupHeight
            )
        }
    }

    private fun updateScrollbarState() {
        val scrollOffsetRange = this.scrollOffsetRange
        scrollbarEnabled = scrollOffsetRange > 0
        thumbOffset = if (scrollbarEnabled) (this.thumbOffsetRange.toLong() * viewHelper.scrollOffset / scrollOffsetRange).toInt() else 0
    }

    private fun layoutView(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        val scrollX = view.scrollX
        val scrollY = view.scrollY
        view.layout(scrollX + left, scrollY + top, scrollX + right, scrollY + bottom)
    }

    private fun onScrollChanged() {
        updateScrollbarState()
        if (!scrollbarEnabled) {
            return
        }

        animationHelper.showScrollbar(trackView, thumbView)
        postAutoHideScrollbar()
    }

    private fun onTouchEvent(event: MotionEvent): Boolean {
        if (!scrollbarEnabled) {
            return false
        }

        val eventX = event.x
        val eventY = event.y
        val padding = this.padding!!
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = eventX
                downY = eventY

                if (thumbView.alpha > 0 && isInViewTouchTarget(thumbView, eventX, eventY)) {
                    dragStartY = eventY
                    dragStartThumbOffset = thumbOffset
                    setDragging(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging && isInViewTouchTarget(trackView, downX, downY)
                    && abs(eventY - downY) > touchSlop
                ) {
                    if (isInViewTouchTarget(thumbView, downX, downY)) {
                        dragStartY = lastY
                        dragStartThumbOffset = thumbOffset
                    } else {
                        dragStartY = eventY
                        dragStartThumbOffset = (eventY - padding.top - thumbHeight / 2f).toInt()
                        scrollToThumbOffset(dragStartThumbOffset)
                    }
                    setDragging(true)
                }

                if (isDragging) {
                    val thumbOffset = dragStartThumbOffset + (eventY - dragStartY).toInt()
                    scrollToThumbOffset(thumbOffset)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setDragging(false)
        }

        lastY = eventY

        return isDragging
    }

    private fun isInView(view: View, x: Float, y: Float): Boolean {
        val scrollX = view.scrollX
        val scrollY = view.scrollY
        return x >= view.left - scrollX && x < view.right - scrollX && y >= view.top - scrollY && y < view.bottom - scrollY
    }

    private fun isInViewTouchTarget(view: View, x: Float, y: Float): Boolean {
        val scrollX = view.scrollX
        val scrollY = view.scrollY
        return isInTouchTarget(
            x, view.left - scrollX, view.right - scrollX, 0,
            view.width
        )
                && isInTouchTarget(
            y, view.top - scrollY, view.bottom - scrollY, 0,
            view.height
        )
    }

    private fun isInTouchTarget(position: Float, viewStart: Int, viewEnd: Int, parentStart: Int, parentEnd: Int): Boolean {
        // 1. Calculate how much extra space we need to reach the 20dp target
        val viewSize = viewEnd - viewStart
        val paddingNeeded = if (viewSize < minTouchTargetSize) (minTouchTargetSize - viewSize) / 2 else 0

        // 2. Expand the bounds
        val expandedStart = viewStart - paddingNeeded
        val expandedEnd = viewEnd + paddingNeeded

        // 3. Return true if touch is within these expanded bounds
        return position >= expandedStart && position < expandedEnd
    }

    private fun scrollToThumbOffset(thumbOffset: Int) {
        var thumbOffset = thumbOffset
        val thumbOffsetRange = this.thumbOffsetRange
        thumbOffset = MathUtils.clamp(thumbOffset, 0, thumbOffsetRange)
        val scrollOffset =
            (this.scrollOffsetRange.toLong() * thumbOffset / thumbOffsetRange).toInt()
        viewHelper.scrollTo(scrollOffset)
    }

    private val scrollOffsetRange: Int get() = viewHelper.scrollRange - view.height

    private val thumbOffsetRange: Int get() {
        val padding = this.padding!!
        return view.height - padding.top - padding.bottom - thumbHeight
    }

    private fun setDragging(dragging: Boolean) {
        if (isDragging == dragging) {
            return
        }
        isDragging = dragging

        if (isDragging) {
            view.parent.requestDisallowInterceptTouchEvent(true)
        }

        trackView.isPressed = isDragging
        thumbView.isPressed = isDragging

        if (isDragging) {
            cancelAutoHideScrollbar()
            animationHelper.showScrollbar(trackView, thumbView)
            animationHelper.showPopup(popupView)
        } else {
            postAutoHideScrollbar()
            animationHelper.hidePopup(popupView)
        }
    }

    private fun postAutoHideScrollbar() {
        cancelAutoHideScrollbar()
        if (animationHelper.isScrollbarAutoHideEnabled) {
            view.postDelayed(
                autoHideScrollbarRunnable,
                this@FastScroller.animationHelper.scrollbarAutoHideDelayMillis.toLong()
            )
        }
    }

    private fun autoHideScrollbar() {
        if (isDragging) {
            return
        }
        animationHelper.hideScrollbar(trackView, thumbView)
    }

    private fun cancelAutoHideScrollbar() {
        view.removeCallbacks(autoHideScrollbarRunnable)
    }

    interface ViewHelper {
        fun addOnPreDrawListener(onPreDraw: Runnable)

        fun addOnScrollChangedListener(onScrollChanged: Runnable)

        fun addOnTouchEventListener(onTouchEvent: (MotionEvent) -> Boolean)

        val scrollRange: Int

        val scrollOffset: Int

        fun scrollTo(offset: Int)

        val popupText: CharSequence? get() = null
    }

    interface AnimationHelper {
        fun showScrollbar(trackView: View, thumbView: View)

        fun hideScrollbar(trackView: View, thumbView: View)

        val isScrollbarAutoHideEnabled: Boolean

        val scrollbarAutoHideDelayMillis: Int

        fun showPopup(popupView: View)

        fun hidePopup(popupView: View)
    }

    companion object {
        private fun requireNonNegative(value: Int, message: String): Int {
            require(value >= 0) { message }
            return value
        }
    }

}
