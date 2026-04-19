package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentDealHistoryBinding
import com.google.android.material.tabs.TabLayoutMediator

class DealHistoryFragment : Fragment() {

    private var _binding: FragmentDealHistoryBinding? = null
    private val binding get() = _binding!!

    val viewModel: DealHistoryViewModel by viewModels()

    private var mediator: TabLayoutMediator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDealHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tbHistory.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.viewPagerHistory.adapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> CompletedDealsTabFragment()
                else -> PastDealsTabFragment()
            }
        }

        val labels = listOf(
            getString(R.string.tab_completed_deals),
            getString(R.string.tab_past_deals),
        )

        mediator = TabLayoutMediator(binding.tabLayoutHistory, binding.viewPagerHistory) { tab, position ->
            tab.text = labels[position]
        }
        mediator!!.attach()
    }

    override fun onDestroyView() {
        mediator?.detach()
        mediator = null
        super.onDestroyView()
        _binding = null
    }
}
