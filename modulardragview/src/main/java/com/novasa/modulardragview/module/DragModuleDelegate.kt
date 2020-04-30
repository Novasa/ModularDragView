package com.novasa.modulardragview.module

import android.annotation.SuppressLint
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.CallSuper
import com.novasa.modulardragview.view.DragView

/**
 * Created by mikkelschlager on 10/08/16.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class DragModuleDelegate : DragView.Delegate {

    lateinit var dragView: DragView
        private set

    @SuppressLint("UseSparseArrays")
    private val dragModules = HashMap<Int, DragModule>()
    private var openDragModule: DragModule? = null

    private var isTeasing: Boolean = false
    private val teaseInterpolator by lazy {
        AccelerateDecelerateInterpolator()
    }

    protected abstract fun createDragModule(dragView: DragView, direction: Int): DragModule?

    fun getDragModule(direction: Int): DragModule? = dragModules[direction]

    var enabled: Boolean
        get() = dragView.isDragEnabled
        set(value) {
            dragView.isDragEnabled = value
            if (!value) {
                reset()
            }
        }

    @JvmOverloads
    fun reset(animate: Boolean = false) {
        cancelTease()
        dragView.reset(animate)
    }

    @JvmOverloads
    fun tease(delay: Long = 0L) {

        if (delay > 0) {
            dragView.postDelayed({
                tease(0)
            }, delay)
            return
        }

        val right: DragModule? = dragModules[DragView.DIRECTION_RIGHT]
        val left: DragModule? = dragModules[DragView.DIRECTION_LEFT]

        right?.let { r ->
            isTeasing = true

            tease(r, 180) {
                left?.let { l ->
                    tease(l, 250, this::endTease)
                } ?: endTease()
            }
        } ?: left?.let { l ->
            isTeasing = true
            tease(l, 180, this::endTease)
        }
    }

    private fun tease(module: DragModule, duration: Long, end: () -> Unit) =
        dragView.animateToPosition(module.teaseX, duration, teaseInterpolator, end)

    private fun endTease() = dragView.animateToPosition(0f, 180, teaseInterpolator) {
        isTeasing = false
    }

    fun cancelTease() {
        if (isTeasing) {
            dragView.stopAnimation()
            isTeasing = false
        }
    }

    @CallSuper
    override fun onDragViewInit(dragView: DragView) {
        this.dragView = dragView

        with(dragModules) {
            if (isNotEmpty()) {
                values.forEach {
                    it.detach()
                }
                clear()
            }
        }

        val directions = intArrayOf(DragView.DIRECTION_RIGHT, DragView.DIRECTION_LEFT)

        for (direction in directions) {
            val module = createDragModule(dragView, direction)
            if (module != null) {
                dragModules[direction] = module
            }
        }
    }

    @CallSuper
    override fun onDragViewLayout(dragView: DragView) = dragModules.values.forEach {
        it.onLayout()
    }

    @CallSuper
    override fun canDragViewDrag(dragView: DragView, direction: Int): Boolean = dragModules[direction] != null

    @CallSuper
    override fun getDragViewMaxDrag(dragView: DragView, direction: Int): Float = dragModules[direction]?.maxDrag ?: 0f

    @CallSuper
    override fun getDragViewDragFactor(dragView: DragView, direction: Int, dragDirection: Int, x0: Float, dx: Float): Float = dragModules[direction]?.dragFactor(x0, dx)
        ?: 1f

    override fun onDragViewDragBegin(dragView: DragView, direction: Int) {
    }

    @CallSuper
    override fun onDragViewWillDrag(dragView: DragView, direction: Int, x0: Float, x1: Float): Float {
        cancelTease()
        return x1
    }

    @CallSuper
    override fun onDragViewChanged(dragView: DragView, direction: Int, x0: Float, x1: Float, dragged: Boolean) {
        val module = dragModules[direction]
        if (module != openDragModule) {
            openDragModule?.onChanged(x0, x1, dragged)
            openDragModule = module
        }

        module?.onChanged(x0, x1, dragged)
    }

    @CallSuper
    override fun onDragViewReset(dragView: DragView) {
        dragModules.values.forEach {
            it.onReset()
        }
    }

    @CallSuper
    override fun onDragViewSwipe(dragView: DragView, dragDirection: Int, swipeDirection: Int, x: Float, v: Float, div: Float): Boolean =
        dragModules[dragDirection]?.onSwipe(swipeDirection, x, v, div) ?: false

    @CallSuper
    override fun onDragViewDragEnd(dragView: DragView, direction: Int, x: Float) {

        var shouldReset = true

        dragModules[direction]?.let {
            shouldReset = it.onDragEnded(x)
        }

        if (shouldReset) {
            dragView.reset(true)
        }
    }

    override fun onDragViewTopViewClick(dragView: DragView) {
    }
}
