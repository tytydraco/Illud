package com.draco.illud.recycler_view

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.utils.listItems

class RecyclerViewDragHelper(
    private var adapter: RecyclerViewAdapter,
    private var recyclerView: RecyclerView,
    dragDirs: Int,
    swipeDirs: Int):
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
    /* Start swapping the positions of our list and save it to shared preferences */
    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean
    {
        adapter.swapItems(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    /* Depending on which direction we swipe, process a different action */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
    {
        /* Swipe right to delete */
        if (direction == ItemTouchHelper.RIGHT) {
            adapter.deleteItemWithUndo(viewHolder)
        } else if (direction == ItemTouchHelper.LEFT) {
            val position = viewHolder.adapterPosition
            val targetItem = listItems.get(position)

            /* Send first item to back, else to front */
            if (position == 0) {
                /* To back */
                listItems.remove(position)
                listItems.addToBack(targetItem)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemInserted(listItems.size() - 1)
                recyclerView.scrollToPosition(listItems.size() - 1)
            } else {
                /* To front */
                listItems.remove(position)
                listItems.insert(0, targetItem)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
            }
        }
    }

}