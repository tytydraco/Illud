package com.draco.illud.recycler_view

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.R
import com.draco.illud.activity.ViewMoreActivity
import com.draco.illud.utils.listItems

class RecyclerViewAdapter(
    private val context: Context):
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    private var labels: ArrayList<String> = arrayListOf()
    private var sublabels: ArrayList<String> = arrayListOf()

    /* Holds our views inside our row view */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label = view.findViewById<TextView>(R.id.label)!!
        val sublabel = view.findViewById<TextView>(R.id.sublabel)!!
    }

    /* Inflate our rows for each item */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(view)
    }

    /* Return our item count */
    override fun getItemCount(): Int {
        return labels.size
    }

    /* Swap to items in a list */
    fun swapItems(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                val oldLabel = labels[i + 1]
                val oldSublabel = sublabels[i + 1]
                listItems.set(i + 1, labels[i], sublabels[i])
                listItems.set(i, oldLabel, oldSublabel)
            }
        } else {
            for (i in fromPosition..toPosition + 1) {
                val oldLabel = labels[i - 1]
                val oldSublabel = sublabels[i - 1]
                listItems.set(i - 1, labels[i], sublabels[i])
                listItems.set(i, oldLabel, oldSublabel)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
        update(false)
    }

    /* Save the new contents of the recycler view and update */
    fun update(refresh: Boolean = true) {
        /* Save this */
        listItems.save()

        /* Only show the label in the list view */
        val parsedItemList = listItems.parseListItems()
        val newLabels = parsedItemList.first
        val newSublabels = parsedItemList.second

        /* Set adapter */
        labels = newLabels
        sublabels = newSublabels

        /* Refresh the view while we're at it */
        if (refresh)
            notifyDataSetChanged()
    }

    /* Start the ViewMore activity when we click an item */
    private fun clickListener(position: Int) {
        val itemPair = listItems.get(position)
        val label = itemPair.first
        val sublabel = itemPair.second

        val intent = Intent(context, ViewMoreActivity::class.java)
            .putExtra("label", label)
            .putExtra("sublabel", sublabel)
            .putExtra("position", position)
        context.startActivity(intent)
    }

    /* Configure each holder view */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        /* Hide the sublabel view if there is no content */
        if (sublabels[position].isBlank())
            holder.sublabel.visibility = View.GONE
        else
            holder.sublabel.visibility = View.VISIBLE

        /* Set the text */
        holder.label.text = labels[position]
        holder.sublabel.text = sublabels[position]

        /* Set the external click listener */
        holder.itemView.setOnClickListener {
            clickListener(position)
        }
    }
}