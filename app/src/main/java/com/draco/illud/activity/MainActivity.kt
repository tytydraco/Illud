package com.draco.illud.activity

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
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

    /* Shared Preferences */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPrefsEditor: SharedPreferences.Editor

    /* Close Nfc list */
    private fun closeNfcList() {
        nfcListOpen = false
        title = titleNotes
    }

    /* Open Nfc list */
    private fun openNfcList() {
        nfcListOpen = true
        title = titleEdit
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

    /* Setup toolbar menu actions */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.add_new -> {
                val viewMoreIntent = Intent(this, ViewMoreActivity::class.java)
                startActivityForResult(viewMoreIntent, ViewMoreActivity.activityResultCode)
            }

            R.id.stop_editing -> {
                if (nfcListOpen) {
                    closeNfcList()
                    Snackbar.make(recyclerView, "Closed list for editing.", Snackbar.LENGTH_SHORT)
                        .setAction("Dismiss") {}
                        .show()
                } else
                    Snackbar.make(recyclerView, "List is not in edit mode.", Snackbar.LENGTH_SHORT)
                        .setAction("Dismiss") {}
                        .show()
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

        title = titleNotes

        /* Setup encrypted shared preferences */
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        sharedPreferences = EncryptedSharedPreferences.create(
            "illud_shared_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        sharedPrefsEditor = sharedPreferences.edit()

        /* Register our Nfc helper class */
        nfc = Nfc()
        nfc.registerAdapter(this)
        nfc.setupForegroundIntent(this)

        /* Register our ListItems helper class */
        listItems = ListItems()

        /* Generate default tutorial item */
        val tutorialListItem = ListItem()
        with (tutorialListItem) {
            label = "Welcome to Illud!"
            tag = "Tutorial"
            content = "To use Illud with an NFC tag, simply scan it to start editing.\n\n" +
                    "Once you have finished making changes, scan the NFC tag again to write the new contents.\n\n" +
                    "If you wish to import the contents from an NFC tag without making any changes, " +
                    "press the check mark menu item to stop editing the tag.\n\n" +
                    "All locally stored items are encrypted with AES256 encryption.\n\n" +
                    "Enjoy!"
        }

        /* Load saved list items */
        val listItemsJoinedString = sharedPreferences.getString("listItemsJoinedString", tutorialListItem.toString())!!
        val items = listItems.parseJoinedString(listItemsJoinedString)
        listItems.items.addAll(items)

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

        /* Save local list item changes */
        sharedPrefsEditor.putString("listItemsJoinedString", listItems.generateJoinedString())
        sharedPrefsEditor.apply()
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
