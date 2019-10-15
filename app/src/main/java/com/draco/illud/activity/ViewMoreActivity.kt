package com.draco.illud.activity

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    private lateinit var content: EditText

    /* Internal */
    private var position = -1
    private var deleted = false

    /* Occurs on application start */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_more_activity)

        /* Allow back button functionality */
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        /* Don't put a title */
        title = ""

        val labelText = intent.getStringExtra("label")
        val contentText = intent.getStringExtra("content")

        position = intent.getIntExtra("position", -1)
        label = findViewById(R.id.label)
        content = findViewById(R.id.content)

        /* Set the labels based on what was given to us */
        label.setText(labelText)
        content.setText(contentText)

        /* Start editing label if this is a new item */
        if (position == -1) {
            label.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    /* Setup the toolbar back button */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onNavigateUp()
    }

    /* Setup toolbar */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_view_more, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /* Setup toolbar menu actions */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.delete -> {
                if (position != -1)
                    listItems.remove(position)

                deleted = true
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /* Do not exit if label is not filled in */
    override fun onBackPressed() {
        /* Only if content is filled but label is blank */
        if (label.text.isBlank()) {
            Snackbar.make(content, "Label must not be blank.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(label.windowToken, 0)
            imm.hideSoftInputFromWindow(content.windowToken, 0)

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

        val item = ListItem(label.text.toString(), content.text.toString())

        if (position == -1) {
            listItems.add(item)

            /* The new location of our item is at 0. Prevents item from being recreated */
            position = 0
        } else
            listItems.set(position, item)
    }
}