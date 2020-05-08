package com.draco.illud.utils

class ListItems {
    /* Constants */
    private val divider = "\r" /* Separates individual notes */

    /* Items are stored here */
    var items: ArrayList<ListItem> = arrayListOf()

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
}