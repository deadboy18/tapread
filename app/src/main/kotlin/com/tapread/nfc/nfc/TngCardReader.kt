package com.tapread.nfc.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import com.tapread.nfc.model.TngData
import com.tapread.nfc.model.TngTransaction
import com.tapread.nfc.util.HexUtil
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Touch 'n Go MIFARE Classic card reader.
 *
 * Uses recovered master keys + XOR-based KDF to derive per-card sector keys,
 * then authenticates and reads all accessible sectors.
 *
 * Confirmed working on:
 *   - MyKad + TNG combo cards (MIFARE Classic 1K, Infineon, SAK 0x88)
 *   - Standalone TNG cards (MIFARE Classic 4K, SAK 0x38)
 */
object TngCardReader {

    private val log = LoggerFactory.getLogger("TngCardReader")

    // Master keys (Key A) — recovered 2026-06-04
    private val MASTER_KEYS_A: Map<Int, ByteArray> = mapOf(
        0  to hex("11728019a32f"),
        1  to hex("1c9d6d91461e"),
        2  to hex("69eac233a7a2"),
        3  to hex("7d3d158e1961"),
        4  to hex("8896c6483eea"),
        5  to hex("222b3c401985"),
        6  to hex("95571b1c0953"),
        7  to hex("238a105f49d3"),
        8  to hex("8978541bd015"),
        9  to hex("0c552b956f85"),
        10 to hex("3d5da988d6a9"),
        12 to hex("51cee45f0a7e"),
        13 to hex("0cf2ed61f90b"),
        14 to hex("243af00e4605"),
    )

    // Master keys (Key B) — for sectors where Key A is unknown
    private val MASTER_KEYS_B: Map<Int, ByteArray> = mapOf(
        11 to hex("e22c99acb059"),
        15 to hex("e7e6821312bc"),
    )

    private val TNG_FINGERPRINT = ByteArray(16) { it.toByte() }

    // Factory template written to sector 5 on all new cards — not a real transaction
    // Signature: F9 ED 30 32 30 32 (identical across all cards tested)

    private fun isFactoryTemplate(block: ByteArray): Boolean {
        if (block.size < 6) return false
        return block[0] == 0xF9.toByte() && block[1] == 0xED.toByte() &&
               block[2] == 0x30.toByte() && block[3] == 0x32.toByte() &&
               block[4] == 0x30.toByte() && block[5] == 0x32.toByte()
    }

    // ── KDF ──────────────────────────────────────────────────────────────

    private fun deriveKey(masterKey: ByteArray, uid: ByteArray): ByteArray {
        val u0 = uid[0].toInt() and 0xFF
        val u1 = uid[1].toInt() and 0xFF
        val u2 = uid[2].toInt() and 0xFF
        val u3 = uid[3].toInt() and 0xFF
        val pattern = byteArrayOf(
            (u1 xor u2 xor u3).toByte(),
            uid[1],
            uid[2],
            (((u0 + u1 + u2 + u3) and 0xFF) xor u3).toByte(),
            0, 0
        )
        return ByteArray(6) { i -> (masterKey[i].toInt() xor pattern[i].toInt()).toByte() }
    }

    // ── UID → Serial ────────────────────────────────────────────────────

    private fun uidToSerial(uid: ByteArray): Long {
        val r = uid.reversedArray()
        return ((r[0].toLong() and 0xFF) shl 24) or
               ((r[1].toLong() and 0xFF) shl 16) or
               ((r[2].toLong() and 0xFF) shl 8) or
                (r[3].toLong() and 0xFF)
    }

    // ── Timestamp parsing ───────────────────────────────────────────────

