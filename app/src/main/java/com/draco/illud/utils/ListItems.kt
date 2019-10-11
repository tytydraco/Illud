package com.draco.illud.utils

import android.content.Context
import android.content.SharedPreferences

class ListItems {
    /* Constants */
    private val separator = "\t" /* Separates label from sublabel */
    private val divider = "\r" /* Separates individual notes */

    /* Internal */
    private var rawItems: ArrayList<String> = arrayListOf()

    /* Shared Preferences */
    private val prefsName = "TagDrivePrefs"
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor

    /* Coagulate raw items into a single string */
    fun generateJoinedString(): String {
        return rawItems.joinToString(divider)
    }

    /* Parse raw string and save to rawItems */
    fun parseJoinedString(string: String): Int {
        if (string.isNotBlank()) {
            val split = string.split(divider)
            rawItems.addAll(0, split)
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
        parseJoinedString(prefs.getString("listItems", "")!!)
    }

    /* Setup shared preferences */
    fun setupSharedPrefs(context: Context) {
        prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefsEditor = prefs.edit()
    }

    /* Insert a label : sublabel pair at position */
    fun insert(position: Int, label: String, sublabel: String): Boolean {
        if (label.isNotBlank() && sublabel.isNotBlank())
            rawItems.add(position, "${label}${separator}${sublabel}")
        else if (label.isNotBlank())
            rawItems.add(position, label)
        else
            return false

        save()
        return true
    }

    /* Add a label : sublabel pair at the start */
    fun add(label: String, sublabel: String): Boolean {
        return insert(0, label, sublabel)
    }

    /* Add a label : sublabel pair at the end */
    fun addToBack(label: String, sublabel: String): Boolean {
        return insert(size(), label, sublabel)
    }

    /* Set a label : sublabel pair and preserve its position */
    fun set(position: Int, label: String, sublabel: String) {
        if (label.isNotBlank() && sublabel.isNotBlank())
            rawItems[position] = "${label}${separator}${sublabel}"
        else if (label.isNotBlank())
            rawItems[position] = label
        save()
    }

    /* Remove a label : sublabel pair at position */
    fun remove(position: Int) {
        rawItems.removeAt(position)
        save()
    }

    /* Get a label : sublabel pair at position */
    fun get(position: Int): Pair<String, String> {
        val thisListItem = rawItems[position]
        val splitItem = thisListItem.split(separator)

        var label = thisListItem
        var sublabel = ""

        if (splitItem.size == 2) {
            label = splitItem[0]
            sublabel = splitItem[1]
        }

        return Pair(label, sublabel)
    }

    /* Clear list items */
    fun clear() {
        rawItems.clear()
        save()
    }

    /* Return the size of the list */
    fun size(): Int {
        return rawItems.size
    }

    /* Return label : sublabel for each list item */
    fun parseListItems(): Pair<ArrayList<String>, ArrayList<String>> {
        val labels = arrayListOf<String>()
        val sublabels = arrayListOf<String>()

        for (listItem in rawItems) {
            val splitItems = listItem.split(separator)
            if (splitItems.size == 2) {
                val label = splitItems[0]
                val sublabel = splitItems[1]
                labels.add(label)
                sublabels.add(sublabel)
            } else {
                labels.add(listItem)
                sublabels.add("")
            }
        }

        return Pair(labels, sublabels)
    }
}