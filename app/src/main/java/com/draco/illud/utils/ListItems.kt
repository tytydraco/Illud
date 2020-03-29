package com.draco.illud.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys


class ListItems(private val context: Context) {
    /* Constants */
    private val divider = "\r" /* Separates individual notes */

    /* Items are stored here */
    private var items: ArrayList<ListItem> = arrayListOf()

    /* Shared Preferences */
    private val prefsFileName = "encrypted_prefs"
    private val prefsStringId = "listItems"
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor

    /* Coagulate raw items into a single string */
    fun generateJoinedString(): String {
        return items.joinToString(divider)
    }

    /* Parse raw string and return items */
    fun parseJoinedString(string: String): ArrayList<ListItem> {
        val items = arrayListOf<ListItem>()
        if (string.isNotEmpty()) {
            for (item in string.split(divider))
                items.add(ListItem(item))
        }

        return items
    }

    /* Backup list items */
    fun save() {
        prefsEditor.putString(prefsStringId, generateJoinedString())
        prefsEditor.apply()
    }

    /* Restore backed up list items */
    fun load() {
        /* Empty items since we are loading */
        clear()
        val loadedItems = parseJoinedString(prefs.getString(prefsStringId, "")!!)
        addAllToBack(loadedItems)
    }

    /* Setup shared preferences */
    fun setupSharedPrefs() {
        /* Use AES256 encryption for sensitive data */
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        prefs = EncryptedSharedPreferences.create(
            prefsFileName,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        prefsEditor = prefs.edit()
    }

    /* Insert a label : content pair at position */
    fun insert(position: Int, item: ListItem) {
        items.add(position, item)
    }

    /* Add a label : content pair at the start */
    fun add(item: ListItem) {
        insert(0, item)
    }

    /* Add multiple label : content pairs at the start */
    fun addAll(itemList: ArrayList<ListItem>) {
        items.addAll(0, itemList)
    }

    /* Add a label : content pair at the end */
    fun addToBack(item: ListItem) {
        insert(size(), item)
    }

    /* Add multiple label : content pairs at the end */
    fun addAllToBack(itemList: ArrayList<ListItem>) {
        items.addAll(itemList)
    }

    /* Set a label : content pair and preserve its position */
    fun set(position: Int, item: ListItem) {
        items[position] = item
    }

    /* Set all label : content pairs */
    fun setAll(itemList: ArrayList<ListItem>) {
        items = itemList
    }

    /* Remove a label : content pair at position */
    fun remove(position: Int) {
        items.removeAt(position)
    }

    /* Get a label : content pair at position */
    fun get(position: Int): ListItem {
        return items[position]
    }

    /* Get all a label : content pairs */
    fun getAll(): ArrayList<ListItem> {
        return items
    }

    /* Sort by tags */
    fun sortByTag() {
        items.sortBy { it.label }
        items.sortBy { it.tag }
    }

    /* Sort by labels */
    fun sortByLabel() {
        items.sortBy { it.label }
    }

    /* Sort by total size (largest --> least) */
    fun sortBySize() {
        items.sortByDescending {
            it.label.length +
            it.content.length +
            it.tag.length
        }
    }

    /* Clear list items */
    fun clear() {
        items.clear()
    }

    /* Return the size of the list */
    fun size(): Int {
        return items.size
    }
}