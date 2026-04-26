package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.app.R
import com.aura.app.databinding.FragmentDealTabBinding
import com.aura.app.utils.rootNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NewDealsTabFragment : Fragment() {

    private var _binding: FragmentDealTabBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel: DealDashboardViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: DealOfferAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDealTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (com.aura.app.utils.StubSession.role() == com.aura.app.utils.Constants.ROLE_BRAND) {
            binding.tvEmpty.setText(R.string.empty_sent_deals)
        } else {
            binding.tvEmpty.setText(R.string.empty_new_deals)
        }

        adapter = DealOfferAdapter(
            mode = OfferCardMode.NEW_DEALS,
            onItemClick = { item ->
                CampaignInfoBottomSheet.newInstance(item.deal.dealId)
                    .show(parentFragmentManager, "campaign_info")
            },
            onAccept = { dealId -> dashboardViewModel.acceptDeal(dealId) },
            onReject = { dealId -> confirmReject(dealId) },
        )
        binding.rvDeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDeals.adapter = adapter

        dashboardViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.pbLoading.isVisible = loading
        }

        dashboardViewModel.newDeals.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.rvDeals.isVisible = items.isNotEmpty()
            binding.layoutEmpty.isVisible = items.isEmpty() && dashboardViewModel.isLoading.value == false
        }

        dashboardViewModel.acceptEvent.observe(viewLifecycleOwner) { dealId ->
            if (dealId != null) {
                dashboardViewModel.consumeAcceptEvent()
                rootNavController().navigate(
                    R.id.action_homeContainer_to_chat,
                    bundleOf("dealId" to dealId)
                )
            }
        }
    }

    private fun confirmReject(dealId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_decline, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setBackground(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            .show()

        dialogView.findViewById<android.widget.TextView>(R.id.tv_btn_decline).setOnClickListener {
            dashboardViewModel.rejectDeal(dealId)
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.TextView>(R.id.tv_btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
