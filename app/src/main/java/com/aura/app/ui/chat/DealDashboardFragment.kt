package com.aura.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aura.app.R
import com.aura.app.databinding.FragmentDealDashboardBinding
import com.aura.app.utils.Constants
import com.aura.app.utils.StubSession
import com.aura.app.utils.rootNavController
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DealDashboardFragment : Fragment() {

    private var _binding: FragmentDealDashboardBinding? = null
    private val binding get() = _binding!!

    val viewModel: DealDashboardViewModel by viewModels()
    private val reviewViewModel: ReviewViewModel by activityViewModels()

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

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

        binding.btnPillActive.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.btnPillPending.setOnClickListener { binding.viewPager.currentItem = 1 }

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = renderPillSelection(position)
        }
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback!!)
        renderPillSelection(0)

        binding.btnHistory.setOnClickListener {
            rootNavController().navigate(R.id.action_homeContainer_to_history)
        }

        binding.btnFilter.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.dashboard_filter_coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }

        viewModel.activeDeals.observe(viewLifecycleOwner) { items ->
            binding.tvStatActive.text = items.size.toString().padStart(2, '0')
        }
        viewModel.newDeals.observe(viewLifecycleOwner) { items ->
            binding.tvStatPending.text = items.size.toString().padStart(2, '0')
        }
        viewModel.completedDeals.observe(viewLifecycleOwner) { items ->
            binding.tvStatCompleted.text = items.size.toString().padStart(2, '0')
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

    override fun onResume() {
        super.onResume()
        applyRoleLabels()
    }

    private fun applyRoleLabels() {
        val isBrand = StubSession.role() == Constants.ROLE_BRAND
        binding.btnPillPending.text = getString(
            if (isBrand) R.string.tab_sent_short else R.string.tab_pending_short
        )
        binding.tvStatPendingLabel.text = getString(
            if (isBrand) R.string.dashboard_stat_sent else R.string.dashboard_stat_pending
        )
    }

    private fun renderPillSelection(position: Int) {
        val ctx = requireContext()
        val onPrimary = ContextCompat.getColor(ctx, R.color.colorOnPrimary)
        val onVariant = ContextCompat.getColor(ctx, R.color.colorOnSurfaceVariant)
        val transparent = android.R.color.transparent
        val extrabold = ResourcesCompat.getFont(ctx, R.font.manrope_extrabold)
        val bold = ResourcesCompat.getFont(ctx, R.font.manrope_bold)

        if (position == 0) {
            binding.btnPillActive.setBackgroundResource(R.drawable.bg_dashboard_pill_selected)
            binding.btnPillActive.setTextColor(onPrimary)
            binding.btnPillActive.typeface = extrabold

            binding.btnPillPending.setBackgroundResource(transparent)
            binding.btnPillPending.setTextColor(onVariant)
            binding.btnPillPending.typeface = bold
        } else {
            binding.btnPillPending.setBackgroundResource(R.drawable.bg_dashboard_pill_selected)
            binding.btnPillPending.setTextColor(onPrimary)
            binding.btnPillPending.typeface = extrabold

            binding.btnPillActive.setBackgroundResource(transparent)
            binding.btnPillActive.setTextColor(onVariant)
            binding.btnPillActive.typeface = bold
        }
    }

    override fun onDestroyView() {
        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        super.onDestroyView()
        _binding = null
    }
}
