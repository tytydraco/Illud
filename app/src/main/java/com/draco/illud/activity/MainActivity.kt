package com.draco.illud.activity

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
import com.draco.illud.utils.Nfc
import com.draco.illud.utils.listItems
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var viewAdapter: RecyclerViewAdapter
    private lateinit var viewLayoutManager: RecyclerView.LayoutManager

    /* Private classes */
    private lateinit var nfc: Nfc

    enum class NfcScanAction {
        /* Do nothing, or read contents of tag */
        NONE,

        /* Write to tag */
        WRITE,

        /* Write to tag, and read contents */
        SWAP
    }

    /* Dialog is shown when writing or swapping (lock UI) */
    private var nfcScanAlertDialog: AlertDialog? = null

    /* Should we write, swap, or do nothing (potentially read) on scan */
    private var scanAction = NfcScanAction.NONE

    /* Put the user into scan mode (locks UI until scan) */
    private fun showNfcScanDialog() {
        /* Do not show the dialog if we already have it open */
        if (nfcScanAlertDialog != null &&
            nfcScanAlertDialog!!.isShowing)
            return

        /* Lock the UI for the user */
        nfcScanAlertDialog!!.show()
    }

    /* Dismiss write Nfc tag alert dialog */
    private fun dismissNfcScanDialog() {
        /* Allow any context to dismiss the write dialog */
        if (nfcScanAlertDialog != null)
            nfcScanAlertDialog!!.cancel()
    }

    /* Swap contents of card and local list */
    private fun nfcSwap(intent: Intent) {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Get the contents of the current Nfc tag */
        val nfcContent = Nfc.readBytes(intent)

        /* If for some reason the read fails, abort to prevent corruption */
        if (nfcContent == null) {
            Snackbar.make(recyclerView, "Reading failed, swap aborted.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            /* Dismiss the non-cancellable dialog for the user */
            dismissNfcScanDialog()
            scanAction = NfcScanAction.NONE

            return
        }

        /* Write contents as compressed bytes */
        val exception = Nfc.writeBytes(intent, writeString.toByteArray())

        /* If there was an exception, show the user the issue and abort */
        if (exception != null) {
            Snackbar.make(recyclerView, exception.message!!, Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            /* Dismiss the non-cancellable dialog for the user */
            dismissNfcScanDialog()
            scanAction = NfcScanAction.NONE

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

        /* Dismiss the non-cancellable dialog for the user */
        dismissNfcScanDialog()
        scanAction = NfcScanAction.NONE
    }

    /* Update card contents */
    private fun nfcWrite(intent: Intent) {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Write contents as compressed bytes */
        val exception = Nfc.writeBytes(intent, writeString.toByteArray())

        /* If there was an exception, show the user the issue and abort */
        if (exception != null) {
            Snackbar.make(recyclerView, exception.message!!, Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            /* Dismiss the non-cancellable dialog for the user */
            dismissNfcScanDialog()
            scanAction = NfcScanAction.NONE

            return
        }

        Snackbar.make(recyclerView, "Wrote successfully.", Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .show()

        /* Dismiss the non-cancellable dialog for the user */
        dismissNfcScanDialog()
        scanAction = NfcScanAction.NONE
    }

    /* Read and process card contents */
    private fun nfcRead(intent: Intent) {
        /* Read contents as compressed bytes */
        val nfcContent = Nfc.readBytes(intent)

        /* Tell user we are blank */
        if (nfcContent == null || nfcContent.isEmpty()) {
            Snackbar.make(recyclerView, "Tag has no contents.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return
        }

        /* Ask for confirmation, but keep data in memory so tag can be removed */
        AlertDialog.Builder(this)
            .setTitle("Import")
            .setMessage(getString(R.string.nfc_read_dialog))
            .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                /* Splice the card contents and append the list view for the user */
                val nfcItems = listItems.parseJoinedString(String(nfcContent))
                listItems.addAll(nfcItems)

                /* Append data and scroll up to new data */
                viewAdapter.notifyItemRangeInserted(0, nfcItems.size)
                recyclerView.scrollToPosition(0)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /* Process Nfc tag scan event */
    private fun processNfcTagScanned() {
        if (intent != null && Nfc.startedByNDEF(intent)) {
            when (scanAction) {
                /* Do nothing, or read contents of tag */
                NfcScanAction.NONE -> nfcRead(intent)

                /* Write to tag */
                NfcScanAction.WRITE -> nfcWrite(intent)

                /* Write to tag, and read contents */
                NfcScanAction.SWAP -> nfcSwap(intent)
            }
        }
    }

    private fun setupUI() {
        /* Set our local lateinit variables */
        recyclerView = findViewById(R.id.recycler_view)
        emptyView = findViewById(R.id.recycler_view_empty)
        viewLayoutManager = LinearLayoutManager(this)

        /* Create scan dialog for writing and swapping */
        nfcScanAlertDialog = AlertDialog.Builder(this)
            .setTitle("Scan Nfc Tag")
            .setMessage(getString(R.string.nfc_scan_dialog))
            .setNegativeButton("Cancel", null)
            .setOnDismissListener {
                dismissNfcScanDialog()
                scanAction = NfcScanAction.NONE
            }.create()

        /* Set adapter */
        viewAdapter = RecyclerViewAdapter(
            /* Used to scroll to positions, and for activity context */
            recyclerView,

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

            /* Reposition with long press and drag */
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,

            /* Prioritization shortcuts and dismissals */
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT)

        /* Create and attach our drag and drop handler */
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
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
                startActivity(Intent(this, ViewMoreActivity::class.java))
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

            R.id.write_contents -> {
                showNfcScanDialog()
                scanAction = NfcScanAction.WRITE
            }

            R.id.swap -> {
                showNfcScanDialog()
                scanAction = NfcScanAction.SWAP
            }
        }

        return super.onOptionsItemSelected(item)
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

        /* Setup shared preferences for our list items */
        listItems.setupSharedPrefs(this)

        /* Restore backed up list items if we have any */
        listItems.load()

        /* Setup UI elements */
        setupUI()

        /* Check if we opened the app due to a Nfc event */
        processNfcTagScanned()

        /* Activate dark mode if the system is dark themed */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /* ----- Miscellaneous Setup ----- */

    /* Enable foreground scanning */
    override fun onResume() {
        super.onResume()

        /* When we switch activities, make sure to get updated info */
        if (intent == null || !Nfc.startedByNDEF(intent))
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
