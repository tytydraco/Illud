package com.draco.illud.recycler_view

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.utils.ListItems
import java.util.*

class RecyclerViewDragHelper(
    private var adapter: RecyclerViewAdapter,
    private var recyclerView: RecyclerView,
    private val listItems: ListItems):
    ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
    /* Start swapping the positions of our list and save it to shared preferences */
    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        Collections.swap(listItems.items, viewHolder.adapterPosition, target.adapterPosition)
        adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    /* Depending on which direction we swipe, process a different action */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        /* Swipe right to delete */
        if (direction == ItemTouchHelper.RIGHT) {
            adapter.deleteItemWithUndo(viewHolder)
        } else if (direction == ItemTouchHelper.LEFT) {
            val position = viewHolder.adapterPosition
            val targetItem = listItems.items[position]

            /* Send first item to back, else to front */
            if (position == 0) {
                /* To back */
                listItems.items.removeAt(position)
                listItems.items.add(listItems.items.size, targetItem)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemInserted(listItems.items.size - 1)
                recyclerView.scrollToPosition(listItems.items.size - 1)
            } else {
                /* To front */
                listItems.items.removeAt(position)
                listItems.items.add(0, targetItem)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
            }
        }
    }

}