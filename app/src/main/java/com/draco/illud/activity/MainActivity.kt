package com.draco.illud.activity

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.draco.illud.R
import com.draco.illud.recycler_view.RecyclerViewAdapter
import com.draco.illud.recycler_view.RecyclerViewDragHelper
import com.draco.illud.utils.ListItem
import com.draco.illud.utils.ListItems
import com.draco.illud.utils.Nfc
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    /* UI elements */
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var viewAdapter: RecyclerViewAdapter
    private lateinit var viewLayoutManager: RecyclerView.LayoutManager
    private lateinit var nfcModeMenuItem: MenuItem

    /* If user has a temporary Nfc list open for editing */
    enum class NfcMode {
        UPLOAD,
        DOWNLOAD,
        SWAP
    }
    private var nfcMode = NfcMode.UPLOAD

    /* Private classes */
    private lateinit var nfc: Nfc
    private lateinit var listItems: ListItems

    /* Shared Preferences */
    private lateinit var sharedPreferences: SharedPreferences

    /* Update card contents  */
    private fun nfcExport(intent: Intent): Boolean {
        val writeString = listItems.generateJoinedString()
        val exception = nfc.writeBytes(intent, writeString.toByteArray())

        if (exception != null) {
            Snackbar.make(recyclerView, exception.message!!, Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.snackbar_dismiss)) {}
                .show()

            return false
        }

        Snackbar.make(recyclerView, getString(R.string.snackbar_exported_successfully), Snackbar.LENGTH_SHORT)
            .setAction(getString(R.string.snackbar_dismiss)) {}
            .show()

        return true
    }

    /* Read and process card contents (appends) */
    private fun nfcImport(intent: Intent): Boolean {
        val nfcContent = nfc.readBytes(intent)

        /* Preserve pre-import items */
        val backupItems = ArrayList(listItems.items)

        /* Splice the card contents and append the list view for the user */
        val nfcItems = listItems.parseJoinedString(String(nfcContent))
        listItems.items.addAll(nfcItems)

        /* Update information */
        viewAdapter.notifyItemRangeInserted(backupItems.size, nfcItems.size)

        Snackbar.make(recyclerView, getString(R.string.snackbar_imported_successfully), Snackbar.LENGTH_SHORT)
            .setAction(getString(R.string.snackbar_undo)) {
                listItems.items = backupItems
                viewAdapter.notifyItemRangeRemoved(backupItems.size, nfcItems.size)
            }
            .show()

        return true
    }

    /* Swap internal contents with card contents */
    private fun nfcSwap(intent: Intent): Boolean {
        val nfcContent = nfc.readBytes(intent)

        /* Preserve pre-import items */
        val backupItems = ArrayList(listItems.items)

        /* Splice the card contents */
        val nfcItems = listItems.parseJoinedString(String(nfcContent))

        /* Write list items to tag */
        val writeString = listItems.generateJoinedString()
        val exception = nfc.writeBytes(intent, writeString.toByteArray())

        if (exception != null) {
            /* Restore backed up list */
            listItems.items = backupItems
            viewAdapter.notifyItemRangeRemoved(backupItems.size, nfcItems.size)
            Snackbar.make(recyclerView, exception.message!!, Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.snackbar_dismiss)) {}
                .show()

            return false
        }

        /* Finalize adding list items */
        listItems.items.clear()
        listItems.items.addAll(nfcItems)

        /* Update information */
        viewAdapter.notifyDataSetChanged()

        Snackbar.make(recyclerView, getString(R.string.snackbar_swapped_successfully), Snackbar.LENGTH_SHORT)
            .setAction(getString(R.string.snackbar_dismiss)) {}
            .show()

        return true
    }

    /* Process Nfc tag scan event */
    private fun handleNfcScan(intent: Intent) {
        /* Make sure we are processing an Nfc tag */
        if (!nfc.startedByNDEF(intent))
            return

        when (nfcMode) {
            NfcMode.UPLOAD -> nfcExport(intent)
            NfcMode.DOWNLOAD -> nfcImport(intent)
            NfcMode.SWAP -> nfcSwap(intent)
        }
    }

    /* Handle action bar events */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_new -> {
                val viewMoreIntent = Intent(this, ViewMoreActivity::class.java)
                startActivityForResult(viewMoreIntent, ViewMoreActivity.activityResultCode,
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }

            R.id.nfc_mode -> {
                when (nfcMode) {
                    NfcMode.UPLOAD -> {
                        nfcMode = NfcMode.DOWNLOAD
                        nfcModeMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_file_download_24dp)
                        Snackbar.make(recyclerView, getString(R.string.snackbar_mode_import), Snackbar.LENGTH_SHORT)
                            .setAction(getString(R.string.snackbar_dismiss)) {}
                            .show()
                    }

                    NfcMode.DOWNLOAD -> {
                        nfcMode = NfcMode.SWAP
                        nfcModeMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_swap_vert_24)
                        Snackbar.make(recyclerView, getString(R.string.snackbar_mode_swap), Snackbar.LENGTH_SHORT)
                            .setAction(getString(R.string.snackbar_dismiss)) {}
                            .show()
                    }

                    NfcMode.SWAP -> {
                        nfcMode = NfcMode.UPLOAD
                        nfcModeMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_file_upload_24dp)
                        Snackbar.make(recyclerView, getString(R.string.snackbar_mode_export), Snackbar.LENGTH_SHORT)
                            .setAction(getString(R.string.snackbar_dismiss)) {}
                            .show()
                    }
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

        /* Setup encrypted shared preferences */
       KeyGenParameterSpec.Builder(
           "illud_encrypted_prefs_key",
           KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).build()
        val masterKeyAlias = MasterKey.Builder(this, "illud_encrypted_prefs_key")
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "illud_shared_prefs",
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        /* Register our Nfc helper class */
        nfc = Nfc(this)

        /* Register our ListItems helper class */
        listItems = ListItems()

        /* Generate default tutorial item */
        val tutorialListItem = ListItem()
        with (tutorialListItem) {
            label = "Welcome to Illud!"
            tag = "Tutorial"
            content = getString(R.string.tutorial_text)
        }

        /* Load saved list items */
        val listItemsJoinedString = sharedPreferences.getString("listItemsJoinedString", tutorialListItem.toString())!!
        val items = listItems.parseJoinedString(listItemsJoinedString)
        listItems.items.addAll(items)

        /* Set our local lateinit variables */
        recyclerView = findViewById(R.id.recycler_view)
        emptyView = findViewById(R.id.recycler_view_empty)
        viewLayoutManager = LinearLayoutManager(this)

        viewAdapter = RecyclerViewAdapter(
            this,
            recyclerView,
            listItems,
            emptyView
        )

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
        val editor = sharedPreferences.edit()
        editor.putString("listItemsJoinedString", listItems.generateJoinedString())
        editor.apply()
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

        /* Disable nfc mode if device lacks nfc */
        if (nfc.supportState() == Nfc.State.UNSUPPORTED)
            nfcModeMenuItem.isVisible = false

        return super.onCreateOptionsMenu(menu)
    }
}
