package com.draco.illud.utils

class ListItem {
    /* Separates label from content. Use something that a user cannot manually type. */
    private val componentSeparator = "\u001E"

    /* These can be blank by default */
    var label: String = ""
    var content: String = ""
    var tag: String = ""

    /* Input each item individually */
    constructor(setLabel: String?,
                setContent: String?,
                setTag: String?) {
        if (!setLabel.isNullOrEmpty())
            label = setLabel
        if (!setContent.isNullOrEmpty())
            content = setContent
        if (!setTag.isNullOrEmpty())
            tag = setTag
    }

    /* Import as coagulated string with separators */
    constructor(rawString: String? = null) {
        /* Nothing in the string */
        if (rawString.isNullOrEmpty())
            return

        val splitString: List<String> = rawString.split(componentSeparator)

        if (splitString[0].isNotEmpty())
            label = splitString[0]
        if (splitString.size > 1 && splitString[1].isNotEmpty())
            content = splitString[1]
        if (splitString.size > 2 && splitString[2].isNotEmpty())
            tag = splitString[2]
    }

    /* Return as coagulated string with separators */
    override fun toString(): String {
        return "${label}${componentSeparator}${content}${componentSeparator}${tag}"
    }
}