package com.botpa.turbophotos.gallery.views.refresh

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.botpa.turbophotos.R
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle

@Suppress("RestrictedApi")
class SimpleRefreshHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), RefreshHeader {

    private val refreshIndicator: View

    var onMoved: ((view: View, percent: Float) -> Unit)? = null

    init {
        val view: View = LayoutInflater.from(context).inflate(R.layout.refresh_simple_header, this, true)
        refreshIndicator = view.findViewById(R.id.refreshIndicator)
    }

    override fun getView(): View = this

    override fun getSpinnerStyle(): SpinnerStyle = SpinnerStyle.Translate

    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
        alpha = 0f
    }

    override fun onMoving(isDragging: Boolean, percent: Float, offset: Int, height: Int, maxDragHeight: Int) {
        alpha = percent
        refreshIndicator.scaleX = 0.5f + (percent * 0.5f)
        refreshIndicator.scaleY = 0.5f + (percent * 0.5f)
        onMoved?.invoke(this, percent)
    }

    override fun onStateChanged(refreshLayout: RefreshLayout, oldState: RefreshState, newState: RefreshState) {}
    override fun setPrimaryColors(vararg colors: Int) {}
    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {}
    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {}
    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int = 0
    override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {}
    override fun isSupportHorizontalDrag(): Boolean = false
    override fun autoOpen(duration: Int, dragRate: Float, animationOnly: Boolean): Boolean = false

}