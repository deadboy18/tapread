package com.tapread.nfc.util

object HexUtil {

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    /** ByteArray → "0A 1B 2C" */
    fun toHexSpaced(bytes: ByteArray): String {
        return bytes.joinToString(" ") { byte ->
            val i = byte.toInt() and 0xFF
            "${HEX_CHARS[i shr 4]}${HEX_CHARS[i and 0x0F]}"
        }
    }

    /** ByteArray → "0A1B2C" */
    fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt() and 0xFF
            sb.append(HEX_CHARS[i shr 4])
            sb.append(HEX_CHARS[i and 0x0F])
        }
        return sb.toString()
    }

    /** "0A1B2C" → ByteArray */
    fun fromHex(hex: String): ByteArray {
        val clean = hex.replace(" ", "").replace(":", "")
        check(clean.length % 2 == 0) { "Odd-length hex string" }
        return ByteArray(clean.length / 2) { i ->
            val hi = Character.digit(clean[i * 2], 16)
            val lo = Character.digit(clean[i * 2 + 1], 16)
            ((hi shl 4) or lo).toByte()
        }
    }
}
