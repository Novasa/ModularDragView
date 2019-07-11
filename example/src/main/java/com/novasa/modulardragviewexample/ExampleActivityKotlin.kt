package com.novasa.modulardragviewexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.novasa.modulardragview.module.DragModule
import com.novasa.modulardragview.module.DragModuleDelegate
import com.novasa.modulardragview.module.DragModuleOpenable
import com.novasa.modulardragview.submodule.SubmoduleTranslatingContentView
import com.novasa.modulardragview.view.DragView
import kotlinx.android.synthetic.main.activity_example.*
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
            items.add(Item(i))
        }

        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(items)
        }
    }

    class Adapter(private val items: List<Item>) : RecyclerView.Adapter<Adapter.DragViewHolder>() {

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DragViewHolder = DragViewHolder(DragView(parent.context))

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DragViewHolder, position: Int) {
            holder.itemView.cell_label.text = "Item ${items[position].id}"
            holder.delegate.reset()
        }

        class DragViewHolder(dragView: DragView) : RecyclerView.ViewHolder(dragView) {

            val delegate: DragModuleDelegate = object : DragModuleDelegate() {

                override fun createDragModule(dragView: DragView, direction: Int): DragModule? =
                    DragModuleOpenable(dragView, LayoutInflater.from(dragView.context).inflate(R.layout.cell_drag_button, dragView, false), direction, R.id.cell_drag_button).also {
                        if (direction == DragView.DIRECTION_LEFT) {
                            it.moduleView.setBackgroundResource(R.color.colorAccent)
                        }
                        val btn: Button = it.moduleView.findViewById(R.id.cell_drag_button)
                        btn.text = when (direction) {
                            DragView.DIRECTION_LEFT -> "Left"
                            DragView.DIRECTION_RIGHT -> "Right"
                            else -> "?"
                        }
                        it.addSubmodule(SubmoduleTranslatingContentView(btn))
                    }

                override fun getTopView(dragView: DragView): View = LayoutInflater.from(dragView.context).inflate(R.layout.cell_item, dragView, false)
            }

            init {
                dragView.delegate = delegate
            }
        }
    }
}
