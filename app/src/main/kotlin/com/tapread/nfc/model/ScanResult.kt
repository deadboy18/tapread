package com.tapread.nfc.model

import java.util.UUID

/**
 * A single scan event — either an EMV card (CardData) or a TNG card (TngData).
 */
data class ScanResult(
    val id: String = UUID.randomUUID().toString(),
    val card: CardData? = null,
    val tng: TngData? = null,
    val apduLog: List<ApduEntry> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
    val error: String? = null
) {
    val isTng: Boolean get() = tng != null
    val isEmv: Boolean get() = card != null

    val displayLabel: String
        get() = when {
            tng != null -> tng.displayLabel
            card != null -> {
                val scheme = card.scheme ?: "Unknown"
                val last4 = card.last4
                val wallet = if (card.isTokenized) " (${card.walletType ?: "Tokenized"})" else ""
                "$scheme$wallet • $last4"
            }
            else -> error ?: "Unknown"
        }
}
