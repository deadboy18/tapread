package com.tapread.nfc.util

/**
 * BER-TLV parser for EMV APDU responses.
 * Parses raw bytes into a tree of tag-length-value nodes.
 */
object TlvParser {

    data class TlvNode(
        val tag: String,
        val tagName: String,
        val length: Int,
        val value: String,
        val children: List<TlvNode> = emptyList(),
        val depth: Int = 0
    )

    /** Parse a hex response string into TLV nodes, stripping SW (last 2 bytes) */
    fun parse(hexResponse: String): List<TlvNode> {
        val clean = hexResponse.replace(" ", "").uppercase()
        // Strip status word (last 4 hex chars = 2 bytes) if present
        val data = if (clean.length >= 4 && clean.takeLast(4).matches(Regex("[0-9A-F]{4}"))) {
            clean.dropLast(4)
        } else clean
        return parseNodes(data, 0)
    }

    private fun parseNodes(hex: String, depth: Int): List<TlvNode> {
        val nodes = mutableListOf<TlvNode>()
        var pos = 0
        while (pos < hex.length - 3) {
            try {
                // Read tag (1 or 2 bytes)
                val tagByte = hex.substring(pos, pos + 2).toInt(16)
                val tag: String
                if ((tagByte and 0x1F) == 0x1F) {
                    // Multi-byte tag
                    if (pos + 4 > hex.length) break
                    tag = hex.substring(pos, pos + 4)
                    pos += 4
                } else {
                    tag = hex.substring(pos, pos + 2)
                    pos += 2
                }

                if (pos + 2 > hex.length) break

                // Read length
                val lenByte = hex.substring(pos, pos + 2).toInt(16)
                val length: Int
                if (lenByte == 0x81) {
                    pos += 2
                    if (pos + 2 > hex.length) break
                    length = hex.substring(pos, pos + 2).toInt(16)
                    pos += 2
                } else if (lenByte == 0x82) {
                    pos += 2
                    if (pos + 4 > hex.length) break
                    length = hex.substring(pos, pos + 4).toInt(16)
                    pos += 4
                } else {
                    length = lenByte
                    pos += 2
                }

                val valueEnd = pos + length * 2
                if (valueEnd > hex.length) break
                val value = hex.substring(pos, valueEnd)

                val tagName = EMV_TAGS[tag] ?: "Unknown"
                val isConstructed = (tag.substring(0, 2).toInt(16) and 0x20) != 0

                val children = if (isConstructed && value.length >= 4) {
                    try { parseNodes(value, depth + 1) } catch (_: Exception) { emptyList() }
                } else emptyList()

                nodes.add(TlvNode(tag, tagName, length, value, children, depth))
                pos = valueEnd
            } catch (_: Exception) { break }
        }
        return nodes
    }

    /** Format TLV tree as indented text */
    fun formatTree(nodes: List<TlvNode>, indent: Int = 0): String {
        val sb = StringBuilder()
        for (node in nodes) {
            val prefix = "    ".repeat(indent)
            val valuePreview = if (node.value.length > 40)
                node.value.take(40) + "..." else node.value
            val ascii = tryAscii(node.value)
            val asciiStr = if (ascii != null) " (=$ascii)" else ""

            sb.appendLine("$prefix${node.tag} ${"%02X".format(node.length)} -- ${node.tagName}")
            if (node.children.isEmpty()) {
                sb.appendLine("$prefix    ${formatHexSpaced(valuePreview)}$asciiStr")
            } else {
                sb.append(formatTree(node.children, indent + 1))
            }
        }
        return sb.toString()
    }

    private fun formatHexSpaced(hex: String): String {
        return hex.chunked(2).joinToString(" ")
    }

