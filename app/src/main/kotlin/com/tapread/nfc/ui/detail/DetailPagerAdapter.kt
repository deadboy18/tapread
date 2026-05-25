package com.tapread.nfc.ui.detail

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class DetailPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CardDetailFragment()
            1 -> TransactionsFragment()
            2 -> LogFragment()
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }
}
