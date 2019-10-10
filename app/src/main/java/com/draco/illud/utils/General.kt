package com.draco.illud.utils

import android.view.View
import androidx.core.graphics.toColorInt
import com.google.android.material.snackbar.Snackbar

/*
 * This class just holds some declarations that are accessed in
 * most files, so it makes the most sense to just globalize them
 * instead of pass each one via reference to each class.
 */

/* Our global copy of classes */
var nfc = Nfc()
val listItems = ListItems()

/* Make a snackbar pop-up with a dismissal button */
fun makeSnackbar(view: View, text: String) {
    val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_SHORT)
    snackbar.setAction("Dismiss") {}
    snackbar.view.setBackgroundColor("#000000".toColorInt())
    snackbar.show()
}

/* Make a snackbar pop-up with a dismissal button */
fun makeUndoSnackbar(view: View, text: String, callback: () -> Unit) {
    val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
    snackbar.setAction("Undo") { callback() }
    snackbar.view.setBackgroundColor("#000000".toColorInt())
    snackbar.show()
}