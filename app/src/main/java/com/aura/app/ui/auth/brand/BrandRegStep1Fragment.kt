package com.aura.app.ui.auth.brand

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aura.app.R
import com.aura.app.databinding.FragmentBrandRegStep1Binding

/**
 * BrandRegStep1Fragment
 *
 * Collects: brandName, email, phone, password, confirmPassword,
 *           securityQuestion, securityAnswer.
 *
 * On "Next":
 *   - Validates locally
 *   - Calls viewModel.submitStep1() which triggers Firebase Auth + Firestore seed
 *   - Observes stepSaved(1) to navigate to Step 2
 */
class BrandRegStep1Fragment : Fragment() {

    private var _binding: FragmentBrandRegStep1Binding? = null
    private val binding get() = _binding!!

    private val vm: BrandRegistrationViewModel by activityViewModels { BrandRegistrationViewModel.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandRegStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSecurityQuestionDropdown()
        prefillFields()
        setupObservers()
        setupClickListeners()
    }

    private fun prefillFields() {
        binding.etBrandName.setText(vm.brandName)
        binding.etEmail.setText(vm.email)
        binding.etPhone.setText(vm.phone)
        binding.etSecurityAnswer.setText(vm.securityAnswer)
        // password fields are never pre-filled for security
    }

    private fun setupSecurityQuestionDropdown() {
        val questions = listOf(
            getString(R.string.sq_placeholder),
            getString(R.string.sq_pet_name),
            getString(R.string.sq_mothers_maiden_name),
            getString(R.string.sq_birth_city),
            getString(R.string.sq_high_school_mascot)
        )
        binding.acvSecurityQuestion.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, questions)
        )
        binding.acvSecurityQuestion.setText(
            vm.securityQuestion.ifBlank { questions[0] }, false
        )
    }

    private fun setupObservers() {
        // No loading state on Step 1 — navigation is instant (no network call until Step 5)
    }

    private fun setupClickListeners() {
        binding.layoutBottomNav.btnNavBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutBottomNav.btnNavNext.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            // Save to ViewModel draft — no Firebase call here
            vm.brandName = binding.etBrandName.text.toString().trim()
            vm.email = binding.etEmail.text.toString().trim()
            vm.password = binding.etPassword.text.toString()
            vm.phone = binding.etPhone.text.toString().trim()
            vm.securityQuestion = binding.acvSecurityQuestion.text.toString()
            vm.securityAnswer = binding.etSecurityAnswer.text.toString().trim()

            findNavController().navigate(R.id.action_brand_step1_to_step2)
        }
    }

    private fun validateForm(): Boolean {
        var valid = true

        // Clear previous errors
        listOf(
            binding.tilBrandName, binding.tilEmail, binding.tilPhone,
            binding.tilPassword, binding.tilConfirmPassword, binding.tilSecurityAnswer
        ).forEach { it.error = null }

        if (binding.etBrandName.text.isNullOrBlank()) {
            binding.tilBrandName.error = "Required"; valid = false
        }

        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        if (email.isBlank()) {
            binding.tilEmail.error = "Required"; valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"; valid = false
        }

        if (binding.etPhone.text.isNullOrBlank()) {
            binding.tilPhone.error = "Required"; valid = false
        }

        val pw = binding.etPassword.text?.toString() ?: ""
        when {
            pw.length < 8 -> { binding.tilPassword.error = "Min 8 characters"; valid = false }
            !pw.any { it.isUpperCase() } -> { binding.tilPassword.error = "Need 1 uppercase letter"; valid = false }
            !pw.any { it.isDigit() } -> { binding.tilPassword.error = "Need 1 number"; valid = false }
        }

        val confirm = binding.etConfirmPassword.text?.toString() ?: ""
        if (confirm != pw) {
            binding.tilConfirmPassword.error = "Passwords do not match"; valid = false
        }

        val sq = binding.acvSecurityQuestion.text.toString()
        if (sq.isBlank() || sq == getString(R.string.sq_placeholder)) {
            binding.tilSecurityQuestion.error = "Please select a security question"; valid = false
        } else {
            binding.tilSecurityQuestion.error = null
        }

        if (binding.etSecurityAnswer.text.isNullOrBlank()) {
            binding.tilSecurityAnswer.error = "Required"; valid = false
        }

        return valid
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
