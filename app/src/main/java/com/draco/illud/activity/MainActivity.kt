package com.draco.illud.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.R
import com.draco.illud.recycler_view.RecyclerViewAdapter
import com.draco.illud.recycler_view.RecyclerViewDragHelper
import com.draco.illud.utils.Constants
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
        viewAdapter.notifyItemRangeRemoved(0, listItems.size())
        listItems.clear()
    }

    /* Update card contents (clears list) */
    private fun nfcExport(intent: Intent): Boolean {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Write contents as compressed bytes */
        val exception = nfc.writeBytes(intent, writeString.toByteArray())

        /* If there was an exception, show the user the issue and abort */
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
        /* Read contents as compressed bytes */
        val nfcContent = nfc.readBytes(intent)

        /* Tell user we read a bad tag */
        if (nfcContent == null) {
            Snackbar.make(recyclerView, "Tag could not be read.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return false
        }

        /* Splice the card contents and append the list view for the user */
        val nfcItems = listItems.parseJoinedString(String(nfcContent))
        listItems.addAll(nfcItems)

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
        if (nfcListOpen) {
            if (nfcExport(intent))
                closeNfcList()
        } else {
            if (nfcImport(intent))
                openNfcList()
        }
    }

    /* Setup UI related methods */
    private fun setupUI() {
        /* Make user aware that the current list is their local list */
        title = titleNotes

        /* Set our local lateinit variables */
        recyclerView = findViewById(R.id.recycler_view)
        emptyView = findViewById(R.id.recycler_view_empty)
        viewLayoutManager = LinearLayoutManager(this)

        /* Set adapter */
        viewAdapter = RecyclerViewAdapter(
            /* Used to scroll to positions, and for activity context */
            recyclerView,

            /* Pass our local listItems copy */
            listItems,

            /* View to show when we have an empty list */
            emptyView
        )

        /* Update the recycler view with our new view adapter */
        recyclerView.apply {
            layoutManager = viewLayoutManager
            adapter = viewAdapter
        }

        /* Tell our adapter that we have new data to handle */
        viewAdapter.notifyDataSetChanged()

        /* Setup drag and drop handler */
        val callback = RecyclerViewDragHelper(
            viewAdapter,
            recyclerView,

            /* Pass our local listItems copy */
            listItems,

            /* Reposition with long press and drag */
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,

            /* Prioritization shortcuts and dismissals */
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
        )

        /* Create and attach our drag and drop handler */
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

        /* Activate dark mode if the system is dark themed */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /* Setup toolbar menu actions */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.add_new -> {
                val viewMoreIntent = Intent(this, ViewMoreActivity::class.java)
                startActivityForResult(viewMoreIntent, Constants.VIEW_MORE_ACTIVITY_RESULT_CODE)
            }

            R.id.stop_editing -> {
                if (!nfcListOpen)
                    Toast.makeText(this, "There is nothing to close.", Toast.LENGTH_SHORT).show()
                else
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

            R.id.sort_by_label -> {
                listItems.sortByLabel()
                viewAdapter.notifyItemRangeChanged(0, listItems.size())
            }

            R.id.sort_by_tag -> {
                listItems.sortByTag()
                viewAdapter.notifyItemRangeChanged(0, listItems.size())
            }

            R.id.sort_by_size -> {
                listItems.sortBySize()
                viewAdapter.notifyItemRangeChanged(0, listItems.size())
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /* Process results from ViewMoreActivity and others */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /* ViewMoreActivity */
        if (requestCode == Constants.VIEW_MORE_ACTIVITY_RESULT_CODE) {
            /* Make sure we returned with intent to process data */
            if (resultCode == Activity.RESULT_OK) {
                /* Make sure we have data to process */
                if (data == null)
                    return

                val itemString = data.getStringExtra("item")
                val item = ListItem(itemString)
                val position = data.getIntExtra("position", -1)

                /* If position is -1, we are going to make a new item */
                if (position == -1)
                    listItems.add(item)
                else
                    listItems.set(position, item)
            }
        }
    }

    /* Occurs on application start */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Register our Nfc helper class */
        nfc = Nfc()

        /* Register Nfc adapter */
        nfc.registerAdapter(this)

        /* Allow Nfc tags to be scanned while the app is opened */
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
