package com.botpa.turbophotos.gallery.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.botpa.turbophotos.R
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import androidx.core.content.withStyledAttributes

@Suppress("RestrictedApi")
class CustomRefreshHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), RefreshHeader {

    private val refreshText: TextView
    private val refreshIndicator: View

    private lateinit var textPullToRefresh: String
    private lateinit var textReleaseToRefresh: String
    private lateinit var textRefreshing: String

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.refresh_header, this, true)
        refreshText = view.findViewById(R.id.refreshStatus)
        refreshIndicator = view.findViewById(R.id.refreshIndicator)

        //Get attributes
        context.withStyledAttributes(attrs, R.styleable.CustomRefreshHeader, defStyleAttr, 0) {
            textPullToRefresh = getString(R.styleable.CustomRefreshHeader_textPullToRefresh) ?: "Pull to refresh"
            textReleaseToRefresh = getString(R.styleable.CustomRefreshHeader_textReleaseToRefresh) ?: "Release to refresh"
            textRefreshing = getString(R.styleable.CustomRefreshHeader_textRefreshing) ?: "Refreshing..."
        }
    }

    override fun getView(): View = this

    override fun getSpinnerStyle(): SpinnerStyle = SpinnerStyle.Translate

    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
        alpha = 0f
        refreshIndicator.visibility = GONE
    }

    override fun onStateChanged(refreshLayout: RefreshLayout, oldState: RefreshState, newState: RefreshState) {
        when (newState) {
            RefreshState.None, RefreshState.PullDownToRefresh -> {
                refreshText.text = textPullToRefresh
                refreshIndicator.visibility = GONE
            }
            RefreshState.ReleaseToRefresh -> {
                refreshText.text = textReleaseToRefresh
                refreshIndicator.visibility = GONE
            }
            RefreshState.Refreshing -> {
                refreshText.text = textRefreshing
                refreshIndicator.visibility = VISIBLE
            }
            RefreshState.RefreshFinish -> {
                refreshText.text = ""
                refreshIndicator.visibility = GONE
            }
            else -> {}
        }
    }

    override fun onMoving(isDragging: Boolean, percent: Float, offset: Int, height: Int, maxDragHeight: Int) {
        alpha = percent
    }

    override fun setPrimaryColors(vararg colors: Int) {}
    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {}
    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {}
    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int = 0
    override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {}
    override fun isSupportHorizontalDrag(): Boolean = false
    override fun autoOpen(duration: Int, dragRate: Float, animationOnly: Boolean): Boolean = false

}