package com.novasa.modulardragview.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import kotlin.math.abs
import kotlin.math.max

/**
 * Created by mikkelschlager on 18/10/16.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class DragView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {

        /** Indicates no drag, or that the view is drag view is closed. */
        const val DIRECTION_NONE = 0

        /** Indicates that the view is being dragged right to left, or that the drag view is opened in the corresponding the direction. */
        const val DIRECTION_LEFT = -1

        /** Indicates that the view is being dragged left to right, or that the drag view is opened in the corresponding the direction. */
        const val DIRECTION_RIGHT = 1

        private const val SNAP_MIN_DURATION = 50
        private const val SNAP_MAX_DURATION = 200

        private const val SWIPE_DEFAULT_MIN_VELOCITY = 1.5f

        @JvmStatic
        fun getDirection(x: Float): Int = when {
            x > 0 -> DIRECTION_RIGHT
            x < 0 -> DIRECTION_LEFT
            else -> DIRECTION_NONE
        }
    }

    var delegate: Delegate? = null
        set(value) {
            field = value

            delegate?.let { d ->
                d.onDragViewInit(this)
                topView = d.getDragViewTopView(this)
                if (topView.parent !== this) {
                    addView(
                        topView,
                        LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    )
                }

                doOnLayout {
                    d.onDragViewLayout(this)
                }
            }
        }


    lateinit var topView: View
        private set

    private var touchSlop: Int = 0
    private var density: Float = 0.toFloat()

    private val snapInterpolator = DecelerateInterpolator()
    private var valueAnimator: ValueAnimator? = null

    val isOpen: Boolean
        get() = currentDirection != DIRECTION_NONE

    var currentDirection = DIRECTION_NONE
        private set

    /**
     * Determines if the drag view responds to drag touch events.
     *  Default = true
     */
    var isDragEnabled = true

    /**
     * If the view is openable on both sides, this determines if the drag can continue from one side to the other uninterrupted.
     * If this is false, the user must lift the finger and reapply the drag to open the opposite side once the drag view has been closed.
     *
     * Default = false
     */
    var allowCrossDrag = false

    var swipeMinVelocity: Float = SWIPE_DEFAULT_MIN_VELOCITY

    val dragViewWidth: Float
        get() = width.toFloat()

    /**
     * The current amount that the view is dragged. This is normalized between -1 and 1.
     */
    var currentDragX: Float = 0f
        private set

    private var prevX: Float = 0f
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialDragDirection = DIRECTION_NONE
    private var dragDidBegin = false
    private var dragDidEnd = false
    private var velocityTracker: VelocityTracker? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        density = resources.displayMetrics.density
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        updateLayoutParams {
            width = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {

        if (delegate == null) return false

        // onInterceptTouchEvent is always called for the ACTION_DOWN event.
        // after that, it is only called if there are any viable child views that can potentially receive the event.
        // This means that we have to check for the drag to start in both onInterceptTouchEvent and onTouchEvent

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                dragDidEnd = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragEnabled) {
                    // Check if the drag should start
                    return onMove(event)
                }
            }
        }

        // Do not intercept touch event, let the child handle it
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (delegate == null) return false

        var didSwipe = false

        fun onDragEnded() {
            if (dragDidBegin) {
                dragDidBegin = false
                dragDidEnd = true
                parent.requestDisallowInterceptTouchEvent(false)

                if (!didSwipe) {
                    delegate?.onDragViewDragEnd(this, currentDirection, currentDragX)
                }
            }

            velocityTracker?.run {
                recycle()
                velocityTracker = null
            }
        }

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()

                } else {
                    velocityTracker?.clear()
                }

                velocityTracker?.addMovement(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)

                // We still want to be able to send onClick events if drag is disabled
                return if (isDragEnabled) {
                    onMove(event)

                } else true
            }

            MotionEvent.ACTION_UP -> {

                if (!dragDidBegin) {
                    performClick()

                } else {
                    velocityTracker?.let {
                        it.computeCurrentVelocity(1)
                        val v = it.xVelocity
                        val av = abs(v)
                        val adiv = av / density

                        if (adiv >= swipeMinVelocity) {
                            val swipeDirection = getDirection(v)

                            didSwipe = delegate?.onDragViewSwipe(this, currentDirection, swipeDirection, currentDragX, av, adiv) ?: false
                        }
                    }
                }

                onDragEnded()
                return true
            }

            // Fall through
            MotionEvent.ACTION_CANCEL -> {
                onDragEnded()
                return true
            }
        }

        return false
    }

    override fun performClick(): Boolean {
        if (!super.performClick()) {
            delegate?.onDragViewTopViewClick(this)
        }
        return true
    }

    private fun onMove(event: MotionEvent): Boolean {
        if (dragDidEnd) {
            return false
        }

        val x1 = event.x

        if (dragDidBegin) {
            // The drag has already begun, carry on with it.
            // This is only relevant from the onTouchEvent.
            onDrag(topView.x, x1 - prevX)
            prevX = x1

            return true
        }

        val dx = x1 - initialX
        val dy = event.y - initialY

        val adx = abs(dx)
        val ady = abs(dy)

        // Initiate drag if |dx| > |dy| and |dx| is greater than a lower boundary for drags.
        // If the event has not crossed this boundary, child views can receive clicks
        if (adx > ady && adx > touchSlop) {
            return onBeginDrag(x1, dx)
        }

        return false
    }

    private fun onBeginDrag(x1: Float, dx: Float): Boolean {
        val x0 = topView.x
        val direction: Int

        if (x0 == 0f) {
            direction = getDirection(dx)

            // Check if the drag is legal
            if (delegate?.canDragViewDrag(this, direction) != true) {
                return false
            }

        } else {
            direction = getDirection(x0)
        }

        initialDragDirection = direction
        currentDirection = direction
        dragDidBegin = true
        parent.requestDisallowInterceptTouchEvent(true)

        delegate?.onDragViewDragBegin(this, currentDirection)

        prevX = x1

        return true
    }

    private fun onDrag(x0: Float, dx: Float) {

        // Normalize (-1 - 1)
        val normalizedX0: Float = x0 / dragViewWidth
        var normalizedDx: Float = dx / dragViewWidth

        var direction = getDirection(normalizedX0 + normalizedDx)
        if (direction != currentDirection) {
            // Direction has changed mid drag
            if (direction != DIRECTION_NONE && direction != initialDragDirection && (!allowCrossDrag || delegate?.canDragViewDrag(this, direction) == false)) {
                direction = DIRECTION_NONE
            }
        }

        if (direction != DIRECTION_NONE) {
            val dragDirection = getDirection(normalizedDx)

            val factor = delegate?.getDragViewDragFactor(this, direction, dragDirection, normalizedX0, normalizedDx) ?: 1f
            val max: Float = delegate?.getDragViewMaxDrag(this, direction) ?: 0f

            normalizedDx *= factor

            var normalizedX1 = normalizedX0 + normalizedDx

            if (max > 0f && abs(normalizedX1) > max) {
                normalizedX1 = max * direction
            }

            normalizedX1 = delegate?.onDragViewWillDrag(this, direction, normalizedX0, normalizedX1) ?: normalizedX1
            setDragX(normalizedX1)

        } else {
            setDragX(0f)
        }
    }

    fun setDragX(normalizedX1: Float) {

        // Denormalize
        val absoluteX1: Float = normalizedX1 * dragViewWidth

        if (absoluteX1 != topView.x) {

            val normalizedX0 = currentDragX
            currentDragX = normalizedX1

            topView.x = absoluteX1

            currentDirection = getDirection(currentDragX)

            delegate?.onDragViewChanged(this, currentDirection, normalizedX0, currentDragX, dragDidBegin)

            if (currentDirection == DIRECTION_NONE) {
                delegate?.onDragViewReset(this)
            }
        }
    }

    @JvmOverloads
    fun reset(animate: Boolean = false, end: (() -> Unit)? = null) {
        if (animate) {
            snapToPosition(0f, end)

        } else {
            stopAnimation()
            setDragX(0f)
            end?.invoke()
        }
    }

    @JvmOverloads
    fun snapToPosition(normalizedX1: Float, end: (() -> Unit)? = null) {
        val absX = abs(currentDragX - normalizedX1)
        val duration = (absX * SNAP_MAX_DURATION + (1f - absX) * SNAP_MIN_DURATION).toLong()

        animateToPosition(normalizedX1, duration, snapInterpolator, end)
    }

    fun animateToPosition(x1: Float, duration: Long, interpolator: Interpolator?, end: (() -> Unit)?) {

        stopAnimation()

        valueAnimator = ValueAnimator().also { a ->

            a.setObjectValues(currentDragX, x1)
            a.duration = max(duration, 0L)
            a.interpolator = interpolator
            a.addUpdateListener { animation -> setDragX(animation.animatedValue as Float) }
            a.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    valueAnimator = null
                    end?.invoke()
                }
            })
            a.start()
        }
    }

    fun stopAnimation() {
        valueAnimator?.cancel()
    }

    interface Delegate {

        /**
         * Called when the delegate is set, so its implementation can establish view references.
         * View width is NOT guaranteed to be set here. Wait for onSetup() to be called if width is needed.
         */
        fun onDragViewInit(dragView: DragView)

        /**
         * Called when the view has been layed out, and width has been set
         */
        fun onDragViewLayout(dragView: DragView)

        /**
         * Return the top view, which will be the view that is dragged.
         *
         * This will be called exactly once when setting the delegate.
         */
        fun getDragViewTopView(dragView: DragView): View

        /**
         * If the drag view can be dragged to the specified direction
         * @param direction either [DIRECTION_LEFT] or [DIRECTION_RIGHT]
         */
        fun canDragViewDrag(dragView: DragView, direction: Int): Boolean

        /**
         * The maximum amount the view can be dragged to the specified direction before blocking.
         * @param direction either [DIRECTION_LEFT] or [DIRECTION_RIGHT]
         * @return The amount normalized 0-1.
         */
        fun getDragViewMaxDrag(dragView: DragView, direction: Int): Float

        /**
         * How much the drag view should respond to the drag.
         *
         * @param direction The direction that the drag view has been dragged. either [DIRECTION_LEFT] or [DIRECTION_RIGHT].
         * @param dragDirection The direction of the drag.
         *  This is not the same as dragDirection, as it represents the change since last frame, instead of the absolute drag direction of the view.
         *  E.g. if the view is open to the right, but is being dragged to the left to close it.
         * @param x0 The current position of the drag view, normalized between -1 and 1
         * @param dx The drag event dx, normalized between -1 and 1
         * @return the factor that the drag view should move relative to the drag event.
         * 1 to follow the drag, 0 to lock it in place. Can also be > 1 or < 0.
         */
        fun getDragViewDragFactor(dragView: DragView, direction: Int, dragDirection: Int, x0: Float, dx: Float): Float

        /**
         * Called when the drag begins.
         * @param direction either [DIRECTION_LEFT] or [DIRECTION_RIGHT]
         */
        fun onDragViewDragBegin(dragView: DragView, direction: Int)

        /**
         * Called before the view is about to be dragged. If the drag is undesired, it can be altered.
         *
         * @param x0 normalized x0 (before drag)
         * @param x1 normalized x1 (after drag)
         * @return the desired x1. If no change is needed, x1 should be returned.
         */
        fun onDragViewWillDrag(dragView: DragView, direction: Int, x0: Float, x1: Float): Float

        /**
         * Called whenever the drag view is moved, either by dragging or by the listener
         *
         * @param x0      normalized x0 (before change)
         * @param x1      normalized x1 (after change)
         * @param dragged true if the view is being dragged
         */
        fun onDragViewChanged(dragView: DragView, direction: Int, x0: Float, x1: Float, dragged: Boolean)

        /**
         * Called if the drag view is swiped left
         *
         * @param dragDirection the current drag direction. Either [DIRECTION_LEFT] or [DIRECTION_RIGHT]
         * @param swipeDirection the direction of the swipe, either [DIRECTION_LEFT] or [DIRECTION_RIGHT]
         * @param x   the x position of the view at the time of the swipe release, normalized between -1 and 1
         * @param v   the absolute swipe velocity in pixels/millisecond
         * @param div the absolute swipe velocity in dip/millisecond
         * @return true if the swipe event is handled, false to end the drag normally.
         */
        fun onDragViewSwipe(dragView: DragView, dragDirection: Int, swipeDirection: Int, x: Float, v: Float, div: Float): Boolean

        /**
         * Called when the drag view has been reset - after the animation
         */
        fun onDragViewReset(dragView: DragView)

        /**
         * Called when the drag event ends
         *
         * @param x the normalized x position of the view at the end of the drag
         */
        fun onDragViewDragEnd(dragView: DragView, direction: Int, x: Float)

        /**
         * Called when the top view is clicked
         */
        fun onDragViewTopViewClick(dragView: DragView)
    }
}
