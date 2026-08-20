package com.botpa.turbophotos.screens.album

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView

class AlbumTitleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val minTextSizePx = 12f.spToPx()
    private val maxTextSizePx = 42f.spToPx()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        if (width > 0 && !text.isNullOrEmpty()) {
            refitText(text.toString(), width)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun refitText(text: String, targetWidth: Int) {
        var size = maxTextSizePx
        paint.textSize = size
        while (size > minTextSizePx && paint.measureText(text) > targetWidth) {
            size -= 1f
            paint.textSize = size
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    private fun Float.spToPx(): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, resources.displayMetrics)

}