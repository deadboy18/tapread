package com.tapread.nfc.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.tapread.nfc.R
import com.tapread.nfc.model.TngData
import com.tapread.nfc.model.TngTransaction
import com.tapread.nfc.ui.CardsViewModel

class TngDetailFragment : Fragment() {

    private val viewModel: CardsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val scroll = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        scroll.addView(root)

        viewModel.selectedScan.observe(viewLifecycleOwner) { scan ->
            scan?.tng?.let { tng -> renderTng(root, tng) }
        }

        return scroll
    }

    private fun renderTng(root: LinearLayout, tng: TngData) {
        root.removeAllViews()

        // ── Balance card ──
        val balCard = MaterialCardView(requireContext()).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(6).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }

        val balLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.tng_blue))
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        balLayout.addView(text("TOUCH 'N GO", 14, "#90FFFFFF", bold = true))
        balLayout.addView(text(tng.balanceRm, 42, "#FFFFFF", bold = true).apply {
            setPadding(0, dp(8), 0, dp(8))
        })
        balLayout.addView(text(tng.serialStr, 14, "#B0FFFFFF", mono = true))
        if (tng.cardNumber != null) {
            balLayout.addView(text(formatCardNo(tng.cardNumber!!), 12, "#90FFFFFF", mono = true).apply {
                setPadding(0, dp(4), 0, 0)
            })
        }

        val expiryRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        expiryRow.addView(labelValue("VALID THRU", tng.expiry ?: "—"))
        expiryRow.addView(labelValue("UID", tng.uid).apply {
            setPadding(dp(32), 0, 0, 0)
        })
        balLayout.addView(expiryRow)

        balCard.addView(balLayout)
        root.addView(balCard)

        // ── Info section ──
        root.addView(sectionTitle("Card Information"))
        root.addView(infoRow("Serial Number", tng.serialStr))
        root.addView(infoRow("Card Number", tng.cardNumber ?: "—"))
        root.addView(infoRow("UID", tng.uid))
        root.addView(infoRow("Card Type", tng.cardType ?: "—"))
        root.addView(infoRow("Expiry", tng.expiry ?: "—"))
        root.addView(infoRow("Issue Date", tng.issueDate ?: "—"))
        root.addView(infoRow("TNG Fingerprint", if (tng.isTng == true) "✓ Verified" else "—"))
        root.addView(infoRow("Txn Count", tng.txnCount?.toString() ?: "—"))
        root.addView(infoRow("Sectors Read", "${tng.sectorsRead}/16"))

        if (tng.balanceSen != null) {
            root.addView(infoRow("Balance (sen)", tng.balanceSen.toString()))
        }

        // ── Transactions ──
        if (tng.transactions.isNotEmpty()) {
            root.addView(sectionTitle("Transactions"))
            for (txn in tng.transactions) {
                root.addView(transactionCard(txn))
            }
        }

        // ── Raw dump ──
        if (!tng.rawDump.isNullOrBlank()) {
            root.addView(sectionTitle("Raw Sector Data"))
            val rawText = text(tng.rawDump!!, 11, null, mono = true).apply {
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_surface))
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            rawText.setOnLongClickListener {
                copyToClipboard("TNG Raw Data", tng.rawDump!!)
                true
            }
            root.addView(rawText)
        }
    }

    // ── UI helpers ──────────────────────────────────────────────────────

    private fun text(t: String, sp: Int, color: String? = null, bold: Boolean = false, mono: Boolean = false): TextView {
        return TextView(requireContext()).apply {
            text = t
            textSize = sp.toFloat()
            if (color != null) setTextColor(android.graphics.Color.parseColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            if (mono) typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    private fun sectionTitle(title: String): TextView {
        return TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.tng_blue))
            setPadding(0, dp(20), 0, dp(8))
        }
    }

    private fun infoRow(label: String, value: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))

            addView(TextView(requireContext()).apply {
                text = label
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface_medium))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(requireContext()).apply {
                text = value
                textSize = 14f
                typeface = android.graphics.Typeface.MONOSPACE
                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnLongClickListener { copyToClipboard(label, value); true }
            })
        }
    }

    private fun labelValue(label: String, value: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(requireContext()).apply {
                text = label; textSize = 8f
                setTextColor(android.graphics.Color.parseColor("#90FFFFFF"))
            })
            addView(TextView(requireContext()).apply {
                text = value; textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                typeface = android.graphics.Typeface.MONOSPACE
            })
        }
    }

    private fun transactionCard(txn: TngTransaction): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        topRow.addView(TextView(requireContext()).apply {
            text = txn.label; textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        topRow.addView(TextView(requireContext()).apply {
            text = txn.amountRm; textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(),
                if (txn.label == "Reload") R.color.tng_green else R.color.md_on_surface))
        })
        layout.addView(topRow)
        if (txn.timestamp != null) {
            layout.addView(TextView(requireContext()).apply {
                text = txn.timestamp; textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface_medium))
                setPadding(0, dp(4), 0, 0)
            })
        }
        layout.addView(TextView(requireContext()).apply {
            text = "Balance after: ${txn.balanceAfterRm}  |  Agency: ${txn.agency}"
            textSize = 11f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface_medium))
            setPadding(0, dp(2), 0, 0)
        })
        card.addView(layout)
        return card
    }

    private fun formatCardNo(cn: String): String = cn.chunked(4).joinToString("  ")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun copyToClipboard(label: String, text: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Snackbar.make(requireView(), "$label copied", Snackbar.LENGTH_SHORT).show()
    }
}
