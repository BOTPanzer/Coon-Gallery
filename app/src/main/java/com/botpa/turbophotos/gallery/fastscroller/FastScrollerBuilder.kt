package com.botpa.turbophotos.gallery.fastscroller

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.R
import com.botpa.turbophotos.gallery.fastscroller.FastScroller.AnimationHelper
import com.botpa.turbophotos.gallery.fastscroller.FastScroller.ViewHelper

class FastScrollerBuilder(private val view: ViewGroup) {

    private var viewHelper: ViewHelper? = null
    private var padding: Rect? = null
    private var trackDrawable: Drawable = ContextCompat.getDrawable(view.context, R.drawable.scrollbar_track)!!
    private var thumbDrawable: Drawable = ContextCompat.getDrawable(view.context, R.drawable.scrollbar_thumb)!!
    private var animationHelper: AnimationHelper? = null


    fun setViewHelper(viewHelper: ViewHelper?): FastScrollerBuilder {
        this.viewHelper = viewHelper
        return this
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int): FastScrollerBuilder {
        if (padding == null) {
            padding = Rect()
        }
        padding!!.set(left, top, right, bottom)
        return this
    }

    fun setPadding(padding: Rect?): FastScrollerBuilder {
        if (padding != null) {
            if (this.padding == null) {
                this.padding = Rect()
            }
            this.padding!!.set(padding)
        } else {
            this.padding = null
        }
        return this
    }

    fun setTrackDrawable(trackDrawable: Drawable): FastScrollerBuilder {
        this.trackDrawable = trackDrawable
        return this
    }

    fun setThumbDrawable(thumbDrawable: Drawable): FastScrollerBuilder {
        this.thumbDrawable = thumbDrawable
        return this
    }

    fun setAnimationHelper(animationHelper: AnimationHelper?) {
        this.animationHelper = animationHelper
    }

    fun build(): FastScroller {
        return FastScroller(
            view,
            this.getOrCreateViewHelper,
            padding,
            trackDrawable,
            thumbDrawable,
            this.getOrCreateAnimationHelper
        )
    }

    private val getOrCreateViewHelper: ViewHelper get() {
        if (viewHelper != null) return viewHelper!!

        return if (view is RecyclerView) {
            FastScrollerRecyclerViewHelper(view)
        } else {
            throw UnsupportedOperationException(view.javaClass.simpleName + " is not supported for fast scroll")
        }
    }

    private val getOrCreateAnimationHelper: AnimationHelper get() {
        if (animationHelper != null) return animationHelper!!

        return FastScrollerAnimationHelper(view)
    }

}
