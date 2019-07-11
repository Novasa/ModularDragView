package com.novasa.modulardragview.module

import android.annotation.SuppressLint
import android.view.animation.AccelerateDecelerateInterpolator
import com.novasa.modulardragview.view.DragView

/**
 * Created by mikkelschlager on 10/08/16.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class DragModuleDelegate : DragView.Delegate {

    protected lateinit var dragView: DragView

    @SuppressLint("UseSparseArrays")
    private val dragModules = HashMap<Int, DragModule>()
    private var currentDragDirection: Int = 0

    private var isTeasing: Boolean = false
    private val teaseInterpolator by lazy {
        AccelerateDecelerateInterpolator()
    }

    val isOpen: Boolean
        get() = dragView.dragDirection != DragView.DIRECTION_NONE

    var isDragEnabled: Boolean
        get() = dragView.isDragEnabled
        set(enabled) {
            dragView.isDragEnabled = enabled
        }

    /** If the view has modules on either side, this determines if the drag can continue from one to the other uninterrupted
     *
     * Default = false
     */
    var allowCrossDrag = false

    protected abstract fun createDragModule(dragView: DragView, direction: Int): DragModule?

    fun getDragModule(direction: Int): DragModule? = dragModules[direction]

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

    override fun init(dragView: DragView) {
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

    override fun onSetup() = dragModules.values.forEach {
        it.onSetup()
    }

    override fun canDrag(direction: Int): Boolean = dragModules[direction] != null

    override fun getMaxDrag(direction: Int): Float = dragModules[direction]?.maxDrag ?: 0f

    override fun dragFactor(direction: Int, x0: Float, dx: Float): Float = dragModules[direction]?.dragFactor(x0, dx)
        ?: 1f

    override fun onDragBegin(direction: Int) {
        currentDragDirection = direction
    }

    override fun onDragged(direction: Int, x0: Float, x1: Float): Float {

        cancelTease()

        // Block drag when dragging to a different side than we started
        return if (!allowCrossDrag && direction != currentDragDirection) 0f else x1
    }

    override fun onChanged(direction: Int, x0: Float, x1: Float, dragged: Boolean) {
        dragModules[direction]?.onChanged(x0, x1, dragged)
    }

    override fun onReset() {
        currentDragDirection = DragView.DIRECTION_NONE
        dragModules.values.forEach {
            it.onReset()
        }
    }

    override fun onSwipe(direction: Int, x: Float, v: Float, div: Float): Boolean {
        if (direction == currentDragDirection) {
            dragModules[direction]?.onSwipe(x, v, div)
        }
        return false
    }

    override fun onDragEnded(direction: Int, x: Float) {

        var shouldReset = true

        dragModules[direction]?.let {
            shouldReset = it.onDragEnded(x)
        }

        if (shouldReset) {
            dragView.reset(true)
        }
    }

    override fun onClick() {

    }
}
