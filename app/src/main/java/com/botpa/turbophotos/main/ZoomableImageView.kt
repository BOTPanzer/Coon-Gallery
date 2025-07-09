package com.botpa.turbophotos.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.min

class ZoomableImageView(context: Context, attr: AttributeSet?) : AppCompatImageView(context, attr) {

    private var scaleDetector: ScaleGestureDetector
    private var context: Context
    private var matrix: Matrix = Matrix()


    //Drag modes
    companion object {
        const val NONE: Int = 0
        const val DRAG: Int = 1
        const val ZOOM: Int = 2
        const val CLICK: Int = 3
    }
    private var mode: Int = NONE

    //
    private var fingers: Int = 0
    private var last: PointF = PointF()
    private var start: PointF = PointF()
    private var minScale: Float = 1f
    private var maxScale: Float = 20f
    private var m: FloatArray = FloatArray(9)

    //
    private var redundantXSpace: Float = 0f
    private var redundantYSpace: Float = 0f
    private var width: Float = 0f
    private var height: Float = 0f
    private var saveScale: Float = 1f
    private var right: Float = 0f
    private var bottom: Float = 0f
    private var origWidth: Float = 0f
    private var origHeight: Float = 0f
    private var bmWidth: Float = 0f
    private var bmHeight: Float = 0f

    //Getters
    val pointers: Int get() = fingers
    val zoom: Float get() = saveScale

    //Click
    private var lastClickTimestamp: Long = 0
    private var onClick: Runnable? = null
    private var onZoomChange: Runnable? = null


