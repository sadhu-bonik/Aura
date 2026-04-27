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
import com.aura.app.ui.main.NotificationViewModel
import com.aura.app.utils.Constants
import com.aura.app.utils.SessionManager
import com.aura.app.utils.rootNavController
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalBadgeUtils::class)
class DealDashboardFragment : Fragment() {

    private var _binding: FragmentDealDashboardBinding? = null
    private val binding get() = _binding!!

    val viewModel: DealDashboardViewModel by viewModels {
        DealDashboardViewModel.Factory(requireContext().applicationContext)
    }
    private val reviewViewModel: ReviewViewModel by activityViewModels()
    private val notifViewModel: NotificationViewModel by activityViewModels()

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var notifBadge: BadgeDrawable? = null

    private val authRepository = com.aura.app.data.repository.AuthRepository()
    private val userRepository = com.aura.app.data.repository.UserRepository()
    private val sessionManager by lazy { SessionManager(requireContext().applicationContext) }

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

        binding.btnNotifications.setOnClickListener {
            NotificationBottomSheet.newInstance()
                .show(childFragmentManager, "notifications")
        }

        // Unread badge attached to the dashboard's notification icon. The bottom-nav badge
        // (HomeContainerFragment) is independent — both observe the same NotificationViewModel.
        notifBadge = BadgeDrawable.create(requireContext()).apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.colorError)
            maxCharacterCount = 3
            isVisible = false
        }
        binding.btnNotifications.post {
            notifBadge?.let { BadgeUtils.attachBadgeDrawable(it, binding.btnNotifications) }
        }
        notifViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            notifBadge?.apply {
                if (count > 0) {
                    number = count
                    isVisible = true
                } else {
                    isVisible = false
                }
            }
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
        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            reviewViewModel.pendingReviewDeal.filterNotNull().collect { deal ->
                reviewViewModel.markReviewPromptShown(deal.dealId)
                val currentUserId = resolveUserId() ?: return@collect
                val otherPartyId = if (currentUserId == deal.creatorId) deal.brandId else deal.creatorId
                val otherParty = userRepository.getUserProfile(otherPartyId) ?: return@collect
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
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = resolveUserId() ?: return@launch
            val user = userRepository.getUserProfile(userId)
            val isBrand = user?.role == Constants.ROLE_BRAND
            
            binding.btnPillPending.text = getString(
                if (isBrand) R.string.tab_sent_short else R.string.tab_pending_short
            )
            binding.tvStatPendingLabel.text = getString(
                if (isBrand) R.string.dashboard_stat_sent else R.string.dashboard_stat_pending
            )
        }
    }

    private fun resolveUserId(): String? =
        authRepository.currentUser?.uid ?: sessionManager.getUserId()

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
        notifBadge?.let { BadgeUtils.detachBadgeDrawable(it, binding.btnNotifications) }
        notifBadge = null
        super.onDestroyView()
        _binding = null
    }
}
