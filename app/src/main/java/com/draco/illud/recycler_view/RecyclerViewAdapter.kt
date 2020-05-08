package com.draco.illud.recycler_view

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.R
import com.draco.illud.activity.ViewMoreActivity
import com.draco.illud.utils.ListItems

class RecyclerViewAdapter(
    private val recyclerView: RecyclerView,
    private val listItems: ListItems,
    private val emptyView: View):
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    /* Holds our views inside our row view */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label = view.findViewById<TextView>(R.id.label)!!
        val tag = view.findViewById<TextView>(R.id.tag)!!
    }

    /* Check if list is empty; if so, show the empty view */
    private fun updateEmptyView() {
        if (listItems.items.size == 0)
            emptyView.visibility = View.VISIBLE
        else
            emptyView.visibility = View.GONE
    }

    /* Configure each holder view */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listItems.items[position]

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
            val updatedItem = listItems.items[updatedPosition]

            /* Start the ViewMore activity when we click an item */
            val intent = Intent(recyclerView.context, ViewMoreActivity::class.java)
                .putExtra("itemString", updatedItem.toString())
                .putExtra("position", updatedPosition)

            /* Use parent activity to handle result of item edit */
            (recyclerView.context as Activity)
                .startActivityForResult(intent, ViewMoreActivity.activityResultCode)
        }
    }

    /* Inflate our rows for each item */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(view)
    }

    /* Check if our list is empty when we attach our recycler view */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        updateEmptyView()
    }

    /* Return our item count */
    override fun getItemCount(): Int {
        updateEmptyView()
        return listItems.items.size
    }
}