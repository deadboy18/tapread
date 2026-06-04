package com.tapread.nfc.model

data class TngTransaction(
    val label: String,
    val txnId: Int = 0,
    val agency: String = "",
    val amountSen: Int = 0,
    val balanceAfterSen: Int = 0,
    val timestamp: String? = null
) {
    val amountRm: String get() = "RM %.2f".format(amountSen / 100.0)
    val balanceAfterRm: String get() = "RM %.2f".format(balanceAfterSen / 100.0)
}

data class TngData(
    val uid: String = "",
    val serial: Long = 0,
    var cardNumber: String? = null,
    var expiry: String? = null,
    var issueDate: String? = null,
    var balanceSen: Int? = null,
    var txnCount: Int? = null,
    var isTng: Boolean? = null,
    var cardType: String? = null,       // "MIFARE Classic 1K" or "4K"
    var lastToll: TngTransaction? = null,
    var lastTrip: TngTransaction? = null,
    var lastReload: TngTransaction? = null,
    var lastRetail: TngTransaction? = null,
    var rawDump: String? = null,
    var sectorsRead: Int = 0,
    val error: String? = null
) {
    val balanceRm: String get() = balanceSen?.let { "RM %.2f".format(it / 100.0) } ?: "—"
    val balanceDisplay: String get() = balanceSen?.let { "%.2f".format(it / 100.0) } ?: "—"
    val serialStr: String get() = serial.toString()
    val isSuccess: Boolean get() = error == null && balanceSen != null
    val transactions: List<TngTransaction> get() = listOfNotNull(lastReload, lastToll, lastTrip, lastRetail)

    val displayLabel: String get() = "Touch 'n Go • $serialStr"
}