    //Constructor
    init {
        super.setClickable(true)

        //Init zoomable imageview
        this.context = context
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        scaleType = ScaleType.MATRIX
        fit()

        //Add on click listener
        setOnClickListener { view: View? ->
            val currentTimestamp = System.currentTimeMillis()
            if (currentTimestamp - lastClickTimestamp < 500) {
                //Double click
                //Toast.makeText(context, "" + saveScale, Toast.LENGTH_SHORT).show();
                lastClickTimestamp = 0
            } else {
                lastClickTimestamp = System.currentTimeMillis()
            }
            onClick?.run()
        }

        //Add on touch listener
        setOnTouchListener { v: View?, event: MotionEvent ->
            //Notify scale detector with the event
            scaleDetector.onTouchEvent(event)

            //
            matrix.getValues(m)
            val x = m[Matrix.MTRANS_X]
            val y = m[Matrix.MTRANS_Y]

            //Get fingers count & current position
            if (fingers != event.pointerCount) fingers = event.pointerCount
            val curr = PointF(scaleDetector.focusX, scaleDetector.focusY)

            //Check action
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    //Change mode
                    mode = if (fingers == 0) NONE else if (fingers == 1) DRAG else ZOOM

                    //Save position
                    last[curr.x] = curr.y
                    start.set(last)

                    //Finger down -> Zoom changed
                    onZoomChange?.run()
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    //Change mode
                    mode = if (fingers == 0) NONE else if (fingers == 1) DRAG else ZOOM

                    //Save position
                    last[curr.x] = curr.y

                    //Finger up -> Zoom changed
                    fingers--
                    onZoomChange?.run()
                }

                MotionEvent.ACTION_UP -> {
                    //Change mode
                    mode = if (fingers == 0) NONE else if (fingers == 1) DRAG else ZOOM

                    //Check if clicked
                    val xDiff = abs((curr.x - start.x).toDouble()).toInt()
                    val yDiff = abs((curr.y - start.y).toDouble()).toInt()
                    if (xDiff < CLICK && yDiff < CLICK) performClick()

                    //Finger up -> Zoom changed
                    fingers--
                    onZoomChange?.run()
                }

                MotionEvent.ACTION_MOVE -> {
                    //if the mode is ZOOM or if the mode is DRAG and already zoomed
                    if (mode == ZOOM || (mode == DRAG && saveScale > minScale)) {
                        var deltaX = curr.x - last.x // x difference
                        var deltaY = curr.y - last.y // y difference
                        val scaleWidth = Math.round(origWidth * saveScale).toFloat() // width after applying current scale
                        val scaleHeight = Math.round(origHeight * saveScale).toFloat() // height after applying current scale

                        //if scaleWidth is smaller than the views width
                        //in other words if the image width fits in the view
                        //limit left and right movement
                        if (scaleWidth < width) {
                            //Fit vertically
                            deltaX = 0f
                            if (y + deltaY > 0) deltaY = -y
                            else if (y + deltaY < -bottom) deltaY = -(y + bottom)
                        } else if (scaleHeight < height) {
                            //Fit horizontally
                            deltaY = 0f
                            if (x + deltaX > 0) deltaX = -x
                            else if (x + deltaX < -right) deltaX = -(x + right)
                        } else {
                            //Fit vertically
                            if (y + deltaY > 0) deltaY = -y
                            else if (y + deltaY < -bottom) deltaY = -(y + bottom)

                            //Fit horizontally
                            if (x + deltaX > 0) deltaX = -x
                            else if (x + deltaX < -right) deltaX = -(x + right)
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
    fun setMaxZoom(x: Float) {
        maxScale = x
    }

    fun fit() {
        //Fit to screen.
        val scale: Float
        val scaleX = width / bmWidth
        val scaleY = height / bmHeight
        scale = min(scaleX.toDouble(), scaleY.toDouble()).toFloat()
        matrix.setScale(scale, scale)
        imageMatrix = matrix
        saveScale = 1f

        // Center the image
        redundantYSpace = height - (scale * bmHeight)
        redundantXSpace = width - (scale * bmWidth)
        redundantYSpace /= 2f
        redundantXSpace /= 2f

        matrix.postTranslate(redundantXSpace, redundantYSpace)

        origWidth = width - 2 * redundantXSpace
        origHeight = height - 2 * redundantYSpace
        right = width * saveScale - width - (2 * redundantXSpace * saveScale)
        bottom = height * saveScale - height - (2 * redundantYSpace * saveScale)
        imageMatrix = matrix
    }

    fun setOnClick(runnable: Runnable) {
        onClick = runnable
    }

    fun setOnZoomChange(runnable: Runnable) {
        onZoomChange = runnable
    }

    //Other
    override fun setImageBitmap(bm: Bitmap?) {
        if (bm == null) return
        super.setImageBitmap(bm)
        bmWidth = bm.width.toFloat()
        bmHeight = bm.height.toFloat()
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= scaleFactor
            if (saveScale > maxScale) {
                saveScale = maxScale
                scaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                scaleFactor = minScale / origScale
            }
            right = width * saveScale - width - (2 * redundantXSpace * saveScale)
            bottom = height * saveScale - height - (2 * redundantYSpace * saveScale)
            if (origWidth * saveScale <= width || origHeight * saveScale <= height) {
                matrix.postScale(scaleFactor, scaleFactor, width / 2, height / 2)
                if (scaleFactor < 1) {
                    matrix.getValues(m)
                    val x = m[Matrix.MTRANS_X]
                    val y = m[Matrix.MTRANS_Y]
                    if (scaleFactor < 1) {
                        if (Math.round(origWidth * saveScale) < width) {
                            if (y < -bottom) matrix.postTranslate(0f, -(y + bottom))
                            else if (y > 0) matrix.postTranslate(0f, -y)
                        } else {
                            if (x < -right) matrix.postTranslate(-(x + right), 0f)
                            else if (x > 0) matrix.postTranslate(-x, 0f)
                        }
                    }
                }
            } else {
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                matrix.getValues(m)
                val x = m[Matrix.MTRANS_X]
                val y = m[Matrix.MTRANS_Y]
                if (scaleFactor < 1) {
                    if (x < -right) matrix.postTranslate(-(x + right), 0f)
                    else if (x > 0) matrix.postTranslate(-x, 0f)
                    if (y < -bottom) matrix.postTranslate(0f, -(y + bottom))
                    else if (y > 0) matrix.postTranslate(0f, -y)
                }
            }
            onZoomChange?.run()
            return true
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        height = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        fit()
    }

}
