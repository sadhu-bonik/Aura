package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep5Binding

/**
 * BrandRegStep5Fragment — Final step: campaign setup.
 *
 * This is the ONLY step that talks to Firebase.
 * On "Finish":
 *   - Validates campaign fields
 *   - Calls vm.completeRegistration() which fires:
 *       1. Firebase Auth (create user)
 *       2. Firestore users/{uid}         (BrandAccount)
 *       3. Firestore brandProfiles/{uid} (full BrandProfile with all 5 steps)
 *   - On success → navigates to HomeContainerFragment (feed), clearing back stack
 */
class BrandRegStep5Fragment : Fragment() {

    private var _binding: FragmentBrandRegStep5Binding? = null
    private val binding get() = _binding!!

    private val vm: BrandRegistrationViewModel by activityViewModels { BrandRegistrationViewModel.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandRegStep5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillFields()
        setupCharCounter()
        setupObservers()
        setupClickListeners()
    }

    private fun prefillFields() {
        binding.etCampaignName.setText(vm.campaignName)
        binding.etCampaignBrief.setText(vm.campaignBrief)
    }

    private fun setupCharCounter() {
        binding.etCampaignBrief.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tvCharCount.text = "${s?.length ?: 0} / 2000"
            }
        })
        binding.tvCharCount.text = "${vm.campaignBrief.length} / 2000"
    }

    private fun setupObservers() {
        // Disable Finish button while the single Firebase call is in flight
        vm.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.layoutBottomNav.btnNavNext.isEnabled = !loading
            binding.layoutBottomNav.btnNavNext.alpha = if (loading) 0.5f else 1.0f
        }

        // Show any error from the Firebase call
        vm.error.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                vm.clearError()
            }
        }

        // Navigate to feed when registration completes successfully
        vm.registrationComplete.observe(viewLifecycleOwner) { complete ->
            if (complete) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.welcomeFragment, inclusive = true)
                    .build()
                findNavController().navigate(R.id.action_brand_finish_to_home, null, navOptions)
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.btnAddCampaign.setOnClickListener {
            Toast.makeText(requireContext(), "Multiple campaigns coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.layoutBottomNav.btnNavSaveExit.setOnClickListener {
            Toast.makeText(requireContext(), "Progress saved. You can resume later.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack(R.id.welcomeFragment, false)
        }

        binding.layoutBottomNav.btnNavBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // ── THE ONLY FIREBASE CALL IN THE ENTIRE BRAND FLOW ──────────────────
        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            vm.campaignName = binding.etCampaignName.text.toString().trim()
            vm.campaignBrief = binding.etCampaignBrief.text.toString().trim()

            vm.completeRegistration()
        }
    }

    private fun validateForm(): Boolean {
        binding.tilCampaignName.error = null
        binding.tilCampaignBrief.error = null
        var valid = true

        if (binding.etCampaignName.text.isNullOrBlank()) {
            binding.tilCampaignName.error = "Required"; valid = false
        }
        if (binding.etCampaignBrief.text.isNullOrBlank()) {
            binding.tilCampaignBrief.error = "Required"; valid = false
        }
        return valid
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
