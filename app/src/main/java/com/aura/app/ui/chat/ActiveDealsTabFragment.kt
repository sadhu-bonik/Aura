package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.R
import com.aura.app.databinding.FragmentDealTabBinding
import com.aura.app.utils.StubSession

class ActiveDealsTabFragment : Fragment() {

    private var _binding: FragmentDealTabBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel: DealDashboardViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: ActiveDealAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDealTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmpty.setText(R.string.empty_active_deals_title)

        adapter = ActiveDealAdapter { item ->
            findNavController().navigate(
                R.id.action_dashboard_to_chat,
                android.os.Bundle().apply { putString("dealId", item.deal.dealId) }
            )
        }
        binding.rvDeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDeals.adapter = adapter

        dashboardViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.pbLoading.isVisible = loading
        }

        dashboardViewModel.activeDeals.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.rvDeals.isVisible = items.isNotEmpty()
            binding.layoutEmpty.isVisible = items.isEmpty() && dashboardViewModel.isLoading.value == false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
