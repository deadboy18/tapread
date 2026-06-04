package com.tapread.nfc.ui.detail

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class DetailPagerAdapter(
    fragment: Fragment,
    private val isTng: Boolean = false
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = if (isTng) 2 else 3

    override fun createFragment(position: Int): Fragment {
        return if (isTng) {
            when (position) {
                0 -> TngDetailFragment()
                1 -> LogFragment() // shows raw dump
                else -> throw IllegalArgumentException("Invalid tab: $position")
            }
        } else {
            when (position) {
                0 -> CardDetailFragment()
                1 -> TransactionsFragment()
                2 -> LogFragment()
                else -> throw IllegalArgumentException("Invalid tab: $position")
            }
        }
    }
}
