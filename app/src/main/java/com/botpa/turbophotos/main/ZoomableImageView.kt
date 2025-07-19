package com.botpa.turbophotos.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.botpa.turbophotos.util.Orion
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ZoomableImageView(context: Context, attr: AttributeSet?) : AppCompatImageView(context, attr) {

    private var scaleDetector: ScaleGestureDetector
    private var context: Context
    private var matrix: Matrix = Matrix()
    private var m: FloatArray = FloatArray(9)

    //Drag modes
    private companion object {
        const val NONE: Int = 0
        const val DRAG: Int = 1
        const val ZOOM: Int = 2
        const val CLICK: Int = 3
    }
    private var mode: Int = NONE

    //Touch
    private var last: PointF = PointF()
    private var start: PointF = PointF()
    private var onPointersChanged: Runnable? = null

    var pointers: Int = 0
        private set

    //Sizes
    private var viewSize: Size = Size()
    private var bitmapSize: Size = Size()
    private var originalSize: Size = Size()   //Size when the zoom is 1
    private var originalSpace: Size = Size()  //Space around the image when the zoom is 1

    //Zoom & Scale
    private var minZoom: Float = 1f
    private var maxZoom: Float = 20f
    private var fitScale: Float = 1f
    private var coverScale: Float = 1f
    private var margin: Size = Size()   //The margin around the image (both sides combined, so x = right + left)

    var zoom: Float = 1f
        private set

    //Click
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var lastClickTimestamp: Long = 0
    private val doubleClickDelay: Long = 200
    private var onClick: Runnable? = null
    private var onZoomChanged: Runnable? = null


    //Constructor
    init {
        super.setClickable(true)

        //Init zoomable imageview
        this.context = context
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        scaleType = ScaleType.MATRIX

        //Add on click listener
        setOnClickListener { view: View? ->
            val currentTimestamp = System.currentTimeMillis()
            if (currentTimestamp - lastClickTimestamp < doubleClickDelay) {
                lastClickTimestamp = 0

                //Stop click runnable
                if (onClick != null) handler.removeCallbacks(onClick!!)

                //Animate scale
                animateResize(if (zoom > 1) fitScale else coverScale)
            } else {
                lastClickTimestamp = System.currentTimeMillis()

                //Run click runnable
                if (onClick != null) handler.postDelayed(onClick!!, doubleClickDelay)
            }
        }

        //Add on touch listener
        setOnTouchListener { view: View?, event: MotionEvent ->
            //Notify scale detector with the event
            scaleDetector.onTouchEvent(event)

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
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    //Change mode
                    mode = if (pointers == 0) NONE else if (pointers == 1) DRAG else ZOOM

                    //Save position
                    last[curr.x] = curr.y
                    start.set(last)
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    //Change mode
                    mode = if (pointers == 0) NONE else if (pointers == 1) DRAG else ZOOM

                    //Save position
                    last[curr.x] = curr.y

                    //Pointer up
                    pointers--
                    onPointersChanged?.run()
                }

                MotionEvent.ACTION_UP -> {
                    //Change mode
                    mode = if (pointers == 0) NONE else if (pointers == 1) DRAG else ZOOM

                    //Check if clicked
                    val xDiff = abs((curr.x - start.x).toDouble()).toInt()
                    val yDiff = abs((curr.y - start.y).toDouble()).toInt()
                    if (xDiff < CLICK && yDiff < CLICK) performClick()

                    //Pointer up
                    pointers--
                    onPointersChanged?.run()
                }

                MotionEvent.ACTION_MOVE -> {
                    //if the mode is ZOOM or if the mode is DRAG and already zoomed
                    if (mode == ZOOM || (mode == DRAG && zoom > minZoom)) {
                        var deltaX = curr.x - last.x // x difference
                        var deltaY = curr.y - last.y // y difference
                        val scaledWidth = Math.round(originalSize.x * zoom).toFloat() // width after applying current scale
                        val scaledHeight = Math.round(originalSize.y * zoom).toFloat() // height after applying current scale

                        //if scaleWidth is smaller than the views width
                        //in other words if the image width fits in the view
                        //limit left and right movement
                        if (scaledWidth < viewSize.x) {
                            //Fit vertically
                            deltaX = 0f
                            if (y + deltaY > 0) deltaY = -y
                            else if (y + deltaY < -margin.y) deltaY = -(y + margin.y)
                        } else if (scaledHeight < viewSize.y) {
                            //Fit horizontally
                            deltaY = 0f
                            if (x + deltaX > 0) deltaX = -x
                            else if (x + deltaX < -margin.x) deltaX = -(x + margin.x)
                        } else {
                            //Fit vertically
                            if (y + deltaY > 0) deltaY = -y
                            else if (y + deltaY < -margin.y) deltaY = -(y + margin.y)

                            //Fit horizontally
                            if (x + deltaX > 0) deltaX = -x
                            else if (x + deltaX < -margin.x) deltaX = -(x + margin.x)
                        }

                        //move the image with the matrix
                        matrix.postTranslate(deltaX, deltaY)

                        //set the last touch location to the current
                        last[curr.x] = curr.y
                    }
                }
            }

            //Update image with new matrix
            imageMatrix = matrix
            invalidate()
            true
        }
    }

    //Zoom
    private fun onSizeChanged() {
        //Get X & Y scales
        val scaleX = viewSize.x / bitmapSize.x
        val scaleY = viewSize.y / bitmapSize.y

        //Update fit & cover scales
        fitScale = min(scaleX.toDouble(), scaleY.toDouble()).toFloat()
        coverScale = max(scaleX.toDouble(), scaleY.toDouble()).toFloat()

        //Update redundant space & original size
        val x = (fitScale * bitmapSize.x)
        val y = (fitScale * bitmapSize.y)
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

        //Save scale
        zoom = scale / fitScale

        //Update scale
        matrix.setScale(scale, scale)
        imageMatrix = matrix

        //Center the image
        if (center) {
            matrix.postTranslate(
                (viewSize.x - (scale * bitmapSize.x)) / 2,
                (viewSize.y - (scale * bitmapSize.y)) / 2
            )
            imageMatrix = matrix
        }

        //Update margins
        margin.x = viewSize.x * zoom - viewSize.x - (2 * originalSpace.x * zoom)
        margin.y = viewSize.y * zoom - viewSize.y - (2 * originalSpace.y * zoom)

        //Zoom changed
        onZoomChanged?.run()
    }

    private fun animateResize(scaleEnd: Float) {
        //Get start scale
        val scaleStart = zoom * fitScale

        //Get start & end position
        val posStart = Size(m[Matrix.MTRANS_X], m[Matrix.MTRANS_Y])
        val posEnd = Size((viewSize.x - (scaleEnd * bitmapSize.x)) / 2, (viewSize.y - (scaleEnd * bitmapSize.y)) / 2)

        //Create zoom animator (current scale = zoom * fitScale)
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.setDuration(500L)
        animator.addUpdateListener { animation ->
            val t = animation.animatedValue as Float

            //Zoom
            resize(Orion.lerp(scaleStart, scaleEnd, t), false)

            //Position
            matrix.postTranslate(
                Orion.lerp(posStart.x, posEnd.x, t),
                Orion.lerp(posStart.y, posEnd.y, t)
            )
            imageMatrix = matrix
        }

        //Start animation
        animator.start()
    }

    //Listeners
    fun setOnClick(runnable: Runnable) {
        onClick = runnable
    }

    fun setOnZoomChanged(runnable: Runnable) {
        onZoomChanged = runnable
    }

    fun setOnPointersChanged(runnable: Runnable) {
        onPointersChanged = runnable
    }

    //Other
    override fun setImageBitmap(bitmap: Bitmap?) {
        super.setImageBitmap(bitmap)

        //No bitmap -> Reset size
        if (bitmap == null) {
            bitmapSize.set(0f, 0f)
            return
        }

        //Update bitmap size
        bitmapSize.set(bitmap.width.toFloat(), bitmap.height.toFloat())

        //Notify size changed
        onSizeChanged()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        //Get new size
        val newWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val newHeight = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        //Size didn't change -> Return
        if (viewSize.x == newWidth && viewSize.y == newHeight) return

        //Update size
        viewSize.set(newWidth, newHeight)

        //Notify size changed
        onSizeChanged()
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {

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
            if (originalSize.x * zoom <= viewSize.x || originalSize.y * zoom <= viewSize.y) {
                matrix.postScale(scaleFactor, scaleFactor, viewSize.x / 2, viewSize.y / 2)
                if (scaleFactor < 1) {
                    matrix.getValues(m)
                    val x = m[Matrix.MTRANS_X]
                    val y = m[Matrix.MTRANS_Y]
                    if (Math.round(originalSize.x * zoom) < viewSize.x) {
                        if (y < -margin.y) matrix.postTranslate(0f, -(y + margin.y))
                        else if (y > 0) matrix.postTranslate(0f, -y)
                    } else {
                        if (x < -margin.x) matrix.postTranslate(-(x + margin.x), 0f)
                        else if (x > 0) matrix.postTranslate(-x, 0f)
                    }
                }
            } else {
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                matrix.getValues(m)
                val x = m[Matrix.MTRANS_X]
                val y = m[Matrix.MTRANS_Y]
                if (scaleFactor < 1) {
                    if (x < -margin.x) matrix.postTranslate(-(x + margin.x), 0f)
                    else if (x > 0) matrix.postTranslate(-x, 0f)
                    if (y < -margin.y) matrix.postTranslate(0f, -(y + margin.y))
                    else if (y > 0) matrix.postTranslate(0f, -y)
                }
            }

            //Zoom changed
            onZoomChanged?.run()
            return true
        }

    }

    private inner class Size(var x: Float = 0f, var y: Float = 0f) {

        fun set(x: Float, y: Float) {
            this.x = x
            this.y = y
        }

    }

}
