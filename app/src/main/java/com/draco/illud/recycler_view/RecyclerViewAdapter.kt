package com.draco.illud.recycler_view

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.R
import com.draco.illud.activity.ViewMoreActivity
import com.draco.illud.utils.Constants
import com.draco.illud.utils.ListItems
import com.google.android.material.snackbar.Snackbar

class RecyclerViewAdapter(
    private val recyclerView: RecyclerView,
    private val listItems: ListItems,
    private val emptyView: View):
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    /* Holds our views inside our row view */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bullet = view.findViewById<ImageView>(R.id.bullet)!!
        val label = view.findViewById<TextView>(R.id.label)!!
        val tag = view.findViewById<TextView>(R.id.tag)!!
    }

    /* Check if list is empty; if so, show the empty view */
    private fun checkEmptyView() {
        if (listItems.size() == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    /* Inflate our rows for each item */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(view)
    }

    /* Check if our list is empty when we attach our recycler view */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        checkEmptyView()
    }

    /* Return our item count */
    override fun getItemCount(): Int {
        checkEmptyView()
        return listItems.size()
    }

    /* Swap to items in a list */
    fun swapItems(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                val oldItem = listItems.get(i + 1)
                val newItem = listItems.get(i)
                listItems.set(i + 1, newItem)
                listItems.set(i, oldItem)
            }
        } else {
            for (i in fromPosition..toPosition + 1) {
                val oldItem = listItems.get(i - 1)
                val newItem = listItems.get(i)
                listItems.set(i - 1, newItem)
                listItems.set(i, oldItem)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }

    /* Delete item at viewholder position, but allow user to undo */
    fun deleteItemWithUndo(holder: RecyclerView.ViewHolder) {
        val updatedPosition = holder.adapterPosition
        val updatedItem = listItems.get(updatedPosition)

        listItems.remove(updatedPosition)
        notifyItemRemoved(updatedPosition)

        /* Allow user to undo item deletion temporarily */
        Snackbar.make(recyclerView, "Deleted item.", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                listItems.insert(updatedPosition, updatedItem)
                notifyItemInserted(updatedPosition)
                recyclerView.scrollToPosition(updatedPosition)
            }
            .show()
    }

    /* Configure each holder view */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listItems.get(position)

        /* Clicking the dismiss button */
        holder.bullet.setOnClickListener {
            deleteItemWithUndo(holder)
        }

        /* Set the text */
        holder.label.text = item.label
        holder.tag.text = item.tag

        /* If tag has no contents, remove from view */
        if (holder.tag.text.isBlank())
            holder.tag.visibility = View.GONE
        else
            holder.tag.visibility = View.VISIBLE

        /* Set the external click listener */
        holder.itemView.setOnClickListener {
            /* The local position variable may be outdated */
            val updatedPosition = holder.adapterPosition
            val updatedItem = listItems.get(updatedPosition)

            /* Start the ViewMore activity when we click an item */
            val intent = Intent(recyclerView.context, ViewMoreActivity::class.java)
                .putExtra("itemString", updatedItem.toString())
                .putExtra("position", updatedPosition)

            /* Use parent activity to handle result of item edit */
            (recyclerView.context as Activity)
                .startActivityForResult(intent, Constants.VIEW_MORE_ACTIVITY_RESULT_CODE)
        }
    }
}