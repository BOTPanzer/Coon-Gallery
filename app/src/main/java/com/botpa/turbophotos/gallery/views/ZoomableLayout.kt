package com.botpa.turbophotos.gallery.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.botpa.turbophotos.util.Orion
import kotlin.math.max
import kotlin.math.min
import androidx.core.view.isNotEmpty
import kotlin.math.roundToInt

open class ZoomableLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    //Components
    private var clickHandler: MultiClickHandler = MultiClickHandler()
    private var transformHandler: ZoomTransformHandler = ZoomTransformHandler()
    private var scaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var flingHandler: FlingHandler = FlingHandler()

    //Action modes
    private companion object {
        const val NONE: Int = 0
        const val TAP: Int = 1
        const val DRAG: Int = 2
        const val ZOOM: Int = 3
    }
    private var mode: Int = NONE

    //Touch info
    private var lastTouch: PointF = PointF()
    private var startTouch: PointF = PointF()
    private var swipeDistance: Int = ViewConfiguration.get(context).scaledTouchSlop

    var pointers: Int = 0
        private set

    var onPointersChanged: Runnable? = null

    //Drag
    private val minDragAmount: Float = 5f

    //Zoom
    val zoom get() = transformHandler.zoom

    var doubleTapZoomsToCustom: Boolean = false
    var doubleTapCustomZoom: Float = 2f


    //Constructor
    init {
        //Reduce delay for scaling detection
        scaleDetector.isQuickScaleEnabled = true

        //Set double-tap action
        clickHandler.onDefaultDoubleTapAction = {
            transformHandler.animateResize(
                if (transformHandler.isZoomedIn)
                    transformHandler.fitScale
                else if (doubleTapZoomsToCustom)
                    doubleTapCustomZoom
                else
                    transformHandler.coverScale
            )
        }
    }

    //Touch
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        //Touch event
        scaleDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        //Steal the event if zooming or dragging
        if (event.pointerCount > 1 || mode == ZOOM || mode == DRAG) return true

        //Check action
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                //First pointer down -> Reset state
                mode = NONE
                lastTouch.set(event.x, event.y)
                startTouch.set(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                //Intercept if zoomed & dragged beyond swipe distance
                if (transformHandler.isZoomedIn) {
                    val diff = PointF(event.x - startTouch.x, event.y - startTouch.y)
                    if (diff.x > swipeDistance || diff.y > swipeDistance) {
                        //Start dragging
                        mode = DRAG
                        return true
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //Pass event to velocity tracker
        flingHandler.addMovement(event)

        //Update pointers
        if (pointers != event.pointerCount) pointers = event.pointerCount
        onPointersChanged?.run()

        //Get current position
        val currentPosition = PointF(scaleDetector.focusX, scaleDetector.focusY)

        //Check action
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                //Change mode
                mode = if (pointers == 0) NONE else if (pointers == 1) TAP else ZOOM

                //Save position
                lastTouch.set(currentPosition)
                startTouch.set(lastTouch)

                //Stop active fling
                flingHandler.stop()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                //Change mode
                mode = if (pointers - 1 == 0) NONE else if (pointers - 1 == 1) DRAG else ZOOM

                //Find index of the pointer that will stay down (if 1 pointer remains)
                val actionIndex = event.actionIndex
                val remainingIndex = if (actionIndex == 0) 1 else 0

                //Save position
                if (event.pointerCount == 2) {
                    //Remaining finger's position
                    lastTouch.set(event.getX(remainingIndex), event.getY(remainingIndex))
                } else {
                    //Current position
                    lastTouch.set(currentPosition)
                }

                //Pointer up
                pointers--
                onPointersChanged?.run()
            }
            MotionEvent.ACTION_UP -> {
                //Check if clicked
                if (mode == TAP) {
                    //Clicked
                    performClick()
                } else if (mode == DRAG && transformHandler.isZoomedIn) {
                    //Start fling
                    flingHandler.startFling { deltaX, deltaY ->
                        val tempDelta = PointF(deltaX, deltaY)
                        val clampedDelta = transformHandler.constrainDragDelta(tempDelta)
                        transformHandler.matrix.postTranslate(clampedDelta.x, clampedDelta.y)
                        transformHandler.applyToChild()
                    }
                }

                //Reset mode
                mode = NONE

                //Pointer up
                pointers--
                onPointersChanged?.run()
            }
            MotionEvent.ACTION_MOVE -> {
                //Calculate movement delta
                val delta = PointF(currentPosition.x - lastTouch.x, currentPosition.y - lastTouch.y)

                //Check if changing from tap to drag or already zooming/dragging
                if (mode == TAP && delta.length() >= minDragAmount) {
                    //Start dragging
                    mode = DRAG

                    //Update last touch
                    lastTouch.set(currentPosition)
                } else if (mode == ZOOM || (mode == DRAG && transformHandler.isZoomedIn)) {
                    //Process move delta
                    val clampedDelta = transformHandler.constrainDragDelta(delta)

                    //Update & apply matrix
                    transformHandler.matrix.postTranslate(clampedDelta.x, clampedDelta.y)
                    transformHandler.applyToChild()

                    //Update last touch
                    lastTouch.set(currentPosition)
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        clickHandler.performClick(lastTouch.x, lastTouch.y)
        return super.performClick()
    }

    //Listeners
    fun setOnSingleClickListener(listener: Runnable?) {
        clickHandler.onSingleClick = listener
    }

    fun setOnMultiClickListener(listener: ((x: Float, y: Float, count: Int) -> Boolean)?) {
        clickHandler.onMultiClick = listener
    }

    fun setOnMultiClickFinishedListener(listener: ((count: Int) -> Unit)?) {
        clickHandler.onMultiClickFinished = listener
    }

    fun setOnZoomChangedListener(listener: () -> Unit) {
        transformHandler.onZoomChanged = listener
    }

    //Other
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        transformHandler.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        transformHandler.onLayout()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clickHandler.cancelPendingCallbacks()
        flingHandler.recycle()
    }

    //Helpers
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            transformHandler.onScale(detector)
            return true
        }

    }

    private inner class MultiClickHandler {

        //Config
        private val multiClickDelay: Long = 250
        private var lastClickTimestamp: Long = 0
        private var multiClickCount: Int = 0

        //Multi clicks
        private val finishMultiClickRunnable = Runnable {
            onMultiClickFinished?.invoke(multiClickCount + 1)
            multiClickCount = 0
        }

        //Listeners
        var onSingleClick: Runnable? = null
        var onMultiClick: ((x: Float, y: Float, count: Int) -> Boolean)? = null
        var onMultiClickFinished: ((count: Int) -> Unit)? = null
        var onDefaultDoubleTapAction: (() -> Unit)? = null


        //Actions
        fun performClick(x: Float, y: Float): Boolean {
            //Get handler
            val handler = handler ?: return false

            //Stop multi click finished runnable
            handler.removeCallbacks(finishMultiClickRunnable)

            //Get current timestamp
            val currentTimestamp = System.currentTimeMillis()

            //Check if its multi click
            if (currentTimestamp - lastClickTimestamp > multiClickDelay) {
                //First click

                //Reset multi click count
                multiClickCount = 0

                //Run click runnable
                onSingleClick?.let { handler.postDelayed(it, multiClickDelay) }

                //Save timestamp
                lastClickTimestamp = currentTimestamp
            } else {
                //Multi click

                //Increase multi click count
                multiClickCount++

                //Stop click runnable & wait for multi click finished
                onSingleClick?.let { handler.removeCallbacks(it) }
                handler.postDelayed(finishMultiClickRunnable, multiClickDelay)

                //Perform multi click
                val continueMultiClick = onMultiClick?.invoke(x, y, multiClickCount) ?: false
                if (continueMultiClick) {
                    //Save timestamp
                    lastClickTimestamp = currentTimestamp
                } else {
                    //Reset timestamp
                    lastClickTimestamp = 0

                    //Call multi click finished
                    onMultiClickFinished?.invoke(multiClickCount)
                    multiClickCount = 0

                    //Trigger double-tap action
                    onDefaultDoubleTapAction?.invoke()
                }
            }
            return true
        }

        fun cancelPendingCallbacks() {
            //Get handler
            val handler = handler ?: return

            //Cancel callbacks
            handler.removeCallbacks(finishMultiClickRunnable)
            onSingleClick?.let { handler.removeCallbacks(it) }
        }

    }

    private inner class ZoomTransformHandler {

        private val matrixValues = FloatArray(9)
        val matrix: Matrix = Matrix()

        //Content
        private val child: View? get() = if (isNotEmpty()) getChildAt(0) else null

        //Sizes
        val viewSize = PointF()
        val contentSize = PointF()
        val originalSize = PointF()
        val originalSpace = PointF()
        val margin = PointF()

        var fitScale = 1f
            private set
        var coverScale = 1f
            private set

        //Zoom
        var minZoom = 1f
        var maxZoom = 20f

        var zoom = 1f
            private set

        val isZoomedIn get() = zoom > minZoom

        var onZoomChanged: (() -> Unit)? = null


        //Events
        fun onSizeChanged() {
            //No size
            if (viewSize.x == 0f || viewSize.y == 0f || contentSize.x == 0f || contentSize.y == 0f) return

            //Update fit & cover scales
            val scaleX = viewSize.x / contentSize.x
            val scaleY = viewSize.y / contentSize.y
            fitScale = min(scaleX.toDouble(), scaleY.toDouble()).toFloat()
            coverScale = max(scaleX.toDouble(), scaleY.toDouble()).toFloat()

            //Update original space & size
            val fittedW = fitScale * contentSize.x
            val fittedH = fitScale * contentSize.y
            originalSpace.set((viewSize.x - fittedW) / 2f, (viewSize.y - fittedH) / 2f)
            originalSize.set(fittedW, fittedH)

            //Fit image
            resize(fitScale)
        }

        fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            //Get new size
            val newW = MeasureSpec.getSize(widthMeasureSpec).toFloat()
            val newH = MeasureSpec.getSize(heightMeasureSpec).toFloat()

            //Size didn't change -> Return
            if (viewSize.x == newW && viewSize.y == newH) return

            //Update size using the existing object
            viewSize.set(newW, newH)

            //Notify size changed
            onSizeChanged()
        }

        fun onLayout() {
            child?.let {
                //Get measured size
                val measuredW = it.measuredWidth.toFloat()
                val measuredH = it.measuredHeight.toFloat()

                //Check if size changed
                if (measuredW > 0 && measuredH > 0) {
                    if (contentSize.x != measuredW || contentSize.y != measuredH) {
                        contentSize.set(measuredW, measuredH)
                        onSizeChanged()
                    }
                }
            }
        }

        fun onScale(detector: ScaleGestureDetector) {
            var scaleFactor = detector.scaleFactor
            val origScale = zoom
            zoom *= scaleFactor

            if (zoom > maxZoom) {
                zoom = maxZoom
                scaleFactor = maxZoom / origScale
            } else if (zoom < minZoom) {
                zoom = minZoom
                scaleFactor = minZoom / origScale
            }

            margin.x = viewSize.x * zoom - viewSize.x - (2 * originalSpace.x * zoom)
            margin.y = viewSize.y * zoom - viewSize.y - (2 * originalSpace.y * zoom)

            val focusX = detector.focusX
            val focusY = detector.focusY

            if (originalSize.x * zoom <= viewSize.x || originalSize.y * zoom <= viewSize.y) {
                matrix.postScale(scaleFactor, scaleFactor, viewSize.x / 2f, viewSize.y / 2f)
            } else {
                matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            }

            if (scaleFactor < 1) {
                matrix.getValues(matrixValues)
                val x = matrixValues[Matrix.MTRANS_X]
                val y = matrixValues[Matrix.MTRANS_Y]

                if ((originalSize.x * zoom).roundToInt() < viewSize.x) {
                    if (y < -margin.y) {
                        matrix.postTranslate(0f, -(y + margin.y))
                    } else if (y > 0) {
                        matrix.postTranslate(0f, -y)
                    }
                } else {
                    if (x < -margin.x) {
                        matrix.postTranslate(-(x + margin.x), 0f)
                    } else if (x > 0) {
                        matrix.postTranslate(-x, 0f)
                    }
                }
            }

            applyToChild()

            //Zoom changed
            onZoomChanged?.invoke()
        }

        //Resize
        fun resize(scale: Float, center: Boolean = true) {
            //Either view size or bitmap size is not init yet
            if (scale.isInfinite() || scale.isNaN()) return

            //Save & update scale
            zoom = scale / fitScale
            matrix.setScale(scale, scale)

            //Center the image
            if (center) {
                matrix.postTranslate(
                    (viewSize.x - (scale * contentSize.x)) / 2f,
                    (viewSize.y - (scale * contentSize.y)) / 2f
                )
            }

            //Update margins
            margin.x = viewSize.x * zoom - viewSize.x - (2 * originalSpace.x * zoom)
            margin.y = viewSize.y * zoom - viewSize.y - (2 * originalSpace.y * zoom)
            applyToChild()

            //Zoom changed
            onZoomChanged?.invoke()
        }

        fun animateResize(scaleEnd: Float) {
            //Get start scale
            val scaleStart = zoom * fitScale

            //Get start & end position
            matrix.getValues(matrixValues)
            val posStart = PointF(matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y])
            val posEnd = PointF(
                (viewSize.x - (scaleEnd * contentSize.x)) / 2,
                (viewSize.y - (scaleEnd * contentSize.y)) / 2
            )

            //Create zoom animator (current scale = zoom * fitScale)
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 350L
            animator.addUpdateListener { animation ->
                val t = animation.animatedValue as Float

                //Zoom
                resize(Orion.lerp(scaleStart, scaleEnd, t), false)

                //Position
                matrix.postTranslate(
                    Orion.lerp(posStart.x, posEnd.x, t),
                    Orion.lerp(posStart.y, posEnd.y, t)
                )
                applyToChild()
            }

            //Start animation
            animator.start()
        }

        //Helpers
        fun constrainDragDelta(delta: PointF): PointF {
            matrix.getValues(matrixValues)
            val x = matrixValues[Matrix.MTRANS_X]
            val y = matrixValues[Matrix.MTRANS_Y]

            //Calculate size after applying current scale
            val scaledW = (originalSize.x * zoom).roundToInt().toFloat()
            val scaledH = (originalSize.y * zoom).roundToInt().toFloat()

            //Fit
            val clampedDelta = PointF(delta.x, delta.y)

            if (scaledW < viewSize.x) {
                //Fit vertically
                clampedDelta.x = 0f
                if (y + clampedDelta.y > 0) {
                    clampedDelta.y = -y
                } else if (y + clampedDelta.y < -margin.y) {
                    clampedDelta.y = -(y + margin.y)
                }
            } else if (scaledH < viewSize.y) {
                //Fit horizontally
                clampedDelta.y = 0f
                if (x + clampedDelta.x > 0) {
                    clampedDelta.x = -x
                } else if (x + clampedDelta.x < -margin.x) {
                    clampedDelta.x = -(x + margin.x)
                }
            } else {
                //Fit vertically
                if (y + clampedDelta.y > 0) {
                    clampedDelta.y = -y
                } else if (y + clampedDelta.y < -margin.y) {
                    clampedDelta.y = -(y + margin.y)
                }

                //Fit horizontally
                if (x + clampedDelta.x > 0) {
                    clampedDelta.x = -x
                } else if (x + clampedDelta.x < -margin.x) {
                    clampedDelta.x = -(x + margin.x)
                }
            }

            return clampedDelta
        }

        fun applyToChild() {
            //Get view
            val child = child ?: return

            //Apply matrix
            matrix.getValues(matrixValues)
            child.pivotX = 0f
            child.pivotY = 0f
            child.translationX = matrixValues[Matrix.MTRANS_X]
            child.translationY = matrixValues[Matrix.MTRANS_Y]
            child.scaleX = matrixValues[Matrix.MSCALE_X]
            child.scaleY = matrixValues[Matrix.MSCALE_Y]
        }

    }

    private inner class FlingHandler {

        private val scroller = android.widget.OverScroller(context)
        private var velocityTracker: android.view.VelocityTracker? = null
        private var flingRunnable: Runnable? = null

        private var lastFlingDistance = 0
        private var dirX = 0f
        private var dirY = 0f

        fun addMovement(event: MotionEvent) {
            if (velocityTracker == null) {
                velocityTracker = android.view.VelocityTracker.obtain()
            }
            velocityTracker?.addMovement(event)
        }

        fun stop() {
            scroller.forceFinished(true)
            flingRunnable?.let { removeCallbacks(it) }
            flingRunnable = null
        }

        fun startFling(onStep: (deltaX: Float, deltaY: Float) -> Unit) {
            val tracker = velocityTracker ?: return
            tracker.computeCurrentVelocity(800) //Pixels per second

            val vx = tracker.xVelocity
            val vy = tracker.yVelocity
            val magnitude = kotlin.math.hypot(vx, vy)

            //Ignore tiny flings
            if (magnitude < 100f) return

            stop()

            //Calculate normalized direction vector
            dirX = vx / magnitude
            dirY = vy / magnitude
            lastFlingDistance = 0

            //Fling a single scalar magnitude on X axis
            scroller.fling(
                0, 0,
                magnitude.toInt(), 0,
                0, Int.MAX_VALUE,
                0, 0
            )

            flingRunnable = object : Runnable {
                override fun run() {
                    if (scroller.computeScrollOffset()) {
                        val currDistance = scroller.currX
                        val deltaDistance = (currDistance - lastFlingDistance).toFloat()
                        lastFlingDistance = currDistance

                        // Scale distance delta along the direction vector
                        val deltaX = deltaDistance * dirX
                        val deltaY = deltaDistance * dirY

                        onStep(deltaX, deltaY)
                        postOnAnimation(this)
                    }
                }
            }
            postOnAnimation(flingRunnable!!)
        }

        fun recycle() {
            stop()
            velocityTracker?.recycle()
            velocityTracker = null
        }

    }

}