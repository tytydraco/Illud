package com.draco.illud.recycler_view

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.R
import com.draco.illud.activity.ViewMoreActivity
import com.draco.illud.utils.listItems
import com.google.android.material.snackbar.Snackbar

class RecyclerViewAdapter(
    private val recyclerView: RecyclerView,
    private val snackbarAnchor: View):
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    /* Holds our views inside our row view */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bullet = view.findViewById<ImageView>(R.id.bullet)!!
        val label = view.findViewById<TextView>(R.id.label)!!
        val tag = view.findViewById<TextView>(R.id.tag)!!
    }

    /* Inflate our rows for each item */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(view)
    }

    /* Return our item count */
    override fun getItemCount(): Int {
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

    /* Configure each holder view */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listItems.get(position)

        holder.bullet.setOnClickListener {
            val updatedPosition = holder.adapterPosition
            val updatedItem = listItems.get(updatedPosition)

            listItems.remove(updatedPosition)
            notifyItemRemoved(updatedPosition)

            /* Allow user to undo item deletion temporarily */
            Snackbar.make(snackbarAnchor, "Deleted item.", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    listItems.insert(updatedPosition, updatedItem)
                    notifyItemInserted(updatedPosition)
                    recyclerView.scrollToPosition(position)
                }
                .setAnchorView(snackbarAnchor)
                .show()
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
            recyclerView.context.startActivity(intent)
        }
    }
}