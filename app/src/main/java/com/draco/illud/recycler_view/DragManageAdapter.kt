package com.draco.illud.recycler_view

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.utils.listItems
import com.draco.illud.utils.makeUndoSnackbar

class DragManageAdapter(
    private var adapter: RecyclerViewAdapter,
    dragDirs: Int,
    swipeDirs: Int):
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs)
{

    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean
    {
        adapter.swapItems(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
    {
        if (direction == ItemTouchHelper.RIGHT ||
            direction == ItemTouchHelper.LEFT) {
            val deletedPosition = viewHolder.adapterPosition
            val deletedItem = listItems.get(deletedPosition)

            listItems.remove(deletedPosition)
            adapter.notifyItemRemoved(deletedPosition)
            adapter.update(false)

            /* Allow user to undo item deletion temporarily */
            makeUndoSnackbar(viewHolder.itemView, "Deleted item.") {
                listItems.insert(deletedPosition, deletedItem.first, deletedItem.second)
                adapter.notifyItemInserted(deletedPosition)
                adapter.update(false)
            }
        }
    }

}