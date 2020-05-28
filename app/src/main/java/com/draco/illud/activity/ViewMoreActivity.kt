package com.draco.illud.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.draco.illud.R
import com.draco.illud.utils.ListItem

class ViewMoreActivity : AppCompatActivity() {
    companion object {
        const val activityResultCode = 0
    }

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

    /* Simulate back button press on navigation icon click */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}