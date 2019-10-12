package com.draco.illud.activity

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.draco.illud.R
import com.draco.illud.utils.listItems
import com.draco.illud.utils.makeSnackbar

class ViewMoreActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var label: EditText
    private lateinit var sublabel: EditText

    /* Internal */
    private var position = -1
    private var deleted = false

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

        /* Start editing label if this is a new item */
        if (position == -1) {
            label.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    /* Add back button support */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /* Do not exit if label is not filled in */
    override fun onBackPressed() {
        /* Only if sublabel is filled but label is blank */
        if (label.text.isBlank() &&
            sublabel.text.isNotBlank()) {
            makeSnackbar(label, "Label must not be blank.")

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(label.windowToken, 0)
            imm.hideSoftInputFromWindow(sublabel.windowToken, 0)

            return
        }

        super.onBackPressed()
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
                if (position != -1)
                    listItems.remove(position)

                deleted = true
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /* Save contents on exit */
    override fun onPause() {
        super.onPause()

        /* Don't do auto-save if we press delete */
        if (deleted)
            return

        if (position == -1) {
            listItems.add(label.text.toString(), sublabel.text.toString())

            /* The new location of our item is at 0. Prevents item from being recreated */
            position = 0
        } else
            listItems.set(position, label.text.toString(), sublabel.text.toString())
    }
}