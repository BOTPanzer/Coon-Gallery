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
import androidx.core.view.isEmpty

internal class FastScrollerRecyclerViewHelper(private val view: RecyclerView) : ViewHelper {

    private val tempRect = Rect()
    var hasHeader: Boolean = false

    private var cachedHeaderHeight: Int = -1
    private var cachedItemHeight: Int = -1

    override val scrollRange: Int get() {
        val itemCount = this.itemCount
        if (itemCount == 0) {
            return 0
        }
        val regularHeight = this.itemHeight
        if (regularHeight == 0) {
            return 0
        }

        return if (hasHeader) {
            val firstHeight = this.headerHeight.let { if (it > 0) it else regularHeight }
            view.paddingTop + firstHeight + (itemCount - 1) * regularHeight + view.paddingBottom
        } else {
            view.paddingTop + itemCount * regularHeight + view.paddingBottom
        }
    }

    override val scrollOffset: Int get() {
        val firstRowPosition = this.firstItemPosition
        if (firstRowPosition == RecyclerView.NO_POSITION) {
            return 0
        }
        val regularHeight = this.itemHeight
        val firstItemTop = this.firstItemOffset

        return if (hasHeader) {
            val firstHeight = this.headerHeight.let { if (it > 0) it else regularHeight }
            if (firstRowPosition == 0) {
                view.paddingTop - firstItemTop
            } else {
                firstHeight + (firstRowPosition - 1) * regularHeight + (view.paddingTop - firstItemTop)
            }
        } else {
            firstRowPosition * regularHeight + (view.paddingTop - firstItemTop)
        }
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
        var remainingOffset = offset
        view.stopScroll()

        val regularHeight = this.itemHeight
        if (regularHeight == 0) return

        if (hasHeader) {
            val firstHeight = this.headerHeight.let { if (it > 0) it else regularHeight }
            if (remainingOffset < firstHeight) {
                scrollToPositionWithOffset(0, view.paddingTop - remainingOffset)
            } else {
                remainingOffset -= firstHeight
                val row = 1 + remainingOffset / regularHeight
                val itemOffset = view.paddingTop - (remainingOffset % regularHeight)
                scrollToRowWithOffset(row, itemOffset)
            }
        } else {
            val row = remainingOffset / regularHeight
            val itemOffset = view.paddingTop - (remainingOffset % regularHeight)
            scrollToRowWithOffset(row, itemOffset)
        }
    }

    private fun scrollToRowWithOffset(row: Int, itemOffset: Int) {
        var position = row
        val layoutManager = this.verticalLinearLayoutManager
        if (layoutManager is GridLayoutManager) {
            val lookup = layoutManager.spanSizeLookup
            val spanCount = layoutManager.spanCount

            // Find first position that is in 'row' using binary search
            var low = 0
            var high = layoutManager.itemCount - 1
            while (low <= high) {
                val mid = (low + high) / 2
                val midRow = lookup.getSpanGroupIndex(mid, spanCount)
                if (midRow < row) {
                    low = mid + 1
                } else if (midRow > row) {
                    high = mid - 1
                } else {
                    var p = mid
                    while (p > 0 && lookup.getSpanGroupIndex(p - 1, spanCount) == row) {
                        p--
                    }
                    position = p
                    break
                }
            }
            if (low > high) position = low
        }
        scrollToPositionWithOffset(position, itemOffset)
    }

    private val itemCount: Int get() {
        val layoutManager = this.verticalLinearLayoutManager ?: return 0
        val count = layoutManager.itemCount
        if (count == 0) return 0
        if (layoutManager is GridLayoutManager) {
            return layoutManager.spanSizeLookup.getSpanGroupIndex(count - 1, layoutManager.spanCount) + 1
        }
        return count
    }

    private val itemHeight: Int get() {
        if (view.isEmpty()) return if (cachedItemHeight > 0) cachedItemHeight else 0
        val childCount = view.childCount
        val minPos = if (hasHeader) 1 else 0
        for (i in 0 until childCount) {
            val child = view.getChildAt(i)
            val pos = view.getChildAdapterPosition(child)
            if (pos >= minPos && pos != RecyclerView.NO_POSITION) {
                view.getDecoratedBoundsWithMargins(child, tempRect)
                cachedItemHeight = tempRect.height()
                return cachedItemHeight
            }
        }
        return if (cachedItemHeight > 0) cachedItemHeight else 0
    }

    private val headerHeight: Int get() {
        if (!hasHeader) return 0
        if (view.isEmpty()) return if (cachedHeaderHeight > 0) cachedHeaderHeight else 0
        val childCount = view.childCount
        for (i in 0 until childCount) {
            val child = view.getChildAt(i)
            val pos = view.getChildAdapterPosition(child)
            if (pos == 0) {
                view.getDecoratedBoundsWithMargins(child, tempRect)
                cachedHeaderHeight = tempRect.height()
                return cachedHeaderHeight
            }
        }
        return if (cachedHeaderHeight > 0) cachedHeaderHeight else 0
    }

    private val firstItemPosition: Int get() {
        val position = this.firstItemAdapterPosition
        if (position == RecyclerView.NO_POSITION) return RecyclerView.NO_POSITION
        val layoutManager = this.verticalLinearLayoutManager ?: return RecyclerView.NO_POSITION
        if (layoutManager is GridLayoutManager) {
            return layoutManager.spanSizeLookup.getSpanGroupIndex(position, layoutManager.spanCount)
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
        val linearLayoutManager = this.verticalLinearLayoutManager ?: return
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        linearLayoutManager.scrollToPositionWithOffset(position, offset - view.paddingTop)
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
