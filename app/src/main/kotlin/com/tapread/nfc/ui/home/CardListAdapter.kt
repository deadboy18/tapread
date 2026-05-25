package com.tapread.nfc.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tapread.nfc.R
import com.tapread.nfc.databinding.ItemCardBinding
import com.tapread.nfc.model.ScanResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CardListAdapter(
    private val onClick: (ScanResult) -> Unit
) : ListAdapter<ScanResult, CardListAdapter.CardViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CardViewHolder(
        private val binding: ItemCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: ScanResult) {
            val card = scan.card
            binding.textScheme.text = card.scheme ?: "Unknown"
            binding.textPan.text = card.maskedPan
            binding.textExpiry.text = card.formattedExpiry
            binding.textHolder.text = card.holderName ?: "NOT SUPPLIED"

            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(scan.timestampMs))
            binding.textTimestamp.text = time

            // Card face gradient by scheme
            val gradientRes = when {
                card.scheme == null -> R.drawable.bg_card_face
                card.scheme.contains("mastercard", ignoreCase = true) -> R.drawable.bg_card_face_mc
                card.scheme.contains("visa", ignoreCase = true) -> R.drawable.bg_card_face_visa
                else -> R.drawable.bg_card_face
            }
            binding.cardFace.setBackgroundResource(gradientRes)

            binding.root.setOnClickListener {
                com.tapread.nfc.util.HapticUtil.tick(it)
                onClick(scan)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ScanResult>() {
            override fun areItemsTheSame(old: ScanResult, new: ScanResult) = old.id == new.id
            override fun areContentsTheSame(old: ScanResult, new: ScanResult) = old == new
        }
    }
}
