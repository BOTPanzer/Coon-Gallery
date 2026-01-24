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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.core.view.isNotEmpty
import kotlin.math.roundToInt

class ZoomableLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private var scaleDetector: ScaleGestureDetector
    private var matrix: Matrix = Matrix()
    private var m: FloatArray = FloatArray(9)

    //Content
    private val child: View? get() = if (isNotEmpty()) getChildAt(0) else null

    //Drag modes
    private companion object {
        const val NONE: Int = 0
        const val DRAG: Int = 1
        const val ZOOM: Int = 2
    }
    private var mode: Int = NONE

    //Touch info
    private var onPointersChanged: Runnable? = null
    private var last: PointF = PointF()
    private var start: PointF = PointF()
    private var swipeDistance: Int = ViewConfiguration.get(context).scaledTouchSlop

    var pointers: Int = 0
        private set

    //Sizes
    private var viewSize: PointF = PointF()
    private var contentSize: PointF = PointF()
    private var originalSize: PointF = PointF()
    private var originalSpace: PointF = PointF()

    //Zoom & Scale
    private var margin: PointF = PointF()
    private var minZoom: Float = 1f
    private var maxZoom: Float = 20f
    private var fitScale: Float = 1f
    private var coverScale: Float = 1f

    var zoom: Float = 1f
        private set

    //Options
    private var doubleTapZoomsToCover: Boolean = false
    private var doubleTapZoom: Float = 2f

    //Click
    private var onClick: Runnable? = null
    private var onZoomChanged: Runnable? = null
    private var lastClickTimestamp: Long = 0
    private val doubleClickDelay: Long = 200


    //Constructor
    init {
        //Init scale detector
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        scaleDetector.isQuickScaleEnabled = true //Reduces delay for scaling detection

        //Click event
        setOnClickListener { view ->
            //Get current timestamp
            val currentTimestamp = System.currentTimeMillis()

            //Check if its double click
            if (currentTimestamp - lastClickTimestamp < doubleClickDelay) {
                //Reset timestamp
                lastClickTimestamp = 0

                //Stop click runnable
                if (onClick != null) handler.removeCallbacks(onClick!!)

                //Animate scale
                animateResize(if (zoom > minZoom) fitScale else if (doubleTapZoomsToCover) coverScale else doubleTapZoom)
            } else {
                //Save timestamp
                lastClickTimestamp = currentTimestamp

                //Run click runnable
                if (onClick != null) handler.postDelayed(onClick!!, doubleClickDelay)
            }
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
                last.set(event.x, event.y)
                start.set(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                //Intercept if zoomed & dragged beyond swipe distance
                if (zoom > minZoom) {
                    val diff = PointF(event.x - start.x, event.y - start.y)
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
        //Copy matrix values & get position
        matrix.getValues(m)
        val x = m[Matrix.MTRANS_X]
        val y = m[Matrix.MTRANS_Y]

        //Update pointers
        if (pointers != event.pointerCount) pointers = event.pointerCount
        onPointersChanged?.run()

        //Get current position
        val curr = PointF(scaleDetector.focusX, scaleDetector.focusY)

        //Check action
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                //Change mode
                mode = if (pointers == 0) NONE else if (pointers == 1) DRAG else ZOOM

                //Save position
                last.set(curr)
                start.set(last)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                //Change mode
                mode = if (pointers - 1 == 0) NONE else if (pointers - 1 == 1) DRAG else ZOOM

                //Save position
                last.set(curr)

                //Pointer up
                pointers--
                onPointersChanged?.run()
            }

            MotionEvent.ACTION_UP -> {
                //Reset mode
                mode = NONE

                //Check if clicked
                val xDiff = abs((curr.x - start.x).toDouble()).toInt()
                val yDiff = abs((curr.y - start.y).toDouble()).toInt()
                val clickAreaRadius = swipeDistance * 2
                if (xDiff < clickAreaRadius && yDiff < clickAreaRadius) performClick()

                //Pointer up
                pointers--
                onPointersChanged?.run()
            }

            MotionEvent.ACTION_MOVE -> {
                //If the mode is ZOOM or if the mode is DRAG and already zoomed
                if (mode == ZOOM || (mode == DRAG && zoom > minZoom)) {
                    //Calculate movement delta
                    val delta = PointF(curr.x - last.x, curr.y - last.y)

                    //Calculate size after applying current scale
                    //val scaledSize = PointF(viewSize.x * zoom, viewSize.y * zoom)
                    val scaledSize = PointF(
                        (originalSize.x * zoom).roundToInt().toFloat(),
                        (originalSize.y * zoom).roundToInt().toFloat()
                    )

                    //Fit
                    if (scaledSize.x < viewSize.x) {
                        //Fit vertically
                        delta.x = 0f
                        if (y + delta.y > 0) delta.y = -y
                        else if (y + delta.y < -margin.y) delta.y = -(y + margin.y)
                    } else if (scaledSize.y < viewSize.y) {
                        //Fit horizontally
                        delta.y = 0f
                        if (x + delta.x > 0) delta.x = -x
                        else if (x + delta.x < -margin.x) delta.x = -(x + margin.x)
                    } else {
                        //Fit vertically
                        if (y + delta.y > 0) delta.y = -y
                        else if (y + delta.y < -margin.y) delta.y = -(y + margin.y)

                        //Fit horizontally
                        if (x + delta.x > 0) delta.x = -x
                        else if (x + delta.x < -margin.x) delta.x = -(x + margin.x)
                    }

                    //Update & apply matrix
                    matrix.postTranslate(delta.x, delta.y)
                    applyMatrixToChild()

                    //Update last touch
                    last.set(curr)
                }
            }
        }
        return true
    }

    //Zoom
    private fun applyMatrixToChild() {
        //Get view
        val view = child ?: return

        //Apply matrix
        matrix.getValues(m)
        view.pivotX = 0f
        view.pivotY = 0f
        view.translationX = m[Matrix.MTRANS_X]
        view.translationY = m[Matrix.MTRANS_Y]
        view.scaleX = m[Matrix.MSCALE_X]
        view.scaleY = m[Matrix.MSCALE_Y]
    }

    private fun onSizeChanged() {
        //No size
        if (contentSize.x == 0f || contentSize.y == 0f || viewSize.x == 0f || viewSize.y == 0f) return

        //Get scale
        val scale = PointF(viewSize.x / contentSize.x, viewSize.y / contentSize.y)

        //Update fit & cover scales
        fitScale = min(scale.x.toDouble(), scale.y.toDouble()).toFloat()
        coverScale = max(scale.x.toDouble(), scale.y.toDouble()).toFloat()

        //Update original space & size
        val x = (fitScale * contentSize.x)
        val y = (fitScale * contentSize.y)
        originalSpace.x = (viewSize.x - x) / 2
        originalSpace.y = (viewSize.y - y) / 2
        originalSize.x = x
        originalSize.y = y

        //Fit image
        resize(fitScale)
    }

    private fun resize(scale: Float, center: Boolean = true) {
        //Either view size or bitmap size is not init yet
        if (scale.isInfinite() || scale.isNaN()) return

        //Save & update scale
        zoom = scale / fitScale
        matrix.setScale(scale, scale)

        //Center the image
        if (center) matrix.postTranslate((viewSize.x - (scale * contentSize.x)) / 2, (viewSize.y - (scale * contentSize.y)) / 2)

        //Update margins
        margin.x = viewSize.x * zoom - viewSize.x - (2 * originalSpace.x * zoom)
        margin.y = viewSize.y * zoom - viewSize.y - (2 * originalSpace.y * zoom)
        applyMatrixToChild()

        //Zoom changed
        onZoomChanged?.run()
    }

    private fun animateResize(scaleEnd: Float) {
        //Get start scale
        val scaleStart = zoom * fitScale

        //Get start & end position
        matrix.getValues(m)
        val posStart = PointF(m[Matrix.MTRANS_X], m[Matrix.MTRANS_Y])
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
            applyMatrixToChild()
        }

        //Start animation
        animator.start()
    }

    //Options
    fun setDoubleTapZoomsToCover(value: Boolean) { doubleTapZoomsToCover = value }

    //Listeners
    fun setOnClick(runnable: Runnable) { onClick = runnable }
    fun setOnZoomChanged(runnable: Runnable) { onZoomChanged = runnable }
    fun setOnPointersChanged(runnable: Runnable) { onPointersChanged = runnable }

    //Other
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        //Get new size
        val newSize = PointF(MeasureSpec.getSize(widthMeasureSpec).toFloat(), MeasureSpec.getSize(heightMeasureSpec).toFloat())

        //Size didn't change -> Return
        if (viewSize.x == newSize.x && viewSize.y == newSize.y) return

        //Update size
        viewSize.set(newSize)

        //Notify size changed
        onSizeChanged()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
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

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
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
                matrix.postScale(scaleFactor, scaleFactor, viewSize.x / 2, viewSize.y / 2)
            } else {
                matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            }

            matrix.getValues(m)
            val x = m[Matrix.MTRANS_X]
            val y = m[Matrix.MTRANS_Y]

            if (scaleFactor < 1) {
                if ((originalSize.x * zoom).roundToInt() < viewSize.x) {
                    if (y < -margin.y) matrix.postTranslate(0f, -(y + margin.y))
                    else if (y > 0) matrix.postTranslate(0f, -y)
                } else {
                    if (x < -margin.x) matrix.postTranslate(-(x + margin.x), 0f)
                    else if (x > 0) matrix.postTranslate(-x, 0f)
                }
            }
            applyMatrixToChild()

            //Zoom changed
            onZoomChanged?.run()
            return true
        }

    }

}