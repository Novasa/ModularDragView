package com.novasa.modulardragview.submodule

import android.view.View
import android.view.animation.AccelerateInterpolator
import com.novasa.modulardragview.extension.denormalized
import com.novasa.modulardragview.extension.normalizedClamped
import com.novasa.modulardragview.module.DragModule
import com.novasa.modulardragview.module.DragModuleSwipe
import kotlin.math.abs

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SubmoduleExpandingSwipeLabel(val label: View) : DragModule.Submodule {

    companion object {

        // When the label is fully opaque
        private const val IN_BOUNDARY = .15f

        // How much the label is translated compared to the drag view
        private const val TRANSLATE_FACTOR = .5f

        // Where the label starts it's translation compared to the edge of the drag view
        private const val TRANSLATE_X0 = -.1f

        // How large the label is at the end of the swipe
        private const val SCALE_MAX = 3f
    }

    private val interpolator = AccelerateInterpolator()

    override fun onChanged(module: DragModule, x0: Float, x1: Float, dragged: Boolean) {
        val ax = abs(x1)

        if (module.isWithinSwipeDirection(x1)) {
            val labelX1 = TRANSLATE_X0 * module.direction + x1 * TRANSLATE_FACTOR
            label.x = labelX1 * module.dragWidth

            if (ax <= IN_BOUNDARY) {
                val a = ax.normalizedClamped(0f, IN_BOUNDARY)
                label.alpha = a

            } else if (ax > IN_BOUNDARY && ax <= DragModuleSwipe.OUT_BOUNDARY && label.alpha < 1f) {
                label.alpha = 1f

            } else if (ax > DragModuleSwipe.OUT_BOUNDARY) {
                var out = ax.normalizedClamped(DragModuleSwipe.OUT_BOUNDARY, 1f)
                out = interpolator.getInterpolation(out)

                val a = 1f - out
                val scale = out.denormalized(1f, SCALE_MAX)

                with(label) {
                    alpha = a
                    scaleX = scale
                    scaleY = scale
                }
            }
        }
    }

    override fun onReset(module: DragModule) {
        with(label) {
            x = TRANSLATE_X0
            alpha = 0f
            scaleX = 1f
            scaleY = 1f
        }
    }
}
