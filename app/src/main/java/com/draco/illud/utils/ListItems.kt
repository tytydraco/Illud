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

    /* Parse raw string and save to rawItems */
    fun parseJoinedString(string: String): Int {
        if (string.isNotBlank()) {
            val split = string.split(divider)
            for (item in split)
                listItems.add(ListItem(item))

            save()
            return split.size
        }

        return 0
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
        parseJoinedString(prefs.getString("listItems", "")!!)
    }

    /* Setup shared preferences */
    fun setupSharedPrefs(context: Context) {
        prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefsEditor = prefs.edit()
    }

    /* Insert a label : content pair at position */
    fun insert(position: Int, listItem: ListItem): Boolean {
        listItems.add(position, listItem)

        save()
        return true
    }

    /* Add a label : content pair at the start */
    fun add(listItem: ListItem): Boolean {
        return insert(0, listItem)
    }

    /* Add a label : content pair at the end */
    fun addToBack(listItem: ListItem): Boolean {
        return insert(size(), listItem)
    }

    /* Set a label : content pair and preserve its position */
    fun set(position: Int, listItem: ListItem) {
        listItems[position] = listItem
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