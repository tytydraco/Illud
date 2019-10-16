package com.draco.illud.utils

class ListItem {
    private val separator = "\t" /* Separates label from content */

    var label: String = ""
    var content: String = ""
    var tag: String = ""

    /* Input each item individually */
    constructor(setLabel: String?,
                setContent: String?,
                setTag: String?) {
        if (!setLabel.isNullOrBlank())
            label = setLabel
        if (!setContent.isNullOrBlank())
            content = setContent
        if (!setTag.isNullOrBlank())
            tag = setTag
    }

    /* Import as coagulated string with separators */
    constructor(rawString: String?) {
        /* Nothing in the string */
        if (rawString == null || rawString.isBlank())
            return

        val splitString: List<String> = rawString.split(separator)

        if (!splitString[0].isBlank())
            label = splitString[0]
        if (splitString.size > 1 && !splitString[1].isBlank())
            content = splitString[1]
        if (splitString.size > 2 && !splitString[2].isBlank())
            tag = splitString[2]
    }

    /* Return as coagulated string with separators */
    override fun toString(): String {
        return "${label}${separator}${content}${separator}${tag}"
    }
}