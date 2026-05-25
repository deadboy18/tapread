package com.tapread.nfc.model

import java.util.UUID

/**
 * A single scan event: the card data + the full APDU log captured.
 */
data class ScanResult(
    val id: String = UUID.randomUUID().toString(),
    val card: CardData,
    val apduLog: List<ApduEntry>,
    val timestampMs: Long = System.currentTimeMillis(),
    val error: String? = null
) {
    /** Short display label: "Mastercard • 0641" or "Apple Pay • 0641" */
    val displayLabel: String
        get() {
            val scheme = card.scheme ?: "Unknown"
            val last4 = card.last4
            val wallet = if (card.isTokenized) " (${card.walletType ?: "Tokenized"})" else ""
            return "$scheme$wallet • $last4"
        }
}
