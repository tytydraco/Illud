package com.draco.illud.activity

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    /* UI elements */
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var viewAdapter: RecyclerViewAdapter
    private lateinit var viewLayoutManager: RecyclerView.LayoutManager
    private lateinit var nfcModeMenuItem: MenuItem

    /* If user has a temporary Nfc list open for editing */
    enum class NfcMode {
        UPLOAD,
        DOWNLOAD
    }
    private var nfcMode = NfcMode.DOWNLOAD

    /* Private classes */
    private lateinit var nfc: Nfc
    private lateinit var listItems: ListItems

    /* Shared Preferences */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPrefsEditor: SharedPreferences.Editor

    /* Update card contents  */
    private fun nfcExport(intent: Intent): Boolean {
        val writeString = listItems.generateJoinedString()
        val exception = nfc.writeBytes(intent, writeString.toByteArray())

        if (exception != null) {
            Snackbar.make(recyclerView, exception.message!!, Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()

            return false
        }

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
        if (nfcMode == NfcMode.UPLOAD)
            nfcExport(intent)
        else
            nfcImport(intent)
    }

    /* Setup toolbar menu actions */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.add_new -> {
                val viewMoreIntent = Intent(this, ViewMoreActivity::class.java)
                startActivityForResult(viewMoreIntent, ViewMoreActivity.activityResultCode)
            }

            R.id.nfc_mode -> {
                if (nfcMode == NfcMode.UPLOAD) {
                    nfcMode = NfcMode.DOWNLOAD
                    nfcModeMenuItem.icon = getDrawable(R.drawable.ic_file_download_24dp)
                    Snackbar.make(recyclerView, "Will import items from scanned tag.", Snackbar.LENGTH_SHORT)
                        .setAction("Dismiss") {}
                        .show()
                } else {
                    nfcMode = NfcMode.UPLOAD
                    nfcModeMenuItem.icon = getDrawable(R.drawable.ic_file_upload_24dp)
                    Snackbar.make(recyclerView, "Will upload items to scanned tag.", Snackbar.LENGTH_SHORT)
                        .setAction("Dismiss") {}
                        .show()
                }
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
        nfcModeMenuItem = menu!!.findItem(R.id.nfc_mode)
        return super.onCreateOptionsMenu(menu)
    }
}
