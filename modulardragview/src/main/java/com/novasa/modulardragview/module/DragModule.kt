package com.novasa.modulardragview.module

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.novasa.modulardragview.view.DragView
import java.util.*

/**
 * Created by mikkelschlager on 17/08/16.
 */
abstract class DragModule(val dragView: DragView, val moduleView: View, val direction: Int) {

    private var submodules: MutableList<Submodule>? = null

    val dragWidth: Int
        get() = dragView.width

    abstract val teaseX: Float

    open val maxDrag: Float
        get() = -1f

    interface Submodule {
        fun onChanged(module: DragModule, x0: Float, x1: Float, dragged: Boolean)
        fun onReset(module: DragModule)
    }

    init {
        if (moduleView.parent !== dragView) {
            dragView.addView(moduleView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        // Allow moduleView measuring
        moduleView.visibility = View.INVISIBLE
    }

    fun addSubmodule(submodule: Submodule) {
        if (submodules == null) {
            submodules = ArrayList()
        }

        submodules?.add(submodule)
    }

    private fun showView() {
        if (!moduleView.isVisible) {
            moduleView.isVisible = true
        }
    }

    private fun hideView() {
        if (!moduleView.isInvisible) {
            moduleView.isInvisible = true
        }
    }

    fun isWithinSwipeDirection(x: Float): Boolean = DragView.getDirection(x) == direction

    fun detach() {
        (moduleView.parent as? ViewGroup)?.run {
            removeView(moduleView)
        }
    }

    fun close(animate: Boolean) {
        dragView.reset(animate)
    }

    /* Overridable methods */

    open fun onSetup() {}

    @CallSuper
    open fun onChanged(x0: Float, x1: Float, dragged: Boolean) {
        if (isWithinSwipeDirection(x1)) {
            showView()

        } else {
            hideView()
        }

        submodules?.forEach {
            it.onChanged(this, x0, x1, dragged)
        }
    }

    /**
     * Called if the drag module is swiped.
     *
     * @param direction The direction of the swipe
     * @param x normalized x value of the drag view at the time of the swipe release.
     * @param v the swipe velocity in pixels/millisecond
     * @param div the swipe velocity in dip/millisecond
     * @return true to consume the event, false to end the drag as normal.
     */
    open fun onSwipe(direction: Int, x: Float, v: Float, div: Float): Boolean = false

    /**
     * Called when the drag ends (i.e. the user lifts the finger)
     * This is not called if [.onSwipe] was called and returned true.
     *
     * @param x normalized x value of the drag view at the time of the event.
     * @return true if the drag view should reset, false otherwise.
     */
    open fun onDragEnded(x: Float): Boolean = true

    open fun dragFactor(x0: Float, dx: Float): Float = 1f

    @CallSuper
    open fun onReset() {
        hideView()

        submodules?.forEach {
            it.onReset(this)
        }
    }
}
