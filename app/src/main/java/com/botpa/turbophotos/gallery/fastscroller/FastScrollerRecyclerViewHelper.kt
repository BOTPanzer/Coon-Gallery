package com.botpa.turbophotos.gallery.fastscroller

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.botpa.turbophotos.gallery.fastscroller.FastScroller.ViewHelper
import kotlin.math.max
import androidx.core.view.isEmpty

internal class FastScrollerRecyclerViewHelper(private val view: RecyclerView) : ViewHelper {

    private val tempRect = Rect()

    override val scrollRange: Int get() {
        val itemCount = this.itemCount
        if (itemCount == 0) {
            return 0
        }
        val itemHeight = this.itemHeight
        if (itemHeight == 0) {
            return 0
        }
        return view.paddingTop + itemCount * itemHeight + view.paddingBottom
    }

    override val scrollOffset: Int get() {
        val firstItemPosition = this.firstItemPosition
        if (firstItemPosition == RecyclerView.NO_POSITION) {
            return 0
        }
        val itemHeight = this.itemHeight
        val firstItemTop = this.firstItemOffset
        return view.paddingTop + firstItemPosition * itemHeight - firstItemTop
    }

    override fun addOnPreDrawListener(onPreDraw: Runnable) {
        view.addItemDecoration(object : ItemDecoration() {
            override fun onDraw(
                canvas: Canvas, parent: RecyclerView,
                state: RecyclerView.State
            ) {
                onPreDraw.run()
            }
        })
    }

    override fun addOnScrollChangedListener(onScrollChanged: Runnable) {
        view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrollChanged.run()
            }
        })
    }

    override fun addOnTouchEventListener(onTouchEvent: (MotionEvent) -> Boolean) {
        view.addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(
                recyclerView: RecyclerView,
                event: MotionEvent
            ): Boolean {
                return onTouchEvent.invoke(event)
            }

            override fun onTouchEvent(
                recyclerView: RecyclerView,
                event: MotionEvent
            ) {
                onTouchEvent.invoke(event)
            }
        })
    }

    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        var offset = offset
        view.stopScroll()
        offset -= view.paddingTop
        val itemHeight = this.itemHeight
        // firstItemPosition should be non-negative even if paddingTop is greater than item height.
        val firstItemPosition = max(0, offset / itemHeight)
        val firstItemTop = firstItemPosition * itemHeight - offset
        scrollToPositionWithOffset(firstItemPosition, firstItemTop)
    }

    private val itemCount: Int get() {
        val linearLayoutManager = this.verticalLinearLayoutManager ?: return 0
        var itemCount = linearLayoutManager.getItemCount()
        if (itemCount == 0) return 0
        if (linearLayoutManager is GridLayoutManager) {
            itemCount = (itemCount - 1) / linearLayoutManager.spanCount + 1
        }
        return itemCount
    }

    private val itemHeight: Int get() {
        if (view.isEmpty()) return 0
        val itemView = view.getChildAt(0)
        view.getDecoratedBoundsWithMargins(itemView, tempRect)
        return tempRect.height()
    }

    private val firstItemPosition: Int get() {
        var position = this.firstItemAdapterPosition
        val linearLayoutManager = this.verticalLinearLayoutManager ?: return RecyclerView.NO_POSITION
        if (linearLayoutManager is GridLayoutManager) {
            position /= linearLayoutManager.spanCount
        }
        return position
    }

    private val firstItemAdapterPosition: Int get() {
        if (view.isEmpty()) return RecyclerView.NO_POSITION
        val itemView = view.getChildAt(0)
        val linearLayoutManager = this.verticalLinearLayoutManager ?: return RecyclerView.NO_POSITION
        return linearLayoutManager.getPosition(itemView)
    }

    private val firstItemOffset: Int get() {
        if (view.isEmpty()) return RecyclerView.NO_POSITION
        val itemView = view.getChildAt(0)
        view.getDecoratedBoundsWithMargins(itemView, tempRect)
        return tempRect.top
    }

    private fun scrollToPositionWithOffset(position: Int, offset: Int) {
        var position = position
        var offset = offset
        val linearLayoutManager = this.verticalLinearLayoutManager ?: return
        if (linearLayoutManager is GridLayoutManager) {
            position *= linearLayoutManager.spanCount
        }
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        offset -= view.paddingTop
        linearLayoutManager.scrollToPositionWithOffset(position, offset)
    }

    private val verticalLinearLayoutManager: LinearLayoutManager? get() {
        val layoutManager = view.layoutManager
        if (layoutManager !is LinearLayoutManager) {
            return null
        }
        if (layoutManager.orientation != RecyclerView.VERTICAL) {
            return null
        }
        return layoutManager
    }

}
