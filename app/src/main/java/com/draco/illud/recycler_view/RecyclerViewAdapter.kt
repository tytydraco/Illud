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

class RecyclerViewAdapter(
    private val context: Context):
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    /* Holds our views inside our row view */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bullet = view.findViewById<ImageView>(R.id.bullet)!!
        val label = view.findViewById<TextView>(R.id.label)!!
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
                val oldLabel = oldItem.first
                val oldSublabel = oldItem.second
                val newItem = listItems.get(i)
                val newLabel = newItem.first
                val newSublabel = newItem.second
                listItems.set(i + 1, newLabel, newSublabel)
                listItems.set(i, oldLabel, oldSublabel)
            }
        } else {
            for (i in fromPosition..toPosition + 1) {
                val oldItem = listItems.get(i - 1)
                val oldLabel = oldItem.first
                val oldSublabel = oldItem.second
                val newItem = listItems.get(i)
                val newLabel = newItem.first
                val newSublabel = newItem.second
                listItems.set(i - 1, newLabel, newSublabel)
                listItems.set(i, oldLabel, oldSublabel)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }

    /* Configure each holder view */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listItems.get(position)
        /* Use alternative icon if there is a sublabel */
        val drawable = if (item.second.isBlank())
            context.getDrawable(R.drawable.ic_chevron_right_white_24dp)
        else
            context.getDrawable(R.drawable.ic_short_text_white_24dp)

        holder.bullet.setImageDrawable(drawable)

        /* Set the text */
        holder.label.text = item.first

        /* Set the external click listener */
        holder.itemView.setOnClickListener {
            /* The local position variable may be outdated */
            val updatedPosition = holder.adapterPosition
            val updatedItem = listItems.get(updatedPosition)

            /* Fetch new and updated label information */
            val label = updatedItem.first
            val sublabel = updatedItem.second

            /* Start the ViewMore activity when we click an item */
            val intent = Intent(context, ViewMoreActivity::class.java)
                .putExtra("label", label)
                .putExtra("sublabel", sublabel)
                .putExtra("position", updatedPosition)
            context.startActivity(intent)
        }
    }
}