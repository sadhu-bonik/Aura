package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep2Binding

/**
 * BrandRegStep2Fragment — Brand Identity: Motto + Bio
 *
 * Sends: motto, bio
 * Calls: vm.submitStep2() → saves to brandProfiles/{uid}
 * Navigates: on stepSaved(2) → brand_step3
 */
class BrandRegStep2Fragment : Fragment() {

    private var _binding: FragmentBrandRegStep2Binding? = null
    private val binding get() = _binding!!

    private val vm: BrandRegistrationViewModel by activityViewModels { BrandRegistrationViewModel.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandRegStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillFields()
        setupObservers()
        setupClickListeners()
    }

    private fun prefillFields() {
        binding.etMotto.setText(vm.motto)
        binding.etBio.setText(vm.bio)
    }

    private fun setupObservers() {
        // No loading state on Step 2 — navigation is instant (no network call until Step 5)
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.layoutLogoUpload.setOnClickListener {
            // TODO: implement logo upload with Firebase Storage
        }

        binding.layoutBottomNav.btnNavBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            // Save to ViewModel draft — no Firebase call here
            vm.motto = binding.etMotto.text.toString().trim()
            vm.bio = binding.etBio.text.toString().trim()

            findNavController().navigate(R.id.action_brand_step2_to_step3)
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        binding.tilMotto.error = null
        binding.tilBio.error = null

        if (binding.etMotto.text.isNullOrBlank()) {
            binding.tilMotto.error = "Required"; valid = false
        }
        if (binding.etBio.text.isNullOrBlank()) {
            binding.tilBio.error = "Required"; valid = false
        }
        return valid
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
