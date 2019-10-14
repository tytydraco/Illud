package com.draco.illud.activity

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.draco.illud.R
import com.draco.illud.utils.ListItem
import com.draco.illud.utils.listItems
import com.google.android.material.snackbar.Snackbar

class ViewMoreActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var label: EditText
    private lateinit var sublabel: EditText
    private lateinit var toolbar: Toolbar

    /* Internal */
    private var position = -1
    private var deleted = false

    /* Occurs on application start */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_more_activity)

        title = ""

        val labelText = intent.getStringExtra("label")
        val sublabelText = intent.getStringExtra("sublabel")

        position = intent.getIntExtra("position", -1)
        label = findViewById(R.id.label)
        sublabel = findViewById(R.id.sublabel)
        toolbar = findViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.menu_view_more)

        /* Use proper menu */
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        /* Menu item actions */
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.delete -> {
                    if (position != -1)
                        listItems.remove(position)

                    deleted = true
                    finish()
                    true
                }
                else -> false
            }
        }

        /* Set the labels based on what was given to us */
        label.setText(labelText)
        sublabel.setText(sublabelText)

        /* Start editing label if this is a new item */
        if (position == -1) {
            label.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    /* Do not exit if label is not filled in */
    override fun onBackPressed() {
        /* Only if sublabel is filled but label is blank */
        if (label.text.isBlank() &&
            sublabel.text.isNotBlank()) {
            Snackbar.make(toolbar, "Label must not be blank.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(label.windowToken, 0)
            imm.hideSoftInputFromWindow(sublabel.windowToken, 0)

            return
        }

        super.onBackPressed()
    }

    /* Save contents on exit */
    override fun onPause() {
        super.onPause()

        /* Don't do auto-save if we press delete */
        if (deleted)
            return

        val item = ListItem(label.text.toString(), sublabel.text.toString())

        if (position == -1) {
            listItems.add(item)

            /* The new location of our item is at 0. Prevents item from being recreated */
            position = 0
        } else
            listItems.set(position, item)
    }
}