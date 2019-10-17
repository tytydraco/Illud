package com.draco.illud.utils

import android.content.Context
import android.content.SharedPreferences

class ListItems {
    /* Constants */
    private val divider = "\r" /* Separates individual notes */

    /* Internal */
    private var listItems: ArrayList<ListItem> = arrayListOf()

    /* Shared Preferences */
    private val prefsName = "TagDrivePrefs"
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor

    /* Coagulate raw items into a single string */
    fun generateJoinedString(): String {
        return listItems.joinToString(divider)
    }

    /* Parse raw string and return items */
    fun parseJoinedString(string: String): ArrayList<ListItem> {
        val items = arrayListOf<ListItem>()
        if (string.isNotBlank()) {
            for (item in string.split(divider))
                items.add(ListItem(item))
        }

        return items
    }

    /* Backup list items */
    fun save() {
        prefsEditor.putString("listItems", generateJoinedString())
        prefsEditor.apply()
    }

    /* Restore backed up list items */
    fun load() {
        /* Empty items since we are loading. Do not call clear() due to save() */
        listItems.clear()
        val loadedItems = parseJoinedString(prefs.getString("listItems", "")!!)
        addAllToBack(loadedItems)
    }

    /* Setup shared preferences */
    fun setupSharedPrefs(context: Context) {
        prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefsEditor = prefs.edit()
    }

    /* Insert a label : content pair at position */
    fun insert(position: Int, item: ListItem) {
        listItems.add(position, item)

        save()
    }

    /* Add a label : content pair at the start */
    fun add(item: ListItem) {
        insert(0, item)
    }

    /* Add multiple label : content pairs at the start */
    fun addAll(items: ArrayList<ListItem>) {
        listItems.addAll(0, items)
    }

    /* Add a label : content pair at the end */
    fun addToBack(item: ListItem) {
        insert(size(), item)
    }

    /* Add multiple label : content pairs at the end */
    fun addAllToBack(items: ArrayList<ListItem>) {
        listItems.addAll(items)
    }

    /* Set a label : content pair and preserve its position */
    fun set(position: Int, item: ListItem) {
        listItems[position] = item
        save()
    }

    /* Set all label : content pairs */
    fun setAll(items: ArrayList<ListItem>) {
        listItems = items
        save()
    }

    /* Remove a label : content pair at position */
    fun remove(position: Int) {
        listItems.removeAt(position)
        save()
    }

    /* Get a label : content pair at position */
    fun get(position: Int): ListItem {
        return listItems[position]
    }

    /* Get all a label : content pairs */
    fun getAll(): ArrayList<ListItem> {
        return listItems
    }

    /* Sort based on ListItem contents */
    fun sort() {
        listItems.sortBy { it.label }
        listItems.sortBy { it.tag }
        save()
    }

    /* Clear list items */
    fun clear() {
        listItems.clear()
        save()
    }

    /* Return the size of the list */
    fun size(): Int {
        return listItems.size
    }
}