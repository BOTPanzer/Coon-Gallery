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

class FastScroller(view: ViewGroup, viewHelper: ViewHelper, padding: Rect?, trackDrawable: Drawable, thumbDrawable: Drawable, animationHelper: AnimationHelper) {

    private val mMinTouchTargetSize = (20 * view.resources.displayMetrics.density).toInt()
    private val mTouchSlop: Int

    private val mView: ViewGroup
    private val mViewHelper: ViewHelper
    private var mUserPadding: Rect?
    private val mAnimationHelper: AnimationHelper

    private val mTrackWidth: Int
    private val mThumbWidth: Int
    private val mThumbHeight: Int

    private val mTrackView: View
    private val mThumbView: View
    private val mPopupView: TextView

    private var mScrollbarEnabled = false
    private var mThumbOffset = 0

    private var mDownX = 0f
    private var mDownY = 0f
    private var mLastY = 0f
    private var mDragStartY = 0f
    private var mDragStartThumbOffset = 0
    private var mDragging = false

    private val mAutoHideScrollbarRunnable = Runnable { this.autoHideScrollbar() }

    private val mTempRect = Rect()

    init {
        val context = view.context
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        mView = view
        mViewHelper = viewHelper
        mUserPadding = padding
        mAnimationHelper = animationHelper

        mTrackWidth = requireNonNegative(trackDrawable.intrinsicWidth, "trackDrawable.getIntrinsicWidth() < 0")
        mThumbWidth = requireNonNegative(thumbDrawable.intrinsicWidth, "thumbDrawable.getIntrinsicWidth() < 0")
        mThumbHeight = requireNonNegative(thumbDrawable.intrinsicHeight, "thumbDrawable.getIntrinsicHeight() < 0")

        mTrackView = View(context)
        mTrackView.background = trackDrawable
        mThumbView = View(context)
        mThumbView.background = thumbDrawable
        mPopupView = AppCompatTextView(context)
        mPopupView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val overlay = mView.overlay
        overlay.add(mTrackView)
        overlay.add(mThumbView)
        overlay.add(mPopupView)

        postAutoHideScrollbar()
        mPopupView.alpha = 0f

        mViewHelper.addOnPreDrawListener { this.onPreDraw() }
        mViewHelper.addOnScrollChangedListener { this.onScrollChanged() }
        mViewHelper.addOnTouchEventListener { event -> this.onTouchEvent(event) }
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        if (mUserPadding != null && mUserPadding!!.left == left && mUserPadding!!.top == top && mUserPadding!!.right == right && mUserPadding!!.bottom == bottom) {
            return
        }
        if (mUserPadding == null) {
            mUserPadding = Rect()
        }
        mUserPadding!!.set(left, top, right, bottom)
        mView.invalidate()
    }

    private var padding: Rect?
        get() {
            if (mUserPadding != null) {
                mTempRect.set(mUserPadding!!)
            } else {
                mTempRect.set(mView.paddingLeft, mView.paddingTop, mView.paddingRight, mView.paddingBottom)
            }
            return mTempRect
        }
        set(padding) {
            if (mUserPadding == padding) {
                return
            }
            if (padding != null) {
                if (mUserPadding == null) {
                    mUserPadding = Rect()
                }
                mUserPadding!!.set(padding)
            } else {
                mUserPadding = null
            }
            mView.invalidate()
        }

