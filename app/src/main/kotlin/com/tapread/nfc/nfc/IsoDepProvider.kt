package com.tapread.nfc.nfc

import android.nfc.tech.IsoDep
import com.github.devnied.emvnfccard.parser.IProvider
import com.tapread.nfc.util.HexUtil
import org.slf4j.LoggerFactory

/**
 * Bridges Android's [IsoDep] to the devnied EMV library's [IProvider] interface.
 * Every transceive call is logged via [ApduLogger].
 */
class IsoDepProvider(
    private val logger: ApduLogger
) : IProvider {

    private val log = LoggerFactory.getLogger("IsoDepProvider")
    private var isoDep: IsoDep? = null

    fun setTagCom(tag: IsoDep) {
        this.isoDep = tag
    }

    override fun transceive(command: ByteArray): ByteArray {
        val tag = isoDep ?: throw IllegalStateException("IsoDep not connected")

        val label = guessCommandLabel(command)
        logger.logCommand(command, label)

        return try {
            val response = tag.transceive(command)
            logger.logResponse(command, response, label)
            response
        } catch (e: Exception) {
            log.error("Transceive failed: {}", e.message)
            logger.logResponse(command, byteArrayOf(), "ERROR: ${e.message}")
            throw e
        }
    }

    override fun getAt(): ByteArray {
        return isoDep?.historicalBytes ?: isoDep?.hiLayerResponse ?: byteArrayOf()
    }

    /**
     * Guess a human-readable label for common EMV APDU commands.
     */
    private fun guessCommandLabel(cmd: ByteArray): String {
        if (cmd.size < 4) return "UNKNOWN"
        val ins = cmd[1].toInt() and 0xFF
        return when (ins) {
            0xA4 -> "SELECT"
            0xB2 -> "READ RECORD"
            0xCA -> "GET DATA"
            0xA8 -> "GET PROCESSING OPTIONS"
            0x88 -> "INTERNAL AUTHENTICATE"
            0x82 -> "EXTERNAL AUTHENTICATE"
            0x84 -> "GET CHALLENGE"
            else -> "INS ${HexUtil.toHex(byteArrayOf(cmd[1]))}"
        }
    }
}
