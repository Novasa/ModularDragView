package com.novasa.modulardragview.module

import android.view.View
import com.novasa.modulardragview.extension.normalizedClamped
import com.novasa.modulardragview.view.DragView
import kotlin.math.abs

/**
 * Created by mikkelschlager on 17/08/16.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class TickerDragModule(dragView: DragView, moduleView: View, direction: Int) : DragModule(dragView, moduleView, direction) {

    companion object {

        // How much the view can be dragged. Drag factor will decrease to zero between these values in both directions
        private const val MIN_DRAG = .2f
        private const val MAX_DRAG = .35f
    }

    var callback: Callback? = null

    override val teaseX: Float
        get() = MIN_DRAG * direction

    override val maxDrag: Float
        get() = MAX_DRAG

    interface Callback {
        fun onModuleTicked(module: TickerDragModule)
    }

    override fun dragFactor(x0: Float, dx: Float): Float {
        val ax = abs(x0)
        return if (ax > MIN_DRAG && dx * direction > 0f) {
            1f - ax.normalizedClamped(MIN_DRAG, maxDrag)
        } else super.dragFactor(x0, dx)
    }

    override fun onDragEnded(x: Float): Boolean {
        if (x * direction > MIN_DRAG) {
            dragView.isDragEnabled = false
            callback?.onModuleTicked(this)
        }

        return true
    }

    override fun onReset() {
        super.onReset()
        dragView.isDragEnabled = true
    }
}
