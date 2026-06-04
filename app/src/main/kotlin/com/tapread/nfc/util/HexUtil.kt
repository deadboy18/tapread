package com.tapread.nfc.util

object HexUtil {
    fun toHex(b: ByteArray): String = b.joinToString("") { "%02X".format(it) }
    fun toHexSpaced(b: ByteArray): String = b.joinToString(" ") { "%02X".format(it) }
    fun toHexColons(b: ByteArray): String = b.joinToString(":") { "%02x".format(it) }
}
