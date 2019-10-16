package com.draco.illud.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.draco.illud.R
import com.draco.illud.utils.ListItem
import com.draco.illud.utils.listItems
import com.google.android.material.snackbar.Snackbar

class ViewMoreActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var label: EditText
    private lateinit var content: EditText
    private lateinit var tag: EditText

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

        val itemString = intent.getStringExtra("itemString")
        val thisItem = ListItem(itemString)

        val labelText = thisItem.label
        val contentText = thisItem.content
        val tagText = thisItem.tag

        position = intent.getIntExtra("position", -1)
        label = findViewById(R.id.label)
        content = findViewById(R.id.content)
        tag = findViewById(R.id.tag)

        /* Set the labels based on what was given to us */
        label.setText(labelText)
        content.setText(contentText)
        tag.setText(tagText)

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
            R.id.share -> run {
                val labelText = label.text.toString()
                val contentText = content.text.toString()

                val sendText = when {
                    /* First choice */
                    contentText.isNotBlank() -> contentText

                    /* Second choice */
                    labelText.isNotBlank() -> labelText

                    /* Fail safe */
                    else -> {
                        Snackbar.make(content, "Nothing to share.", Snackbar.LENGTH_SHORT)
                            .setAction("Dismiss") {}
                            .show()

                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(label.windowToken, 0)
                        imm.hideSoftInputFromWindow(content.windowToken, 0)
                        imm.hideSoftInputFromWindow(tag.windowToken, 0)

                        return@run
                    }
                }

                /* Open send-to dialog */
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type="text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, sendText)
                startActivity(Intent.createChooser(shareIntent, "Send to"))
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
            imm.hideSoftInputFromWindow(tag.windowToken, 0)

            return
        }

        super.onBackPressed()
    }

    /* Save contents on exit */
    override fun onPause() {
        super.onPause()

        /* Don't do auto-save if we press delete or if no label */
        if (deleted || label.text.toString().isBlank())
            return

        val item = ListItem(
            label.text.toString(),
            content.text.toString(),
            tag.text.toString()
        )

        if (position == -1) {
            listItems.add(item)

            /* The new location of our item is at 0. Prevents item from being recreated */
            position = 0
        } else
            listItems.set(position, item)
    }
}