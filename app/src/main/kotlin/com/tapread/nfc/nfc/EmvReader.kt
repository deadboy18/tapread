package com.tapread.nfc.nfc

import android.nfc.tech.IsoDep
import com.github.devnied.emvnfccard.model.Application
import com.github.devnied.emvnfccard.model.EmvCard
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.tapread.nfc.model.*
import com.tapread.nfc.util.HexUtil
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmvReader {

    private val log = LoggerFactory.getLogger("EmvReader")

    fun read(isoDep: IsoDep): ScanResult {
        val logger = ApduLogger()
        val provider = IsoDepProvider(logger)

        isoDep.connect()
        isoDep.timeout = 5000
        provider.setTagCom(isoDep)

        val config = EmvTemplate.Config()
            .setContactLess(true)
            .setReadAllAids(true)
            .setReadTransactions(true)
            .setReadCplc(true)
            .setRemoveDefaultParsers(false)
            .setReadAt(true)

        val template = EmvTemplate.Builder()
            .setProvider(provider)
            .setConfig(config)
            .build()

        return try {
            val emvCard: EmvCard = template.readEmvCard()
            val cardData = extractCardData(emvCard, provider, logger)
            ScanResult(card = cardData, apduLog = logger.entries)
        } catch (e: Exception) {
            log.error("EMV read failed", e)
            // Check if it's a contactless-disabled card
            val status = detectContactlessStatus(logger)
            val fallback = CardData(
                pan = null, expiry = null, holderName = null,
                scheme = null, aid = null, aidLabel = null,
                track1 = null, track2 = null,
                atr = null, atrDescription = null,
                contactlessStatus = status,
                contactlessStatusDetail = when (status) {
                    ContactlessStatus.DISABLED -> "NFC is locked on your card or card not activated"
                    ContactlessStatus.BLOCKED -> "Contactless blocked by issuer (conditions not satisfied)"
                    else -> e.message
                }
            )
            ScanResult(card = fallback, apduLog = logger.entries, error = fallback.contactlessStatusDetail)
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }

    private fun extractCardData(emvCard: EmvCard, provider: IsoDepProvider, logger: ApduLogger): CardData {
        val pan = emvCard.cardNumber
        val expiry = emvCard.expireDate
        val holder = buildHolderName(emvCard)

        val apps = emvCard.applications ?: emptyList()
        val primaryApp = apps.firstOrNull()
        val primaryAidHex = primaryApp?.aid?.let { HexUtil.toHex(it) }
        val primaryLabel = primaryApp?.applicationLabel

        val scheme = detectScheme(emvCard, primaryAidHex, primaryLabel)

        val allAids = apps.map { app ->
            AidInfo(
                aid = app.aid?.let { HexUtil.toHex(it) } ?: "unknown",
                label = app.applicationLabel,
                priority = reflectPriority(app)
            )
        }

        val track1 = extractTrack1(emvCard)
        val track2 = extractTrack2(emvCard)

        val atrBytes = provider.getAt()
        val atr = when {
            atrBytes.isNotEmpty() -> HexUtil.toHexSpaced(atrBytes)
            !emvCard.at.isNullOrBlank() -> emvCard.at
            else -> null
        }
        val atrDesc = emvCard.atrDescription?.joinToString("; ")

        val transactions = extractAllTransactions(apps, logger)

        // CPLC data
        val cplcData = extractCplc(emvCard)

        // Contactless status
        val status = if (pan != null) ContactlessStatus.ACTIVE else detectContactlessStatus(logger)
        val statusDetail = when (status) {
            ContactlessStatus.DISABLED -> "NFC is locked on your card or card not activated"
            ContactlessStatus.BLOCKED -> "Contactless blocked by issuer"
            ContactlessStatus.NOT_PAYMENT_CARD -> "Not a payment card"
            ContactlessStatus.ACTIVE -> null
        }

        // Wallet/tokenization detection
        val walletInfo = detectWallet(apps, logger)

        return CardData(
            pan = pan, expiry = expiry, holderName = holder,
            scheme = scheme, aid = primaryAidHex, aidLabel = primaryLabel,
            track1 = track1, track2 = track2,
            atr = atr, atrDescription = atrDesc,
            applicationCount = apps.size, allAids = allAids,
            transactions = transactions,
            cplcData = cplcData,
            contactlessStatus = status,
            contactlessStatusDetail = statusDetail,
            walletType = walletInfo.first,
            isTokenized = walletInfo.second
        )
    }

    /** Detect contactless status from APDU log responses */
    private fun detectContactlessStatus(logger: ApduLogger): ContactlessStatus {
        for (entry in logger.entries) {
            if (entry.label == "SELECT" && entry.response.size >= 2) {
                val sw = ((entry.response[entry.response.size - 2].toInt() and 0xFF) shl 8) or
                        (entry.response[entry.response.size - 1].toInt() and 0xFF)
                return when (sw) {
                    0x6A82 -> ContactlessStatus.DISABLED
                    0x6985 -> ContactlessStatus.BLOCKED
                    else -> ContactlessStatus.ACTIVE
                }
            }
        }
        return ContactlessStatus.ACTIVE
    }

    private fun extractCplc(card: EmvCard): String? {
        return try {
            val cplc = card.cplc ?: return null
            val sb = StringBuilder()
            reflectField<Any>(cplc, "icFabricator")?.let { sb.appendLine("IC Fabricator     :  $it") }
            reflectField<Any>(cplc, "icType")?.let { sb.appendLine("IC Type           :  $it") }
            reflectField<Any>(cplc, "operatingSystemId")?.let { sb.appendLine("OS ID             :  $it") }
            reflectField<Any>(cplc, "operatingSystemDate")?.let { sb.appendLine("OS Release Date   :  $it") }
            reflectField<Any>(cplc, "icManufacturer")?.let { sb.appendLine("IC Manufacturer   :  $it") }
            reflectField<Any>(cplc, "icEmbedder")?.let { sb.appendLine("IC Embedder       :  $it") }
            reflectField<Any>(cplc, "icPrePersonalizer")?.let { sb.appendLine("IC Pre-Personal.  :  $it") }
            reflectField<Any>(cplc, "icPersonalizer")?.let { sb.appendLine("IC Personalizer   :  $it") }
            if (sb.isEmpty()) null else sb.toString().trimEnd()
        } catch (_: Exception) { null }
    }

    // ── Scheme detection ──

    private fun detectScheme(card: EmvCard, aidHex: String?, appLabel: String?): String? {
        val fromAid = aidHex?.let { schemeFromAid(it) }
        if (fromAid != null) return fromAid

        val cardType = card.type
        if (cardType != null) {
            try {
                val m = cardType.javaClass.getDeclaredMethod("getName")
                m.isAccessible = true
                val name = m.invoke(cardType) as? String
                if (!name.isNullOrBlank()) return name
            } catch (_: Exception) {}
            return cardType.name.replace("_", " ").lowercase()
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }

        if (!appLabel.isNullOrBlank()) {
            return when {
                appLabel.contains("visa", true) -> "Visa"
                appLabel.contains("master", true) -> "Mastercard"
                appLabel.contains("amex", true) -> "Amex"
                appLabel.contains("jcb", true) -> "JCB"
                appLabel.contains("union", true) -> "UnionPay"
                else -> appLabel
            }
        }

        val pan = card.cardNumber
        if (!pan.isNullOrBlank()) {
            return when (pan[0]) {
                '4' -> "Visa"; '5' -> "Mastercard"
                '3' -> if (pan.startsWith("34") || pan.startsWith("37")) "Amex" else "JCB"
                '6' -> "Discover/UnionPay"; else -> null
            }
        }
        return null
    }

    private fun schemeFromAid(aid: String): String? {
        val u = aid.uppercase()
        return when {
            u.startsWith("A000000004") -> "Mastercard"
            u.startsWith("A000000003") -> "Visa"
            u.startsWith("A000000025") -> "Amex"
            u.startsWith("A000000065") -> "JCB"
            u.startsWith("A000000333") -> "UnionPay"
            u.startsWith("A000000152") -> "Discover"
            u.startsWith("A000000615") -> "Mastercard"
            else -> null
        }
    }

    // ── Transactions ──

    private fun extractAllTransactions(apps: List<Application>, logger: ApduLogger): List<TransactionInfo> {
        val all = mutableListOf<TransactionInfo>()

        for (app in apps) {
            try {
                val txnList: List<*>? = try {
                    app.listTransactions
                } catch (e: Exception) {
                    log.warn("Direct listTransactions failed: {}", e.message)
                    null
                }

                val finalList: List<*>? = if (txnList.isNullOrEmpty()) {
                    try {
                        val m = app.javaClass.getMethod("getListTransactions")
                        @Suppress("UNCHECKED_CAST")
                        m.invoke(app) as? List<*>
                    } catch (e: Exception) {
                        log.warn("Reflection getListTransactions failed: {}", e.message)
                        null
                    }
                } else txnList

                if (finalList.isNullOrEmpty()) continue
                log.info("App {} has {} transaction records", app.applicationLabel, finalList.size)

                for (record in finalList) {
                    if (record == null) continue
                    try {
                        all.add(extractTransaction(record))
                    } catch (e: Exception) {
                        log.error("Failed to extract transaction: {}", e.message, e)
                    }
                }
            } catch (e: Exception) {
                log.error("Transaction extraction error: {}", e.message)
            }
        }

        // Enrich with times from APDU log (tag 9F21 = Transaction Time)
        val times = extractTimesFromApdu(logger)
        if (times.isNotEmpty()) {
            log.info("Extracted {} transaction times from APDU log", times.size)
            for (i in all.indices) {
                if (i < times.size) {
                    // Prefer APDU time over "00:00:00" from Date object
                    val existingTime = all[i].time
                    if (existingTime == null || existingTime == "00:00:00") {
                        all[i] = all[i].copy(time = times[i])
                    }
                }
            }
        }
        // Clean up: remove "00:00:00" times (no real time data)
        for (i in all.indices) {
            if (all[i].time == "00:00:00") {
                all[i] = all[i].copy(time = null)
            }
        }

        log.info("Total transactions: {}", all.size)
        return all
    }

    /**
     * Extract transaction times from APDU log using the proper EMV approach:
     * 1. Find tag 9F4F (Log Format) to learn the record structure
     * 2. Calculate where 9F21 (time) sits within each flat log record
     * 3. Read BCD time bytes at that offset from each transaction record
     *
     * EMV transaction log records are FLAT (no TLV tags inside) —
     * their structure is defined by the 9F4F template.
     */
    private fun extractTimesFromApdu(logger: ApduLogger): List<String> {
        // Step 1: Find the Log Format (tag 9F4F) in any APDU response
        var logFormat: List<Pair<String, Int>>? = null  // List of (tag, length)
        for (entry in logger.entries) {
            val hex = HexUtil.toHex(entry.response).uppercase()
            logFormat = parseLogFormat(hex)
            if (logFormat != null) break
        }

        if (logFormat == null) {
            log.info("No log format (9F4F) found in APDU data")
            return emptyList()
        }

        // Step 2: Calculate the byte offset of 9F21 within a log record
        var timeOffset = -1
        var totalRecordLen = 0
        for ((tag, len) in logFormat) {
            if (tag == "9F21") {
                timeOffset = totalRecordLen
            }
            totalRecordLen += len
        }

        if (timeOffset < 0) {
            log.info("Tag 9F21 (time) not in log format — card doesn't store transaction time")
            return emptyList()
        }

        log.info("Log format: record={} bytes, time at offset={}", totalRecordLen, timeOffset)

        // Step 3: Find transaction log READ RECORD responses and extract time
        // Transaction log records are the ones whose data length matches totalRecordLen
        val times = mutableListOf<String>()
        for (entry in logger.entries) {
            if (entry.label != "READ RECORD") continue
            val resp = entry.response
            if (resp.size < totalRecordLen + 2) continue // need at least record + SW

            // The response is: [70 LEN <record data>...] 90 00
            // Or just flat record data + 90 00
            val hex = HexUtil.toHex(resp).uppercase()

            // Try to find the record data — it may be wrapped in tag 70
            val recordHex: String? = if (hex.startsWith("70")) {
                // Parse tag 70 length to find the record data
                val lenStart = 2
                val lenByte = hex.substring(lenStart, lenStart + 2).toIntOrNull(16) ?: continue
                val dataStart = if (lenByte == 0x81) lenStart + 4
                    else if (lenByte == 0x82) lenStart + 6
                    else lenStart + 2
                if (dataStart + totalRecordLen * 2 <= hex.length - 4) {
                    hex.substring(dataStart, dataStart + totalRecordLen * 2)
                } else null
            } else null

            // If wrapped in 70, use that; otherwise try the raw response (minus SW)
            val data = recordHex ?: if (resp.size >= totalRecordLen + 2) {
                hex.substring(0, totalRecordLen * 2)
            } else continue

            if (data.length < (timeOffset + 3) * 2) continue

            // Read 3 BCD bytes at timeOffset
            val timeStart = timeOffset * 2
            val hh = data.substring(timeStart, timeStart + 2)
            val mm = data.substring(timeStart + 2, timeStart + 4)
            val ss = data.substring(timeStart + 4, timeStart + 6)

            try {
                val h = hh.toInt(); val m = mm.toInt(); val s = ss.toInt()
                if (h in 0..23 && m in 0..59 && s in 0..59) {
                    times.add("$hh:$mm:$ss")
                    log.debug("Extracted time: {}:{}:{}", hh, mm, ss)
                }
            } catch (_: Exception) {}
        }

        return times
    }

    /**
     * Parse the Log Format (tag 9F4F) from an APDU response hex string.
     * 9F4F contains a list of tag-length pairs defining the transaction log record structure.
     * Returns list of (tagHex, byteLength) or null if not found.
     */
    private fun parseLogFormat(hex: String): List<Pair<String, Int>>? {
        val pos = hex.indexOf("9F4F")
        if (pos < 0) return null

        val lenStart = pos + 4
        if (lenStart + 2 > hex.length) return null
        val formatLen = hex.substring(lenStart, lenStart + 2).toIntOrNull(16) ?: return null
        val dataStart = lenStart + 2
        val dataEnd = dataStart + formatLen * 2
        if (dataEnd > hex.length) return null

        val formatData = hex.substring(dataStart, dataEnd)
        val tags = mutableListOf<Pair<String, Int>>()
        var i = 0

        while (i < formatData.length - 3) {
            // Read tag (1 or 2 bytes)
            val firstByte = formatData.substring(i, i + 2).toIntOrNull(16) ?: break
            val tag: String
            if ((firstByte and 0x1F) == 0x1F) {
                // 2-byte tag
                if (i + 4 > formatData.length) break
                tag = formatData.substring(i, i + 4)
                i += 4
            } else {
                tag = formatData.substring(i, i + 2)
                i += 2
            }

            // Read length (1 byte)
            if (i + 2 > formatData.length) break
            val len = formatData.substring(i, i + 2).toIntOrNull(16) ?: break
            i += 2

            tags.add(Pair(tag, len))
        }

        return if (tags.isNotEmpty()) {
            log.info("Parsed log format 9F4F: {}", tags.map { "${it.first}(${it.second})" })
            tags
        } else null
    }

    /**
     * Extract fields from EmvTransactionRecord using reflection.
     *
     * From library test code (v3.1.0):
     *   record.getAmount()          → float (e.g. 4000 = EUR 40.00)
     *   record.getCyptogramData()   → String (e.g. "80") — NOTE TYPO in library
     *   record.getCurrency()        → CurrencyEnum
     *   record.getTerminalCountry() → CountryCodeEnum
     *   record.getTransactionType() → TransactionTypeEnum
     *   record.getDate()            → Date
     */
    private fun extractTransaction(record: Any): TransactionInfo {
        // Amount (float, in minor units → divide by 100)
        val rawAmount = reflectField<Any>(record, "amount")
        val amount = when (rawAmount) {
            is Float -> String.format("%.2f", rawAmount / 100f)
            is Double -> String.format("%.2f", rawAmount / 100.0)
            is Number -> String.format("%.2f", rawAmount.toDouble() / 100.0)
            else -> rawAmount?.toString()
        }

        // Currency (CurrencyEnum → toString gives e.g. "MYR")
        val currency = reflectField<Any>(record, "currency")?.toString()

        // Date
        val date = reflectField<Date>(record, "date")

        // Time — extract from the Date object (library may combine 9A+9F21)
        val timeStr = date?.let {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it)
        }
        // If time is midnight, it might mean "no time data" or actual midnight
        // We keep it and let the APDU enrichment override if needed

        // Cryptogram — NOTE: library returns String (like "80"), NOT byte[]
        val cryptogram = reflectField<String>(record, "cyptogramData")
            ?: reflectField<Any>(record, "cyptogramData")?.toString()
            ?: reflectField<Any>(record, "cryptogramData")?.toString()

        // Terminal country (CountryCodeEnum)
        val country = reflectField<Any>(record, "terminalCountry")?.toString()

        // Transaction type (TransactionTypeEnum)
        val txnType = reflectField<Any>(record, "transactionType")?.toString()

        // ATC counter
        val atc = reflectField<Any>(record, "transactionCounter")?.toString()

        return TransactionInfo(
            date = date,
            amount = amount,
            currency = currency,
            country = country,
            cryptogram = cryptogram,
            atc = atc,
            transactionType = txnType,
            time = timeStr,
            cryptogramType = null
        )
    }

    // ── Wallet/Token Detection ──

    /**
     * Detect if the card is a tokenized mobile wallet.
     * Returns Pair(walletName, isTokenized).
     *
     * Detection strategies:
     * 1. Application labels containing wallet names
     * 2. Known tokenized AID patterns
     * 3. APDU response patterns (tag 9F6E form factor indicator)
     */
    private fun detectWallet(apps: List<Application>, logger: ApduLogger): Pair<String?, Boolean> {
        // Strategy 1: Application labels containing wallet names
        for (app in apps) {
            val label = app.applicationLabel?.uppercase() ?: continue
            when {
                label.contains("APPLE") -> return Pair("Apple Pay", true)
                label.contains("GOOGLE") -> return Pair("Google Pay", true)
                label.contains("SAMSUNG") -> return Pair("Samsung Pay", true)
                label.contains("GARMIN") -> return Pair("Garmin Pay", true)
                label.contains("FITBIT") -> return Pair("Fitbit Pay", true)
                label.contains("HUAWEI") -> return Pair("Huawei Pay", true)
                label.contains("XIAOMI") || label.contains("MI PAY") -> return Pair("Xiaomi Pay", true)
                label.contains("WEARABLE") -> return Pair("Wearable Payment", true)
                label.contains("PAY") && !label.contains("PAYWAVE") && !label.contains("PAYPASS") ->
                    return Pair(app.applicationLabel ?: "Mobile Pay", true)
            }
        }

        // Note: Tag 9F6E (Form Factor Indicator) exists on ALL Visa cards including physical.
        // Only flag as tokenized when we're confident (label-based above).
        // Physical cards have 9F6E with consumer device = 00 (standard card).

        return Pair(null, false)
    }

    // ── Track data ──

    private fun extractTrack1(card: EmvCard): String? {
        return try {
            val t = card.track1 ?: return null
            val f = t.javaClass.getDeclaredField("raw"); f.isAccessible = true
            (f.get(t) as? ByteArray)?.let { HexUtil.toHexSpaced(it) }
        } catch (_: Exception) { null }
    }

    private fun extractTrack2(card: EmvCard): String? {
        return try {
            val t = card.track2 ?: return null
            val f = t.javaClass.getDeclaredField("raw"); f.isAccessible = true
            (f.get(t) as? ByteArray)?.let { HexUtil.toHexSpaced(it) }
        } catch (_: Exception) { try { card.track2?.toString() } catch (_: Exception) { null } }
    }

    // ── Helpers ──

    private fun buildHolderName(card: EmvCard): String? {
        val last = card.holderLastname; val first = card.holderFirstname
        return when {
            !last.isNullOrBlank() && !first.isNullOrBlank() -> "$last/$first"
            !last.isNullOrBlank() -> last; !first.isNullOrBlank() -> first
            else -> null
        }
    }

    private fun reflectPriority(app: Any): Int? = try {
        val f = app.javaClass.getDeclaredField("priority"); f.isAccessible = true
        (f.get(app) as? Number)?.toInt()
    } catch (_: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun <T> reflectField(obj: Any, name: String): T? = try {
        val f = obj.javaClass.getDeclaredField(name); f.isAccessible = true; f.get(obj) as? T
    } catch (_: Exception) { null }
}
