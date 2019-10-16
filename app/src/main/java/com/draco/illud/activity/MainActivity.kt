package com.draco.illud.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.R
import com.draco.illud.recycler_view.DragManageAdapter
import com.draco.illud.recycler_view.RecyclerViewAdapter
import com.draco.illud.utils.Nfc
import com.draco.illud.utils.listItems
import com.draco.illud.utils.nfc
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var bottomAppBar: BottomAppBar
    private lateinit var addNew: FloatingActionButton

    /* Internal */
    private var writeContentsAlertDialog: AlertDialog? = null /* Alert dialog for when user writes to car */
    private var swapContentsAlertDialog: AlertDialog? = null /* Alert dialog for when user swaps with car */
    private var tagWriteMode = false /* When the next tag is scanned, should we write to it? */
    private var tagSwapMode = false /* When the next tag is scanned, should we swap with it? */

    /* Put the user into write mode (locks UI until scan) */
    private fun putUserIntoWriteMode() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Write Nfc Tag")
            .setMessage("Hold a tag to the back of the device until this dialog disappears.")
            .setNegativeButton("Cancel", null)
            .setOnDismissListener {
                dismissWriteContentsAlertDialog()
            }

        writeContentsAlertDialog = builder.create()
        writeContentsAlertDialog!!.show()

        /* Next time we scan a tag, write to it */
        tagWriteMode = true
    }

    /* Put the user into swap mode (locks UI until scan) */
    private fun putUserIntoSwapMode() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Swap With Nfc Tag")
            .setMessage("Hold a tag to the back of the device until this dialog disappears.")
            .setNegativeButton("Cancel", null)
            .setOnDismissListener {
                dismissSwapContentsAlertDialog()
            }

        swapContentsAlertDialog = builder.create()
        swapContentsAlertDialog!!.show()

        /* Next time we scan a tag, write to it */
        tagSwapMode = true
    }

    /* Dismiss write Nfc tag alert dialog */
    private fun dismissWriteContentsAlertDialog() {
        /* Allow any context to dismiss the write dialog */
        if (writeContentsAlertDialog != null)
            writeContentsAlertDialog!!.cancel()

        tagWriteMode = false
    }

    /* Dismiss swap Nfc tag alert dialog */
    private fun dismissSwapContentsAlertDialog() {
        /* Allow any context to dismiss the swap dialog */
        if (swapContentsAlertDialog != null)
            swapContentsAlertDialog!!.cancel()

        tagSwapMode = false
    }

    private fun nfcSwap(intent: Intent) {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Get the contents of the current Nfc tag */
        var nfcContent = Nfc.readBytes(intent)

        /* If for some reason the read fails, use blank contents */
        if (nfcContent == null)
            nfcContent = byteArrayOf()

        /* Write contents as compressed bytes */
        val success = Nfc.writeBytes(intent, writeString.toByteArray())

        if (success) {
            /* Splice the card contents and append the list view for the user */
            viewAdapter.notifyItemRangeRemoved(0, listItems.size())
            listItems.clear()

            val addedSize = listItems.parseJoinedString(String(nfcContent))

            /* Append data and scroll up to new data */
            viewAdapter.notifyItemRangeInserted(0, addedSize)
            recyclerView.scrollToPosition(0)
        }

        val message = if (success)
            "Swapped successfully."
        else
            "Contents too large."

        Snackbar.make(addNew, message, Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .setAnchorView(bottomAppBar)
            .show()

        /* Dismiss the non-cancellable dialog for the user */
        dismissSwapContentsAlertDialog()
    }

    /* Update card contents */
    private fun nfcWrite(intent: Intent) {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Write contents as compressed bytes */
        val success = Nfc.writeBytes(intent, writeString.toByteArray())
        val message = if (success)
            "Wrote successfully."
        else
            "Contents too large."

        Snackbar.make(addNew, message, Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .setAnchorView(bottomAppBar)
            .show()

        /* Dismiss the non-cancellable dialog for the user */
        dismissWriteContentsAlertDialog()
    }

    /* Read and process card contents */
    private fun nfcRead(intent: Intent) {
        /* Read contents as compressed bytes */
        val nfcContent = Nfc.readBytes(intent)

        /* Tell user we are blank. */
        if (nfcContent == null || nfcContent.isEmpty()) {
            Snackbar.make(addNew, "Tag has no contents.", Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .setAnchorView(bottomAppBar)
                .show()
            return
        }

        /* Ask for confirmation, but keep data in memory so tag can be removed */
        AlertDialog.Builder(this)
            .setTitle("Import")
            .setMessage("Import items from this tag?")
            .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                /* Splice the card contents and append the list view for the user */
                val addedSize = listItems.parseJoinedString(String(nfcContent))

                /* Append data and scroll up to new data */
                viewAdapter.notifyItemRangeInserted(0, addedSize)
                recyclerView.scrollToPosition(0)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    /* Tell the user to check device compatibility */
    private fun warnUserAboutNfcStatus(nfcState: Nfc.State) {
        val nfcStateString = when (nfcState) {
            Nfc.State.SUPPORTED_OFF -> "Please enable Nfc."
            Nfc.State.UNSUPPORTED -> "This device lacks Nfc."
            else -> return
        }

        Snackbar.make(addNew, nfcStateString, Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .setAnchorView(bottomAppBar)
            .show()
    }

    /* Process Nfc tag scan event */
    private fun nfcTagScanHandler() {
        if (intent != null && Nfc.startedByNDEF(intent)) {
            when {
                tagWriteMode -> nfcWrite(intent)
                tagSwapMode -> nfcSwap(intent)
                else -> nfcRead(intent)
            }
        }
    }

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
        nfcTagScanHandler()
    }

    /* Occurs on application start */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Register ourselves */
        nfc = Nfc()

        /* Register Nfc adapter */
        nfc.registerAdapter(this)

        /* Allow Nfc tags to be scanned */
        nfc.setupForegroundIntent(this)

        /* Setup shared preferences */
        listItems.setupSharedPrefs(this)

        /* Restore backed up list items */
        listItems.load()

        /* Setup UI elements */
        recyclerView = findViewById(R.id.recycler_view)
        viewManager = LinearLayoutManager(this)
        bottomAppBar = findViewById(R.id.bottom_app_bar)
        addNew = findViewById(R.id.add_new)

        /* Use proper menu */
        bottomAppBar.replaceMenu(R.menu.menu_main)

        /* Add new item */
        addNew.setOnClickListener {
            startActivity(Intent(this, ViewMoreActivity::class.java))
        }

        /* Menu item actions */
        bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.write_contents -> {
                    /* Make sure we still have Nfc on */
                    val nfcCurrentState = nfc.supportState()
                    if (nfcCurrentState != Nfc.State.SUPPORTED_ON)
                        warnUserAboutNfcStatus(nfcCurrentState)
                    else
                        putUserIntoWriteMode()
                    true
                }
                R.id.swap -> {
                    /* Make sure we still have Nfc on */
                    val nfcCurrentState = nfc.supportState()
                    if (nfcCurrentState != Nfc.State.SUPPORTED_ON)
                        warnUserAboutNfcStatus(nfcCurrentState)
                    else
                        putUserIntoSwapMode()
                    true
                }
                R.id.wipe -> {
                    AlertDialog.Builder(this)
                        .setTitle("Clear")
                        .setMessage("Are you sure you would like to clear your list?")
                        .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                            viewAdapter.notifyItemRangeRemoved(0, listItems.size())
                            listItems.clear()
                        }
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show()
                    true
                }
                else -> false
            }
        }

        /* Set adapter */
        viewAdapter = RecyclerViewAdapter(this)

        /* Update the recycler view */
        recyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        /* Setup our list view */
        viewAdapter.notifyDataSetChanged()

        /* Setup drag and drop handler */
        val callback = DragManageAdapter(
            addNew,
            viewAdapter,
            recyclerView,
            ItemTouchHelper.UP or
            ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT or
                    ItemTouchHelper.LEFT)
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerView)

        /* Check if we opened the app due to a Nfc event */
        nfcTagScanHandler()
    }
}
