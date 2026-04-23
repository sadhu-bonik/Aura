package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import com.aura.app.R
import com.aura.app.databinding.FragmentDealDashboardBinding
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.aura.app.utils.rootNavController
import com.google.android.material.tabs.TabLayoutMediator

class DealDashboardFragment : Fragment() {

    private var _binding: FragmentDealDashboardBinding? = null
    private val binding get() = _binding!!

    val viewModel: DealDashboardViewModel by viewModels()
    private val reviewViewModel: ReviewViewModel by activityViewModels()

    private var mediator: TabLayoutMediator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDealDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> ActiveDealsTabFragment()
                else -> NewDealsTabFragment()
            }
        }

        val tabLabels = listOf(
            getString(R.string.tab_active_deals),
            if (StubSession.role() == Constants.ROLE_BRAND) {
                getString(R.string.tab_sent_deals)
            } else {
                getString(R.string.tab_new_deals)
            }
        )

        mediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val tabView = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_tab_with_dot, null)
            tabView.findViewById<TextView>(R.id.tv_tab_label).text = tabLabels[position]
            tab.customView = tabView
        }
        mediator!!.attach()

        binding.tbDashboard.inflateMenu(R.menu.menu_deal_dashboard)
        updateSwitchMenuTitle()

        binding.tbDashboard.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_history -> {
                    rootNavController().navigate(R.id.action_homeContainer_to_history)
                    true
                }
                R.id.action_switch_user -> {
                    StubSession.switchToNext()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_switched_to_user, StubSession.displayName()),
                        Toast.LENGTH_SHORT
                    ).show()
                    updateSwitchMenuTitle()
                    viewModel.load()
                    true
                }
                else -> false
            }
        }

        viewModel.hasNewPendingForCreator.observe(viewLifecycleOwner) { hasPending ->
            val newDealsTab = binding.tabLayout.getTabAt(1)
            newDealsTab?.customView?.findViewById<View>(R.id.view_tab_dot)?.isVisible = hasPending
        }

        viewLifecycleOwner.lifecycleScope.launch {
            reviewViewModel.pendingReviewDeal.filterNotNull().collect { deal ->
                reviewViewModel.markReviewPromptShown(deal.dealId)
                val otherPartyId = if (StubSession.role() == Constants.ROLE_CREATOR) deal.brandId else deal.creatorId
                val otherParty = com.aura.app.utils.StubData.users[otherPartyId] ?: return@collect
                ReviewFlow.newInstance(deal.dealId, otherPartyId, otherParty.displayName, otherParty.profileImageUrl)
                    .show(childFragmentManager, "review_flow")
            }
        }
    }

    private fun updateSwitchMenuTitle() {
        val item = binding.tbDashboard.menu.findItem(R.id.action_switch_user) ?: return
        item.title = getString(R.string.menu_switch_to_user, StubSession.nextUserDisplayName())
    }

    override fun onDestroyView() {
        mediator?.detach()
        mediator = null
        super.onDestroyView()
        _binding = null
    }
}
