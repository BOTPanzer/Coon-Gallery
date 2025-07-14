package com.botpa.turbophotos.main

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
    private val doubleClickDelay: Long = 250
    private var lastClickTimestamp: Long = 0
    private var onClick: Runnable? = null
    private var onZoomChange: Runnable? = null
    private var handler: Handler = Handler(Looper.getMainLooper())


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

                //Zoom
                resize(if (zoom > 1) fitScale else coverScale)
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

            //Get fingers count & current position
            if (pointers != event.pointerCount) pointers = event.pointerCount
            val curr = PointF(scaleDetector.focusX, scaleDetector.focusY)

            //Check action
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    //Change mode
                    mode = if (pointers == 0) NONE else if (pointers == 1) DRAG else ZOOM

                    //Save position
                    last[curr.x] = curr.y
                    start.set(last)

                    //Finger down -> Zoom changed
                    onZoomChange?.run()
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    //Change mode
                    mode = if (pointers == 0) NONE else if (pointers == 1) DRAG else ZOOM

                    //Save position
                    last[curr.x] = curr.y

                    //Finger up -> Zoom changed
                    pointers--
                    onZoomChange?.run()
                }

                MotionEvent.ACTION_UP -> {
                    //Change mode
                    mode = if (pointers == 0) NONE else if (pointers == 1) DRAG else ZOOM

                    //Check if clicked
                    val xDiff = abs((curr.x - start.x).toDouble()).toInt()
                    val yDiff = abs((curr.y - start.y).toDouble()).toInt()
                    if (xDiff < CLICK && yDiff < CLICK) performClick()

                    //Finger up -> Zoom changed
                    pointers--
                    onZoomChange?.run()
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
    private inner class Size(var x: Float = 0f, var y: Float = 0f) {

        fun set(x: Float, y: Float) {
            this.x = x
            this.y = y
        }

    }

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

    private fun resize(scale: Float) {
        //Either view size or bitmap size is not init yet
        if (scale.isInfinite() || scale.isNaN()) return

        //Save scale
        zoom = scale / fitScale

        //Update scale
        matrix.setScale(scale, scale)
        imageMatrix = matrix

        //Center the image
        matrix.postTranslate(
            (viewSize.x - (scale * bitmapSize.x)) / 2,
            (viewSize.y - (scale * bitmapSize.y)) / 2
        )
        imageMatrix = matrix

        //Update margins
        margin.x = viewSize.x * zoom - viewSize.x - (2 * originalSpace.x * zoom)
        margin.y = viewSize.y * zoom - viewSize.y - (2 * originalSpace.y * zoom)
    }

    //Listeners
    fun setOnClick(runnable: Runnable) {
        onClick = runnable
    }

    fun setOnZoomChange(runnable: Runnable) {
        onZoomChange = runnable
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

        //Update scale & refit image in view
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

        //Update scale & refit image in view
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
            onZoomChange?.run()
            return true
        }

    }

}
