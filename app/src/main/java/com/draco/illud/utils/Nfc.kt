package com.draco.illud.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.*
import android.nfc.tech.Ndef
import java.io.IOException

class Nfc {
    /* Device Nfc State */
    enum class State {
        /* Device lacks Nfc support on a hardware level */
        UNSUPPORTED,
        /* The device supports Nfc, but it is currently off */
        SUPPORTED_OFF,
        /* The device supports Nfc, and it is currently on */
        SUPPORTED_ON
    }

    /* Custom exception for non-writable tags */
    class NotWritableException(message: String? = null): Exception(message)

    /* Check if Nfc supported */
    private var nfcAdapter: NfcAdapter? = null

    /* Read NDEF while app is running */
    private var nfcPendingIntent: PendingIntent? = null

    /* Public static functions */
    companion object {

        /* Mime type for NDEF record. P.S.: Takes up Nfc tag space */
        private const val mimeType: String = "text/tagdrive"

        /* Return the maximum amount of bytes that the tag can hold */
        fun maxSize(intent: Intent?): Int {
            val currentTag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            var maxSize: Int

            /* Try to write to the tag; if fail, return false */
            try {
                val ndef = Ndef.get(currentTag)
                ndef.connect()
                maxSize = ndef.maxSize
                ndef.close()
            } catch (e: Exception) {
                maxSize = 0
            }

            return maxSize
        }

        /* Get the byte contents of a Nfc tag */
        fun readBytes(intent: Intent?): ByteArray? {
            val parcelables = intent?.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

            /* Don't process nothing! */
            if (parcelables.isNullOrEmpty())
                return null

            /* Get the first record */
            val ndefMessage = parcelables[0] as NdefMessage
            val ndefRecord = ndefMessage.records[0]

            /* Return the content of the first record */
            return ndefRecord.payload
        }

        /* Try to write a ByteArray to a tag. Return true if succeeded */
        fun writeBytes(intent: Intent?, bytes: ByteArray): Exception? {
            var exception: Exception? = null
            val currentTag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            /* Connect to the tag */
            val ndef = Ndef.get(currentTag)

            /* Try to write to the tag; if fail, return false */
            try {
                ndef.connect()

                /* Don't bother writing if read-only */
                if (!ndef.isWritable) {
                    ndef.close()
                    throw NotWritableException()
                }

                /* Write the message */
                val record = createByteRecord(bytes)
                ndef.writeNdefMessage(NdefMessage(record))
            } catch (_: NotWritableException) {
                /* Tag is write only */
                exception = NotWritableException("Tag not writable.")
            } catch (_: FormatException) {
                /* Malformed NDEF */
                exception = FormatException("There was an internal error.")
            } catch (_: IOException) {
                /* Cancelled or unexpected data */
                exception = IOException("Write unexpectedly cancelled.")
            } catch (_: TagLostException) {
                /* Tag removed too quickly */
                exception = TagLostException("Tag disconnected.")
            }

            /* Close */
            ndef.close()

            return exception
        }

        /* Check if the passed intent was caused by a tag */
        fun startedByNDEF(intent: Intent?): Boolean {
            val action = intent?.action

            /* Any of these actions are valid for an Nfc tag scan */
            return (NfcAdapter.ACTION_NDEF_DISCOVERED == action)
        }

        /* Create binary record */
        private fun createByteRecord(bytes: ByteArray): NdefRecord {
            /* Use binary, without language code */
            return NdefRecord.createMime(mimeType, bytes)
        }
    }

    /* Setup for scanning Nfc tags while in foreground */
    fun setupForegroundIntent(context: Context) {
        nfcPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, context.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            0
        )
    }

    /* Enable foreground scanning (call from onResume) */
    fun enableForegroundIntent(activity: Activity) {
        /* Nfc must be on */
        if (supportState() != State.SUPPORTED_ON)
            return

        /* Allow scanning while app is open */
        nfcAdapter?.enableForegroundDispatch(
            activity,
            nfcPendingIntent,
            null,
            null
        )
    }

    /* Disable foreground scanning (call from onPause) */
    fun disableForegroundIntent(activity: Activity) {
        /* Nfc must be on */
        if (supportState() != State.SUPPORTED_ON)
            return

        /* Disable scanning while app is closed */
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    /* Try to register the Nfc adapter */
    fun registerAdapter(context: Context) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    }

    /* Check if Nfc is supported on the current device */
    fun supportState(): State {
        /* Device lacks hardware support */
        if (nfcAdapter == null)
            return State.UNSUPPORTED

        /* Device has Nfc disabled */
        if (!nfcAdapter?.isEnabled!!)
            return State.SUPPORTED_OFF

        /* Device has Nfc enabled */
        return State.SUPPORTED_ON
    }
}