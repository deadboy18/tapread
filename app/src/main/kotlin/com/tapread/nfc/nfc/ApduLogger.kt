package com.tapread.nfc.nfc

import com.tapread.nfc.model.ApduEntry
import com.tapread.nfc.util.HexUtil
import org.slf4j.LoggerFactory

/**
 * Intercepts and logs every APDU exchange between the phone and the card.
 * Accumulated entries are retrievable after a scan completes.
 */
class ApduLogger {

    private val log = LoggerFactory.getLogger("ApduLogger")
    private val _entries = mutableListOf<ApduEntry>()

    val entries: List<ApduEntry> get() = _entries.toList()

    fun logCommand(command: ByteArray, label: String = "") {
        log.debug(">> CMD [{}]: {}", label, HexUtil.toHexSpaced(command))
    }

    fun logResponse(command: ByteArray, response: ByteArray, label: String = "") {
        log.debug("<< RSP [{}]: {}", label, HexUtil.toHexSpaced(response))
        _entries.add(ApduEntry(command.copyOf(), response.copyOf(), label))
    }

    fun clear() {
        _entries.clear()
    }
}
