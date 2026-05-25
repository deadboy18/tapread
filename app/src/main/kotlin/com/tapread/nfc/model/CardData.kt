package com.tapread.nfc.model

import java.util.Date

data class CardData(
    val pan: String?,
    val expiry: Date?,
    val holderName: String?,
    val scheme: String?,
    val aid: String?,
    val aidLabel: String?,
    val track1: String?,
    val track2: String?,
    val atr: String?,
    val atrDescription: String?,
    val applicationCount: Int = 1,
    val allAids: List<AidInfo> = emptyList(),
    val transactions: List<TransactionInfo> = emptyList(),
    // Extended fields
    val serviceCode: String? = null,
    val panSequenceNumber: String? = null,
    val effectiveDate: String? = null,
    val issuerCountryCode: String? = null,
    val currencyCode: String? = null,
    val applicationLabel: String? = null,
    val contactlessStatus: ContactlessStatus = ContactlessStatus.ACTIVE,
    val contactlessStatusDetail: String? = null,
    // CPLC
    val cplcData: String? = null,
    // Wallet/tokenization
    val walletType: String? = null,       // "Apple Pay", "Google Pay", "Samsung Pay", etc.
    val isTokenized: Boolean = false      // True if DPAN detected
) {
    val last4: String get() = pan?.takeLast(4) ?: "????"

    val maskedPan: String get() {
        val p = pan ?: return "••••  ••••  ••••  ••••"
        if (p.length < 8) return p
        val first4 = p.take(4)
        val last4 = p.takeLast(4)
        return "$first4  ••••  ••••  $last4"
    }

    val formattedPan: String get() {
        val p = pan ?: return "••••  ••••  ••••  ••••"
        return p.chunked(4).joinToString("  ")
    }

    val formattedExpiry: String get() {
        val d = expiry ?: return "--/--"
        @Suppress("DEPRECATION")
        val month = String.format("%02d", d.month + 1)
        @Suppress("DEPRECATION")
        val year = String.format("%02d", d.year % 100)
        return "$month/$year"
    }
}

data class AidInfo(
    val aid: String,
    val label: String?,
    val priority: Int?
)

data class TransactionInfo(
    val date: Date?,
    val amount: String?,
    val currency: String?,
    val country: String?,
    val cryptogram: String?,
    val atc: String?,
    val transactionType: String?,
    val time: String? = null,           // Transaction time HH:mm:ss
    val cryptogramType: String? = null  // ARQC, TC, AAC
)

enum class ContactlessStatus {
    ACTIVE,          // Normal read
    DISABLED,        // PPSE returned 6A82 (app removed/disabled)
    BLOCKED,         // PPSE returned 6985 (conditions not satisfied)
    NOT_PAYMENT_CARD // Tag responds but no PPSE
}