    private fun tryAscii(hex: String): String? {
        if (hex.length < 2 || hex.length > 64) return null
        val bytes = try {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (_: Exception) { return null }
        val str = String(bytes, Charsets.US_ASCII)
        return if (str.all { it in ' '..'~' }) str else null
    }

    /** Decode service code (3 digits from Track 2) */
    fun decodeServiceCode(code: String): String {
        if (code.length < 3) return code
        val d1 = when (code[0]) {
            '1' -> "International interchange"
            '2' -> "International + IC chip"
            '5' -> "National interchange"
            '6' -> "National + IC chip"
            '7' -> "Private use"
            else -> "Position 1: ${code[0]}"
        }
        val d2 = when (code[1]) {
            '0' -> "Normal"
            '2' -> "Issuer to be contacted via online"
            '4' -> "Issuer to be contacted via online (unless explicit bilateral)"
            else -> "Position 2: ${code[1]}"
        }
        val d3 = when (code[2]) {
            '0' -> "No restrictions, PIN required"
            '1' -> "No restrictions"
            '2' -> "Goods and services only"
            '3' -> "ATM only, PIN required"
            '4' -> "Cash only"
            '5' -> "Goods and services only, PIN required"
            '6' -> "No restrictions, PIN prompting"
            '7' -> "Goods and services only, PIN prompting"
            else -> "Position 3: ${code[2]}"
        }
        return "$d1\n$d2\n$d3"
    }

    /** Decode CVM (Cardholder Verification Method) list */
    fun decodeCvmList(hex: String): String {
        if (hex.length < 8) return "Raw: $hex"
        val sb = StringBuilder()
        // First 8 chars = amount X, next 8 = amount Y
        var pos = 16 // skip the two amount fields
        var ruleNum = 1
        while (pos + 4 <= hex.length) {
            val cvmCode = hex.substring(pos, pos + 2).toInt(16)
            val condCode = hex.substring(pos + 2, pos + 4).toInt(16)
            val method = when (cvmCode and 0x3F) {
                0x00 -> "Fail CVM processing"
                0x01 -> "Plaintext PIN by ICC"
                0x02 -> "Enciphered PIN online"
                0x03 -> "Plaintext PIN by ICC + signature"
                0x04 -> "Enciphered PIN by ICC"
                0x05 -> "Enciphered PIN by ICC + signature"
                0x1E -> "Signature"
                0x1F -> "No CVM required"
                0x20 -> "CDCVM (on-device CVM)"
                else -> "Method 0x${"%02X".format(cvmCode and 0x3F)}"
            }
            val condition = when (condCode) {
                0x00 -> "Always"
                0x01 -> "If unattended cash"
                0x02 -> "If not unattended cash or manual"
                0x03 -> "If terminal supports CVM"
                0x06 -> "If amount under X"
                0x07 -> "If amount over X"
                0x08 -> "If amount under Y"
                0x09 -> "If amount over Y"
                else -> "Condition 0x${"%02X".format(condCode)}"
            }
            val failAction = if (cvmCode and 0x40 != 0) " [apply next if fail]" else " [fail if unsuccessful]"
            sb.appendLine("  $ruleNum. $method — $condition$failAction")
            pos += 4
            ruleNum++
        }
        return sb.toString().trimEnd()
    }

    // EMV tag dictionary
    private val EMV_TAGS = mapOf(
        "6F" to "FCI Template",
        "84" to "Dedicated File (DF) Name",
        "A5" to "FCI Proprietary Template",
        "BF0C" to "FCI Issuer Discretionary Data",
        "61" to "Application Template",
        "4F" to "Application Identifier (AID)",
        "50" to "Application Label",
        "87" to "Application Priority Indicator",
        "9F12" to "Application Preferred Name",
        "9F11" to "Issuer Code Table Index",
        "9F38" to "PDOL (Processing Data Object List)",
        "70" to "EMV Record Template",
        "77" to "Response Template Format 2",
        "80" to "Response Template Format 1",
        "82" to "Application Interchange Profile",
        "94" to "Application File Locator",
        "57" to "Track 2 Equivalent Data",
        "5A" to "Application PAN",
        "5F20" to "Cardholder Name",
        "5F24" to "Application Expiration Date",
        "5F25" to "Application Effective Date",
        "5F28" to "Issuer Country Code",
        "5F2A" to "Transaction Currency Code",
        "5F2D" to "Language Preference",
        "5F34" to "PAN Sequence Number",
        "8C" to "CDOL1 (Card Risk Mgmt DOL 1)",
        "8D" to "CDOL2 (Card Risk Mgmt DOL 2)",
        "8E" to "CVM List",
        "9F02" to "Amount Authorized",
        "9F03" to "Amount Other",
        "9F07" to "Application Usage Control",
        "9F08" to "App Version Number",
        "9F09" to "Terminal App Version",
        "9F0D" to "Issuer Action Code - Default",
        "9F0E" to "Issuer Action Code - Denial",
        "9F0F" to "Issuer Action Code - Online",
        "9F10" to "Issuer Application Data",
        "9F1A" to "Terminal Country Code",
        "9F1F" to "Track 1 Discretionary Data",
        "9F21" to "Transaction Time",
        "9F26" to "Application Cryptogram",
        "9F27" to "Cryptogram Information Data",
        "9F33" to "Terminal Capabilities",
        "9F34" to "CVM Results",
        "9F35" to "Terminal Type",
        "9F36" to "Application Transaction Counter",
        "9F37" to "Unpredictable Number",
        "9F42" to "Application Currency Code",
        "9F44" to "Application Currency Exponent",
        "9F45" to "Data Authentication Code",
        "9F46" to "ICC Public Key Certificate",
        "9F47" to "ICC Public Key Exponent",
        "9F48" to "ICC Public Key Remainder",
        "9F49" to "DDOL",
        "9F4A" to "Static Data Authentication Tag List",
        "9F4D" to "Log Entry",
        "9F4F" to "Log Format",
        "9F6C" to "Card Transaction Qualifiers",
        "9F6E" to "Form Factor Indicator",
        "9F7C" to "Customer Exclusive Data",
        "9A" to "Transaction Date",
        "9C" to "Transaction Type",
        "56" to "Track 1 Data"
    )
}
