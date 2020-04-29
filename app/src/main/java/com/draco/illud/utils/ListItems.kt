package com.draco.illud.utils

class ListItems {
    /* Constants */
    private val divider = "\r" /* Separates individual notes */

    /* Items are stored here */
    private var items: ArrayList<ListItem> = arrayListOf()

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

    /* Sort by labels */
    fun sort() {
        items.sortBy { it.label }
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