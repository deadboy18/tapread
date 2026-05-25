package com.tapread.nfc.ui.home

import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tapread.nfc.R
import com.tapread.nfc.databinding.FragmentHomeBinding
import com.tapread.nfc.model.ScanResult
import com.tapread.nfc.ui.CardsViewModel
import com.tapread.nfc.ui.detail.DetailFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CardsViewModel by activityViewModels()
    private lateinit var adapter: CardListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CardListAdapter { scan -> openDetail(scan) }
        binding.recyclerCards.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCards.adapter = adapter

        viewModel.scans.observe(viewLifecycleOwner) { scans ->
            adapter.submitList(scans)
            binding.emptyState.visibility = if (scans.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerCards.visibility = if (scans.isEmpty()) View.GONE else View.VISIBLE
        }

        updateNfcStatus()
    }

    override fun onResume() {
        super.onResume()
        updateNfcStatus()
    }

    private fun updateNfcStatus() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext())
        when {
            nfcAdapter == null -> {
                binding.textNfcStatus.visibility = View.VISIBLE
                binding.textNfcStatus.text = "⚠ This device does not have NFC"
            }
            !nfcAdapter.isEnabled -> {
                binding.textNfcStatus.visibility = View.VISIBLE
                binding.textNfcStatus.text = "⚠ NFC is disabled — enable it in Settings"
            }
            else -> {
                binding.textNfcStatus.visibility = View.GONE
            }
        }
    }

    private fun openDetail(scan: ScanResult) {
        viewModel.selectScan(scan)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DetailFragment(), "detail")
            .addToBackStack("detail")
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
