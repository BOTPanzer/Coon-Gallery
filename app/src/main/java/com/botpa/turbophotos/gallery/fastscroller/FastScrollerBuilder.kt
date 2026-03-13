package com.botpa.turbophotos.gallery.fastscroller

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.gallery.fastscroller.FastScroller.AnimationHelper
import com.botpa.turbophotos.gallery.fastscroller.FastScroller.ViewHelper

class FastScrollerBuilder(private val mView: ViewGroup) {

    private var mViewHelper: ViewHelper? = null

    private var mPadding: Rect? = null

    private var mTrackDrawable: Drawable? = null

    private var mThumbDrawable: Drawable? = null

    private var mAnimationHelper: AnimationHelper? = null

    fun setViewHelper(viewHelper: ViewHelper?): FastScrollerBuilder {
        mViewHelper = viewHelper
        return this
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int): FastScrollerBuilder {
        if (mPadding == null) {
            mPadding = Rect()
        }
        mPadding!!.set(left, top, right, bottom)
        return this
    }

    fun setPadding(padding: Rect?): FastScrollerBuilder {
        if (padding != null) {
            if (mPadding == null) {
                mPadding = Rect()
            }
            mPadding!!.set(padding)
        } else {
            mPadding = null
        }
        return this
    }

    fun setTrackDrawable(trackDrawable: Drawable): FastScrollerBuilder {
        mTrackDrawable = trackDrawable
        return this
    }

    fun setThumbDrawable(thumbDrawable: Drawable): FastScrollerBuilder {
        mThumbDrawable = thumbDrawable
        return this
    }

    fun setAnimationHelper(animationHelper: AnimationHelper?) {
        mAnimationHelper = animationHelper
    }

    fun build(): FastScroller {
        return FastScroller(
            mView,
            this.getOrCreateViewHelper, mPadding, mTrackDrawable!!, mThumbDrawable!!,
            this.getOrCreateAnimationHelper
        )
    }

    private val getOrCreateViewHelper: ViewHelper get() {
        if (mViewHelper != null) {
            return mViewHelper!!
        }
        return when (mView) {
            is RecyclerView -> {
                RecyclerViewHelper(mView)
            }
            else -> {
                throw UnsupportedOperationException(mView.javaClass.simpleName + " is not supported for fast scroll")
            }
        }
    }

    private val getOrCreateAnimationHelper: AnimationHelper get() {
        if (mAnimationHelper != null) {
            return mAnimationHelper!!
        }
        return DefaultAnimationHelper(mView)
    }

}
