package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.aura.app.R
import com.aura.app.databinding.FragmentDealTabBinding

class CompletedDealsTabFragment : Fragment() {

    private var _binding: FragmentDealTabBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: DealHistoryViewModel by viewModels({ requireParentFragment() })
    private val reviewViewModel: ReviewViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: DealOfferAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDealTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmpty.setText(R.string.empty_completed_deals)

        adapter = DealOfferAdapter(
            mode = OfferCardMode.COMPLETED,
            onItemClick = { item ->
                findNavController().navigate(
                    R.id.action_history_to_chat,
                    bundleOf("dealId" to item.deal.dealId)
                )
            },
            onChevronClick = { item ->
                CampaignInfoBottomSheet.newInstance(item.deal.dealId)
                    .show(parentFragmentManager, "campaign_info")
            }
        )
        binding.rvDeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDeals.adapter = adapter

        historyViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.pbLoading.isVisible = loading
        }

        historyViewModel.completedDeals.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.rvDeals.isVisible = items.isNotEmpty()
            binding.layoutEmpty.isVisible = items.isEmpty() && historyViewModel.isLoading.value == false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            reviewViewModel.reviewsByDealId.collect { map ->
                adapter.setReviewsData(true, map)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
