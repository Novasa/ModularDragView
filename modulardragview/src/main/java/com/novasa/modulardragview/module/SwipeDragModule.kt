package com.novasa.modulardragview.module

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import androidx.annotation.CallSuper
import com.novasa.modulardragview.view.DragView
import kotlin.math.abs

/**
 * A drag module that supports swiping the drag view
 * Created by mikkelschlager on 17/08/16.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class SwipeDragModule(dragView: DragView, moduleView: View, direction: Int) : DragModule(dragView, moduleView, direction) {

    companion object {

        /** How much the view must be dragged to activate the swipe  */
        const val OUT_BOUNDARY = .35f
    }

    interface Callback {
        fun onModuleSwipeStarted(module: SwipeDragModule)
        fun onModuleSwipeFinished(module: SwipeDragModule)
    }

    var callback: Callback? = null

    protected val interpolator = AccelerateInterpolator()

    override val teaseX: Float
        get() = OUT_BOUNDARY * direction

    override fun onSwipe(direction: Int, x: Float, v: Float, div: Float): Boolean {
        return if (direction == this.direction) {
            // We only want to initiate a swipe if it's in direction of the currently open module.
            swipe(x, v)
            true
        } else false
    }

    override fun onDragEnded(x: Float): Boolean {
        if (abs(x) > OUT_BOUNDARY) {
            swipe(x, 0f)
            return false
        }

        return true
    }

    private fun swipe(x: Float, velocity: Float) {
        dragView.isDragEnabled = false

        val duration: Long
        val interp: Interpolator?
        if (velocity <= 0f) {
            duration = 200L
            interp = interpolator

        } else {
            val remaining = (1f - abs(x)) * dragWidth
            duration = (remaining / velocity).toLong()
            interp = null
        }

        callback?.onModuleSwipeStarted(this)

        dragView.animateToPosition(direction.toFloat(), duration, interp) {
            callback?.onModuleSwipeFinished(this)
        }
    }

    @CallSuper
    override fun onReset() {
        super.onReset()
        dragView.isDragEnabled = true
    }
}
