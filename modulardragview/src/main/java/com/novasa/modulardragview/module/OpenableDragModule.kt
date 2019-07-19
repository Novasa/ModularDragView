package com.novasa.modulardragview.module

import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.novasa.modulardragview.extension.normalizedClamped
import com.novasa.modulardragview.view.DragView
import kotlin.math.abs

/**
 * A drag module that can be opened
 * Created by mikkelschlager on 17/08/16.
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class OpenableDragModule(dragView: DragView, moduleView: View, direction: Int) : DragModule(dragView, moduleView, direction) {

    companion object {

        /** How much the view can be dragged past the content width, relative to the drag view width */
        private const val ADDITIONAL_MAX_DRAG = .15f
    }

    val contentView: View by lazy {
        getContentView(dragView, moduleView, direction).also {
            if (it.parent !== moduleView && moduleView is ViewGroup) {
                moduleView.addView(contentView)
            }
        }
    }

    /**
     * Should return the view that determines the width of the openable container.
     *
     * It should typically be a child of the module view.
     */
    abstract fun getContentView(dragView: DragView, moduleView: View, direction: Int): View

    protected val contentWidth: Float
        get() = contentView.width / dragWidth.toFloat()

    override val teaseX: Float
        get() = contentWidth * direction

    override val maxDrag: Float
        get() = contentWidth + ADDITIONAL_MAX_DRAG

    override fun dragFactor(x0: Float, dx: Float): Float {
        val ax = abs(x0)
        val w = contentWidth
        return if (ax > w && dx * direction > 0f) {
            1f - ax.normalizedClamped(w, maxDrag)
        } else super.dragFactor(x0, dx)
    }

    override fun onDragEnded(x: Float): Boolean {
        val w = contentWidth
        return if (abs(x) > w * .5f) {
            dragView.snapToPosition(w * direction)
            false

            // Returning true to reset drag view
        } else true
    }

    override fun onSwipe(direction: Int, x: Float, v: Float, div: Float): Boolean {
        if (direction == this.direction) {
            Log.d("TAG", "open")
            open(true)

        } else {
            Log.d("TAG", "close")
            close(true)
        }

        return true
    }

    @JvmOverloads
    fun open(animated: Boolean = true) {
        val x1 = contentWidth * direction
        if (animated) {
            dragView.snapToPosition(x1)

        } else {
            dragView.setDragX(x1)
        }
    }
}
