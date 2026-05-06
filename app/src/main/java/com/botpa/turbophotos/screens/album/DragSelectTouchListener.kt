package com.botpa.turbophotos.screens.album

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.botpa.turbophotos.util.Orion
import kotlin.math.max
import kotlin.math.min

class DragSelectTouchListener(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val startOffset: Int = 0,
    private val onSelectRange: (from: Int, to: Int, min: Int, max: Int) -> Unit,
    private val onSingleTap: (position: Int) -> Unit,
    private val onDragSelectingChanged: (isDragSelecting: Boolean) -> Unit
) : RecyclerView.SimpleOnItemTouchListener() {

    //Touch
    private var lastX = 0f
    private var lastY = 0f
    private var isScrollbarTap = false

    //Drag selecting
    private var isDragSelecting = false
    private var startPosition = RecyclerView.NO_POSITION
    private var lastPosition = RecyclerView.NO_POSITION
    private var minPosition = RecyclerView.NO_POSITION
    private var maxPosition = RecyclerView.NO_POSITION

    //Automatic scrolling when touching the border
    private var maxScrollSpeed = 2000f  //Pixels per second
    private var currentScrollSpeed = 0  //Pixels per second
    private val scrollHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            //Get delta based on screen refresh rate
            val delta = 1 / context.display.refreshRate

            //Check if should scroll
            if (currentScrollSpeed != 0) {
                //Scroll
                recyclerView.scrollBy(0, (currentScrollSpeed * delta).toInt())

                //Update selection with current touch position
                updateSelection()
            }

            //Continue loop
            scrollHandler.postDelayed(this, (delta * 1000).toLong())
        }
    }


    //Gestures
    private val gestureDetector = GestureDetector(recyclerView.context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onLongPress(event: MotionEvent) {
            //Check if is scrollbar tap
            if (isScrollbarTap) return

            //Update touch info
            lastX = event.x
            lastY = event.y

            //Get hovered view position
            val pos = getHoveredViewPosition(lastX, lastY)
            if (pos < startOffset) return

            //Start drag selection
            setIsDragSelecting(true)

            //Reset positions
            startPosition = pos
            lastPosition = pos
            minPosition = pos
            maxPosition = pos

            //Select first item
            onSelectRange(
                startPosition - startOffset,
                lastPosition - startOffset,
                minPosition - startOffset,
                maxPosition - startOffset
            )
        }

        override fun onSingleTapUp(event: MotionEvent): Boolean {
            //Update touch info
            lastX = event.x
            lastY = event.y

            //Get hovered view position
            val pos = getHoveredViewPosition(lastX, lastY)
            if (pos < startOffset) return true

            //Single tap
            onSingleTap(pos - startOffset)
            return true
        }

    })

    override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent): Boolean {
        //Save if touching scrollbar
        isScrollbarTap = isTouchOnScrollbar(event)

        //Handle touch events
        gestureDetector.onTouchEvent(event)

        //Check if drag selecting
        if (isDragSelecting) {
            //Drag selecting -> Steal all events until the finger is lifted
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                //Stop touching -> Stop drag selecting
                setIsDragSelecting(false)
            }
            return true
        }
        return false
    }

    override fun onTouchEvent(reciclerView: RecyclerView, event: MotionEvent) {
        //Check if drag selecting
        if (!isDragSelecting) return

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                //Update touch info
                lastX = event.x
                lastY = event.y

                //Scroll if touching borders
                updateScrollSpeed(lastY)

                //Update selection with current touch position
                updateSelection()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setIsDragSelecting(false)
            }
        }
    }

    //Helpers
    private fun setIsDragSelecting(newIsDragSelecting: Boolean) {
        //Toggle selecting
        isDragSelecting = newIsDragSelecting

        //Toggle autoscroll when dragging on a border
        if (isDragSelecting) {
            scrollHandler.post(autoScrollRunnable)
        } else {
            currentScrollSpeed = 0
            scrollHandler.removeCallbacks(autoScrollRunnable)
        }

        //Notify selecting changed
        onDragSelectingChanged(isDragSelecting)
    }

    private fun updateSelection() {
        //Get hovered view position
        val pos = getHoveredViewPosition(lastX, lastY)
        if (pos < startOffset || pos == lastPosition) return

        //Update positions
        lastPosition = pos
        minPosition = min(minPosition, pos)
        maxPosition = max(maxPosition, pos)

        //Select range
        onSelectRange(
            min(startPosition, lastPosition) - startOffset,
            max(startPosition, lastPosition) - startOffset,
            minPosition - startOffset,
            maxPosition - startOffset
        )
    }

    private fun updateScrollSpeed(y: Float) {
        //Get info
        val height = recyclerView.height
        val scrollAreaHeight = height / 5

        //Check scroll
        if (y < scrollAreaHeight) {
            //Calculate percentage towards top
            val percent = (scrollAreaHeight - y) / scrollAreaHeight

            //Calculate scroll speed
            currentScrollSpeed = Orion.lerp(0f, -maxScrollSpeed, percent).toInt()
        } else if (y > height - scrollAreaHeight) {
            //Calculate percentage towards bot
            val percent = (y - height + scrollAreaHeight) / scrollAreaHeight

            //Calculate scroll speed
            currentScrollSpeed = Orion.lerp(0f, maxScrollSpeed, percent).toInt()
        } else {
            //Reset scroll speed
            currentScrollSpeed = 0
        }
    }

    private fun isTouchOnScrollbar(event: MotionEvent): Boolean {
        val touchTargetWidth = (48 * context.resources.displayMetrics.density).toInt() //48dp is the standard accessible touch target size
        val viewWidth = recyclerView.width

        return if (recyclerView.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL) {
            event.x <= touchTargetWidth
        } else {
            event.x >= viewWidth - touchTargetWidth
        }
    }

    private fun getHoveredViewPosition(x: Float, y: Float): Int {
        //Get hovered view
        val view = recyclerView.findChildViewUnder(lastX, lastY) ?: return -1

        //Get view position
        return recyclerView.getChildAdapterPosition(view)
    }

}