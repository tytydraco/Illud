package com.draco.illud.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.draco.illud.R
import com.draco.illud.utils.listItems

class ViewMoreActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var label: EditText
    private lateinit var sublabel: EditText

    /* Internal */
    private var position = -1

    /* Occurs on application start */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_more_activity)

        title = ""
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val labelText = intent.getStringExtra("label")
        val sublabelText = intent.getStringExtra("sublabel")

        position = intent.getIntExtra("position", -1)
        label = findViewById(R.id.label)
        sublabel = findViewById(R.id.sublabel)

        /* Set the labels based on what was given to us */
        label.setText(labelText)
        sublabel.setText(sublabelText)
    }

    /* Add back button support */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /* Inflate top menu buttons */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_view_more, menu)
        return true
    }

    /* Top menu item button actions */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                if (position != -1) {
                    listItems.remove(position)
                    listItems.save()
                }

                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /* Save contents on exit */
    override fun onPause() {
        super.onPause()

        if (position == -1)
            listItems.add(label.text.toString(), sublabel.text.toString())
        else
            listItems.set(position, label.text.toString(), sublabel.text.toString())
    }
}