    private fun onPreDraw() {
        updateScrollbarState()
        mTrackView.visibility = if (mScrollbarEnabled) View.VISIBLE else View.INVISIBLE
        mThumbView.visibility = if (mScrollbarEnabled) View.VISIBLE else View.INVISIBLE
        if (!mScrollbarEnabled) {
            mPopupView.visibility = View.INVISIBLE
            return
        }

        val layoutDirection = mView.layoutDirection
        mTrackView.layoutDirection = layoutDirection
        mThumbView.layoutDirection = layoutDirection
        mPopupView.layoutDirection = layoutDirection

        val isLayoutRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
        val viewWidth = mView.width
        val viewHeight = mView.height

        val padding = this.padding!!
        val trackLeft = if (isLayoutRtl) padding.left else viewWidth - padding.right - mTrackWidth
        layoutView(mTrackView, trackLeft, padding.top, trackLeft + mTrackWidth, max(viewHeight - padding.bottom, padding.top))
        val thumbLeft = if (isLayoutRtl) padding.left else viewWidth - padding.right - mThumbWidth
        val thumbTop = padding.top + mThumbOffset
        layoutView(mThumbView, thumbLeft, thumbTop, thumbLeft + mThumbWidth, thumbTop + mThumbHeight)

        val popupText = mViewHelper.popupText
        val hasPopup = !TextUtils.isEmpty(popupText)
        mPopupView.visibility = if (hasPopup) View.VISIBLE else View.INVISIBLE
        if (hasPopup) {
            val popupLayoutParams = mPopupView.layoutParams as FrameLayout.LayoutParams
            if (mPopupView.text != popupText) {
                mPopupView.text = popupText
                val widthMeasureSpec = ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY), (padding.left + padding.right + mThumbWidth + popupLayoutParams.leftMargin + popupLayoutParams.rightMargin), popupLayoutParams.width)
                val heightMeasureSpec = ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(viewHeight, View.MeasureSpec.EXACTLY), (padding.top + padding.bottom + popupLayoutParams.topMargin + popupLayoutParams.bottomMargin), popupLayoutParams.height)
                mPopupView.measure(widthMeasureSpec, heightMeasureSpec)
            }
            val popupWidth = mPopupView.measuredWidth
            val popupHeight = mPopupView.measuredHeight
            val popupLeft = if (isLayoutRtl)
                padding.left + mThumbWidth + popupLayoutParams.leftMargin
            else
                (viewWidth - padding.right - mThumbWidth - popupLayoutParams.rightMargin - popupWidth)
            val popupAnchorY: Int = when (popupLayoutParams.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.LEFT -> 0
                Gravity.CENTER_HORIZONTAL -> popupHeight / 2
                Gravity.RIGHT -> popupHeight
                else -> 0
            }
            val thumbAnchorY: Int
            when (popupLayoutParams.gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                Gravity.TOP -> thumbAnchorY = mThumbView.getPaddingTop()
                Gravity.CENTER_VERTICAL -> {
                    val thumbPaddingTop = mThumbView.getPaddingTop()
                    thumbAnchorY = thumbPaddingTop + ((mThumbHeight - thumbPaddingTop
                            - mThumbView.getPaddingBottom())) / 2
                }

                Gravity.BOTTOM -> thumbAnchorY = mThumbHeight - mThumbView.getPaddingBottom()
                else -> thumbAnchorY = mThumbView.getPaddingTop()
            }
            val popupTop = MathUtils.clamp(
                thumbTop + thumbAnchorY - popupAnchorY,
                padding.top + popupLayoutParams.topMargin,
                viewHeight - padding.bottom - popupLayoutParams.bottomMargin - popupHeight
            )
            layoutView(
                mPopupView, popupLeft, popupTop, popupLeft + popupWidth,
                popupTop + popupHeight
            )
        }
    }

    private fun updateScrollbarState() {
        val scrollOffsetRange = this.scrollOffsetRange
        mScrollbarEnabled = scrollOffsetRange > 0
        mThumbOffset = if (mScrollbarEnabled) (this.thumbOffsetRange.toLong() * mViewHelper.scrollOffset / scrollOffsetRange).toInt() else 0
    }

    private fun layoutView(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        val scrollX = mView.scrollX
        val scrollY = mView.scrollY
        view.layout(scrollX + left, scrollY + top, scrollX + right, scrollY + bottom)
    }

    private fun onScrollChanged() {
        updateScrollbarState()
        if (!mScrollbarEnabled) {
            return
        }

        mAnimationHelper.showScrollbar(mTrackView, mThumbView)
        postAutoHideScrollbar()
    }

    private fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mScrollbarEnabled) {
            return false
        }

        val eventX = event.x
        val eventY = event.y
        val padding = this.padding!!
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = eventX
                mDownY = eventY

                if (mThumbView.alpha > 0 && isInViewTouchTarget(mThumbView, eventX, eventY)) {
                    mDragStartY = eventY
                    mDragStartThumbOffset = mThumbOffset
                    setDragging(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!mDragging && isInViewTouchTarget(mTrackView, mDownX, mDownY)
                    && abs(eventY - mDownY) > mTouchSlop
                ) {
                    if (isInViewTouchTarget(mThumbView, mDownX, mDownY)) {
                        mDragStartY = mLastY
                        mDragStartThumbOffset = mThumbOffset
                    } else {
                        mDragStartY = eventY
                        mDragStartThumbOffset = (eventY - padding.top - mThumbHeight / 2f).toInt()
                        scrollToThumbOffset(mDragStartThumbOffset)
                    }
                    setDragging(true)
                }

                if (mDragging) {
                    val thumbOffset = mDragStartThumbOffset + (eventY - mDragStartY).toInt()
                    scrollToThumbOffset(thumbOffset)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setDragging(false)
        }

        mLastY = eventY

        return mDragging
    }

    private fun isInView(view: View, x: Float, y: Float): Boolean {
        val scrollX = mView.scrollX
        val scrollY = mView.scrollY
        return x >= view.left - scrollX && x < view.right - scrollX && y >= view.top - scrollY && y < view.bottom - scrollY
    }

    private fun isInViewTouchTarget(view: View, x: Float, y: Float): Boolean {
        val scrollX = mView.scrollX
        val scrollY = mView.scrollY
        return isInTouchTarget(
            x, view.left - scrollX, view.right - scrollX, 0,
            mView.width
        )
                && isInTouchTarget(
            y, view.top - scrollY, view.bottom - scrollY, 0,
            mView.height
        )
    }

    private fun isInTouchTarget(position: Float, viewStart: Int, viewEnd: Int, parentStart: Int, parentEnd: Int): Boolean {
        val viewSize = viewEnd - viewStart
        if (viewSize >= mMinTouchTargetSize) {
            return position >= viewStart && position < viewEnd
        }
        var touchTargetStart = viewStart - (mMinTouchTargetSize - viewSize) / 2
        if (touchTargetStart < parentStart) {
            touchTargetStart = parentStart
        }
        var touchTargetEnd = touchTargetStart + mMinTouchTargetSize
        if (touchTargetEnd > parentEnd) {
            touchTargetEnd = parentEnd
            touchTargetStart = touchTargetEnd - mMinTouchTargetSize
            if (touchTargetStart < parentStart) {
                touchTargetStart = parentStart
            }
        }
        return position >= touchTargetStart && position < touchTargetEnd
    }

    private fun scrollToThumbOffset(thumbOffset: Int) {
        var thumbOffset = thumbOffset
        val thumbOffsetRange = this.thumbOffsetRange
        thumbOffset = MathUtils.clamp(thumbOffset, 0, thumbOffsetRange)
        val scrollOffset =
            (this.scrollOffsetRange.toLong() * thumbOffset / thumbOffsetRange).toInt()
        mViewHelper.scrollTo(scrollOffset)
    }

    private val scrollOffsetRange: Int get() = mViewHelper.scrollRange - mView.height

    private val thumbOffsetRange: Int get() {
        val padding = this.padding!!
        return mView.height - padding.top - padding.bottom - mThumbHeight
    }

    private fun setDragging(dragging: Boolean) {
        if (mDragging == dragging) {
            return
        }
        mDragging = dragging

        if (mDragging) {
            mView.parent.requestDisallowInterceptTouchEvent(true)
        }

        mTrackView.isPressed = mDragging
        mThumbView.isPressed = mDragging

        if (mDragging) {
            cancelAutoHideScrollbar()
            mAnimationHelper.showScrollbar(mTrackView, mThumbView)
            mAnimationHelper.showPopup(mPopupView)
        } else {
            postAutoHideScrollbar()
            mAnimationHelper.hidePopup(mPopupView)
        }
    }

    private fun postAutoHideScrollbar() {
        cancelAutoHideScrollbar()
        if (mAnimationHelper.isScrollbarAutoHideEnabled) {
            mView.postDelayed(
                mAutoHideScrollbarRunnable,
                mAnimationHelper.scrollbarAutoHideDelayMillis.toLong()
            )
        }
    }

    private fun autoHideScrollbar() {
        if (mDragging) {
            return
        }
        mAnimationHelper.hideScrollbar(mTrackView, mThumbView)
    }

    private fun cancelAutoHideScrollbar() {
        mView.removeCallbacks(mAutoHideScrollbarRunnable)
    }

    interface ViewHelper {
        fun addOnPreDrawListener(onPreDraw: Runnable)

        fun addOnScrollChangedListener(onScrollChanged: Runnable)

        fun addOnTouchEventListener(onTouchEvent: (MotionEvent) -> Boolean)

        val scrollRange: Int

        val scrollOffset: Int

        fun scrollTo(offset: Int)

        val popupText: CharSequence?
            get() = null
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
