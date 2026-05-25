package com.tapread.nfc.ui.detail

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView
import com.tapread.nfc.R
import com.tapread.nfc.databinding.FragmentTransactionsBinding
import com.tapread.nfc.model.TransactionInfo
import com.tapread.nfc.ui.CardsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CardsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectedScan.observe(viewLifecycleOwner) { scan ->
            val txns = scan?.card?.transactions ?: emptyList()
            if (txns.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.transactionsContainer.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.transactionsContainer.visibility = View.VISIBLE
                renderTransactions(txns)
            }
        }
    }

    private fun renderTransactions(transactions: List<TransactionInfo>) {
        binding.transactionsContainer.removeAllViews()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dp8 = dpToPx(8)
        val dp12 = dpToPx(12)
        val dp16 = dpToPx(16)

        for (txn in transactions) {
            val card = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp8) }
                radius = dp12.toFloat()
                cardElevation = dpToPx(2).toFloat()
                setContentPadding(dp16, dp12, dp16, dp12)
            }

            val content = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Header row: chevron + date + amount (always visible)
            val headerRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val chevron = ImageView(requireContext()).apply {
                setImageResource(android.R.drawable.arrow_down_float)
                layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)).apply {
                    setMargins(0, 0, dp8, 0)
                }
            }

            val dateText = TextView(requireContext()).apply {
                val date = txn.date?.let { dateFormat.format(it) } ?: "Unknown"
                text = date
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            }

            val amountText = TextView(requireContext()).apply {
                val currency = txn.currency ?: ""
                val amount = txn.amount ?: "?"
                text = "$currency $amount $currency"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            }

            headerRow.addView(chevron)
            headerRow.addView(dateText)
            headerRow.addView(amountText)
            content.addView(headerRow)

            // Expandable detail section (hidden by default)
            val detailSection = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(dpToPx(32), dp8, 0, 0)
            }

            // Transaction type
            txn.transactionType?.let {
                addDetailRow(detailSection, "Transaction type :", it)
            }

            // Terminal country
            txn.country?.let {
                addDetailRow(detailSection, "Terminal Country :", it)
            }

            // Transaction time
            txn.time?.let {
                addDetailRow(detailSection, "Transaction Time :", it)
            }

            // Cryptogram
            txn.cryptogram?.let {
                val cryptoText = TextView(requireContext()).apply {
                    text = "Cryptogram (9F26) : $it"
                    typeface = android.graphics.Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(0, dpToPx(4), 0, 0)
                }
                detailSection.addView(cryptoText)
            }

            // ATC
            txn.atc?.let {
                val atcText = TextView(requireContext()).apply {
                    text = "ATC (9F36)        : $it"
                    typeface = android.graphics.Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                }
                detailSection.addView(atcText)
            }

            content.addView(detailSection)

            // Toggle expand/collapse on click
            card.setOnClickListener {
                com.tapread.nfc.util.HapticUtil.tick(it)
                if (detailSection.visibility == View.GONE) {
                    detailSection.visibility = View.VISIBLE
                    chevron.setImageResource(android.R.drawable.arrow_up_float)
                } else {
                    detailSection.visibility = View.GONE
                    chevron.setImageResource(android.R.drawable.arrow_down_float)
                }
            }

            card.addView(content)
            binding.transactionsContainer.addView(card)
        }
    }

    private fun addDetailRow(parent: LinearLayout, label: String, value: String) {
        val row = TextView(requireContext()).apply {
            text = "$label  $value"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        parent.addView(row)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
