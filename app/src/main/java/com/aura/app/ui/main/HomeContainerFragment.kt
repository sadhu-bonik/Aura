package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentHomeContainerBinding
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

@OptIn(ExperimentalBadgeUtils::class)
class HomeContainerFragment : Fragment() {

    private var _binding: FragmentHomeContainerBinding? = null
    private val binding get() = _binding!!

    private val notifViewModel: NotificationViewModel by activityViewModels()
    private var dealsBadge: BadgeDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager.findFragmentById(R.id.nav_host_fragment_home) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)
        applyBottomNavInsets()

        // Set up notification badge on the Deals tab
        dealsBadge = binding.bottomNav.getOrCreateBadge(R.id.navigation_deals).apply {
            isVisible = false
            backgroundColor = resources.getColor(R.color.colorError, null)
        }
        notifViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            dealsBadge?.apply {
                if (count > 0) {
                    number = count
                    isVisible = true
                } else {
                    isVisible = false
                }
            }
        }

        // Incomplete brand accounts arrive here after login. Open EditProfile so they can
        // complete the required fields (motto already set from registration; need ≥1 tag).
        // Only fires once per arrival — the arg is cleared so navigating back to the home
        // container (e.g. from chat) doesn't kick the user back into edit profile.
        val setupRequired = arguments?.getBoolean("brandSetupRequired", false) ?: false
        if (savedInstanceState == null && setupRequired) {
            arguments?.putBoolean("brandSetupRequired", false)
            navController.navigate(R.id.navigation_edit_profile)
        }
    }

    override fun onDestroyView() {
        binding.bottomNav.removeBadge(R.id.navigation_deals)
        super.onDestroyView()
        _binding = null
    }

    private fun applyBottomNavInsets() {
        val baseBottomPadding = binding.bottomNav.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = baseBottomPadding + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.bottomNav)
    }
}
