package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep3Binding

/**
 * BrandRegStep3Fragment — Company Verification + Social Links
 *
 * Sends: legalName, repName, companyEmail, linkedinUrl, twitterHandle
 * File upload (layoutFileUpload) is a placeholder — not wired to Storage yet.
 * Calls: vm.submitStep3()
 * Navigates: on stepSaved(3) → brand_step4
 */
class BrandRegStep3Fragment : Fragment() {

    private var _binding: FragmentBrandRegStep3Binding? = null
    private val binding get() = _binding!!

    private val vm: BrandRegistrationViewModel by activityViewModels { BrandRegistrationViewModel.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandRegStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillFields()
        setupObservers()
        setupClickListeners()
    }

    private fun prefillFields() {
        binding.etLegalName.setText(vm.legalName)
        binding.etRepName.setText(vm.repName)
        binding.etCompanyEmail.setText(vm.companyEmail)
        binding.etLinkedin.setText(vm.linkedinUrl)
        binding.etTwitter.setText(vm.twitterHandle)
    }

    private fun setupObservers() {
        // No loading state on Step 3 — navigation is instant (no network call until Step 5)
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { findNavController().navigateUp() }

        binding.layoutFileUpload.setOnClickListener {
            // TODO: implement verification doc upload with Firebase Storage
        }

        binding.layoutBottomNav.btnNavBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            // Save to ViewModel draft — no Firebase call here
            vm.legalName = binding.etLegalName.text.toString().trim()
            vm.repName = binding.etRepName.text.toString().trim()
            vm.companyEmail = binding.etCompanyEmail.text.toString().trim()
            vm.linkedinUrl = binding.etLinkedin.text.toString().trim()
            vm.twitterHandle = binding.etTwitter.text.toString().trim()

            findNavController().navigate(R.id.action_brand_step3_to_step4)
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        binding.tilLegalName.error = null
        binding.tilRepName.error = null
        binding.tilCompanyEmail.error = null

        if (binding.etLegalName.text.isNullOrBlank()) {
            binding.tilLegalName.error = "Required"; valid = false
        }
        if (binding.etRepName.text.isNullOrBlank()) {
            binding.tilRepName.error = "Required"; valid = false
        }

        val email = binding.etCompanyEmail.text?.toString()?.trim() ?: ""
        if (email.isBlank()) {
            binding.tilCompanyEmail.error = "Required"; valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilCompanyEmail.error = "Enter a valid email"; valid = false
        }

        // LinkedIn and Twitter are optional — no validation required
        return valid
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
