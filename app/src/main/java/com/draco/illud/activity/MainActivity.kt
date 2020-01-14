package com.draco.illud.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.draco.illud.R
import com.draco.illud.recycler_view.RecyclerViewAdapter
import com.draco.illud.recycler_view.RecyclerViewDragHelper
import com.draco.illud.utils.Nfc
import com.draco.illud.utils.listItems
import com.draco.illud.utils.nfc
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var viewAdapter: RecyclerViewAdapter
    private lateinit var viewLayoutManager: RecyclerView.LayoutManager
    private lateinit var bottomAppBar: BottomAppBar
    private lateinit var addNew: FloatingActionButton

    /* Internal */
    private var writeContentsAlertDialog: AlertDialog? = null
    private var swapContentsAlertDialog: AlertDialog? = null
    private var readContentsAlertDialog: AlertDialog? = null
    private var tagWriteMode = false
    private var tagSwapMode = false

    /* Put the user into write mode (locks UI until scan) */
    private fun putUserIntoWriteMode() {
        /* Do not show the dialog if we already have it open */
        if (writeContentsAlertDialog != null &&
            writeContentsAlertDialog!!.isShowing)
            return

        /* Lock the UI for the user */
        writeContentsAlertDialog!!.show()

        /* Next time we scan a tag, write to it */
        tagWriteMode = true
    }

    /* Put the user into swap mode (locks UI until scan) */
    private fun putUserIntoSwapMode() {
        /* Do not show the dialog if we already have it open */
        if (swapContentsAlertDialog != null &&
            swapContentsAlertDialog!!.isShowing)
            return

        /* Lock the UI for the user */
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

    /* Swap contents of card and local list */
    private fun nfcSwap(intent: Intent) {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Get the contents of the current Nfc tag */
        var nfcContent = Nfc.readBytes(intent)

        /* If for some reason the read fails, use blank contents */
        if (nfcContent == null)
            nfcContent = byteArrayOf()

        /* Write contents as compressed bytes */
        val exception = Nfc.writeBytes(intent, writeString.toByteArray())

        /* Based on our result, show a special message */
        var message = "Swapped successfully."

        /* If there was an exception, show the user the issue */
        if (exception != null)
            message = exception.message!!
        else {
            /* Splice the card contents and append the list view for the user */
            viewAdapter.notifyItemRangeRemoved(0, listItems.size())
            listItems.clear()

            /* If we were able to write to the tag, overwrite our list */
            val nfcItems = listItems.parseJoinedString(String(nfcContent))
            listItems.addAll(nfcItems)

            /* Append data and scroll up to new data */
            viewAdapter.notifyItemRangeInserted(0, nfcItems.size)
            recyclerView.scrollToPosition(0)
        }

        Snackbar.make(addNew, message, Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .setAnchorView(addNew)
            .show()

        /* Dismiss the non-cancellable dialog for the user */
        dismissSwapContentsAlertDialog()
    }

    /* Update card contents */
    private fun nfcWrite(intent: Intent) {
        /* Store everything in the first NDEF record */
        val writeString = listItems.generateJoinedString()

        /* Write contents as compressed bytes */
        val exception = Nfc.writeBytes(intent, writeString.toByteArray())

        /* Based on our result, show a special message */
        var message = "Wrote successfully."

        if (exception != null)
            message = exception.message!!

        Snackbar.make(addNew, message, Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .setAnchorView(addNew)
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
                .setAnchorView(addNew)
                .show()
            return
        }

        /* Ask for confirmation, but keep data in memory so tag can be removed */
        val dialog = AlertDialog.Builder(this)
            .setTitle("Import")
            .setMessage("Import items from this tag?")
            .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                /* Splice the card contents and append the list view for the user */
                val nfcItems = listItems.parseJoinedString(String(nfcContent))
                listItems.addAll(nfcItems)

                /* Append data and scroll up to new data */
                viewAdapter.notifyItemRangeInserted(0, nfcItems.size)
                recyclerView.scrollToPosition(0)
            }
            .setNegativeButton("Cancel", null)

        /* Do not show the dialog if we already have it open */
        if (readContentsAlertDialog != null &&
            readContentsAlertDialog!!.isShowing)
            return

        readContentsAlertDialog = dialog.create()
        readContentsAlertDialog!!.show()
    }

    /* Tell the user to check device compatibility */
    private fun warnUserAboutNfcStatus(nfcState: Nfc.State) {
        /* Based on our Nfc status, show a special message */
        val nfcStateString = when (nfcState) {
            Nfc.State.SUPPORTED_OFF -> "Please enable Nfc."
            Nfc.State.UNSUPPORTED -> "This device lacks Nfc."
            else -> return
        }

        Snackbar.make(addNew, nfcStateString, Snackbar.LENGTH_SHORT)
            .setAction("Dismiss") {}
            .setAnchorView(addNew)
            .show()
    }

    /* Process Nfc tag scan event */
    private fun processNfcTagScanned() {
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
        processNfcTagScanned()
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
        emptyView = findViewById(R.id.recycler_view_empty)
        viewLayoutManager = LinearLayoutManager(this)
        bottomAppBar = findViewById(R.id.bottom_app_bar)
        addNew = findViewById(R.id.add_new)

        /* Use proper menu */
        bottomAppBar.replaceMenu(R.menu.menu_main)

        /* Create write dialog */
        writeContentsAlertDialog = AlertDialog.Builder(this)
            .setTitle("Write Nfc Tag")
            .setMessage("Hold a tag to the back of the device until this dialog disappears.")
            .setNegativeButton("Cancel", null)
            .setOnDismissListener {
                dismissWriteContentsAlertDialog()
            }.create()

        /* Create swap dialog */
        swapContentsAlertDialog = AlertDialog.Builder(this)
            .setTitle("Swap With Nfc Tag")
            .setMessage("Hold a tag to the back of the device until this dialog disappears.")
            .setNegativeButton("Cancel", null)
            .setOnDismissListener {
                dismissSwapContentsAlertDialog()
            }.create()

        /* Add new item */
        addNew.setOnClickListener {
            startActivity(Intent(this, ViewMoreActivity::class.java))
        }

        /* Menu navigation action */
        bottomAppBar.setNavigationOnClickListener {
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
        }

        /* Menu item actions */
        bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.sort -> {
                    /* Sort by tag, then by label */
                    viewAdapter.sort()
                    true
                }
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
                else -> false
            }
        }

        /* Set adapter */
        viewAdapter = RecyclerViewAdapter(
            /* This includes context */
            recyclerView,

            /* Our add button as a snackbar anchor */
            addNew,

            /* View to show when we have an empty list */
            emptyView
        )

        /* Update the recycler view */
        recyclerView.apply {
            layoutManager = viewLayoutManager
            adapter = viewAdapter
        }

        /* Setup our list view */
        viewAdapter.notifyDataSetChanged()

        /* Setup drag and drop handler */
        val callback = RecyclerViewDragHelper(
            /* Our add button as a snackbar anchor */
            addNew,
            viewAdapter,
            recyclerView,
            /* Supported vertical directions */
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            /* Supported horizontal directions */
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT)

        /* Create and attach our drag and drop handler */
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

        /* Check if we opened the app due to a Nfc event */
        processNfcTagScanned()
    }
}
