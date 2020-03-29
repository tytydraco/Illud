package com.draco.illud.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.draco.illud.R
import com.draco.illud.utils.ListItem

class ViewMoreActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var label: EditText
    private lateinit var content: EditText
    private lateinit var tag: EditText

    /* Position to insert the list item into (-1 means its a new item) */
    private var position = -1

    /* Occurs on application start */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_more)

        /* Setup our local UI elements */
        label = findViewById(R.id.label)
        content = findViewById(R.id.content)
        tag = findViewById(R.id.tag)

        /* Allow back button functionality */
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        /* Change title to be more relevant */
        title = "View More"

        /* Import the item that was passed to us as a raw string */
        val itemString = intent.getStringExtra("itemString")
        position = intent.getIntExtra("position", -1)

        /* Create a new list item object based on the intent extras */
        val thisItem = ListItem(itemString)

        /* Set the labels based on what was given to us */
        label.setText(thisItem.label)
        content.setText(thisItem.content)
        tag.setText(thisItem.tag)

        /* Start editing label if this is a new item (position == -1) */
        if (position == -1) {
            label.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    /* Simulate back button press on navigation icon click */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    /* Setup and inflate toolbar */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_view_more, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /* Setup toolbar menu actions */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.share -> run {
                val labelText = label.text.toString()
                val contentText = content.text.toString()

                val sendText = when {
                    /* First choice is both (label: content)*/
                    contentText.isNotBlank() &&
                    labelText.isNotBlank() -> "$labelText:\n$contentText"

                    /* Second choice is content */
                    contentText.isNotBlank() -> contentText

                    /* Third choice is label */
                    labelText.isNotBlank() -> labelText

                    /* If there's nothing to share, just ignore request */
                    else -> return@run
                }

                /* Open send-to dialog with our copy text */
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type="text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, sendText)
                startActivity(Intent.createChooser(shareIntent, "Send to"))
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /* Save contents on exit */
    override fun finish() {
        val resultIntent = Intent()

        /* Create a new list item object based on the new contents */
        val item = ListItem(
            label.text.toString(),
            content.text.toString(),
            tag.text.toString()
        )

        resultIntent.putExtra("item", item.toString())
        resultIntent.putExtra("position", position)

        /* Tell our calling activity what our user wants to do */
        setResult(Activity.RESULT_OK, resultIntent)
        super.finish()
    }
}