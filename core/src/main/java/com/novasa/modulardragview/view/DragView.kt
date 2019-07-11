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

/**
 * Created by mikkelschlager on 18/10/16.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class DragView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {

        const val DIRECTION_NONE = 0
        const val DIRECTION_LEFT = -1
        const val DIRECTION_RIGHT = 1

        private const val SNAP_MIN_DURATION = 50
        private const val SNAP_MAX_DURATION = 200

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
                d.init(this)
                topView = d.getTopView(this)
                if (topView.parent !== this) {
                    addView(topView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }

                doOnLayout {
                    d.onSetup()
                }
            }
        }


    lateinit var topView: View
        private set

    private var touchSlop: Int = 0
    private var density: Float = 0.toFloat()

    private val snapInterpolator = DecelerateInterpolator()
    private var valueAnimator: ValueAnimator? = null

    var dragDirection = DIRECTION_NONE
        private set

    var isDragEnabled = true

    val dragViewWidth: Float
        get() = width.toFloat()

    var currentDragX: Float = 0f
        private set

    private var prevX: Float = 0f
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var dragDidBegin = false
    private var dragDidEnd = false
    private var velocityTracker: VelocityTracker? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        val vc = ViewConfiguration.get(context)
        touchSlop = vc.scaledTouchSlop
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
                    delegate?.onDragEnded(dragDirection, currentDragX)
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
                        didSwipe = delegate?.onSwipe(dragDirection, currentDragX, av, adiv) ?: false
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
            delegate?.onClick()
        }
        return true
    }

    private fun onMove(event: MotionEvent): Boolean {
        if (dragDidEnd) {
            return false
        }

        if (dragDidBegin) {
            // The drag has already begun, carry on with it.
            // This is only relevant from the onTouchEvent.
            onDrag(event.x - prevX)
            prevX = event.x

            return true
        }

        val dx = event.x - initialX
        val dy = event.y - initialY

        val adx = abs(dx)
        val ady = abs(dy)

        // Initiate drag if |dx| > |dy| and |dx| is greater than a lower boundary for drags.
        // If the event has not crossed this boundary, child views can receive clicks
        if (adx > ady && adx > touchSlop) {
            val x = topView.x
            val direction: Int

            if (x == 0f) {
                direction = getDirection(dx)

                // Check if the drag is legal
                if (delegate?.canDrag(direction) != true) {
                    return false
                }

            } else {
                direction = getDirection(x)
            }

            dragDirection = direction
            dragDidBegin = true
            parent.requestDisallowInterceptTouchEvent(true)

            delegate?.onDragBegin(dragDirection)

            prevX = event.x

            return true
        }

        return false
    }

    private fun onDrag(absDx: Float) {

        // Normalize (-1 - 1)
        val normalizedX0: Float = topView.x / dragViewWidth
        var normalizedDx: Float = absDx / dragViewWidth

        val direction = getDirection(normalizedX0 + normalizedDx)
        if (direction != dragDirection) {
            // Direction has changed mid drag
            if (delegate?.canDrag(direction) == true) {
                dragDirection = direction

            } else {
                normalizedDx = -normalizedX0
            }
        }

        val factor = delegate?.dragFactor(dragDirection, normalizedX0, normalizedDx) ?: 1f
        val max: Float = delegate?.getMaxDrag(dragDirection) ?: 0f

        normalizedDx *= factor

        var normalizedX1 = normalizedX0 + normalizedDx

        if (max > 0f && abs(normalizedX1) > max) {
            normalizedX1 = max * dragDirection
        }

        normalizedX1 = delegate?.onDragged(dragDirection, normalizedX0, normalizedX1) ?: normalizedX1
        setDragX(normalizedX1)
    }

    fun setDragX(normalizedX1: Float) {

        // Denormalize
        val absoluteX1: Float = normalizedX1 * dragViewWidth

        if (absoluteX1 != topView.x) {

            val normalizedX0 = currentDragX
            currentDragX = normalizedX1

            topView.x = absoluteX1

            if (currentDragX == 0f && !dragDidBegin) {
                dragDirection = DIRECTION_NONE
            }

            delegate?.onChanged(dragDirection, normalizedX0, currentDragX, dragDidBegin)

            if (dragDirection == DIRECTION_NONE) {
                delegate?.onReset()
            }
        }
    }

    @JvmOverloads
    fun reset(animate: Boolean = false) {
        if (animate) {
            snapToPosition(0f)

        } else {
            stopAnimation()
            setDragX(0f)
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
            a.duration = duration
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
         * Return the top view, which will be the view that is dragged.
         */
        fun getTopView(dragView: DragView): View

        /**
         * Called when the delegate is set, so its implementation can establish view references.
         * View width is NOT guaranteed to be set here. Wait for onSetup() to be called if width is needed.
         */
        fun init(dragView: DragView)

        /**
         * Called when the view has been set up, and width has been set
         */
        fun onSetup()

        /**
         * If the drag view can be dragged to the specified direction
         * @param direction either [.DIRECTION_LEFT] or [.DIRECTION_RIGHT]
         */
        fun canDrag(direction: Int): Boolean

        /**
         * The maximum amount the view can be dragged to the specified direction before blocking.
         * @param direction either [.DIRECTION_LEFT] or [.DIRECTION_RIGHT]
         * @return The amount normalized 0-1.
         */
        fun getMaxDrag(direction: Int): Float

        /**
         * How much the drag view should respond to the drag.
         *
         * @param x0 The current position of the drag view, normalized between -1 and 1
         * @param dx The drag event dx, normalized between -1 and 1
         * @return the factor that the drag view should move relative to the drag event.
         * 1 to follow the drag, 0 to lock it in place. Can also be > 1 or < 0.
         */
        fun dragFactor(direction: Int, x0: Float, dx: Float): Float

        /**
         * Called when the drag begins.
         * @param direction either [.DIRECTION_LEFT] or [.DIRECTION_RIGHT]
         */
        fun onDragBegin(direction: Int)

        /**
         * Called when the view has been dragged.
         *
         * @param x0 normalized x0 (before drag)
         * @param x1 normalized x1 (after drag)
         * @return the desired x1. If no change is needed, x1 should be returned.
         */
        fun onDragged(direction: Int, x0: Float, x1: Float): Float

        /**
         * Called whenever the drag view is moved, either by dragging or by the listener
         *
         * @param x0      normalized x0 (before change)
         * @param x1      normalized x1 (after change)
         * @param dragged true if the view is being dragged
         */
        fun onChanged(direction: Int, x0: Float, x1: Float, dragged: Boolean)

        /**
         * Called if the drag view is swiped left
         *
         * @param direction the direction of the swipe, either [.DIRECTION_LEFT] or [.DIRECTION_RIGHT]
         * @param x   the x position of the view at the time of the swipe release, normalized between -1 and 1
         * @param v   the absolute swipe velocity in pixels/millisecond
         * @param div the absolute swipe velocity in dip/millisecond
         * @return true if the swipe event is handled, false to end the drag normally.
         */
        fun onSwipe(direction: Int, x: Float, v: Float, div: Float): Boolean

        /**
         * Called when the drag view has been reset - after the animation
         */
        fun onReset()

        /**
         * Called when the drag event ends
         *
         * @param x the normalized x position of the view at the end of the drag
         */
        fun onDragEnded(direction: Int, x: Float)

        /**
         * Called when the drag view is clicked
         */
        fun onClick()
    }
}
