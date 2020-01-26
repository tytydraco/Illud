package com.draco.illud.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
    /* UI elements */
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var viewAdapter: RecyclerViewAdapter
    private lateinit var viewLayoutManager: RecyclerView.LayoutManager

    /* Private classes */
    private lateinit var nfc: Nfc
    private lateinit var listItems: ListItems

    /* Swap contents of card and local list */
    private fun nfcSwap(intent: Intent) {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Get the contents of the current Nfc tag */
        val nfcContent = nfc.readBytes(intent)

        /* If for some reason the read fails, abort to prevent corruption */
        if (nfcContent == null) {
            Snackbar.make(recyclerView, "Reading failed, swap aborted.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return
        }

        /* Write contents as compressed bytes */
        val exception = nfc.writeBytes(intent, writeString.toByteArray())

        /* If there was an exception, show the user the issue and abort */
        if (exception != null) {
            Snackbar.make(recyclerView, exception.message!!, Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return
        }

        /* Splice the card contents and append the list view for the user */
        viewAdapter.notifyItemRangeRemoved(0, listItems.size())
        listItems.clear()

        /* If we were able to write to the tag, overwrite our list */
        val nfcItems = listItems.parseJoinedString(String(nfcContent))
        listItems.addAll(nfcItems)

        /* Append data and scroll up to new data */
        viewAdapter.notifyItemRangeInserted(0, nfcItems.size)
        recyclerView.scrollToPosition(0)

        Snackbar.make(recyclerView, "Swapped successfully.", Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .show()
    }

    /* Update card contents */
    private fun nfcWrite(intent: Intent) {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Write contents as compressed bytes */
        val exception = nfc.writeBytes(intent, writeString.toByteArray())

        /* If there was an exception, show the user the issue and abort */
        if (exception != null) {
            Snackbar.make(recyclerView, exception.message!!, Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return
        }

        Snackbar.make(recyclerView, "Wrote successfully.", Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .show()
    }

    /* Read and process card contents */
    private fun nfcRead(intent: Intent) {
        /* Read contents as compressed bytes */
        val nfcContent = nfc.readBytes(intent)

        /* Tell user we are blank */
        if (nfcContent == null || nfcContent.isEmpty()) {
            Snackbar.make(recyclerView, "Tag has no contents.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return
        }

        /* Splice the card contents and append the list view for the user */
        val nfcItems = listItems.parseJoinedString(String(nfcContent))
        listItems.addAll(nfcItems)

        /* Append data and scroll up to new data */
        viewAdapter.notifyItemRangeInserted(0, nfcItems.size)
        recyclerView.scrollToPosition(0)
    }

    /* Process Nfc tag scan event */
    private fun processNfcTagScanned() {
        if (intent != null) {
            /* Keep intent saved so we can process it later */
            val scannedIntent = intent

            /* Make sure we are processing an Nfc tag */
            if (!nfc.startedByNDEF(intent))
                return

            AlertDialog.Builder(this)
                .setTitle("Tag Action")
                .setMessage("Keep the NFC tag on the device and select an action to perform for the NFC tag.")
                .setPositiveButton("Read") { _: DialogInterface, _: Int ->
                    nfcRead(scannedIntent)
                }
                .setNegativeButton("Write") { _: DialogInterface, _: Int ->
                    nfcWrite(scannedIntent)
                }
                .setNeutralButton("Swap") { _: DialogInterface, _: Int ->
                    nfcSwap(scannedIntent)
                }
                .show()
        }
    }

    private fun setupUI() {
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
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT)

        /* Create and attach our drag and drop handler */
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

        /* Activate dark mode if the system is dark themed */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /* Setup and inflate toolbar */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /* Setup toolbar menu actions */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.add_new -> {
                val viewMoreIntent = Intent(this, ViewMoreActivity::class.java)
                startActivityForResult(viewMoreIntent, Constants.VIEW_MORE_ACTIVITY_RESULT_CODE)
            }

            R.id.delete -> {
                AlertDialog.Builder(this)
                    .setTitle("Delete All")
                    .setMessage(getString(R.string.list_items_clear))
                    .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                        viewAdapter.notifyItemRangeRemoved(0, listItems.size())
                        listItems.clear()
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

            R.id.sort_by_length -> {
                listItems.sortByLength()
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
                if (position == -1) {
                    listItems.add(item)
                } else {
                    listItems.set(position, item)
                }
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
        listItems = ListItems(this)

        /* Setup shared preferences for our list items */
        listItems.setupSharedPrefs()

        /* Restore backed up list items if we have any */
        listItems.load()

        /* Setup UI elements */
        setupUI()

        /* Check if we opened the app due to a Nfc event */
        processNfcTagScanned()
    }

    /* ----- Miscellaneous Setup ----- */

    /* Enable foreground scanning */
    override fun onResume() {
        super.onResume()

        /* When we switch activities, make sure to get updated info */
        if (intent == null || !nfc.startedByNDEF(intent))
            viewAdapter.notifyDataSetChanged()

        nfc.enableForegroundIntent(this)

        /* Invalidate our old Nfc intent */
        intent = null
    }

    /* Disable foreground scanning */
    override fun onPause() {
        super.onPause()
        nfc.disableForegroundIntent(this)
    }

    /* Catch Nfc tag scan in our foreground intent filter */
    override fun onNewIntent(thisIntent: Intent?) {
        super.onNewIntent(thisIntent)

        /* This is so that other functions can see our Nfc intent */
        intent = thisIntent

        /* Call Nfc tag handler if we are sure this is an Nfc scan */
        processNfcTagScanned()
    }
}
