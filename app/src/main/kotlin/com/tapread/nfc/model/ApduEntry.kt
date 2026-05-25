package com.tapread.nfc.model

/**
 * A single APDU command/response pair captured during card communication.
 */
data class ApduEntry(
    val command: ByteArray,
    val response: ByteArray,
    val label: String = "",
    val timestampMs: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApduEntry) return false
        return command.contentEquals(other.command) &&
                response.contentEquals(other.response) &&
                label == other.label
    }

    override fun hashCode(): Int {
        var result = command.contentHashCode()
        result = 31 * result + response.contentHashCode()
        result = 31 * result + label.hashCode()
        return result
    }
}