    private fun parseTimestamp(data: ByteArray, offset: Int = 0): String? {
        if (offset + 4 > data.size) return null
        val v = ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset+1].toInt() and 0xFF) shl 16) or
                ((data[offset+2].toInt() and 0xFF) shl 8) or
                 (data[offset+3].toInt() and 0xFF)
        if (v == 0) return null
        val hour   = (v ushr 27) and 0x1F
        val minute = (v ushr 21) and 0x3F
        val second = (v ushr 15) and 0x3F
        val year   = ((v ushr 9) and 0x3F) + 1990
        val month  = (v ushr 5) and 0x0F
        val day    = v and 0x1F
        return "%d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)
    }

    private fun parseDatestamp(data: ByteArray, offset: Int = 0): String? {
        if (offset + 2 > data.size) return null
        val v = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset+1].toInt() and 0xFF)
        if (v == 0) return null
        val day   = v and 0x1F
        val month = (v ushr 5) and 0x0F
        val year  = ((v ushr 9) and 0x3F) + 1990
        return "%d-%02d-%02d".format(year, month, day)
    }

    // ── Luhn ────────────────────────────────────────────────────────────

    private fun luhnDigit(number: String): Char {
        val digits = number.map { it.digitToInt() }
        val odd = digits.filterIndexed { i, _ -> (digits.size - i) % 2 == 1 }.sum()
        val even = digits.filterIndexed { i, _ -> (digits.size - i) % 2 == 0 }
            .sumOf { d -> (d * 2).let { if (it > 9) it - 9 else it } }
        return ((10 - (odd + even) % 10) % 10).digitToChar()
    }

    // ── Transaction parsing ─────────────────────────────────────────────

    private fun parseTransaction(block: ByteArray, label: String): TngTransaction? {
        if (block.size < 16 || block.all { it == 0.toByte() }) return null
        val txnId = ((block[0].toInt() and 0xFF) shl 8) or (block[1].toInt() and 0xFF)
        val agency = HexUtil.toHex(block.sliceArray(2..5))
        val balAfter = ByteBuffer.wrap(block, 6, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val amount = ((block[10].toInt() and 0xFF) shl 8) or (block[11].toInt() and 0xFF)
        val ts = parseTimestamp(block, 12)
        return TngTransaction(
            label = label,
            txnId = txnId,
            agency = agency,
            amountSen = amount,
            balanceAfterSen = balAfter,
            timestamp = ts
        )
    }

    // ── Main read ───────────────────────────────────────────────────────

    fun read(tag: Tag): TngData {
        val uid = tag.id ?: return TngData(error = "No UID")
        if (uid.size != 4) return TngData(error = "UID not 4 bytes: ${uid.size}")

        val serial = uidToSerial(uid)
        val uidHex = HexUtil.toHexColons(uid)

        val mfc = MifareClassic.get(tag)
            ?: return TngData(uid = uidHex, serial = serial, error = "MifareClassic unavailable")

        val sectors = mutableMapOf<Int, MutableMap<Int, ByteArray>>()
        var chipType: String? = null

        try {
            mfc.connect()

            // Detect chip type
            chipType = when (mfc.type) {
                MifareClassic.TYPE_CLASSIC -> when (mfc.size) {
                    MifareClassic.SIZE_1K -> "MIFARE Classic 1K"
                    MifareClassic.SIZE_4K -> "MIFARE Classic 4K"
                    MifareClassic.SIZE_MINI -> "MIFARE Classic Mini"
                    else -> "MIFARE Classic (${mfc.size} bytes)"
                }
                MifareClassic.TYPE_PLUS -> "MIFARE Plus"
                MifareClassic.TYPE_PRO -> "MIFARE Pro"
                else -> "MIFARE Unknown"
            }

            // Read all sectors — Key A first, then Key B for sectors without Key A
            val totalSectors = mfc.sectorCount
            for (sector in 0 until totalSectors) {
                val keyA = MASTER_KEYS_A[sector]?.let { deriveKey(it, uid) }
                val keyB = MASTER_KEYS_B[sector]?.let { deriveKey(it, uid) }

                try {
                    val authed = when {
                        keyA != null && mfc.authenticateSectorWithKeyA(sector, keyA) -> true
                        keyB != null && mfc.authenticateSectorWithKeyB(sector, keyB) -> true
                        else -> false
                    }
                    if (authed) {
                        val sectorData = mutableMapOf<Int, ByteArray>()
                        val firstBlock = mfc.sectorToBlock(sector)
                        val blockCount = mfc.getBlockCountInSector(sector)
                        for (b in 0 until minOf(blockCount, 3)) {
                            try {
                                sectorData[b] = mfc.readBlock(firstBlock + b)
                            } catch (e: Exception) {
                                log.warn("Read error sector $sector block $b: ${e.message}")
                            }
                        }
                        sectors[sector] = sectorData
                    }
                } catch (e: Exception) {
                    log.warn("Sector $sector: ${e.message}")
                }
            }
        } catch (e: Exception) {
            return TngData(uid = uidHex, serial = serial, error = e.message)
        } finally {
            try { mfc.close() } catch (_: Exception) {}
        }

        // ── Decode ──────────────────────────────────────────────────

        val builder = TngData(uid = uidHex, serial = serial, sectorsRead = sectors.size)
        builder.cardType = chipType

        // Sector 0: identity
        sectors[0]?.let { s ->
            s[1]?.let { builder.isTng = it.contentEquals(TNG_FINGERPRINT) }
            s[2]?.let { b ->
                val cardNoRaw = ((b[7].toLong() and 0xFF) shl 24) or
                                ((b[8].toLong() and 0xFF) shl 16) or
                                ((b[9].toLong() and 0xFF) shl 8) or
                                 (b[10].toLong() and 0xFF)
                val base = "6014640" + cardNoRaw.toString().padStart(10, '0')
                builder.cardNumber = base + luhnDigit(base)
                builder.expiry = parseDatestamp(b, 14)
            }
        }

        // Sector 1: issue info
        sectors[1]?.get(0)?.let { b ->
            builder.issueDate = parseDatestamp(b, 14)
        }

        // Sector 2: balance
        sectors[2]?.get(0)?.let { b ->
            builder.balanceSen = ByteBuffer.wrap(b, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        }

        // Sector 3: transaction counter
        sectors[3]?.get(0)?.let { b ->
            val raw = ByteBuffer.wrap(b, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
            builder.txnCount = 0xF9FF - (raw and 0xFFFF)
        }

        // Sector 5: last toll (skip if factory template)
        sectors[5]?.get(0)?.let {
            if (!isFactoryTemplate(it)) {
                builder.lastToll = parseTransaction(it, "Toll")
            }
        }

        // Sector 6: last trip
        sectors[6]?.get(0)?.let { builder.lastTrip = parseTransaction(it, "Transit") }

        // Sector 7: last reload
        sectors[7]?.get(0)?.let { builder.lastReload = parseTransaction(it, "Reload") }

        // Sector 8: last retail
        sectors[8]?.get(0)?.let { builder.lastRetail = parseTransaction(it, "Retail") }

        // Raw sector data for display
        val rawDump = StringBuilder()
        for (sector in sectors.keys.sorted()) {
            rawDump.appendLine("─── Sector $sector ───")
            sectors[sector]?.let { s ->
                for (block in s.keys.sorted()) {
                    val abs = sector * 4 + block
                    rawDump.appendLine("  Blk $abs: ${HexUtil.toHexSpaced(s[block]!!)}")
                }
            }
        }
        builder.rawDump = rawDump.toString()

        return builder
    }

    private fun hex(s: String) = ByteArray(s.length / 2) { i ->
        s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
