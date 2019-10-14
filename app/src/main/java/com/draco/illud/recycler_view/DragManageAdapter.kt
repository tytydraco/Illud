package com.draco.illud.recycler_view

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.utils.listItems
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.snackbar.Snackbar

class DragManageAdapter(
    private val bottomAppBar: BottomAppBar,
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
        val position = viewHolder.adapterPosition
        val targetItem = listItems.get(position)
        if (direction == ItemTouchHelper.RIGHT) {
            listItems.remove(position)
            adapter.notifyItemRemoved(position)

            /* Allow user to undo item deletion temporarily */
            Snackbar.make(bottomAppBar, "Deleted item.", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    listItems.insert(position, targetItem)
                    adapter.notifyItemInserted(position)
                }
                .setAnchorView(bottomAppBar)
                .show()
        } else if (direction == ItemTouchHelper.LEFT) {
            /* Send first item to back, else to front */
            if (position == 0) {
                /* To back */
                listItems.remove(position)
                listItems.addToBack(targetItem)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemInserted(listItems.size())
            } else {
                /* To front */
                listItems.remove(position)
                listItems.insert(0, targetItem)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemInserted(0)
            }
        }
    }

}