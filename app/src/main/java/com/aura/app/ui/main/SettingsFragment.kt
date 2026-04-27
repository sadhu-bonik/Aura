package com.aura.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentSettingsBinding
import com.aura.app.data.repository.UserRepository
import com.aura.app.ui.auth.AuthViewModel
import com.aura.app.utils.SessionManager
import com.bumptech.glide.Glide
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Settings screen — displays user profile overview and menu items for
 * Account, Edit Profile, Privacy, Log Out, and Delete Account.
 *
 * Currently only Edit Profile and Log Out are functional.
 * Account, Privacy, and Delete Account are shown but disabled.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModel.Factory()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()
        setupClickListeners()
    }

    /**
     * Loads the current user's profile to populate the avatar, name, and subtitle.
     */
    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val sessionManager = SessionManager(requireContext())
            val userId = sessionManager.getUserId() ?: return@launch
            val userRepository = UserRepository()
            val user = userRepository.getUserProfile(userId) ?: return@launch

            binding.tvSettingsName.text = user.displayName.ifBlank { user.email }
            binding.tvSettingsSubtitle.text = user.role.replaceFirstChar { it.uppercase() }

            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(this@SettingsFragment)
                    .load(user.profileImageUrl)
                    .centerCrop()
                    .into(binding.ivSettingsAvatar)
            }
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnSettingsBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // === Enabled options ===

        // Edit Profile — navigate to the existing EditProfileFragment
        binding.rowEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_editProfile)
        }

        // Logout — reuses AuthViewModel.logout(), then navigates to Welcome.
        // Wipe the activity ViewModelStore so any user-scoped state (notifications,
        // reviews, deal dashboard) doesn't leak from the previous account into the next.
        binding.rowLogout.setOnClickListener {
            authViewModel.logout(requireContext())
            requireActivity().viewModelStore.clear()
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.homeContainerFragment, true)
                .build()
            requireActivity().findNavController(R.id.nav_host_fragment)
                .navigate(R.id.welcomeFragment, null, navOptions)
        }

        // Account — opens the Account Settings screen
        binding.rowAccount.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_accountSettings)
        }

        // === Disabled options — show "Coming soon" Toast ===

        binding.rowPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), R.string.settings_coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.rowDeleteAccount.setOnClickListener {
            Toast.makeText(requireContext(), R.string.settings_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
