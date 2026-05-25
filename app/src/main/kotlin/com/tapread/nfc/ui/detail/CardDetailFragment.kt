package com.tapread.nfc.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.tapread.nfc.R
import com.tapread.nfc.databinding.FragmentCardDetailBinding
import com.tapread.nfc.model.CardData
import com.tapread.nfc.model.ContactlessStatus
import com.tapread.nfc.ui.CardsViewModel
import com.tapread.nfc.util.HapticUtil

class CardDetailFragment : Fragment() {

    private var _binding: FragmentCardDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CardsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.selectedScan.observe(viewLifecycleOwner) { scan ->
            scan?.let { renderCard(it.card, it.apduLog) }
        }
    }

    private fun renderCard(card: CardData, apduLog: List<com.tapread.nfc.model.ApduEntry>) {
        val maskEnabled = viewModel.maskPan

        // Card face
        binding.textCardPan.text = if (maskEnabled) card.maskedPan else card.formattedPan
        binding.textCardExpiry.text = card.formattedExpiry
        binding.textCardHolder.text = card.holderName ?: "NOT SUPPLIED"
        binding.textCardScheme.text = card.scheme ?: "Unknown"

        // Scheme gradient + brand logo
        val scheme = card.scheme?.lowercase() ?: ""
        when {
            scheme.contains("mastercard") -> {
                binding.cardFaceBackground.setBackgroundResource(R.drawable.bg_card_face_mc)
                binding.imgBrandLogo.setImageResource(R.drawable.ic_mastercard)
                binding.imgBrandLogo.visibility = View.VISIBLE
            }
            scheme.contains("visa") -> {
                binding.cardFaceBackground.setBackgroundResource(R.drawable.bg_card_face_visa)
                binding.imgBrandLogo.setImageResource(R.drawable.ic_visa)
                binding.imgBrandLogo.visibility = View.VISIBLE
            }
            else -> {
                binding.cardFaceBackground.setBackgroundResource(R.drawable.bg_card_face)
                binding.imgBrandLogo.visibility = View.GONE
            }
        }

        // Copy PAN button
        binding.btnCopyPan.visibility = if (card.pan != null) View.VISIBLE else View.GONE
        binding.btnCopyPan.setOnClickListener {
            HapticUtil.tick(it)
            copyToClipboard("Card Number", card.pan ?: "")
        }

        // BIN lookup button
        binding.btnBinLookup.visibility = if (card.pan != null && card.pan.length >= 6) View.VISIBLE else View.GONE
        binding.btnBinLookup.setOnClickListener {
            HapticUtil.tick(it)
            val bin = card.pan?.take(8) ?: card.pan?.take(6) ?: ""
            val url = "https://bincheck.io/details/$bin"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // Contactless warning
        if (card.contactlessStatus != ContactlessStatus.ACTIVE) {
            binding.textContactlessWarning.visibility = View.VISIBLE
            binding.textContactlessWarning.text = card.contactlessStatusDetail
                ?: "NFC is locked on your card or card not activated"
            binding.textContactlessWarning.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.status_warn)
            )
        } else {
            binding.textContactlessWarning.visibility = View.GONE
        }

        // Extended details
        val panDisplay = if (maskEnabled) card.maskedPan else card.formattedPan
        val sb = StringBuilder()

        sb.appendLine("Application number :  ${card.applicationCount}")
        sb.appendLine()

        sb.appendLine("─── Track 1 ──────────────────────")
        sb.appendLine("Expire date    :  ${card.formattedExpiry}")
        sb.appendLine("Card number    :  $panDisplay")
        sb.appendLine("Holder name    :  ${card.holderName ?: "NOT SUPPLIED"}")
        if (card.track1 != null) sb.appendLine("Track 1 raw    :  ${card.track1}")
        sb.appendLine()

        sb.appendLine("─── Track 2 ──────────────────────")
        sb.appendLine("Card number    :  $panDisplay")
        sb.appendLine("Expire date    :  ${card.formattedExpiry}")
        if (card.track2 != null) {
            sb.appendLine("Track 2 raw    :  ${card.track2}")
            // Parse service code from Track 2 (after 'D' separator + expiry)
            val t2clean = card.track2.replace(" ", "")
            val dIdx = t2clean.indexOf("D", ignoreCase = true)
            if (dIdx > 0 && dIdx + 8 <= t2clean.length) {
                val serviceCode = t2clean.substring(dIdx + 5, dIdx + 8)
                if (serviceCode.all { it.isDigit() }) {
                    sb.appendLine("Service code   :  $serviceCode")
                    sb.appendLine("Decoded        :")
                    val decoded = com.tapread.nfc.util.TlvParser.decodeServiceCode(serviceCode)
                    for (line in decoded.split("\n")) {
                        sb.appendLine("  $line")
                    }
                }
            }
        }
        sb.appendLine()

        sb.appendLine("─── AIDs ─────────────────────────")
        if (card.allAids.isNotEmpty()) {
            for (aid in card.allAids) {
                val label = aid.label ?: "Unknown"
                val priority = aid.priority?.let { "Priority %02d".format(it) } ?: ""
                sb.appendLine("${aid.aid}  $label  $priority")
            }
        } else sb.appendLine("No AIDs found")
        sb.appendLine()

        // CVM List — extract tag 8E from APDU log
        val cvmHex = extractTagFromApdu(apduLog, "8E")
        if (cvmHex != null) {
            sb.appendLine("─── CVM (Verification Methods) ───")
            sb.appendLine(com.tapread.nfc.util.TlvParser.decodeCvmList(cvmHex))
            sb.appendLine()
        }

        sb.appendLine("─── Chip (ATR/ATS) ───────────────")
        sb.appendLine("ATS bytes  :  ${card.atr ?: "N/A"}")
        if (!card.atrDescription.isNullOrBlank()) {
            sb.appendLine("Card issuer  :  ${card.atrDescription}")
            sb.appendLine("(possible)")
        }

        if (!card.cplcData.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("─── CPLC (Card Production) ───────")
            sb.append(card.cplcData)
        }

        sb.appendLine()
        sb.appendLine()
        sb.appendLine("─── Wallet Detection ─────────────")
        if (card.isTokenized) {
            sb.appendLine("Type     :  📱 ${card.walletType ?: "Tokenized"}")
            sb.appendLine("Card     :  DPAN (Device PAN)")
        } else {
            sb.appendLine("Type     :  Physical Card")
        }

        sb.appendLine()
        sb.appendLine("─── NFC Status ───────────────────")
        when (card.contactlessStatus) {
            ContactlessStatus.ACTIVE -> sb.appendLine("Status  :  ✅ Contactless Active")
            ContactlessStatus.DISABLED -> sb.appendLine("Status  :  ⚠️ NFC Locked / Not Activated")
            ContactlessStatus.BLOCKED -> sb.appendLine("Status  :  ⚠️ Blocked by Issuer")
            ContactlessStatus.NOT_PAYMENT_CARD -> sb.appendLine("Status  :  ℹ️ Not a Payment Card")
        }

        binding.textExtendedDetails.text = sb.toString()

        // Copy extended details button
        binding.btnCopyDetails.setOnClickListener {
            HapticUtil.tick(it)
            copyToClipboard("Card Details", sb.toString())
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Snackbar.make(requireView(), "$label copied to clipboard", Snackbar.LENGTH_SHORT).show()
    }

    /** Extract a specific EMV tag value from the APDU log responses */
    private fun extractTagFromApdu(
        apduLog: List<com.tapread.nfc.model.ApduEntry>,
        tagHex: String
    ): String? {
        val tag = tagHex.uppercase()
        for (entry in apduLog) {
            if (entry.response.size < 6) continue
            val hex = com.tapread.nfc.util.HexUtil.toHex(entry.response).uppercase()
            val pos = hex.indexOf(tag)
            if (pos < 0) continue
            // Read length after tag
            val lenStart = pos + tag.length
            if (lenStart + 2 > hex.length) continue
            val lenByte = hex.substring(lenStart, lenStart + 2).toIntOrNull(16) ?: continue
            val valStart = lenStart + 2
            val valEnd = valStart + lenByte * 2
            if (valEnd <= hex.length) {
                return hex.substring(valStart, valEnd)
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
