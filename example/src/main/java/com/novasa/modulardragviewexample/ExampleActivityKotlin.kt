package com.novasa.modulardragviewexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.novasa.modulardragview.module.*
import com.novasa.modulardragview.submodule.ExpandingSwipeViewSubmodule
import com.novasa.modulardragview.submodule.TranslatingContentViewSubmodule
import com.novasa.modulardragview.view.DragView
import kotlinx.android.synthetic.main.activity_example.*
import kotlinx.android.synthetic.main.cell_drag_button.view.*
import kotlinx.android.synthetic.main.cell_drag_label.view.*
import kotlinx.android.synthetic.main.cell_item.view.*

class ExampleActivityKotlin : AppCompatActivity() {

    companion object {
        private const val ITEM_COUNT = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        val items = ArrayList<Item>(ITEM_COUNT)
        for (i in 1..ITEM_COUNT) {
            items.add(Item(i, i % 3 + 1))
        }

        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(items)
        }
    }

    class Adapter(private val items: List<Item>) : RecyclerView.Adapter<Adapter.DragViewHolder>() {

        companion object {
            const val VIEW_TYPE_OPENABLE = 1
            const val VIEW_TYPE_SWIPE = 2
            const val VIEW_TYPE_TICKER = 3
        }

        override fun getItemCount(): Int = items.size

        override fun getItemViewType(position: Int): Int {
            return items[position].type
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DragViewHolder {
            val view = DragView(parent.context)
            return when (viewType) {
                VIEW_TYPE_OPENABLE -> OpenableViewHolder(view)
                VIEW_TYPE_SWIPE -> SwipeViewHolder(view)
                VIEW_TYPE_TICKER -> TickerViewHolder(view)
                else -> throw IllegalArgumentException("View type $viewType not implemented")
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DragViewHolder, position: Int) {
            holder.itemView.cellLabel.text = "Item ${items[position].id}"
            holder.delegate.reset()
        }

        abstract class DragViewHolder(dragView: DragView) : RecyclerView.ViewHolder(dragView) {

            val delegate: DragModuleDelegate = object : DragModuleDelegate() {

                override fun createDragModule(dragView: DragView, direction: Int): DragModule? =
                    dragModule(dragView, direction)?.also {
                        if (direction == DragView.DIRECTION_LEFT) {
                            it.moduleView.setBackgroundResource(R.color.colorAccent)
                        }
                    }

                override fun getTopView(dragView: DragView): View =
                    LayoutInflater.from(dragView.context).inflate(R.layout.cell_item, dragView, false)
            }

            abstract fun dragModule(dragView: DragView, direction: Int): DragModule?

            init {
                dragView.delegate = delegate
            }
        }

        class OpenableViewHolder(dragView: DragView) : DragViewHolder(dragView) {
            override fun dragModule(dragView: DragView, direction: Int): DragModule? =
                OpenableDragModule(dragView, LayoutInflater.from(dragView.context).inflate(R.layout.cell_drag_button, dragView, false), direction, R.id.cellDragButton).also {

                    with(it.moduleView.cellDragButton) {
                        text = when (direction) {
                            DragView.DIRECTION_LEFT -> "Open Left"
                            DragView.DIRECTION_RIGHT -> "Open Right"
                            else -> "?"
                        }
                        it.addSubmodule(TranslatingContentViewSubmodule(this))
                    }
                }
        }

        class SwipeViewHolder(dragView: DragView) : DragViewHolder(dragView) {
            override fun dragModule(dragView: DragView, direction: Int): DragModule? =
                SwipeDragModule(dragView, LayoutInflater.from(dragView.context).inflate(R.layout.cell_drag_label, dragView, false), direction).also {

                    with(it.moduleView.cellDragLabel) {
                        text = when (direction) {
                            DragView.DIRECTION_LEFT -> "Swipe Left"
                            DragView.DIRECTION_RIGHT -> "Swipe Right"
                            else -> "?"
                        }
                        it.addSubmodule(ExpandingSwipeViewSubmodule(this))
                    }

                    it.callback = object : SwipeDragModule.Callback {
                        override fun onModuleSwipeStarted(module: SwipeDragModule) {

                        }

                        override fun onModuleSwipeFinished(module: SwipeDragModule) {
                            dragView.postDelayed({
                                dragView.reset(true)
                            }, 1000)
                        }
                    }
                }
        }

        class TickerViewHolder(dragView: DragView) : DragViewHolder(dragView) {

            override fun dragModule(dragView: DragView, direction: Int): DragModule? =
                TickerDragModule(dragView, LayoutInflater.from(dragView.context).inflate(R.layout.cell_drag_label, dragView, false), direction).also {
                    with(it.moduleView.cellDragLabel) {
                        text = when (direction) {
                            DragView.DIRECTION_LEFT -> "Tick Left"
                            DragView.DIRECTION_RIGHT -> "Tick Right"
                            else -> "?"
                        }
                        it.addSubmodule(TranslatingContentViewSubmodule(this))
                    }

                    it.callback = object : TickerDragModule.Callback {
                        override fun onModuleTicked(module: TickerDragModule) {

                        }
                    }
                }
        }
    }
}
