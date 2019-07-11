package com.novasa.modulardragview.submodule

import android.view.View
import com.novasa.modulardragview.extension.normalizedClamped
import com.novasa.modulardragview.module.DragModule
import com.novasa.modulardragview.view.DragView

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SubmoduleTranslatingContentView(val contentView: View) : DragModule.Submodule {

    override fun onChanged(module: DragModule, x0: Float, x1: Float, dragged: Boolean) {
        if (module.isWithinSwipeDirection(x1)) {

            val direction = module.direction
            val dragWidth = module.dragWidth.toFloat()
            val x = x1 * direction
            val w: Float = (contentView.width / dragWidth)

            var contentX1 = 0f
            when (direction) {
                DragView.DIRECTION_RIGHT -> contentX1 = x * .5f
                DragView.DIRECTION_LEFT -> contentX1 = 1f - x * .5f
            }

            // Half way between drag and edge
            contentX1 -= w * .5f

            contentView.x = contentX1 * dragWidth

            if (x <= w) {
                val a = x.normalizedClamped(0f, w)
                contentView.alpha = a

            } else if (x > w && contentView.alpha < 1f) {
                contentView.alpha = 1f
            }
        }
    }

    override fun onReset(module: DragModule) {
        contentView.alpha = 0f
    }
}
