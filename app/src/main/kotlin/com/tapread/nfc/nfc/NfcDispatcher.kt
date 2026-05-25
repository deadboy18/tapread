package com.tapread.nfc.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import org.slf4j.LoggerFactory

/**
 * Manages NFC ReaderMode enablement and tag dispatch.
 *
 * Uses [NfcAdapter.enableReaderMode] (modern API) instead of foreground dispatch.
 * This gives explicit tech-list control and skips the platform NFC sound.
 */
class NfcDispatcher(
    private val activity: Activity,
    private val onTagDiscovered: (Tag) -> Unit
) {
    private val log = LoggerFactory.getLogger("NfcDispatcher")
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isNfcAvailable: Boolean get() = nfcAdapter != null
    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        log.info("Tag discovered: {}", tag.techList?.joinToString())
        onTagDiscovered(tag)
    }

    /**
     * Enable reader mode. Call from onResume().
     */
    fun enableReaderMode() {
        val adapter = nfcAdapter ?: return

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        adapter.enableReaderMode(activity, readerCallback, flags, options)
        log.debug("Reader mode enabled")
    }

    /**
     * Disable reader mode. Call from onPause().
     */
    fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
        log.debug("Reader mode disabled")
    }

    companion object {
        /**
         * Extract IsoDep from a discovered tag, if available.
         */
        fun getIsoDep(tag: Tag): IsoDep? {
            return IsoDep.get(tag)
        }
    }
}
