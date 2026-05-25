package com.tapread.nfc.ui.detail

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.tapread.nfc.R
import com.tapread.nfc.databinding.FragmentLogBinding
import com.tapread.nfc.model.ApduEntry
import com.tapread.nfc.ui.CardsViewModel
import com.tapread.nfc.util.HexUtil
import com.tapread.nfc.util.TlvParser

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CardsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectedScan.observe(viewLifecycleOwner) { scan ->
            val log = scan?.apduLog ?: emptyList()
            if (log.isEmpty()) {
                binding.textLog.text = "No APDU log available."
            } else {
                binding.textLog.text = buildColoredLog(log)
            }
        }

        binding.fabShare.setOnClickListener {
            com.tapread.nfc.util.HapticUtil.tick(it)
            shareLog()
        }
    }

    private fun buildColoredLog(entries: List<ApduEntry>): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        val cmdColor = ContextCompat.getColor(requireContext(), R.color.apdu_cmd)
        val respColor = ContextCompat.getColor(requireContext(), R.color.apdu_resp)
        val parseColor = ContextCompat.getColor(requireContext(), R.color.apdu_parse)

        for ((index, entry) in entries.withIndex()) {
            if (index > 0) ssb.append("\n\n")

            // Label
            if (entry.label.isNotBlank()) {
                ssb.append("── ${entry.label} ──\n")
            }

            // Command (green) with > prefix
            val cmdHex = "> ${HexUtil.toHexSpaced(entry.command)}\n"
            val cmdStart = ssb.length
            ssb.append(cmdHex)
            ssb.setSpan(ForegroundColorSpan(cmdColor), cmdStart, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Response (blue) with < prefix
            val respHex = "< ${HexUtil.toHexSpaced(entry.response)}\n"
            val respStart = ssb.length
            ssb.append(respHex)
            ssb.setSpan(ForegroundColorSpan(respColor), respStart, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Status word description
            if (entry.response.size >= 2) {
                val sw1 = entry.response[entry.response.size - 2].toInt() and 0xFF
                val sw2 = entry.response[entry.response.size - 1].toInt() and 0xFF
                val swDesc = describeStatusWord(sw1, sw2)
                val swStart = ssb.length
                ssb.append("  $swDesc\n")
                ssb.setSpan(ForegroundColorSpan(parseColor), swStart, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // TLV tree (grey, indented)
            if (entry.response.size > 4) {
                try {
                    val respHexStr = HexUtil.toHex(entry.response)
                    val nodes = TlvParser.parse(respHexStr)
                    if (nodes.isNotEmpty()) {
                        val tree = TlvParser.formatTree(nodes)
                        val treeStart = ssb.length
                        ssb.append(tree)
                        ssb.setSpan(ForegroundColorSpan(parseColor), treeStart, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                } catch (_: Exception) { /* skip TLV parse errors */ }
            }
        }

        return ssb
    }

    private fun describeStatusWord(sw1: Int, sw2: Int): String {
        val sw = (sw1 shl 8) or sw2
        return when {
            sw == 0x9000 -> "90 00 -- Command OK"
            sw1 == 0x61 -> "61 ${"%02X".format(sw2)} -- ${sw2} bytes available"
            sw == 0x6A82 -> "6A 82 -- File/application not found"
            sw == 0x6A83 -> "6A 83 -- Record not found"
            sw == 0x6985 -> "69 85 -- Conditions of use not satisfied"
            sw == 0x6984 -> "69 84 -- Referenced data invalidated"
            sw == 0x6A81 -> "6A 81 -- Function not supported"
            sw == 0x6A88 -> "6A 88 -- Referenced data not found"
            sw1 == 0x6C -> "6C ${"%02X".format(sw2)} -- Wrong Le, should be ${sw2}"
            sw == 0x6700 -> "67 00 -- Wrong length"
            sw == 0x6982 -> "69 82 -- Security status not satisfied"
            else -> "${"%02X".format(sw1)} ${"%02X".format(sw2)}"
        }
    }

    private fun shareLog() {
        val scan = viewModel.selectedScan.value ?: return
        val text = buildString {
            appendLine("TapRead APDU Log")
            appendLine("Card: ${scan.displayLabel}")
            appendLine("═".repeat(50))
            appendLine()
            for (entry in scan.apduLog) {
                if (entry.label.isNotBlank()) appendLine("── ${entry.label} ──")
                appendLine("> ${HexUtil.toHexSpaced(entry.command)}")
                appendLine("< ${HexUtil.toHexSpaced(entry.response)}")
                // Include TLV tree in export
                try {
                    val nodes = TlvParser.parse(HexUtil.toHex(entry.response))
                    if (nodes.isNotEmpty()) appendLine(TlvParser.formatTree(nodes))
                } catch (_: Exception) {}
                appendLine()
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "TapRead APDU Log — ${scan.displayLabel}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share APDU Log"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
