package com.draco.illud.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.R
import com.draco.illud.recycler_view.RecyclerViewAdapter
import com.draco.illud.recycler_view.RecyclerViewDragHelper
import com.draco.illud.utils.ListItem
import com.draco.illud.utils.ListItems
import com.draco.illud.utils.Nfc
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    /* Constants */
    private val titleNotes = "Notes"
    private val titleEdit = "Editing"

    /* UI elements */
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var viewAdapter: RecyclerViewAdapter
    private lateinit var viewLayoutManager: RecyclerView.LayoutManager

    /* If user has a temporary Nfc list open for editing */
    private var nfcListOpen = false

    /* Private classes */
    private lateinit var nfc: Nfc
    private lateinit var listItems: ListItems

    /* Close Nfc list */
    private fun closeNfcList() {
        nfcListOpen = false
        title = titleNotes
        emptyView.text = getString(R.string.recycler_view_scan)
    }

    /* Open Nfc list */
    private fun openNfcList() {
        nfcListOpen = true
        title = titleEdit
        emptyView.text = getString(R.string.recycler_view_empty)
    }

    /* Clear list and update adapter */
    private fun clearList() {
        viewAdapter.notifyItemRangeRemoved(0, listItems.items.size)
        listItems.items.clear()
    }

    /* Update card contents (clears list) */
    private fun nfcExport(intent: Intent): Boolean {
        val writeString = listItems.generateJoinedString()
        val exception = nfc.writeBytes(intent, writeString.toByteArray())

        if (exception != null) {
            Snackbar.make(recyclerView, exception.message!!, Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return false
        }

        /* Clear our list for the next import */
        clearList()

        Snackbar.make(recyclerView, "Exported successfully.", Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .show()

        return true
    }

    /* Read and process card contents (appends) */
    private fun nfcImport(intent: Intent): Boolean {
        val nfcContent = nfc.readBytes(intent)

        if (nfcContent == null) {
            Snackbar.make(recyclerView, "Tag could not be read.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return false
        }

        /* Splice the card contents and append the list view for the user */
        val nfcItems = listItems.parseJoinedString(String(nfcContent))
        listItems.items.addAll(nfcItems)

        /* Append data and scroll up to new data */
        viewAdapter.notifyItemRangeInserted(0, nfcItems.size)
        recyclerView.scrollToPosition(0)

        Snackbar.make(recyclerView, "Imported successfully.", Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .show()

        return true
    }

    /* Process Nfc tag scan event */
    private fun handleNfcScan(intent: Intent) {
        /* Make sure we are processing an Nfc tag */
        if (!nfc.startedByNDEF(intent))
            return

        /* Either import or export */
        if (nfcListOpen && nfcExport(intent))
            closeNfcList()
        else if (nfcImport(intent))
            openNfcList()
    }

    /* Setup UI related methods */
    private fun setupUI() {
        title = titleNotes

        /* Set our local lateinit variables */
        recyclerView = findViewById(R.id.recycler_view)
        emptyView = findViewById(R.id.recycler_view_empty)
        viewLayoutManager = LinearLayoutManager(this)

        viewAdapter = RecyclerViewAdapter(recyclerView, listItems, emptyView)

        recyclerView.apply {
            layoutManager = viewLayoutManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))
        }


        viewAdapter.notifyDataSetChanged()

        /* Setup drag and drop handler */
        val callback = RecyclerViewDragHelper(viewAdapter, recyclerView, listItems)

        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    /* Setup toolbar menu actions */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.add_new -> {
                val viewMoreIntent = Intent(this, ViewMoreActivity::class.java)
                startActivityForResult(viewMoreIntent, ViewMoreActivity.activityResultCode)
            }

            R.id.stop_editing -> {
                    closeNfcList()
            }

            R.id.delete -> {
                AlertDialog.Builder(this)
                    .setTitle("Delete All")
                    .setMessage(getString(R.string.list_items_clear))
                    .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                        clearList()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            R.id.sort -> {
                listItems.items.sortBy { it.label }
                viewAdapter.notifyItemRangeChanged(0, listItems.items.size)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /* Process results from ViewMoreActivity and others */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /* ViewMoreActivity */
        if (resultCode == Activity.RESULT_OK &&
            requestCode == ViewMoreActivity.activityResultCode &&
            data != null) {
            val itemString = data.getStringExtra("item")
            val item = ListItem(itemString)
            val position = data.getIntExtra("position", -1)

            /* If position is -1, we are going to make a new item */
            if (position == -1)
                listItems.items.add(item)
            else
                listItems.items[position] = item
        }
    }

    /* Occurs on application start */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Register our Nfc helper class */
        nfc = Nfc()
        nfc.registerAdapter(this)
        nfc.setupForegroundIntent(this)

        /* Register our ListItems helper class */
        listItems = ListItems()

        /* Setup UI elements */
        setupUI()

        /* If we opened the app by scanning a tag, process it */
        handleNfcScan(intent)
    }

    /* ----- Miscellaneous Setup ----- */

    /* Enable foreground scanning */
    override fun onResume() {
        super.onResume()
        nfc.enableForegroundIntent(this)

        /* When we switch activities, make sure to get updated info */
        viewAdapter.notifyDataSetChanged()
    }

    /* Disable foreground scanning */
    override fun onPause() {
        super.onPause()
        nfc.disableForegroundIntent(this)
    }

    /* Catch Nfc tag scan in our foreground intent filter */
    override fun onNewIntent(thisIntent: Intent?) {
        super.onNewIntent(thisIntent)

        /* Call Nfc tag handler if we are sure this is an Nfc scan */
        if (thisIntent != null)
            handleNfcScan(thisIntent)
    }

    /* Setup and inflate toolbar */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
