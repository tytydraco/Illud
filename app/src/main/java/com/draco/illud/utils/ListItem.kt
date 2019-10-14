package com.draco.illud.utils

class ListItem {
    private val separator = "\t" /* Separates label from content */

    var label: String = ""
    var content: String = ""

    /* Input each item individually */
    constructor(setLabel: String?,
                 setContent: String?) {
        if (!setLabel.isNullOrBlank())
            label = setLabel
        if (!setContent.isNullOrBlank())
            content = setContent
    }

    /* Import as coagulated string with separators */
    constructor(rawString: String) {
        val splitString: List<String?> = rawString.split(separator)

        if (!splitString[0].isNullOrBlank())
            label = splitString[0]!!
        if (!splitString[1].isNullOrBlank())
            content = splitString[1]!!
    }

    /* Return as coagulated string with separators */
    override fun toString(): String {
        return "${label}${separator}${content}"
    }
}